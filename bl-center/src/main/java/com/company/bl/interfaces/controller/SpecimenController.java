package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.RegisterSpecimensRequest;
import com.company.bl.interfaces.dto.RetryLabelPrintRequest;
import com.company.bl.interfaces.vo.ApplicationDetailResponse;
import com.company.bl.interfaces.vo.LabelPrintRetryResponse;
import com.company.bl.interfaces.vo.SpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import com.company.bl.interfaces.vo.TrackingEventResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specimens")
@RequiredArgsConstructor
public class SpecimenController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/register")
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

    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/label-batches/{batchNo}/retry")
    public LabelPrintRetryResponse retryLabelPrint(@PathVariable("batchNo") String batchNo,
                                                   @Valid @RequestBody RetryLabelPrintRequest request,
                                                   HttpServletRequest httpServletRequest) {
        SpecimenWorkflowAppService.LabelPrintRetryResult result = specimenWorkflowAppService.retryLabelPrint(
            new SpecimenWorkflowAppService.RetryLabelPrintCommand(
                batchNo,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getPrinterCode(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new LabelPrintRetryResponse(
            result.labelPrintBatchNo(),
            result.retriedCount(),
            result.successCount(),
            result.failedCount(),
            result.allSuccessful(),
            result.message());
    }

    @RequirePermission(M2PermissionCodes.SPECIMEN_TRACKING_QUERY)
    @GetMapping("/barcodes/{barcode}/tracking")
    public ApplicationDetailResponse getTrackingByBarcode(@PathVariable("barcode") String barcode) {
        return toApplicationDetail(specimenWorkflowAppService.getTrackingByBarcode(barcode));
    }

    private ApplicationDetailResponse toApplicationDetail(ApplicationTracking tracking) {
        return new ApplicationDetailResponse(
            tracking.application().getId().value(),
            tracking.application().getApplicationNo(),
            tracking.application().getPatientId(),
            tracking.application().getPatientName(),
            tracking.application().getPatientGender(),
            tracking.application().getPatientAge(),
            tracking.application().getApplicationType(),
            tracking.application().getStatus().name(),
            tracking.application().getApplicationFormStatus().name(),
            tracking.application().getExternalOrderNo(),
            tracking.application().getThirdPartySource(),
            tracking.application().getSourceHospitalId(),
            tracking.application().getSourceHospitalName(),
            tracking.application().getSubmittingDepartmentId(),
            tracking.application().getSubmittingDepartmentName(),
            tracking.application().getSubmittingDoctorUserId(),
            tracking.application().getSubmittingDoctorName(),
            tracking.application().getClinicalDiagnosis(),
            tracking.application().getClinicalSymptom(),
            tracking.application().getSpecimenSite(),
            stringify(tracking.application().getApplicationDate()),
            stringify(tracking.application().getSubmissionDate()),
            tracking.currentNode(),
            tracking.abnormal(),
            tracking.specimens().stream().map(this::toSpecimenSummary).toList(),
            tracking.events().stream().map(event -> new TrackingEventResponse(
                event.nodeCode(),
                event.eventType(),
                event.eventStatus(),
                stringify(event.eventTime()),
                event.operatorName(),
                event.sourceTerminal(),
                event.eventContent()))
                .toList(),
            tracking.application().getRemarks(),
            stringify(tracking.application().getCreatedAt()),
            stringify(tracking.application().getUpdatedAt()));
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
