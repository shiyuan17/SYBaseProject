package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.application.service.SpecimenWorkflowModels;
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
@Tag(name = "Clinical Submission", description = "Specimen fixation workflow APIs")
public class SpecimenFixationController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @Operation(summary = "Start fixation", description = "Advance specimen into fixing status.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/start")
    public FixationResponse start(@Valid @RequestBody SpecimenFixationRequest request,
                                  HttpServletRequest httpServletRequest) {
        SpecimenWorkflowModels.FixationResult result = specimenWorkflowAppService.startFixation(
            toCommand(request, httpServletRequest));
        return toFixationResponse(result);
    }

    @Operation(summary = "Complete fixation", description = "Advance specimen into fixation completed status.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/complete")
    public FixationResponse complete(@Valid @RequestBody SpecimenFixationRequest request,
                                     HttpServletRequest httpServletRequest) {
        SpecimenWorkflowModels.FixationResult result = specimenWorkflowAppService.completeFixation(
            toCommand(request, httpServletRequest));
        return toFixationResponse(result);
    }

    @Operation(summary = "List pending fixations", description = "Query pending fixation specimens with paging.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @GetMapping("/pending")
    public PendingSpecimenPageResponse listPending(
        @Parameter(description = "Page number, starting from 1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "Page size, default 20")
        @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "Application ID")
        @RequestParam(required = false) String applicationId,
        @Parameter(description = "Specimen serial number")
        @RequestParam(required = false) String specimenNo,
        @Parameter(description = "Submitting department ID")
        @RequestParam(required = false) String departmentId,
        @Parameter(description = "Fixation status")
        @RequestParam(required = false) String fixationStatus,
        @Parameter(description = "Verification status")
        @RequestParam(required = false) String verificationStatus,
        @Parameter(description = "Start date")
        @RequestParam(required = false) String dateFrom,
        @Parameter(description = "End date")
        @RequestParam(required = false) String dateTo
    ) {
        SpecimenWorkflowModels.PendingSpecimenPage result = specimenWorkflowAppService.listPendingFixations(
            new SpecimenWorkflowModels.PendingSpecimenQuery(
                page,
                size,
                applicationId,
                specimenNo,
                departmentId,
                fixationStatus,
                verificationStatus,
                dateFrom,
                dateTo));
        return new PendingSpecimenPageResponse(
            result.items().stream().map(this::toPendingItem).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    private SpecimenWorkflowModels.FixationCommand toCommand(SpecimenFixationRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        return new SpecimenWorkflowModels.FixationCommand(
            request.getSpecimenBarcode(),
            request.getFixationLiquidType(),
            resolveUserId(null, httpServletRequest),
            resolveOperatorName(null, httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks());
    }

    private FixationResponse toFixationResponse(SpecimenWorkflowModels.FixationResult result) {
        return new FixationResponse(
            result.specimenId(),
            result.barcode(),
            result.fixationStatus(),
            stringify(result.fixationCompletedAt()),
            result.operatorUserId(),
            result.operatorName(),
            result.fixationLiquidType());
    }

    private PendingSpecimenItemResponse toPendingItem(SpecimenWorkflowModels.PendingSpecimenItem item) {
        return new PendingSpecimenItemResponse(
            item.applicationId(),
            item.applicationNo(),
            item.patientName(),
            item.submittingDepartmentId(),
            item.submittingDepartmentName(),
            item.transportOrderId(),
            item.specimenId(),
            item.specimenNo(),
            item.barcode(),
            item.containerName(),
            item.containerCount(),
            item.specimenStatus(),
            item.fixationStatus(),
            stringify(item.fixationStartedAt()),
            stringify(item.fixationCompletedAt()),
            item.fixationLiquidType(),
            item.fixationOperatorUserId(),
            item.fixationOperatorName(),
            item.verificationStatus(),
            stringify(item.verificationStartedAt()),
            stringify(item.verificationCompletedAt()),
            stringify(item.specimenConfirmedAt()),
            item.checkInStatus(),
            stringify(item.checkedInAt()),
            item.checkedInByName(),
            resolveAbnormalType(item.specimenStatus(), item.fixationStatus(), item.abnormalFlag()),
            item.abnormalFlag() ? 1 : 0,
            "RECEIVED".equals(item.specimenStatus()) ? 0 : 1,
            item.abnormalFlag(),
            stringify(item.registeredAt()),
            stringify(item.latestTrackingAt()),
            item.abnormalFlag());
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        return RequestOperatorContext.currentUserId(request);
    }

    private String resolveOperatorName(String bodyOperatorName, HttpServletRequest request) {
        return RequestOperatorContext.currentOperatorName(request);
    }

    private String resolveAbnormalType(String specimenStatus, String fixationStatus, boolean abnormalFlag) {
        if ("REJECTED".equals(specimenStatus) || "RETURNED".equals(specimenStatus)) {
            return specimenStatus;
        }
        if ("ABNORMAL".equals(fixationStatus)) {
            return "FIXATION_ABNORMAL";
        }
        return abnormalFlag ? "WORKFLOW_ABNORMAL" : null;
    }
}
