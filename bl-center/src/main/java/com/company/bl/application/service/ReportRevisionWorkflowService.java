package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.ReportRevisionRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
class ReportRevisionWorkflowService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final ReportRevisionRepository reportRevisionRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;

    ReportRevisionWorkflowService(DiagnosticReportRepository diagnosticReportRepository,
                                  ReportRevisionRepository reportRevisionRepository,
                                  TechnicalWorkflowRepository technicalWorkflowRepository,
                                  DiagnosticReportSupport diagnosticReportSupport) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.reportRevisionRepository = reportRevisionRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
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
        return new DiagnosticReportModels.ReportRevisionResult(updated.id(), updated.caseId(), updated.reportId(), updated.requestStatus(), updated.approvedVersionNo());
    }
}
