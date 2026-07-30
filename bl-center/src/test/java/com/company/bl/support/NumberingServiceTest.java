package com.company.bl.support;

import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import com.company.bl.support.application.CheckItemRuleService;
import com.company.bl.support.infrastructure.CheckItemRuleRepository;
import com.company.bl.support.infrastructure.SupportJdbcRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@ActiveProfiles("test")
@Import({
    SupportJdbcRepository.class,
    CheckItemRuleRepository.class,
    CheckItemRuleService.class,
    OperationAuditService.class,
    NumberingService.class
})
class NumberingServiceTest {

    @Autowired
    private NumberingService numberingService;

    @Autowired
    private SupportJdbcRepository supportJdbcRepository;

    @Test
    void shouldGenerateUniqueNumbersUnderConcurrency() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(20);
        Set<String> generated = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < 20; i++) {
            executorService.submit(() -> {
                try {
                    generated.add(numberingService.generatePathologyNo());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdownNow();
        assertEquals(20, generated.size());
    }

    @Test
    void shouldWriteSuccessAuditLogWhenUpdatingRule() {
        numberingService.updateRule("NR_APPLICATION", new NumberingService.UpdateNumberingRuleCommand(
            "APZ", "yyyyMMdd", 4, "DAILY", "GLOBAL", true, "success audit"));

        assertTrue(supportJdbcRepository.findOperationLogs("SUPPORT").stream().anyMatch(log ->
            "update_numbering_rule".equals(log.get("operation_name"))
                && "SUCCESS".equals(log.get("operation_result"))
                && "NR_APPLICATION".equals(log.get("business_id"))));
    }

    @Test
    void shouldWriteFailureAuditLogWhenUpdatingRuleFails() {
        assertThrows(RuntimeException.class, () -> numberingService.updateRule("NR_MISSING",
            new NumberingService.UpdateNumberingRuleCommand(
                "APZ", "yyyyMMdd", 4, "DAILY", "GLOBAL", true, "failure audit")));

        assertTrue(supportJdbcRepository.findOperationLogs("SUPPORT").stream().anyMatch(log ->
            "update_numbering_rule".equals(log.get("operation_name"))
                && "FAILED".equals(log.get("operation_result"))
                && "NR_MISSING".equals(log.get("business_id"))));
    }

    @Test
    void shouldRejectUpdatingCheckItemRuleThroughLegacyEndpoint() {
        assertThatThrownBy(() -> numberingService.updateRule(
            "NR_CHECK_ITEM_RESEARCH",
            new NumberingService.UpdateNumberingRuleCommand(
                "BYPASS", "yyyyMMdd", 4, "DAILY", "GLOBAL", true, "legacy bypass")))
            .hasMessageContaining("check-item rule API");
    }

    @Test
    void shouldGenerateMasterDataDepartmentCodeFromSeededRule() {
        String code = numberingService.generateDepartmentCode();

        assertTrue(code.startsWith("DEPT-"));
    }
}
