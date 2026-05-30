package com.company.bl.integration.infrastructure;

import java.time.LocalDateTime;

public final class M6HistoricalReportRows {

    private M6HistoricalReportRows() {
    }

    public record CreateHistoricalImportJobRow(
        String id,
        String sourceSystem,
        String patientId,
        String pathologyNo,
        String applicationNo,
        String importStatus,
        String requestedByUserId,
        String requestedByName,
        int totalCount,
        int successCount,
        int failureCount,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String lastErrorMessage,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record HistoricalImportJobRow(
        String id,
        String sourceSystem,
        String patientId,
        String pathologyNo,
        String applicationNo,
        String importStatus,
        String requestedByUserId,
        String requestedByName,
        int totalCount,
        int successCount,
        int failureCount,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String lastErrorMessage,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateHistoricalReportRow(
        String id,
        String importJobId,
        String sourceSystem,
        String externalReportNo,
        String patientId,
        String patientName,
        String pathologyNo,
        String applicationNo,
        LocalDateTime reportDate,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload,
        String sourceDepartmentName,
        String sourceDoctorName,
        String attachmentUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record HistoricalReportRow(
        String id,
        String importJobId,
        String sourceSystem,
        String externalReportNo,
        String patientId,
        String patientName,
        String pathologyNo,
        String applicationNo,
        LocalDateTime reportDate,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload,
        String sourceDepartmentName,
        String sourceDoctorName,
        String attachmentUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CreateHistoricalReportVersionRow(
        String id,
        String historicalReportId,
        int versionNo,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record HistoricalReportVersionRow(
        String id,
        String historicalReportId,
        int versionNo,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
