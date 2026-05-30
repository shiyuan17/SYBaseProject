package com.company.bl.integration.infrastructure;

import java.time.LocalDateTime;

public final class M6IntegrationTaskRows {

    private M6IntegrationTaskRows() {
    }

    public record CreateIntegrationTaskRow(
        String id,
        String taskType,
        String businessType,
        String businessId,
        String stageCode,
        String externalSystem,
        String requestPayload,
        String responsePayload,
        String taskStatus,
        int retryCount,
        int maxRetryCount,
        LocalDateTime nextRetryAt,
        LocalDateTime lastAttemptAt,
        String lastErrorCode,
        String lastErrorMessage,
        String compensationStatus,
        String reconciliationStatus,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record IntegrationTaskRow(
        String id,
        String taskType,
        String businessType,
        String businessId,
        String stageCode,
        String externalSystem,
        String requestPayload,
        String responsePayload,
        String taskStatus,
        int retryCount,
        int maxRetryCount,
        LocalDateTime nextRetryAt,
        LocalDateTime lastAttemptAt,
        String lastErrorCode,
        String lastErrorMessage,
        String compensationStatus,
        String reconciliationStatus,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
