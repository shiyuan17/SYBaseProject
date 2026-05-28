package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.application.service.DiagnosticReportViews;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.vo.DiagnosticWorkbenchResponse;
import com.company.bl.interfaces.vo.PendingDiagnosticTaskResponse;
import com.company.bl.interfaces.vo.ReportTrackingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pathology-cases")
@Tag(name = "医生流程", description = "病例级诊断工作台与报告追踪接口")
public class PathologyCaseDiagnosticController {

    private final DiagnosticReportAppService diagnosticReportAppService;

    public PathologyCaseDiagnosticController(DiagnosticReportAppService diagnosticReportAppService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
    }

    @Operation(summary = "查询病例诊断工作台", description = "按病例 ID 或病理号查询诊断工作台聚合信息。")
    @RequirePermission(M4PermissionCodes.WORKBENCH_QUERY)
    @GetMapping("/{id}/diagnostic-workbench")
    public DiagnosticWorkbenchResponse getDiagnosticWorkbench(
        @Parameter(description = "病例 ID 或病理号") @PathVariable("id") String caseIdentifier
    ) {
        DiagnosticReportViews.DiagnosticWorkbenchView result =
            diagnosticReportAppService.getDiagnosticWorkbench(caseIdentifier);
        return new DiagnosticWorkbenchResponse(
            result.caseId(),
            result.applicationNo(),
            result.pathologyNo(),
            result.caseStatus(),
            result.patientName(),
            result.submittingDepartmentName(),
            result.submittingDoctorName(),
            result.clinicalDiagnosis(),
            result.applicationFormArchiveStatus(),
            result.applicationFormArchiveLocation(),
            result.applicationFormImageUrl(),
            result.specimens().stream().map(item -> new DiagnosticWorkbenchResponse.SpecimenSummary(
                item.specimenId(), item.specimenNo(), item.barcode(), item.specimenName(), item.specimenStatus())).toList(),
            result.blocks().stream().map(item -> new DiagnosticWorkbenchResponse.BlockSummary(
                item.blockId(), item.specimenId(), item.blockCode(), item.embeddingBoxNo(), item.description(),
                item.archiveStatus(), item.archiveLocation(), item.loanStatus())).toList(),
            result.slides().stream().map(item -> new DiagnosticWorkbenchResponse.SlideSummary(
                item.slideId(), item.specimenId(), item.embeddingBoxId(), item.slideNo(), item.slideStatus(), item.qualityStatus(),
                item.archiveStatus(), item.archiveLocation(), item.loanStatus())).toList(),
            result.diagnosticTasks().stream().map(this::toTaskResponse).toList(),
            toCurrentReport(result.currentReport()),
            result.recentEvents().stream().map(item -> new DiagnosticWorkbenchResponse.EventSummary(
                item.nodeCode(), item.eventType(), item.eventStatus(), item.eventTime(), item.operatorName(), item.eventContent())).toList(),
            result.revisions().stream().map(this::toRevisionSummary).toList(),
            result.medicalOrders().stream().map(this::toMedicalOrderSummary).toList(),
            result.consultations().stream().map(this::toConsultationSummary).toList(),
            result.hasPendingRevision());
    }

