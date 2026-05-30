package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.application.service.SpecimenWorkflowModels;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SpecimenCheckInRequest;
import com.company.bl.interfaces.dto.SpecimenConfirmRequest;
import com.company.bl.interfaces.dto.RegisterSpecimensRequest;
import com.company.bl.interfaces.dto.RetryLabelPrintRequest;
import com.company.bl.interfaces.vo.ApplicationDetailResponse;
import com.company.bl.interfaces.vo.ApplicationListItemResponse;
import com.company.bl.interfaces.vo.LabelPrintRetryResponse;
import com.company.bl.interfaces.vo.LatestSpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.RegistrationSnapshotResponse;
import com.company.bl.interfaces.vo.SpecimenManagementItemResponse;
import com.company.bl.interfaces.vo.SpecimenManagementPageResponse;
import com.company.bl.interfaces.vo.SpecimenManagementSummaryResponse;
import com.company.bl.interfaces.vo.SpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import com.company.bl.interfaces.vo.SpecimenVerificationRecordResponse;
import com.company.bl.interfaces.vo.TrackingEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/specimens")
@RequiredArgsConstructor
@Tag(name = "Clinical Specimen Workflow", description = "Specimen registration, management, label retry and tracking APIs")
public class SpecimenController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @Operation(summary = "Register specimens", description = "Register specimens under an application and attempt label printing.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Registration succeeded", useReturnTypeSchema = true))
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/register")
    public ResponseEntity<SpecimenRegistrationResponse> register(@Valid @RequestBody RegisterSpecimensRequest request,
                                                                HttpServletRequest httpServletRequest) {
        SpecimenWorkflowModels.SpecimenRegistrationResult result = specimenWorkflowAppService.registerSpecimens(
            new SpecimenWorkflowModels.RegisterSpecimensCommand(
                request.getApplicationId(),
                request.getPrinterCode(),
                request.getCollectionScene(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                resolveOperatorName(request.getOperatorName(), httpServletRequest),
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

    @Operation(summary = "Retry label printing", description = "Retry printing for a label batch.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/label-batches/{batchNo}/retry")
    public LabelPrintRetryResponse retryLabelPrint(
        @Parameter(description = "Label print batch number") @PathVariable("batchNo") String batchNo,
        @Valid @RequestBody RetryLabelPrintRequest request,
        HttpServletRequest httpServletRequest
    ) {
        SpecimenWorkflowModels.LabelPrintRetryResult result = specimenWorkflowAppService.retryLabelPrint(
            new SpecimenWorkflowModels.RetryLabelPrintCommand(
                batchNo,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                resolveOperatorName(request.getOperatorName(), httpServletRequest),
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

    @Operation(summary = "List specimen management items", description = "Query the specimen management workbench with filters and summary statistics.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping
    public SpecimenManagementPageResponse listSpecimens(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "applicationNo", required = false) String applicationNo,
        @RequestParam(value = "departmentId", required = false) String departmentId,
        @RequestParam(value = "specimenStatus", required = false) String specimenStatus,
        @RequestParam(value = "labelPrintStatus", required = false) String labelPrintStatus,
        @RequestParam(value = "abnormalFlag", required = false) Boolean abnormalFlag,
        @RequestParam(value = "dateFrom", required = false) String dateFrom,
        @RequestParam(value = "dateTo", required = false) String dateTo
    ) {
        SpecimenWorkflowModels.SpecimenManagementListPage result =
            specimenWorkflowAppService.listSpecimenManagementItems(
                new SpecimenWorkflowModels.SpecimenManagementListQuery(
                    page,
                    size,
                    keyword,
                    applicationNo,
                    departmentId,
                    specimenStatus,
                    labelPrintStatus,
                    abnormalFlag,
                    dateFrom,
                    dateTo));
        return new SpecimenManagementPageResponse(
            result.items().stream().map(this::toSpecimenManagementItem).toList(),
            result.page(),
            result.size(),
            result.total(),
            new SpecimenManagementSummaryResponse(
                result.summary().totalCount(),
                result.summary().labelPrintedCount(),
                result.summary().pendingLabelCount(),
                result.summary().abnormalCount()));
    }

    @Operation(summary = "Lookup application for registration", description = "Resolve registration context by application number.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping("/applications/lookup")
    public ApplicationListItemResponse lookupRegistrationApplication(
        @Parameter(description = "Application number") @RequestParam("applicationNo") String applicationNo
    ) {
        return toApplicationListItem(specimenWorkflowAppService.getRegistrationApplicationByApplicationNo(applicationNo));
    }

    @Operation(summary = "Get latest registration result", description = "Query the latest specimen registration result by application id.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping("/applications/{applicationId}/latest-registration")
    public LatestSpecimenRegistrationResponse getLatestRegistration(
        @Parameter(description = "Application id") @PathVariable("applicationId") String applicationId
    ) {
        SpecimenWorkflowModels.LatestSpecimenRegistrationResult result =
            specimenWorkflowAppService.getLatestRegistrationResult(applicationId);
        return new LatestSpecimenRegistrationResponse(
            result.applicationId(),
            result.labelPrintBatchNo(),
            result.labelPrintSuccess(),
            result.labelPrintMessage(),
            toRegistrationSnapshot(result.registrationSnapshot()),
            result.specimens().stream().map(this::toSpecimenSummary).toList());
    }

    @Operation(summary = "Get tracking by barcode", description = "Query application tracking data by specimen barcode.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_TRACKING_QUERY)
    @GetMapping("/barcodes/{barcode}/tracking")
    public ApplicationDetailResponse getTrackingByBarcode(
        @Parameter(description = "Specimen barcode") @PathVariable("barcode") String barcode
    ) {
        return toApplicationDetail(specimenWorkflowAppService.getTrackingByBarcode(barcode));
    }

    @Operation(summary = "List specimen verification records", description = "Query verification records by specimen barcode.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_TRACKING_QUERY)
    @GetMapping("/barcodes/{barcode}/verification-records")
    public List<SpecimenVerificationRecordResponse> listVerificationRecords(
        @Parameter(description = "Specimen barcode") @PathVariable("barcode") String barcode
    ) {
        return specimenWorkflowAppService.listSpecimenVerificationRecords(barcode).stream()
            .map(item -> new SpecimenVerificationRecordResponse(
                item.applicationId(),
                item.barcode(),
                item.operatorName(),
                item.remarks(),
                item.result(),
                item.specimenId(),
                item.terminalCode(),
                item.verificationType(),
                stringify(item.verifiedAt())))
            .toList();
    }

    @Operation(summary = "Confirm specimen", description = "Confirm a fixed specimen before check-in.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/barcodes/{barcode}/confirm")
    public SpecimenSummaryResponse confirm(
        @Parameter(description = "Specimen barcode") @PathVariable("barcode") String barcode,
        @Valid @RequestBody SpecimenConfirmRequest request,
        HttpServletRequest httpServletRequest
    ) {
        Specimen specimen = specimenWorkflowAppService.confirmSpecimen(
            new SpecimenWorkflowModels.ConfirmSpecimenCommand(
                barcode,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                resolveOperatorName(request.getOperatorName(), httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return toSpecimenSummary(specimen);
    }

    @Operation(summary = "Check in specimen", description = "Check in a confirmed specimen before transport.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/barcodes/{barcode}/check-in")
    public SpecimenSummaryResponse checkIn(
        @Parameter(description = "Specimen barcode") @PathVariable("barcode") String barcode,
        @Valid @RequestBody SpecimenCheckInRequest request,
        HttpServletRequest httpServletRequest
    ) {
        Specimen specimen = specimenWorkflowAppService.checkInSpecimen(
            new SpecimenWorkflowModels.CheckInSpecimenCommand(
                barcode,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                resolveOperatorName(request.getOperatorName(), httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return toSpecimenSummary(specimen);
    }

    private ApplicationDetailResponse toApplicationDetail(ApplicationTracking tracking) {
        List<SpecimenSummaryResponse> specimenSummaries = tracking.specimens().stream().map(this::toSpecimenSummary).toList();
        Map<String, SpecimenSummaryResponse> specimenMap = specimenSummaries.stream()
            .collect(Collectors.toMap(SpecimenSummaryResponse::id, Function.identity()));
        SpecimenWorkflowModels.ApplicationOperationState operationState =
            specimenWorkflowAppService.resolveApplicationOperationState(tracking.application());
        return new ApplicationDetailResponse(
            tracking.application().getId().value(),
            tracking.application().getApplicationNo(),
            tracking.application().getPatientId(),
            resolvePatientCheckStatus(tracking),
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
            stringify(tracking.application().getSpecimenRemovalTime()),
            resolveLatestEventTime(tracking, "FIXATION", "COMPLETED"),
            resolveLatestSpecimenConfirmedAt(tracking.specimens()),
            tracking.currentNode(),
            tracking.abnormal(),
            operationState.editable(),
            operationState.deletable(),
            operationState.voided(),
            operationState.disabledReason(),
            null,
            false,
            buildReceiptAbnormalSummary(tracking.specimens()),
            countUnreceivedSpecimens(tracking.specimens()),
            specimenSummaries,
            tracking.events().stream().map(event -> toTrackingEventResponse(event, specimenMap)).toList(),
            tracking.application().getRemarks(),
            stringify(tracking.application().getCreatedAt()),
            stringify(tracking.application().getUpdatedAt()));
    }

    private TrackingEventResponse toTrackingEventResponse(
        com.company.bl.domain.model.TrackingEvent event,
        Map<String, SpecimenSummaryResponse> specimenMap
    ) {
        SpecimenSummaryResponse specimen = event.specimenId() == null ? null : specimenMap.get(event.specimenId());
        return new TrackingEventResponse(
            event.nodeCode(),
            event.eventType(),
            event.eventStatus(),
            stringify(event.eventTime()),
            event.operatorName(),
            event.sourceTerminal(),
            event.specimenId(),
            specimen == null ? null : specimen.specimenNo(),
            specimen == null ? null : specimen.barcode(),
            event.eventContent());
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
            resolveVerificationStatus(specimen),
            stringify(specimen.verificationStartedAt()),
            stringify(specimen.verificationCompletedAt()),
            resolveBarcodeBindingStatus(specimen),
            specimen.labelPrintStatus(),
            stringify(specimen.specimenConfirmedAt()),
            resolveCheckInStatus(specimen),
            stringify(specimen.checkedInAt()),
            specimen.checkedInByName(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            splitCommaSeparated(specimen.qualityIssueCodes()),
            resolveAbnormalType(specimen),
            specimen.unqualifiedReason());
    }

    private RegistrationSnapshotResponse toRegistrationSnapshot(
        SpecimenWorkflowModels.RegistrationSnapshot snapshot
    ) {
        if (snapshot == null) {
            return null;
        }
        return new RegistrationSnapshotResponse(
            snapshot.collectionScene(),
            snapshot.operatorUserId(),
            snapshot.operatorName(),
            snapshot.printerCode(),
            snapshot.terminalCode(),
            snapshot.remarks());
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

    private SpecimenManagementItemResponse toSpecimenManagementItem(
        SpecimenWorkflowModels.SpecimenManagementListItem item
    ) {
        return new SpecimenManagementItemResponse(
            item.specimenId(),
            item.specimenNo(),
            item.barcode(),
            item.applicationId(),
            item.applicationNo(),
            item.patientName(),
            item.submittingDepartmentId(),
            item.submittingDepartmentName(),
            item.specimenName(),
            item.specimenType(),
            item.specimenSite(),
            item.specimenCount(),
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
            stringify(item.specimenConfirmedAt()),
            item.checkInStatus(),
            stringify(item.checkedInAt()),
            item.checkedInByName(),
            item.barcode() == null || item.barcode().isBlank() ? "UNBOUND" : "BOUND",
            item.labelPrintStatus(),
            item.labelPrintBatchNo(),
            resolveAbnormalType(item.specimenStatus(), item.fixationStatus(), item.abnormalFlag()),
            item.specimenStatus(),
            stringify(item.registeredAt()),
            stringify(item.latestTrackingAt()),
            item.abnormalFlag());
    }

    private ApplicationListItemResponse toApplicationListItem(
        SpecimenWorkflowModels.ApplicationListItem item
    ) {
        return new ApplicationListItemResponse(
            item.id(),
            item.applicationNo(),
            item.patientName(),
            item.patientGender(),
            item.patientAge(),
            item.status(),
            item.submittingDepartmentName(),
            item.submittingDoctorName(),
            item.applicationType(),
            item.applicationFormStatus(),
            item.currentNode(),
            item.abnormalFlag(),
            item.registeredSpecimenCount(),
            item.latestLabelPrintStatus(),
            item.editable(),
            item.deletable(),
            item.voided(),
            item.operationDisabledReason(),
            stringify(item.applicationDate()),
            stringify(item.submissionDate()),
            stringify(item.createdAt()),
            stringify(item.updatedAt()));
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
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

    private String resolvePatientCheckStatus(ApplicationTracking tracking) {
        return tracking.application().getPatientId() == null || tracking.application().getPatientId().isBlank()
            ? null
            : "PENDING";
    }

    private String resolveLatestEventTime(ApplicationTracking tracking, String nodeCode, String eventType) {
        return tracking.events().stream()
            .filter(event -> nodeCode.equals(event.nodeCode()) && eventType.equals(event.eventType()))
            .reduce((first, second) -> second)
            .map(event -> stringify(event.eventTime()))
            .orElse(null);
    }

    private String buildReceiptAbnormalSummary(List<Specimen> specimens) {
        long abnormalCount = specimens.stream()
            .filter(specimen -> resolveAbnormalType(specimen) != null)
            .count();
        if (abnormalCount == 0) {
            return null;
        }
        return "存在 " + abnormalCount + " 条标本处于异常或待回查状态";
    }

    private int countUnreceivedSpecimens(List<Specimen> specimens) {
        return (int) specimens.stream()
            .filter(specimen -> specimen.receiptStatus() == null || !"RECEIVED".equalsIgnoreCase(specimen.receiptStatus()))
            .count();
    }

    private String resolveVerificationStatus(Specimen specimen) {
        return specimen.verificationStatus();
    }

    private String resolveCheckInStatus(Specimen specimen) {
        return specimen.checkInStatus() == null || specimen.checkInStatus().isBlank()
            ? "NOT_CHECKED_IN"
            : specimen.checkInStatus();
    }

    private String resolveBarcodeBindingStatus(Specimen specimen) {
        return specimen.barcode() == null || specimen.barcode().isBlank() ? "UNBOUND" : "BOUND";
    }

    private String resolveLatestSpecimenConfirmedAt(List<Specimen> specimens) {
        return specimens.stream()
            .map(Specimen::specimenConfirmedAt)
            .filter(java.util.Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .map(this::stringify)
            .orElse(null);
    }

    private String resolveAbnormalType(Specimen specimen) {
        return resolveAbnormalType(
            specimen.specimenStatus() == null ? null : specimen.specimenStatus().name(),
            specimen.fixationStatus() == null ? null : specimen.fixationStatus().name(),
            specimen.unqualifiedReason() != null && !specimen.unqualifiedReason().isBlank()
                || specimen.qualityCheckResult() != null && "FAILED".equalsIgnoreCase(specimen.qualityCheckResult())
                || specimen.specimenStatus() != null && ("REJECTED".equals(specimen.specimenStatus().name()) || "RETURNED".equals(specimen.specimenStatus().name()))
        );
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
