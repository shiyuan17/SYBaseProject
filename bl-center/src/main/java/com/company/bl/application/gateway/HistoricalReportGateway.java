package com.company.bl.application.gateway;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoricalReportGateway {

    List<ImportedHistoricalReport> fetch(FetchHistoricalReportsRequest request);

    record FetchHistoricalReportsRequest(
        String sourceSystem,
        String patientId,
        String pathologyNo,
        String applicationNo,
        LocalDateTime from,
        LocalDateTime to
    ) {
    }

    record ImportedHistoricalReport(
        String externalReportNo,
        String sourceSystem,
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
        List<ImportedHistoricalReportVersion> versions
    ) {
    }

    record ImportedHistoricalReportVersion(
        int versionNo,
        String finalDiagnosis,
        String reportSummary,
        String rawPayload
    ) {
    }
}
