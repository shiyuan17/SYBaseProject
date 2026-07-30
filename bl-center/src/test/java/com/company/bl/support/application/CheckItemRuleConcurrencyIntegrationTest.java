package com.company.bl.support.application;

import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.infrastructure.CheckItemRuleRepository;
import com.company.bl.support.infrastructure.SupportJdbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@ActiveProfiles("test")
@Import({CheckItemRuleRepository.class, CheckItemRuleService.class, SupportJdbcRepository.class})
class CheckItemRuleConcurrencyIntegrationTest {

    @Autowired
    private CheckItemRuleService service;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldKeepConcurrentApplicationTypeCountersUniqueAndIndependent() throws Exception {
        List<Callable<String>> requests = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            requests.add(() -> service.generatePathologyNo("ROUTINE"));
        }
        for (int index = 0; index < 5; index++) {
            requests.add(() -> service.generatePathologyNo("CONSULTATION"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> futures = executor.invokeAll(requests);
            List<String> generated = new ArrayList<>();
            for (Future<String> future : futures) {
                generated.add(future.get());
            }

            assertThat(new HashSet<>(generated)).hasSize(13);
            assertThat(generated).filteredOn(number -> number.startsWith("BL")).hasSize(8);
            assertThat(generated).filteredOn(number -> number.startsWith("HZ")).hasSize(5);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldNotReservePreviewOrConsumeNumberWhenOuterTransactionRollsBack() {
        String previewBefore = service.preview("RESEARCH").suggestedPathologyNo();
        assertThat(service.preview("RESEARCH").suggestedPathologyNo()).isEqualTo(previewBefore);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        String rolledBackNumber = transaction.execute(status -> {
            String generated = service.generatePathologyNo("RESEARCH");
            status.setRollbackOnly();
            return generated;
        });

        assertThat(rolledBackNumber).isEqualTo(previewBefore);
        assertThat(service.preview("RESEARCH").suggestedPathologyNo()).isEqualTo(previewBefore);
    }

    @Test
    void shouldValidateHistoricalFloorAfterConcurrentGenerationCommits() throws Exception {
        CheckItemRuleService.CheckItemRuleView rule = service.listRules().stream()
            .filter(item -> "RESEARCH".equals(item.applicationType()))
            .findFirst()
            .orElseThrow();
        String applicationId = "APP-CHECK-ITEM-RACE";
        String caseId = "CASE-CHECK-ITEM-RACE";
        requiresNew().executeWithoutResult(status -> {
            jdbcTemplate.update("""
                insert into applications (id, application_no, application_type, status)
                values (?, ?, 'RESEARCH', 'RECEIVED')
                """, applicationId, "APP-NO-CHECK-ITEM-RACE");
            jdbcTemplate.update("""
                insert into pathology_cases (id, application_id, pathology_no, case_status)
                values (?, ?, ?, 'RECEIVED')
                """, caseId, applicationId, "ORIGINAL-CHECK-ITEM-RACE");
        });
        CountDownLatch generationReady = new CountDownLatch(1);
        CountDownLatch releaseGeneration = new CountDownLatch(1);
        CountDownLatch updateStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> generation = executor.submit(() -> requiresNew().execute(status -> {
                String pathologyNo = service.generatePathologyNo("RESEARCH");
                jdbcTemplate.update("update pathology_cases set pathology_no = ? where id = ?", pathologyNo, caseId);
                generationReady.countDown();
                await(releaseGeneration);
                return pathologyNo;
            }));
            assertThat(generationReady.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Throwable> lowering = executor.submit(() -> {
                updateStarted.countDown();
                try {
                    service.updateRule(rule.id(), new CheckItemRuleService.SaveCheckItemRuleCommand(
                        "RESEARCH", "YEAR", true, "KY{YY}{0:D5}", 0));
                    return null;
                } catch (Throwable exception) {
                    return exception;
                }
            });
            assertThat(updateStarted.await(5, TimeUnit.SECONDS)).isTrue();
            releaseGeneration.countDown();

            assertThat(generation.get(5, TimeUnit.SECONDS)).startsWith("KY");
            assertThat(lowering.get(5, TimeUnit.SECONDS))
                .isInstanceOf(BlBusinessException.class)
                .hasMessageContaining("historical maximum");
        } finally {
            releaseGeneration.countDown();
            executor.shutdownNow();
            requiresNew().executeWithoutResult(status -> {
                jdbcTemplate.update("delete from pathology_cases where id = ?", caseId);
                jdbcTemplate.update("delete from applications where id = ?", applicationId);
                service.updateRule(rule.id(), new CheckItemRuleService.SaveCheckItemRuleCommand(
                    "RESEARCH", "YEAR", true, "KY{YY}{0:D5}", rule.currentMaxSequence()));
            });
        }
    }

    @Test
    void shouldRejectConcurrentDeletionAfterManualRuleUseWithoutMatchingCaseHistory() throws Exception {
        CheckItemRuleService.CheckItemRuleView rule = service.listRules().stream()
            .filter(item -> !"RESEARCH".equals(item.applicationType()))
            .filter(item -> pathologyCaseCount(item.applicationType()) == 0L)
            .filter(item -> item.currentMaxSequence() == 0L)
            .findFirst()
            .orElseThrow();
        requiresNew().executeWithoutResult(status -> {
            jdbcTemplate.update("delete from numbering_counters where rule_code = ?", ruleCode(rule.id()));
            service.updateRule(rule.id(), new CheckItemRuleService.SaveCheckItemRuleCommand(
                rule.applicationType(), "NONE", false, "", 0));
        });
        assertThat(pathologyCaseCount(rule.applicationType())).isZero();

        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch releaseAcceptance = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> acceptance = executor.submit(() -> requiresNew().executeWithoutResult(status -> {
                service.validateAndAcceptCandidate("CASE-MANUAL", rule.applicationType(), "MANUAL-CHECK-001");
                accepted.countDown();
                await(releaseAcceptance);
            }));
            assertThat(accepted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Throwable> deletion = executor.submit(() -> {
                try {
                    service.deleteRule(rule.id());
                    return null;
                } catch (Throwable exception) {
                    return exception;
                }
            });
            releaseAcceptance.countDown();

            acceptance.get(5, TimeUnit.SECONDS);
            assertThat(deletion.get(5, TimeUnit.SECONDS))
                .isInstanceOf(BlBusinessException.class)
                .hasMessageContaining("cannot be deleted");
            assertThat(jdbcTemplate.queryForObject("""
                select count(*) from numbering_counters
                where rule_code = ? and period_key = '__USED__' and current_value = 1
                """, Long.class, ruleCode(rule.id()))).isEqualTo(1L);
        } finally {
            releaseAcceptance.countDown();
            executor.shutdownNow();
            requiresNew().executeWithoutResult(status -> {
                jdbcTemplate.update("delete from numbering_counters where rule_code = ?", ruleCode(rule.id()));
                service.updateRule(rule.id(), new CheckItemRuleService.SaveCheckItemRuleCommand(
                    rule.applicationType(), rule.numberPeriod(), rule.autoIncrement(),
                    rule.prefixRule(), rule.currentMaxSequence()));
            });
        }
    }

    private TransactionTemplate requiresNew() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    private long pathologyCaseCount(String applicationType) {
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from pathology_cases pc
            join applications a on a.id = pc.application_id
            where upper(a.application_type) = ?
            """, Long.class, applicationType);
        return count == null ? 0L : count;
    }

    private String ruleCode(String ruleId) {
        return jdbcTemplate.queryForObject(
            "select rule_code from numbering_rules where id = ?", String.class, ruleId);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent test step");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent test step", exception);
        }
    }
}
