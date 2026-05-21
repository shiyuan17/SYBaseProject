package com.company.bl.infrastructure.integration;

import com.company.bl.application.gateway.HistoricalReportGateway;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StubHistoricalReportGateway implements HistoricalReportGateway {

    @Override
    public List<ImportedHistoricalReport> fetch(FetchHistoricalReportsRequest request) {
        String sourceSystem = request.sourceSystem() == null || request.sourceSystem().isBlank()
            ? "MOCK_HIS"
            : request.sourceSystem().trim();
        List<ImportedHistoricalReport> reports = List.of(
            new ImportedHistoricalReport(
                "HIS-RPT-001",
                sourceSystem,
                "PAT-HIS-001",
                "Mock History One",
                "PATH-HIS-001",
                "APP-HIS-001",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                "Papillary thyroid carcinoma",
                "Historical diagnosis summary 1",
                "{\"reportNo\":\"HIS-RPT-001\"}",
                "Pathology",
                "Dr History",
                "https://mock/his/1",
                List.of(
                    new ImportedHistoricalReportVersion(1, "Papillary thyroid carcinoma", "Historical diagnosis summary 1", "{\"version\":1}")
                )),
            new ImportedHistoricalReport(
                "HIS-RPT-002",
                sourceSystem,
                "PAT-HIS-002",
                "Mock History Two",
                "PATH-HIS-002",
                "APP-HIS-002",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                "Adenocarcinoma",
                "Historical diagnosis summary 2",
                "{\"reportNo\":\"HIS-RPT-002\"}",
                "Pathology",
                "Dr History",
                "https://mock/his/2",
                List.of(
                    new ImportedHistoricalReportVersion(1, "Adenocarcinoma", "Historical diagnosis summary 2", "{\"version\":1}"),
                    new ImportedHistoricalReportVersion(2, "Adenocarcinoma revised", "Historical diagnosis summary 2 revised", "{\"version\":2}")
                )));
        return reports.stream()
            .filter(report -> request.patientId() == null || request.patientId().isBlank() || request.patientId().equals(report.patientId()))
            .filter(report -> request.pathologyNo() == null || request.pathologyNo().isBlank() || request.pathologyNo().equals(report.pathologyNo()))
            .filter(report -> request.applicationNo() == null || request.applicationNo().isBlank() || request.applicationNo().equals(report.applicationNo()))
            .filter(report -> request.from() == null || !report.reportDate().isBefore(request.from()))
            .filter(report -> request.to() == null || !report.reportDate().isAfter(request.to()))
            .toList();
    }
}
