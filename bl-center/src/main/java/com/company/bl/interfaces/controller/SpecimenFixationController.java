package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SpecimenFixationRequest;
import com.company.bl.interfaces.vo.FixationResponse;
import com.company.bl.interfaces.vo.PendingSpecimenItemResponse;
import com.company.bl.interfaces.vo.PendingSpecimenPageResponse;
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
public class SpecimenFixationController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/start")
    public FixationResponse start(@Valid @RequestBody SpecimenFixationRequest request,
                                  HttpServletRequest httpServletRequest) {
        SpecimenWorkflowAppService.FixationResult result = specimenWorkflowAppService.startFixation(
            toCommand(request, httpServletRequest));
        return new FixationResponse(result.specimenId(), result.barcode(), result.fixationStatus());
    }

    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/complete")
    public FixationResponse complete(@Valid @RequestBody SpecimenFixationRequest request,
                                     HttpServletRequest httpServletRequest) {
        SpecimenWorkflowAppService.FixationResult result = specimenWorkflowAppService.completeFixation(
            toCommand(request, httpServletRequest));
        return new FixationResponse(result.specimenId(), result.barcode(), result.fixationStatus());
    }

    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @GetMapping("/pending")
    public PendingSpecimenPageResponse listPending(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) String applicationId,
                                                   @RequestParam(required = false) String departmentId,
                                                   @RequestParam(required = false) String dateFrom,
                                                   @RequestParam(required = false) String dateTo) {
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
        if (bodyUserId != null && !bodyUserId.isBlank()) {
            return bodyUserId.trim();
        }
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }
}
