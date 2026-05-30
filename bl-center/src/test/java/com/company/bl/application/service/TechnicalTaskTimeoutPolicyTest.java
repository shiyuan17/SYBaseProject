package com.company.bl.application.service;

import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.masterdata.application.SystemConfigService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TechnicalTaskTimeoutPolicyTest {

    @Test
    void snapshotShouldUseDefaultsWhenConfigIsInvalid() {
        SystemConfigService systemConfigService = mock(SystemConfigService.class);
        when(systemConfigService.getConfigValue(TechnicalTaskTimeoutPolicy.GROSSING_TIMEOUT_RULE, "240")).thenReturn("invalid");
        when(systemConfigService.getConfigValue(TechnicalTaskTimeoutPolicy.DEHYDRATION_TIMEOUT_RULE, "720")).thenReturn("720");
        when(systemConfigService.getConfigValue(TechnicalTaskTimeoutPolicy.STAINING_TIMEOUT_RULE, "240")).thenReturn("240");

        TechnicalTaskTimeoutPolicy policy = new TechnicalTaskTimeoutPolicy(systemConfigService);
        var snapshot = policy.snapshot(LocalDateTime.of(2026, 5, 30, 12, 0));

        assertThat(snapshot.ruleFor(TechnicalWorkflowConstants.NODE_GROSSING).timeoutMinutes()).isEqualTo(240);
        assertThat(snapshot.ruleFor(TechnicalWorkflowConstants.NODE_DEHYDRATION).timeoutMinutes()).isEqualTo(720);
    }

    @Test
    void evaluateShouldMarkTimedOutActiveTasks() {
        TechnicalTaskTimeoutPolicy policy = new TechnicalTaskTimeoutPolicy(mock(SystemConfigService.class));
        var snapshot = new TechnicalTaskTimeoutPolicy.TimeoutSnapshot(
            LocalDateTime.of(2026, 5, 30, 14, 0),
            java.util.Map.of(
                TechnicalWorkflowConstants.NODE_GROSSING,
                new TechnicalTaskTimeoutPolicy.TimeoutRule(TechnicalWorkflowConstants.NODE_GROSSING, "technical.timeout.grossingMinutes", 60)));

        TechnicalWorkflowRecords.TechnicalTask task = new TechnicalWorkflowRecords.TechnicalTask(
            "TASK-1",
            "APP-1",
            "APP-NO-1",
            "CASE-1",
            "PATH-1",
            "SP-1",
            TechnicalWorkflowConstants.NODE_GROSSING,
            TechnicalWorkflowConstants.TASK_IN_PROGRESS,
            "SPECIMEN",
            "OBJ-1",
            null,
            "NORMAL",
            TechnicalWorkflowConstants.NODE_GROSSING,
            "ST-1",
            "Station",
            "user-1",
            "Operator",
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.of(2026, 5, 30, 12, 0),
            LocalDateTime.of(2026, 5, 30, 12, 0),
            null);

        assertThat(policy.evaluate(task, snapshot).timedOut()).isTrue();
    }
}