    @Operation(summary = "查询病例报告追踪", description = "按病例 ID 或病理号查询诊断任务、报告状态、版本摘要和关键时间线。")
    @RequirePermission(M4PermissionCodes.REPORT_TRACKING_QUERY)
    @GetMapping("/{id}/report-tracking")
    public ReportTrackingResponse getReportTracking(
        @Parameter(description = "病例 ID 或病理号") @PathVariable("id") String caseIdentifier
    ) {
        DiagnosticReportViews.ReportTrackingView result =
            diagnosticReportAppService.getReportTracking(caseIdentifier);
        return new ReportTrackingResponse(
            result.caseId(),
            result.applicationNo(),
            result.pathologyNo(),
            result.caseStatus(),
            result.patientName(),
            result.applicationFormArchiveStatus(),
            result.applicationFormArchiveLocation(),
            result.applicationFormImageUrl(),
            result.diagnosticTasks().stream().map(this::toTaskResponse).toList(),
            toCurrentReport(result.currentReport()),
            result.versions().stream().map(item -> new ReportTrackingResponse.ReportVersionSummary(
                item.versionId(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(), item.signedAt(), item.createdAt())).toList(),
            result.events().stream().map(item -> new DiagnosticWorkbenchResponse.EventSummary(
                item.nodeCode(), item.eventType(), item.eventStatus(), item.eventTime(), item.operatorName(), item.eventContent())).toList(),
            result.revisions().stream().map(this::toRevisionSummary).toList(),
            result.medicalOrders().stream().map(this::toMedicalOrderSummary).toList(),
            result.consultations().stream().map(this::toConsultationSummary).toList(),
            result.latestEffectiveVersionNo(),
            result.currentDraftVersionNo(),
            result.hasPendingRevision());
    }

    private PendingDiagnosticTaskResponse toTaskResponse(DiagnosticReportModels.TaskView item) {
        return new PendingDiagnosticTaskResponse(
            item.id(),
            item.applicationId(),
            item.applicationNo(),
            item.patientName(),
            item.caseId(),
            item.pathologyNo(),
            item.taskType(),
            item.taskStatus(),
            item.diagnosisDoctorUserId(),
            item.diagnosisDoctorName(),
            item.primaryDoctorUserId(),
            item.primaryDoctorName(),
            item.reviewerUserId(),
            item.reviewerName(),
            item.assignedAt(),
            item.acceptedAt(),
            item.completedAt(),
            item.remarks());
    }

    private DiagnosticWorkbenchResponse.CurrentReportSummary toCurrentReport(DiagnosticReportViews.PathologyReportView item) {
        if (item == null) {
            return null;
        }
        return new DiagnosticWorkbenchResponse.CurrentReportSummary(
            item.reportId(),
            item.reportNo(),
            item.reportStatus(),
            item.clinicalDiagnosis(),
            item.grossExam(),
            item.microscopicExam(),
            item.finalDiagnosis(),
            item.richTextContent(),
            item.submittedAt(),
            item.reviewedAt(),
            item.signedAt(),
            item.publishedAt(),
            item.reviewerName(),
            item.signedByName(),
            item.versionNo());
    }

    private DiagnosticWorkbenchResponse.RevisionRequestSummary toRevisionSummary(DiagnosticReportViews.RevisionRequestView item) {
        return new DiagnosticWorkbenchResponse.RevisionRequestSummary(
            item.requestId(),
            item.reportId(),
            item.currentVersionNo(),
            item.requestStatus(),
            item.requestReason(),
            item.requestedByName(),
            item.requestedAt(),
            item.reviewedByName(),
            item.reviewedAt(),
            item.rejectReason(),
            item.approvedVersionNo());
    }

    private DiagnosticWorkbenchResponse.MedicalOrderSummary toMedicalOrderSummary(DiagnosticReportViews.MedicalOrderView item) {
        return new DiagnosticWorkbenchResponse.MedicalOrderSummary(
            item.orderId(),
            item.caseId(),
            item.pathologyNo(),
            item.applicationNo(),
            item.patientName(),
            item.orderNumber(),
            item.orderType(),
            item.orderContent(),
            item.executionScope(),
            item.billingStatus(),
            item.status(),
            item.doctorName(),
            item.executorName(),
            item.orderDate(),
            item.acceptedAt(),
            item.completedAt(),
            item.cancelledAt(),
            item.remarks());
    }

    private DiagnosticWorkbenchResponse.ConsultationSummary toConsultationSummary(DiagnosticReportViews.ConsultationView item) {
        return new DiagnosticWorkbenchResponse.ConsultationSummary(
            item.consultationId(),
            item.consultationType(),
            item.status(),
            item.requestedByName(),
            item.requestedAt(),
            item.hostName(),
            item.completedAt(),
            item.opinion(),
            item.participantCount());
    }
}
