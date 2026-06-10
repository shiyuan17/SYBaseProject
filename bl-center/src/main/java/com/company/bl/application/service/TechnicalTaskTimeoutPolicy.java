package com.company.bl.application.service;

import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.masterdata.application.SystemConfigService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
class TechnicalTaskTimeoutPolicy {

    static final String GROSSING_TIMEOUT_RULE = "technical.timeout.grossingMinutes";
    static final String DEHYDRATION_TIMEOUT_RULE = "technical.timeout.dehydrationMinutes";
    static final String SLICING_TIMEOUT_RULE = "technical.timeout.slicingMinutes";
    static final String STAINING_TIMEOUT_RULE = "technical.timeout.stainingMinutes";

    private static final int DEFAULT_GROSSING_TIMEOUT_MINUTES = 240;
    private static final int DEFAULT_DEHYDRATION_TIMEOUT_MINUTES = 720;
    private static final int DEFAULT_SLICING_TIMEOUT_MINUTES = 240;
    private static final int DEFAULT_STAINING_TIMEOUT_MINUTES = 240;
    private static final Set<String> ACTIVE_STATUSES = Set.of(
        TechnicalWorkflowConstants.TASK_PENDING,
        TechnicalWorkflowConstants.TASK_IN_PROGRESS,
        TechnicalWorkflowConstants.TASK_EMBEDDING_CONFIRM_PENDING
    );

    private final SystemConfigService systemConfigService;

    TechnicalTaskTimeoutPolicy(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    TimeoutSnapshot snapshot(LocalDateTime evaluatedAt) {
        Map<String, TimeoutRule> rules = new LinkedHashMap<>();
        rules.put(TechnicalWorkflowConstants.NODE_GROSSING, new TimeoutRule(
            TechnicalWorkflowConstants.NODE_GROSSING,
            GROSSING_TIMEOUT_RULE,
            loadTimeoutMinutes(GROSSING_TIMEOUT_RULE, DEFAULT_GROSSING_TIMEOUT_MINUTES)));
        rules.put(TechnicalWorkflowConstants.NODE_DEHYDRATION, new TimeoutRule(
            TechnicalWorkflowConstants.NODE_DEHYDRATION,
            DEHYDRATION_TIMEOUT_RULE,
            loadTimeoutMinutes(DEHYDRATION_TIMEOUT_RULE, DEFAULT_DEHYDRATION_TIMEOUT_MINUTES)));
        rules.put(TechnicalWorkflowConstants.NODE_SLICING, new TimeoutRule(
            TechnicalWorkflowConstants.NODE_SLICING,
            SLICING_TIMEOUT_RULE,
            loadTimeoutMinutes(SLICING_TIMEOUT_RULE, DEFAULT_SLICING_TIMEOUT_MINUTES)));
        rules.put(TechnicalWorkflowConstants.NODE_STAINING, new TimeoutRule(
            TechnicalWorkflowConstants.NODE_STAINING,
            STAINING_TIMEOUT_RULE,
            loadTimeoutMinutes(STAINING_TIMEOUT_RULE, DEFAULT_STAINING_TIMEOUT_MINUTES)));
        return new TimeoutSnapshot(evaluatedAt, rules);
    }

    TimeoutEvaluation evaluate(TechnicalWorkflowRecords.TechnicalTask task, TimeoutSnapshot snapshot) {
        TimeoutRule rule = snapshot.ruleFor(task.taskType());
        if (rule == null || task.createdAt() == null) {
            return TimeoutEvaluation.notApplicable();
        }
        LocalDateTime deadlineAt = task.createdAt().plusMinutes(rule.timeoutMinutes());
        boolean timedOut = ACTIVE_STATUSES.contains(task.taskStatus()) && snapshot.evaluatedAt().isAfter(deadlineAt);
        return new TimeoutEvaluation(deadlineAt, rule.ruleCode(), timedOut);
    }

    private int loadTimeoutMinutes(String configKey, int defaultValue) {
        String raw = systemConfigService.getConfigValue(configKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    record TimeoutSnapshot(LocalDateTime evaluatedAt, Map<String, TimeoutRule> rules) {

        TimeoutRule ruleFor(String taskType) {
            return rules.get(taskType);
        }

        LocalDateTime thresholdFor(String taskType) {
            TimeoutRule rule = ruleFor(taskType);
            return rule == null ? null : evaluatedAt.minusMinutes(rule.timeoutMinutes());
        }
    }

    record TimeoutRule(String taskType, String ruleCode, int timeoutMinutes) {
    }

    record TimeoutEvaluation(LocalDateTime deadlineAt, String timeoutRuleCode, boolean timedOut) {

        private static TimeoutEvaluation notApplicable() {
            return new TimeoutEvaluation(null, null, false);
        }
    }
}
