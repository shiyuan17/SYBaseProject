package com.company.bl.support.application;

import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.infrastructure.CheckItemRuleRepository;
import com.company.bl.support.infrastructure.CheckItemRuleRepository.CheckItemRuleRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckItemRuleServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-30T08:00:00Z"), ZoneOffset.UTC);

    private CheckItemRuleRepository repository;
    private CheckItemRuleService service;

    @BeforeEach
    void setUp() {
        repository = mock(CheckItemRuleRepository.class);
        service = new CheckItemRuleService(repository, CLOCK);
    }

    @Test
    void shouldPreviewNextAvailableNumberWithoutAdvancingCounter() {
        CheckItemRuleRow rule = automaticRule("ROUTINE", "BL{YYYY}{MM}{DD}{0:D4}", "DAILY", 4);
        when(repository.findByApplicationType("ROUTINE")).thenReturn(rule);
        when(repository.currentCounterValue(rule.ruleCode(), "20260730")).thenReturn(41L);
        when(repository.pathologyNumberExists("BL202607300042", null)).thenReturn(false);

        CheckItemRuleService.PathologyNumberPreview preview = service.preview("routine");

        assertThat(preview.configured()).isTrue();
        assertThat(preview.autoIncrement()).isTrue();
        assertThat(preview.manualRequired()).isFalse();
        assertThat(preview.suggestedPathologyNo()).isEqualTo("BL202607300042");
        verify(repository, never()).nextCounterValue(rule.ruleCode(), "20260730");
    }

    @Test
    void shouldRequireManualNumberWhenRuleIsMissingOrNotAutomatic() {
        when(repository.findByApplicationType("RESEARCH")).thenReturn(null);
        when(repository.findByApplicationType("FROZEN")).thenReturn(
            new CheckItemRuleRow("NR_FROZEN", "RULE_FROZEN", CheckItemRuleRepository.BIZ_PREFIX + "FROZEN",
                "FROZEN", "", false, "NONE", 1, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK)));

        assertThat(service.preview("RESEARCH"))
            .extracting("configured", "autoIncrement", "manualRequired", "suggestedPathologyNo")
            .containsExactly(false, false, true, null);
        assertThat(service.preview("FROZEN"))
            .extracting("configured", "autoIncrement", "manualRequired", "suggestedPathologyNo")
            .containsExactly(true, false, true, null);
    }

    @Test
    void shouldAcceptMatchingOverrideAndAdvanceCounter() {
        CheckItemRuleRow rule = automaticRule("CONSULTATION", "HZ{YY}{0:D5}", "YEARLY", 5);
        when(repository.findByApplicationTypeForUpdate("CONSULTATION")).thenReturn(rule);
        when(repository.pathologyNumberExists("HZ2600123", "CASE-1")).thenReturn(false);

        service.validateAndAcceptCandidate("CASE-1", "CONSULTATION", "HZ2600123");

        verify(repository).advanceCounterToAtLeast(rule.ruleCode(), "2026", 123L);
    }

    @Test
    void shouldRejectOverrideOutsideCurrentRuleAndPeriod() {
        CheckItemRuleRow rule = automaticRule("CONSULTATION", "HZ{YY}{0:D5}", "YEARLY", 5);
        when(repository.findByApplicationTypeForUpdate("CONSULTATION")).thenReturn(rule);
        when(repository.pathologyNumberExists("HZ2500123", "CASE-1")).thenReturn(false);

        assertThatThrownBy(() -> service.validateAndAcceptCandidate(
            "CASE-1", "CONSULTATION", "HZ2500123"))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("does not match selected application type");
    }

    @Test
    void shouldRejectInvalidPeriodTemplateAndHistoricalCounterLowering() {
        CheckItemRuleService.SaveCheckItemRuleCommand missingMonth = new CheckItemRuleService.SaveCheckItemRuleCommand(
            "RESEARCH", "MONTH", true, "KY{YY}{0:D5}", 0);
        assertThatThrownBy(() -> service.createRule(missingMonth))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("month placeholder");

        CheckItemRuleRow current = automaticRule("RESEARCH", "KY{YY}{0:D5}", "YEARLY", 5);
        when(repository.findByIdForUpdate(current.id())).thenReturn(current);
        when(repository.findPathologyNumbers()).thenReturn(List.of("KY2600042", "OTHER-999"));
        CheckItemRuleService.SaveCheckItemRuleCommand lowering = new CheckItemRuleService.SaveCheckItemRuleCommand(
            "RESEARCH", "YEAR", true, "KY{YY}{0:D5}", 41);

        assertThatThrownBy(() -> service.updateRule(current.id(), lowering))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("historical maximum 42");
    }

    @Test
    void shouldBlockDeletionAfterAnyUse() {
        CheckItemRuleRow rule = automaticRule("RESEARCH", "KY{YY}{0:D5}", "YEARLY", 5);
        when(repository.findByIdForUpdate(rule.id())).thenReturn(rule);
        when(repository.hasAdvancedCounter(rule.ruleCode())).thenReturn(true);

        assertThatThrownBy(() -> service.deleteRule(rule.id()))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("cannot be deleted");
        verify(repository, never()).delete(rule);
    }

    @Test
    void shouldPersistUsageMarkerForAcceptedManualNumber() {
        CheckItemRuleRow rule = new CheckItemRuleRow(
            "NR_RESEARCH", "RULE_RESEARCH", CheckItemRuleRepository.BIZ_PREFIX + "RESEARCH",
            "RESEARCH", "", false, "NONE", 1, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK));
        when(repository.findByApplicationTypeForUpdate("RESEARCH")).thenReturn(rule);
        when(repository.pathologyNumberExists("MANUAL-001", "CASE-1")).thenReturn(false);

        service.validateAndAcceptCandidate("CASE-1", "RESEARCH", "MANUAL-001");

        verify(repository).markRuleUsed(rule.ruleCode());
    }

    private CheckItemRuleRow automaticRule(String type, String template, String resetPolicy, int sequenceLength) {
        return new CheckItemRuleRow(
            "NR_" + type,
            "RULE_" + type,
            CheckItemRuleRepository.BIZ_PREFIX + type,
            type,
            template,
            true,
            resetPolicy,
            sequenceLength,
            LocalDateTime.now(CLOCK),
            LocalDateTime.now(CLOCK));
    }
}
