package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreatePathologyReportRequest;
import com.company.bl.interfaces.dto.DiagnosticTaskActionRequest;
import com.company.bl.interfaces.dto.RejectPathologyReportRequest;
import com.company.bl.interfaces.dto.UpdatePathologyReportDraftRequest;
import com.company.bl.interfaces.vo.PathologyReportOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pathology-reports")
@Tag(name = "医生流程", description = "病理报告草稿、审核、签发与发布接口")
public class PathologyReportController extends TechnicalControllerSupport {

    private final DiagnosticReportAppService diagnosticReportAppService;

    public PathologyReportController(DiagnosticReportAppService diagnosticReportAppService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
    }

    @Operation(summary = "创建病理报告草稿", description = "基于病例和诊断任务创建首份病理报告草稿。")
    @RequirePermission(M4PermissionCodes.REPORT_CREATE)
    @PostMapping
    public PathologyReportOperationResponse create(@Valid @RequestBody CreatePathologyReportRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.createReport(
            new DiagnosticReportModels.CreatePathologyReportCommand(
                request.getCaseId(),
                request.getTaskId(),
                request.getClinicalDiagnosis(),
                request.getGrossExam(),
                request.getMicroscopicExam(),
                request.getFinalDiagnosis(),
                request.getRichTextContent(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "保存病理报告草稿", description = "更新当前病理报告草稿，不生成历史版本。")
    @RequirePermission(M4PermissionCodes.REPORT_CREATE)
    @PostMapping("/{id}/save-draft")
    public PathologyReportOperationResponse saveDraft(@PathVariable("id") String reportId,
                                                      @Valid @RequestBody UpdatePathologyReportDraftRequest request,
                                                      HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.saveDraft(
            new DiagnosticReportModels.UpdateReportDraftCommand(
                reportId,
                request.getClinicalDiagnosis(),
                request.getGrossExam(),
                request.getMicroscopicExam(),
                request.getFinalDiagnosis(),
                request.getRichTextContent(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "提交病理报告", description = "提交病理报告进入审核流程。")
    @RequirePermission(M4PermissionCodes.REPORT_SUBMIT)
    @PostMapping("/{id}/submit")
    public PathologyReportOperationResponse submit(@PathVariable("id") String reportId,
                                                   @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.submitReport(
            new DiagnosticReportModels.ReportActionCommand(
                reportId,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "审核病理报告", description = "审核通过已提交的病理报告。")
    @RequirePermission(M4PermissionCodes.REPORT_REVIEW)
    @PostMapping("/{id}/review")
    public PathologyReportOperationResponse review(@PathVariable("id") String reportId,
                                                   @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.reviewReport(
            new DiagnosticReportModels.ReportActionCommand(
                reportId,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "驳回病理报告", description = "将已提交病理报告驳回至草稿状态。")
    @RequirePermission(M4PermissionCodes.REPORT_REVIEW)
    @PostMapping("/{id}/reject")
    public PathologyReportOperationResponse reject(@PathVariable("id") String reportId,
                                                   @Valid @RequestBody RejectPathologyReportRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.rejectReport(
            new DiagnosticReportModels.RejectReportCommand(
                reportId,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRejectReason()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "签发病理报告", description = "签发已审核通过的病理报告，并生成签发版本快照。")
    @RequirePermission(M4PermissionCodes.REPORT_SIGN)
    @PostMapping("/{id}/sign")
    public PathologyReportOperationResponse sign(@PathVariable("id") String reportId,
                                                 @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                 HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.signReport(
            new DiagnosticReportModels.ReportActionCommand(
                reportId,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }

    @Operation(summary = "发布病理报告", description = "发布已签发病理报告，并完成诊断任务闭环。")
    @RequirePermission(M4PermissionCodes.REPORT_PUBLISH)
    @PostMapping("/{id}/publish")
    public PathologyReportOperationResponse publish(@PathVariable("id") String reportId,
                                                    @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                    HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PathologyReportResult result = diagnosticReportAppService.publishReport(
            new DiagnosticReportModels.ReportActionCommand(
                reportId,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new PathologyReportOperationResponse(
            result.reportId(), result.caseId(), result.reportNo(), result.reportStatus(), result.versionNo(), result.versionStatus());
    }
}
