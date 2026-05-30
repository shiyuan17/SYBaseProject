package com.company.bl.integration.infrastructure;

import java.time.LocalDateTime;

public final class M6StatisticsRows {

    private M6StatisticsRows() {
    }

    public record StatIndicatorDefinitionRow(
        String id,
        String indicatorCode,
        String indicatorName,
        String indicatorCategory,
        String metricScope,
        String aggregationType,
        String description,
        int sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record StatReportTemplateRow(
        String id,
        String templateCode,
        String templateName,
        String templateType,
        String indicatorCode,
        String defaultColumns,
        String parameterSchema,
        int sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateStatExportJobRow(
        String id,
        String exportNo,
        String templateId,
        String indicatorCode,
        String exportStatus,
        String filterPayload,
        String fileName,
        String contentType,
        String requestedByUserId,
        String requestedByName,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt
    ) {
    }
}
