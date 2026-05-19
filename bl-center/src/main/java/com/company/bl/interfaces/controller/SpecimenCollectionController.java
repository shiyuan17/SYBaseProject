package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.domain.model.Specimen;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.RegisterSpecimensRequest;
import com.company.bl.interfaces.vo.SpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/specimen-collections")
@RequiredArgsConstructor
public class SpecimenCollectionController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping
    public ResponseEntity<SpecimenRegistrationResponse> register(@Valid @RequestBody RegisterSpecimensRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        SpecimenWorkflowAppService.SpecimenRegistrationResult result = specimenWorkflowAppService.registerSpecimens(
            new SpecimenWorkflowAppService.RegisterSpecimensCommand(
                request.getApplicationId(),
                request.getPrinterCode(),
                request.getCollectionScene(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getItems().stream().map(item -> new SpecimenWorkflowAppService.SpecimenRegistrationItem(
                    item.getSpecimenNameStandardized(),
                    item.getSpecimenType(),
                    item.getSpecimenSite(),
                    item.getCollectionMode(),
                    item.getSpecimenCount(),
                    item.getBarcode(),
                    item.getClinicalSymptom()))
                    .toList()));
        return ResponseEntity.status(201).body(new SpecimenRegistrationResponse(
            result.labelPrintBatchNo(),
            result.labelPrintSuccess(),
            result.labelPrintMessage(),
            result.specimens().stream().map(this::toSpecimenSummary).toList()));
    }

    private SpecimenSummaryResponse toSpecimenSummary(Specimen specimen) {
        return new SpecimenSummaryResponse(
            specimen.id(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenNameStandardized(),
            specimen.specimenType(),
            specimen.specimenSite(),
            specimen.specimenCount(),
            specimen.specimenStatus().name(),
            specimen.fixationStatus().name(),
            specimen.labelPrintStatus());
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        if (bodyUserId != null && !bodyUserId.isBlank()) {
            return bodyUserId.trim();
        }
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }
}
