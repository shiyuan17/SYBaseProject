package com.company.bl.integration.application;

import com.company.bl.domain.repository.DiagnosticReportRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class FormalReportIssueScheduler {

    private final DiagnosticReportRepository diagnosticReportRepository;

    public FormalReportIssueScheduler(DiagnosticReportRepository diagnosticReportRepository) {
        this.diagnosticReportRepository = diagnosticReportRepository;
    }

    @Scheduled(fixedDelayString = "${bl.report-issue.scheduler.fixed-delay-ms:60000}")
    public void issueScheduledFormalReports() {
        LocalDateTime now = LocalDateTime.now();
        List<DiagnosticReportRepository.ReportVersion> dueVersions =
            diagnosticReportRepository.findScheduledReportVersionsDue(now);
        if (dueVersions.isEmpty()) {
            return;
        }
        diagnosticReportRepository.markReportVersionsIssued(
            dueVersions.stream().map(DiagnosticReportRepository.ReportVersion::id).toList(),
            now
        );
    }
}
