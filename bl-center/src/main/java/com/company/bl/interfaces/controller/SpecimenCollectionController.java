package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.application.service.SpecimenWorkflowModels;
import com.company.bl.domain.model.Specimen;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.RegisterSpecimensRequest;
import com.company.bl.interfaces.vo.SpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specimen-collections")
@RequiredArgsConstructor
@Tag(name = "临床送检", description = "标本采集登记接口")
public class SpecimenCollectionController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @Operation(summary = "采集场景登记标本", description = "在采集场景下登记申请单标本并触发标签打印。")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "登记成功", useReturnTypeSchema = true))
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping
    public ResponseEntity<SpecimenRegistrationResponse> register(@Valid @RequestBody RegisterSpecimensRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        SpecimenWorkflowModels.SpecimenRegistrationResult result = specimenWorkflowAppService.registerSpecimens(
            new SpecimenWorkflowModels.RegisterSpecimensCommand(
                request.getApplicationId(),
                request.getPrinterCode(),
                request.getCollectionScene(),
                resolveUserId(null, httpServletRequest),
                resolveOperatorName(null, httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getItems().stream().map(item -> new SpecimenWorkflowModels.SpecimenRegistrationItem(
                    item.getSpecimenNameStandardized(),
                    item.getSpecimenType(),
                    item.getSpecimenSite(),
                    item.getCollectionMode(),
                    item.getSpecimenCount(),
                    item.getContainerName(),
                    item.getContainerCount(),
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
            specimen.collectionMode(),
            specimen.clinicalSymptom(),
            specimen.specimenCount(),
            specimen.containerName(),
            specimen.containerCount(),
            specimen.specimenStatus().name(),
            specimen.fixationStatus().name(),
            specimen.verificationStatus(),
            specimen.verificationStartedAt() == null ? null : specimen.verificationStartedAt().toString(),
            specimen.verificationCompletedAt() == null ? null : specimen.verificationCompletedAt().toString(),
            specimen.barcode() == null || specimen.barcode().isBlank() ? "UNBOUND" : "BOUND",
            specimen.labelPrintStatus(),
            specimen.specimenConfirmedAt() == null ? null : specimen.specimenConfirmedAt().toString(),
            specimen.checkInStatus() == null || specimen.checkInStatus().isBlank() ? "NOT_CHECKED_IN" : specimen.checkInStatus(),
            specimen.checkedInAt() == null ? null : specimen.checkedInAt().toString(),
            specimen.checkedInByName(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            splitCommaSeparated(specimen.qualityIssueCodes()),
            specimen.unqualifiedReason() == null || specimen.unqualifiedReason().isBlank() ? null : "QUALITY_EXCEPTION",
            specimen.unqualifiedReason());
    }

    private List<String> splitCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .toList();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        return RequestOperatorContext.currentUserId(request);
    }

    private String resolveOperatorName(String bodyOperatorName, HttpServletRequest request) {
        return RequestOperatorContext.currentOperatorName(request);
    }
}
