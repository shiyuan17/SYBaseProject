package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
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

    @Operation(summary = "查询病例诊断工作台", description = "按病例 ID 查询诊断工作台聚合信息。")
    @RequirePermission(M4PermissionCodes.WORKBENCH_QUERY)
    @GetMapping("/{id}/diagnostic-workbench")
    public DiagnosticWorkbenchResponse getDiagnosticWorkbench(@Parameter(description = "病例 ID") @PathVariable("id") String caseId) {
        DiagnosticReportModels.DiagnosticWorkbenchView result = diagnosticReportAppService.getDiagnosticWorkbench(caseId);
        return new DiagnosticWorkbenchResponse(
            result.caseId(),
            result.applicationNo(),
            result.pathologyNo(),
            result.caseStatus(),
            result.patientName(),
            result.submittingDepartmentName(),
            result.submittingDoctorName(),
            result.clinicalDiagnosis(),
            result.specimens().stream().map(item -> new DiagnosticWorkbenchResponse.SpecimenSummary(
                item.specimenId(), item.specimenNo(), item.barcode(), item.specimenName(), item.specimenStatus())).toList(),
            result.blocks().stream().map(item -> new DiagnosticWorkbenchResponse.BlockSummary(
                item.blockId(), item.specimenId(), item.blockCode(), item.embeddingBoxNo(), item.description())).toList(),
            result.slides().stream().map(item -> new DiagnosticWorkbenchResponse.SlideSummary(
                item.slideId(), item.specimenId(), item.embeddingBoxId(), item.slideNo(), item.slideStatus(), item.qualityStatus())).toList(),
            result.diagnosticTasks().stream().map(this::toTaskResponse).toList(),
            result.currentReport() == null ? null : new DiagnosticWorkbenchResponse.CurrentReportSummary(
                result.currentReport().reportId(),
                result.currentReport().reportNo(),
                result.currentReport().reportStatus(),
                result.currentReport().clinicalDiagnosis(),
                result.currentReport().grossExam(),
                result.currentReport().microscopicExam(),
                result.currentReport().finalDiagnosis(),
                result.currentReport().richTextContent(),
                result.currentReport().submittedAt(),
                result.currentReport().reviewedAt(),
                result.currentReport().signedAt(),
                result.currentReport().publishedAt(),
                result.currentReport().reviewerName(),
                result.currentReport().signedByName(),
                result.currentReport().versionNo()),
            result.recentEvents().stream().map(item -> new DiagnosticWorkbenchResponse.EventSummary(
                item.nodeCode(), item.eventType(), item.eventStatus(), item.eventTime(), item.operatorName(), item.eventContent())).toList());
    }

    @Operation(summary = "查询病例报告追踪", description = "按病例 ID 查询诊断任务、报告状态、版本摘要和事件时间线。")
    @RequirePermission(M4PermissionCodes.REPORT_TRACKING_QUERY)
    @GetMapping("/{id}/report-tracking")
    public ReportTrackingResponse getReportTracking(@Parameter(description = "病例 ID") @PathVariable("id") String caseId) {
        DiagnosticReportModels.ReportTrackingView result = diagnosticReportAppService.getReportTracking(caseId);
        return new ReportTrackingResponse(
            result.caseId(),
            result.applicationNo(),
            result.pathologyNo(),
            result.caseStatus(),
            result.patientName(),
            result.diagnosticTasks().stream().map(this::toTaskResponse).toList(),
            result.currentReport() == null ? null : new DiagnosticWorkbenchResponse.CurrentReportSummary(
                result.currentReport().reportId(),
                result.currentReport().reportNo(),
                result.currentReport().reportStatus(),
                result.currentReport().clinicalDiagnosis(),
                result.currentReport().grossExam(),
                result.currentReport().microscopicExam(),
                result.currentReport().finalDiagnosis(),
                result.currentReport().richTextContent(),
                result.currentReport().submittedAt(),
                result.currentReport().reviewedAt(),
                result.currentReport().signedAt(),
                result.currentReport().publishedAt(),
                result.currentReport().reviewerName(),
                result.currentReport().signedByName(),
                result.currentReport().versionNo()),
            result.versions().stream().map(item -> new ReportTrackingResponse.ReportVersionSummary(
                item.versionId(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(), item.signedAt(), item.createdAt())).toList(),
            result.events().stream().map(item -> new DiagnosticWorkbenchResponse.EventSummary(
                item.nodeCode(), item.eventType(), item.eventStatus(), item.eventTime(), item.operatorName(), item.eventContent())).toList());
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
}
