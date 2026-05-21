package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateReportRevisionRequest;
import com.company.bl.interfaces.dto.ReviewReportRevisionRequest;
import com.company.bl.interfaces.vo.ReportRevisionOperationResponse;
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
@RequestMapping("/api/v1/report-revision-requests")
@Tag(name = "医生流程", description = "报告修订申请与审批接口")
public class ReportRevisionRequestController extends TechnicalControllerSupport {

    private final DiagnosticReportAppService diagnosticReportAppService;

    public ReportRevisionRequestController(DiagnosticReportAppService diagnosticReportAppService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
    }

    @Operation(summary = "创建报告修订申请", description = "对已签发或已发布报告发起修订申请。")
    @RequirePermission(M4PermissionCodes.REVISION_REQUEST_CREATE)
    @PostMapping
    public ReportRevisionOperationResponse create(@Valid @RequestBody CreateReportRevisionRequest request,
                                                  HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.ReportRevisionResult result = diagnosticReportAppService.createRevisionRequest(
            new DiagnosticReportModels.CreateReportRevisionRequestCommand(
                request.getReportId(),
                request.getRequestReason(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new ReportRevisionOperationResponse(
            result.requestId(),
            result.caseId(),
            result.reportId(),
            result.requestStatus(),
            result.approvedVersionNo());
    }

    @Operation(summary = "批准报告修订申请", description = "批准后当前报告进入新版本草稿。")
    @RequirePermission(M4PermissionCodes.REVISION_APPROVE)
    @PostMapping("/{id}/approve")
    public ReportRevisionOperationResponse approve(@PathVariable("id") String requestId,
                                                   @Valid @RequestBody ReviewReportRevisionRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.ReportRevisionResult result = diagnosticReportAppService.approveRevisionRequest(
            new DiagnosticReportModels.ReviewReportRevisionCommand(
                requestId,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getRejectReason()));
        return new ReportRevisionOperationResponse(
            result.requestId(),
            result.caseId(),
            result.reportId(),
            result.requestStatus(),
            result.approvedVersionNo());
    }

    @Operation(summary = "驳回报告修订申请", description = "驳回后当前报告状态保持不变。")
    @RequirePermission(M4PermissionCodes.REVISION_APPROVE)
    @PostMapping("/{id}/reject")
    public ReportRevisionOperationResponse reject(@PathVariable("id") String requestId,
                                                  @Valid @RequestBody ReviewReportRevisionRequest request,
                                                  HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.ReportRevisionResult result = diagnosticReportAppService.rejectRevisionRequest(
            new DiagnosticReportModels.ReviewReportRevisionCommand(
                requestId,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getRejectReason()));
        return new ReportRevisionOperationResponse(
            result.requestId(),
            result.caseId(),
            result.reportId(),
            result.requestStatus(),
            result.approvedVersionNo());
    }
}
