package com.company.bl.integration.application;

import com.company.bl.application.gateway.HistoricalReportGateway;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.integration.infrastructure.M6JdbcRepository;
import com.company.common.web.observability.ObservedOperation;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class HistoricalReportService {

    private static final String BUSINESS_TYPE_IMPORT_JOB = "HISTORICAL_IMPORT_JOB";
    private static final String EXTERNAL_SYSTEM_DEFAULT = "MOCK_HIS";

    private final M6JdbcRepository repository;
    private final IntegrationManagementService integrationManagementService;
    private final HistoricalReportGateway historicalReportGateway;
    private final OperationAuditService operationAuditService;

    public HistoricalReportService(M6JdbcRepository repository,
                                   IntegrationManagementService integrationManagementService,
                                   HistoricalReportGateway historicalReportGateway,
                                   OperationAuditService operationAuditService) {
        this.repository = repository;
        this.integrationManagementService = integrationManagementService;
        this.historicalReportGateway = historicalReportGateway;
        this.operationAuditService = operationAuditService;
    }

    @ObservedOperation(
        operation = "historical_report_import",
        successCounter = "historical_report_import_total",
        failureCounter = "historical_report_import_failed_total",
        durationMetric = "historical_report_import_duration")
    @Transactional
    public HistoricalImportJobView importReports(ImportHistoricalReportsCommand command) {
        return operationAuditService.audit("M6", "HISTORY", "import_historical_reports", () -> {
            LocalDateTime now = LocalDateTime.now();
            String jobId = "HIJ-" + UUID.randomUUID();
            String sourceSystem = command.sourceSystem() == null || command.sourceSystem().isBlank()
                ? EXTERNAL_SYSTEM_DEFAULT
                : command.sourceSystem();
            repository.insertHistoricalImportJob(new M6JdbcRepository.CreateHistoricalImportJobRow(
                jobId, sourceSystem, command.patientId(), command.pathologyNo(), command.applicationNo(),
                "RUNNING", command.operatorUserId(), command.operatorName(), 0, 0, 0, now, null, null,
                command.remarks(), now, now));
            String taskId = integrationManagementService.openTask(new IntegrationManagementService.CreateIntegrationTaskCommand(
                "HISTORY_IMPORT", BUSINESS_TYPE_IMPORT_JOB, jobId, "FETCH_REPORTS", sourceSystem, command.toString()));
            try {
                List<HistoricalReportGateway.ImportedHistoricalReport> reports = historicalReportGateway.fetch(
                    new HistoricalReportGateway.FetchHistoricalReportsRequest(
                        sourceSystem, command.patientId(), command.pathologyNo(), command.applicationNo(), command.from(), command.to()));
                int successCount = 0;
                for (HistoricalReportGateway.ImportedHistoricalReport report : reports) {
                    upsertHistoricalReport(jobId, report);
                    successCount++;
                }
                repository.updateHistoricalImportJob(new M6JdbcRepository.HistoricalImportJobRow(
                    jobId, sourceSystem, command.patientId(), command.pathologyNo(), command.applicationNo(), "COMPLETED",
                    command.operatorUserId(), command.operatorName(), reports.size(), successCount, reports.size() - successCount,
                    now, LocalDateTime.now(), null, command.remarks(), now, LocalDateTime.now()));
                integrationManagementService.markSuccess(taskId, "{\"count\":" + reports.size() + "}");
                return toJobView(repository.findHistoricalImportJobById(jobId));
            } catch (RuntimeException exception) {
                repository.updateHistoricalImportJob(new M6JdbcRepository.HistoricalImportJobRow(
                    jobId, sourceSystem, command.patientId(), command.pathologyNo(), command.applicationNo(), "FAILED",
                    command.operatorUserId(), command.operatorName(), 0, 0, 0, now, LocalDateTime.now(),
                    exception.getMessage(), command.remarks(), now, LocalDateTime.now()));
                integrationManagementService.markFailure(taskId, BlErrorCode.EXTERNAL_INTEGRATION_UNAVAILABLE.code(),
                    exception.getMessage(), "{\"failed\":true}", true);
                throw exception;
            }
        }, HistoricalImportJobView::id, () -> "history-import", command.operatorUserId(), command.operatorName(), () -> "history import");
    }

    @Transactional(readOnly = true)
    public List<HistoricalImportJobView> listImportJobs(String sourceSystem,
                                                        String importStatus,
                                                        String patientId,
                                                        String pathologyNo,
                                                        String applicationNo) {
        return repository.findHistoricalImportJobs(sourceSystem, importStatus, patientId, pathologyNo, applicationNo).stream()
            .map(this::toJobView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricalReportView> listHistoricalReports(String sourceSystem,
                                                            String patientId,
                                                            String pathologyNo,
                                                            String applicationNo,
                                                            String externalReportNo,
                                                            LocalDateTime from,
                                                            LocalDateTime to) {
        return repository.findHistoricalReports(sourceSystem, patientId, pathologyNo, applicationNo, externalReportNo, from, to).stream()
            .map(this::toHistoricalView)
            .toList();
    }

    private void upsertHistoricalReport(String jobId, HistoricalReportGateway.ImportedHistoricalReport report) {
        LocalDateTime now = LocalDateTime.now();
        M6JdbcRepository.HistoricalReportRow existing =
            repository.findHistoricalReportBySourceAndExternalNo(report.sourceSystem(), report.externalReportNo());
        String historicalReportId = existing == null ? "HR-" + UUID.randomUUID() : existing.id();
        if (existing == null) {
            repository.insertHistoricalReport(new M6JdbcRepository.CreateHistoricalReportRow(
                historicalReportId, jobId, report.sourceSystem(), report.externalReportNo(), report.patientId(), report.patientName(),
                report.pathologyNo(), report.applicationNo(), report.reportDate(), report.finalDiagnosis(), report.reportSummary(),
                report.rawPayload(), report.sourceDepartmentName(), report.sourceDoctorName(), report.attachmentUrl(), now, now));
        } else {
            repository.updateHistoricalReport(new M6JdbcRepository.HistoricalReportRow(
                historicalReportId, jobId, report.sourceSystem(), report.externalReportNo(), report.patientId(), report.patientName(),
                report.pathologyNo(), report.applicationNo(), report.reportDate(), report.finalDiagnosis(), report.reportSummary(),
                report.rawPayload(), report.sourceDepartmentName(), report.sourceDoctorName(), report.attachmentUrl(), existing.createdAt(), now));
            repository.deleteHistoricalReportVersions(historicalReportId);
        }
        int versionNo = 1;
        List<HistoricalReportGateway.ImportedHistoricalReportVersion> versions = report.versions();
        if (versions == null || versions.isEmpty()) {
            repository.insertHistoricalReportVersion(new M6JdbcRepository.CreateHistoricalReportVersionRow(
                "HRV-" + UUID.randomUUID(), historicalReportId, versionNo, report.finalDiagnosis(), report.reportSummary(), report.rawPayload(), now, now));
        } else {
            for (HistoricalReportGateway.ImportedHistoricalReportVersion version : versions) {
                repository.insertHistoricalReportVersion(new M6JdbcRepository.CreateHistoricalReportVersionRow(
                    "HRV-" + UUID.randomUUID(), historicalReportId, version.versionNo(), version.finalDiagnosis(),
                    version.reportSummary(), version.rawPayload(), now, now));
            }
        }
    }

    private HistoricalImportJobView toJobView(M6JdbcRepository.HistoricalImportJobRow row) {
        if (row == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Historical import job not found");
        }
        IntegrationManagementService.IntegrationTaskView task =
            integrationManagementService.findLatestTask(BUSINESS_TYPE_IMPORT_JOB, row.id(), "FETCH_REPORTS");
        return new HistoricalImportJobView(
            row.id(),
            row.sourceSystem(),
            row.patientId(),
            row.pathologyNo(),
            row.applicationNo(),
            row.importStatus(),
            row.totalCount(),
            row.successCount(),
            row.failureCount(),
            row.requestedByUserId(),
            row.requestedByName(),
            row.requestedAt() == null ? null : row.requestedAt().toString(),
            row.completedAt() == null ? null : row.completedAt().toString(),
            row.lastErrorMessage(),
            row.remarks(),
            task == null ? null : task.id(),
            task == null ? 0 : task.retryCount(),
            task == null ? 0 : task.maxRetryCount(),
            task == null ? null : task.lastErrorCode(),
            task == null ? null : task.lastErrorMessage(),
            task == null ? null : task.compensationStatus(),
            task == null ? null : task.reconciliationStatus());
    }

    private HistoricalReportView toHistoricalView(M6JdbcRepository.HistoricalReportRow row) {
        List<HistoricalReportVersionView> versions = repository.findHistoricalReportVersions(row.id()).stream()
            .map(item -> new HistoricalReportVersionView(
                item.id(), item.versionNo(), item.finalDiagnosis(), item.reportSummary(), item.createdAt() == null ? null : item.createdAt().toString()))
            .toList();
        return new HistoricalReportView(
            row.id(),
            row.importJobId(),
            row.sourceSystem(),
            row.externalReportNo(),
            row.patientId(),
            row.patientName(),
            row.pathologyNo(),
            row.applicationNo(),
            row.reportDate() == null ? null : row.reportDate().toString(),
            row.finalDiagnosis(),
            row.reportSummary(),
            row.sourceDepartmentName(),
            row.sourceDoctorName(),
            row.attachmentUrl(),
            versions);
    }

    public record ImportHistoricalReportsCommand(
        String sourceSystem,
        String patientId,
        String pathologyNo,
        String applicationNo,
        LocalDateTime from,
        LocalDateTime to,
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record HistoricalImportJobView(
        String id,
        String sourceSystem,
        String patientId,
        String pathologyNo,
        String applicationNo,
        String importStatus,
        int totalCount,
        int successCount,
        int failureCount,
        String requestedByUserId,
        String requestedByName,
        String requestedAt,
        String completedAt,
        String lastErrorMessage,
        String remarks,
        String integrationTaskId,
        int retryCount,
        int maxRetryCount,
        String taskLastErrorCode,
        String taskLastErrorMessage,
        String compensationStatus,
        String reconciliationStatus
    ) {
    }

    public record HistoricalReportView(
        String id,
        String importJobId,
        String sourceSystem,
        String externalReportNo,
        String patientId,
        String patientName,
        String pathologyNo,
        String applicationNo,
        String reportDate,
        String finalDiagnosis,
        String reportSummary,
        String sourceDepartmentName,
        String sourceDoctorName,
        String attachmentUrl,
        List<HistoricalReportVersionView> versions
    ) {
    }

    public record HistoricalReportVersionView(
        String id,
        int versionNo,
        String finalDiagnosis,
        String reportSummary,
        String createdAt
    ) {
    }
}
