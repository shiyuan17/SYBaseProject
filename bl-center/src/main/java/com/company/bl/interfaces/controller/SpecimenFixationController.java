package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SpecimenFixationRequest;
import com.company.bl.interfaces.vo.FixationResponse;
import com.company.bl.interfaces.vo.PendingSpecimenItemResponse;
import com.company.bl.interfaces.vo.PendingSpecimenPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/specimen-fixations")
@RequiredArgsConstructor
@Tag(name = "临床送检", description = "标本固定开始、完成与待处理查询接口")
public class SpecimenFixationController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @Operation(summary = "开始固定", description = "将标本推进到固定开始状态。")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/start")
    public FixationResponse start(@Valid @RequestBody SpecimenFixationRequest request,
                                  HttpServletRequest httpServletRequest) {
        SpecimenWorkflowAppService.FixationResult result = specimenWorkflowAppService.startFixation(
            toCommand(request, httpServletRequest));
        return new FixationResponse(result.specimenId(), result.barcode(), result.fixationStatus());
    }

    @Operation(summary = "完成固定", description = "将标本推进到固定完成状态。")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/complete")
    public FixationResponse complete(@Valid @RequestBody SpecimenFixationRequest request,
                                     HttpServletRequest httpServletRequest) {
        SpecimenWorkflowAppService.FixationResult result = specimenWorkflowAppService.completeFixation(
            toCommand(request, httpServletRequest));
        return new FixationResponse(result.specimenId(), result.barcode(), result.fixationStatus());
    }

    @Operation(summary = "查询待固定标本", description = "分页查询当前待固定处理的标本列表。")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @GetMapping("/pending")
    public PendingSpecimenPageResponse listPending(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                   @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
                                                   @Parameter(description = "申请单 ID") @RequestParam(required = false) String applicationId,
                                                   @Parameter(description = "送检科室 ID") @RequestParam(required = false) String departmentId,
                                                   @Parameter(description = "开始日期") @RequestParam(required = false) String dateFrom,
                                                   @Parameter(description = "结束日期") @RequestParam(required = false) String dateTo) {
        SpecimenWorkflowAppService.PendingSpecimenPage result = specimenWorkflowAppService.listPendingFixations(
            new SpecimenWorkflowAppService.PendingSpecimenQuery(page, size, applicationId, departmentId, dateFrom, dateTo));
        return new PendingSpecimenPageResponse(
            result.items().stream().map(this::toPendingItem).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    private SpecimenWorkflowAppService.FixationCommand toCommand(SpecimenFixationRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        return new SpecimenWorkflowAppService.FixationCommand(
            request.getSpecimenBarcode(),
            request.getFixationLiquidType(),
            resolveUserId(request.getOperatorUserId(), httpServletRequest),
            request.getOperatorName(),
            request.getTerminalCode(),
            request.getRemarks());
    }

    private PendingSpecimenItemResponse toPendingItem(SpecimenWorkflowAppService.PendingSpecimenItem item) {
        return new PendingSpecimenItemResponse(
            item.applicationId(),
            item.applicationNo(),
            item.patientName(),
            item.submittingDepartmentId(),
            item.submittingDepartmentName(),
            item.specimenId(),
            item.specimenNo(),
            item.barcode(),
            item.specimenStatus(),
            item.fixationStatus(),
            stringify(item.registeredAt()),
            stringify(item.latestTrackingAt()),
            item.abnormalFlag());
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }
}
