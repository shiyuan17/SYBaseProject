package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.ReportRevisionRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.notification.application.WorkflowNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class ReportRevisionWorkflowService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final ReportRevisionRepository reportRevisionRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;
    private final WorkflowNotificationService workflowNotificationService;

    ReportRevisionWorkflowService(DiagnosticReportRepository diagnosticReportRepository,
                                  ReportRevisionRepository reportRevisionRepository,
                                  TechnicalWorkflowRepository technicalWorkflowRepository,
                                  DiagnosticReportSupport diagnosticReportSupport,
                                  WorkflowNotificationService workflowNotificationService) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.reportRevisionRepository = reportRevisionRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
        this.workflowNotificationService = workflowNotificationService;
    }

    @Transactional
    DiagnosticReportModels.ReportRevisionResult createRevisionRequest(DiagnosticReportModels.CreateReportRevisionRequestCommand command) {
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(command.reportId());
        if (!DiagnosticReportConstants.REPORT_SIGNED.equals(report.reportStatus())
            && !DiagnosticReportConstants.REPORT_PUBLISHED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report cannot request revision");
        }
        diagnosticReportSupport.ensureAssignedDoctor(diagnosticReportSupport.getDiagnosticTask(report.taskId()), command.operatorUserId());
        if (reportRevisionRepository.findPendingRevisionRequestByReportId(report.id()).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Pending revision request already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        String requestId = diagnosticReportSupport.nextId("RR");
        reportRevisionRepository.insertRevisionRequest(new ReportRevisionRepository.CreateReportRevisionRequestCommand(
            requestId,
            report.caseId(),
            report.id(),
            report.versionNo(),
            DiagnosticReportConstants.REVISION_PENDING,
            command.requestReason(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.remarks()));
        diagnosticReportSupport.insertWorkflowEvent(report.caseId(), "REPORT_REVISION_REQUEST", "REQUEST", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.requestReason());
        ReportRevisionRepository.ReportRevisionRequest request = reportRevisionRepository.findRevisionRequestById(requestId).orElseThrow();
        workflowNotificationService.notifyUsers(new WorkflowNotificationService.BulkNotificationCommand(
            WorkflowNotificationService.TOPIC_REPORT_REVISION,
            WorkflowNotificationService.CATEGORY_TODO_TASK,
            WorkflowNotificationService.LEVEL_MEDIUM,
            "收到报告修订申请",
            "病理号 %s 的报告修订申请待你审批。".formatted(report.pathologyNo()),
            "病理号 %s 的报告修订申请待审批".formatted(report.pathologyNo()),
            null,
            "/doctor-workflow/revision",
            buildRevisionQuery(request.id(), request.caseId(), request.reportId()),
            "查看申请",
            command.operatorUserId(),
            false,
            List.of(new WorkflowNotificationService.Recipient(report.signedByUserId(), report.signedByName()))
        ));
        return new DiagnosticReportModels.ReportRevisionResult(request.id(), request.caseId(), request.reportId(), request.requestStatus(), request.approvedVersionNo());
    }

    @Transactional
    DiagnosticReportModels.ReportRevisionResult approveRevisionRequest(DiagnosticReportModels.ReviewReportRevisionCommand command) {
        ReportRevisionRepository.ReportRevisionRequest request = reportRevisionRepository.findRevisionRequestById(command.requestId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Revision request not found"));
        if (!DiagnosticReportConstants.REVISION_PENDING.equals(request.requestStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Revision request is not pending");
        }
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(request.reportId());
        int nextVersionNo = report.versionNo() + 1;
        LocalDateTime now = LocalDateTime.now();
        reportRevisionRepository.approveRevisionRequest(request.id(), command.operatorUserId(), command.operatorName(), nextVersionNo, command.remarks(), now);
        diagnosticReportRepository.resetPathologyReportForRevision(report.id(), nextVersionNo, command.remarks(), now);
        diagnosticReportRepository.revertDiagnosticTaskToInProgress(report.taskId(), command.remarks());
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "DIAGNOSING");
        diagnosticReportSupport.insertWorkflowEvent(report.caseId(), "REPORT_REVISION_APPROVE", "APPROVE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.remarks());
        ReportRevisionRepository.ReportRevisionRequest updated = reportRevisionRepository.findRevisionRequestById(request.id()).orElseThrow();
        workflowNotificationService.notifyUsers(new WorkflowNotificationService.BulkNotificationCommand(
            WorkflowNotificationService.TOPIC_REPORT_REVISION,
            WorkflowNotificationService.CATEGORY_SYSTEM_MESSAGE,
            WorkflowNotificationService.LEVEL_MEDIUM,
            "报告修订申请已通过",
            "病理号 %s 的报告修订申请已通过，请继续修订报告。".formatted(report.pathologyNo()),
            "病理号 %s 的报告修订申请已通过".formatted(report.pathologyNo()),
            null,
            "/doctor-workflow/revision",
            buildRevisionQuery(updated.id(), updated.caseId(), updated.reportId()),
            "查看结果",
            command.operatorUserId(),
            false,
            List.of(new WorkflowNotificationService.Recipient(updated.requestedByUserId(), updated.requestedByName()))
        ));
        return new DiagnosticReportModels.ReportRevisionResult(updated.id(), updated.caseId(), updated.reportId(), updated.requestStatus(), updated.approvedVersionNo());
    }

    @Transactional
    DiagnosticReportModels.ReportRevisionResult rejectRevisionRequest(DiagnosticReportModels.ReviewReportRevisionCommand command) {
        ReportRevisionRepository.ReportRevisionRequest request = reportRevisionRepository.findRevisionRequestById(command.requestId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Revision request not found"));
        if (!DiagnosticReportConstants.REVISION_PENDING.equals(request.requestStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Revision request is not pending");
        }
        LocalDateTime now = LocalDateTime.now();
        reportRevisionRepository.rejectRevisionRequest(request.id(), command.operatorUserId(), command.operatorName(), command.rejectReason(), now);
        diagnosticReportSupport.insertWorkflowEvent(request.caseId(), "REPORT_REVISION_REJECT", "REJECT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.rejectReason());
        ReportRevisionRepository.ReportRevisionRequest updated = reportRevisionRepository.findRevisionRequestById(request.id()).orElseThrow();
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(updated.reportId());
        workflowNotificationService.notifyUsers(new WorkflowNotificationService.BulkNotificationCommand(
            WorkflowNotificationService.TOPIC_REPORT_REVISION,
            WorkflowNotificationService.CATEGORY_SYSTEM_MESSAGE,
            WorkflowNotificationService.LEVEL_MEDIUM,
            "报告修订申请已驳回",
            "病理号 %s 的报告修订申请已驳回，请查看原因。".formatted(report.pathologyNo()),
            "病理号 %s 的报告修订申请已驳回".formatted(report.pathologyNo()),
            null,
            "/doctor-workflow/revision",
            buildRevisionQuery(updated.id(), updated.caseId(), updated.reportId()),
            "查看结果",
            command.operatorUserId(),
            false,
            List.of(new WorkflowNotificationService.Recipient(updated.requestedByUserId(), updated.requestedByName()))
        ));
        return new DiagnosticReportModels.ReportRevisionResult(updated.id(), updated.caseId(), updated.reportId(), updated.requestStatus(), updated.approvedVersionNo());
    }

    private Map<String, String> buildRevisionQuery(String requestId, String caseId, String reportId) {
        Map<String, String> query = new LinkedHashMap<>();
        putIfPresent(query, "requestId", requestId);
        putIfPresent(query, "caseId", caseId);
        putIfPresent(query, "reportId", reportId);
        return query;
    }

    private void putIfPresent(Map<String, String> query, String key, String value) {
        if (value != null && !value.isBlank()) {
            query.put(key, value);
        }
    }
}
