package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.application.service.SpecimenWorkflowModels;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SpecimenVerificationRequest;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specimen-verifications")
@RequiredArgsConstructor
@Tag(name = "Clinical Submission", description = "Specimen verification workflow APIs")
public class SpecimenVerificationController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @Operation(summary = "Start specimen verification", description = "Start the first step of specimen verification.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/start")
    public SpecimenSummaryResponse start(@Valid @RequestBody SpecimenVerificationRequest request,
                                         HttpServletRequest httpServletRequest) {
        SpecimenWorkflowModels.SpecimenVerificationResult result =
            specimenWorkflowAppService.startSpecimenVerification(toCommand(request, httpServletRequest));
        return toResponse(result);
    }

    @Operation(summary = "Complete specimen verification", description = "Complete the second step of specimen verification.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/complete")
    public SpecimenSummaryResponse complete(@Valid @RequestBody SpecimenVerificationRequest request,
                                            HttpServletRequest httpServletRequest) {
        SpecimenWorkflowModels.SpecimenVerificationResult result =
            specimenWorkflowAppService.completeSpecimenVerification(toCommand(request, httpServletRequest));
        return toResponse(result);
    }

    private SpecimenWorkflowModels.SpecimenVerificationCommand toCommand(
        SpecimenVerificationRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return new SpecimenWorkflowModels.SpecimenVerificationCommand(
            request.getSpecimenBarcode(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(request.getOperatorName(), httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks()
        );
    }

    private SpecimenSummaryResponse toResponse(SpecimenWorkflowModels.SpecimenVerificationResult result) {
        return new SpecimenSummaryResponse(
            result.id(),
            result.specimenNo(),
            result.barcode(),
            result.specimenName(),
            result.specimenType(),
            result.specimenSite(),
            result.collectionMode(),
            result.clinicalSymptom(),
            result.specimenCount(),
            result.containerName(),
            result.containerCount(),
            result.specimenStatus(),
            result.fixationStatus(),
            result.verificationStatus(),
            stringify(result.verificationStartedAt()),
            stringify(result.verificationCompletedAt()),
            result.barcode() == null || result.barcode().isBlank() ? "UNBOUND" : "BOUND",
            result.labelPrintStatus(),
            null,
            "NOT_CHECKED_IN",
            null,
            null,
            result.receiptStatus(),
            result.qualityCheckResult(),
            List.of(),
            result.abnormalReason() == null || result.abnormalReason().isBlank() ? null : "QUALITY_EXCEPTION",
            result.abnormalReason()
        );
    }

    private String resolveUserId(HttpServletRequest request) {
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }

    private String resolveOperatorName(String bodyOperatorName, HttpServletRequest request) {
        Object currentLoginName = request.getAttribute(ApiPermissionContext.CURRENT_LOGIN_NAME);
        if (currentLoginName instanceof String loginName && !loginName.isBlank()) {
            return loginName.trim();
        }
        return bodyOperatorName == null ? null : bodyOperatorName.trim();
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
