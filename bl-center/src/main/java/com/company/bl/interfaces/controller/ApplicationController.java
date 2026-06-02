package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ApplicationPatientIdentityResolver;
import com.company.bl.application.service.CreateApplicationAppService;
import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.application.service.SpecimenWorkflowQueryModels;
import com.company.bl.application.service.UpdateApplicationAppService;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.interfaces.assembler.ApplicationRepresentationAssembler;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateApplicationRequest;
import com.company.bl.interfaces.dto.UpdateApplicationRequest;
import com.company.bl.interfaces.vo.ApplicationDetailResponse;
import com.company.bl.interfaces.vo.ApplicationDuplicateCheckItemResponse;
import com.company.bl.interfaces.vo.ApplicationDuplicateCheckResponse;
import com.company.bl.interfaces.vo.ApplicationIdResponse;
import com.company.bl.interfaces.vo.ApplicationListItemResponse;
import com.company.bl.interfaces.vo.ApplicationPatientLookupResponse;
import com.company.bl.interfaces.vo.ApplicationPageResponse;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import com.company.bl.interfaces.vo.TrackingEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "临床送检", description = "病理申请单创建、详情查询与流程追踪接口")
public class ApplicationController {

    private final CreateApplicationAppService createApplicationAppService;
    private final UpdateApplicationAppService updateApplicationAppService;
    private final SpecimenWorkflowAppService specimenWorkflowAppService;
    private final ApplicationRepresentationAssembler applicationRepresentationAssembler;
    private final ApplicationPatientIdentityResolver patientIdentityResolver;

    @Operation(summary = "创建病理申请单", description = "创建新的病理申请单。")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "创建成功", useReturnTypeSchema = true))
    @RequirePermission(M2PermissionCodes.APPLICATION_CREATE)
    @PostMapping
    public ResponseEntity<ApplicationIdResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationIdResponse response = applicationRepresentationAssembler.toIdResponse(
            createApplicationAppService.create(applicationRepresentationAssembler.toCommand(request)));
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "更新病理申请单", description = "更新未进入下游流程的病理申请单。")
    @RequirePermission(M2PermissionCodes.APPLICATION_UPDATE)
    @PatchMapping("/{id}")
    public ApplicationIdResponse update(
        @Parameter(description = "申请单 ID") @PathVariable("id") String id,
        @Valid @RequestBody UpdateApplicationRequest request
    ) {
        return applicationRepresentationAssembler.toIdResponse(
            updateApplicationAppService.update(id, applicationRepresentationAssembler.toCommand(request)).getId());
    }

    @Operation(summary = "作废病理申请单", description = "逻辑作废未进入下游流程的病理申请单。")
    @RequirePermission(M2PermissionCodes.APPLICATION_DELETE)
    @DeleteMapping("/{id}")
    public ApplicationIdResponse delete(
        @Parameter(description = "申请单 ID") @PathVariable("id") String id
    ) {
        return applicationRepresentationAssembler.toIdResponse(
            updateApplicationAppService.voidApplication(id).getId());
    }

    @Operation(summary = "分页查询申请单", description = "按筛选条件分页查询申请单列表。")
    @RequirePermission(M2PermissionCodes.APPLICATION_DETAIL_QUERY)
    @GetMapping
    public ApplicationPageResponse list(
        @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "申请单号，模糊匹配") @RequestParam(required = false) String applicationNo,
        @Parameter(description = "患者姓名，模糊匹配") @RequestParam(required = false) String patientName,
        @Parameter(description = "送检科室 ID") @RequestParam(required = false) String submittingDepartmentId,
        @Parameter(description = "申请类型") @RequestParam(required = false) String applicationType,
        @Parameter(description = "申请单表单状态") @RequestParam(required = false) String applicationFormStatus,
        @Parameter(description = "申请开始日期") @RequestParam(required = false) String dateFrom,
        @Parameter(description = "申请结束日期") @RequestParam(required = false) String dateTo
    ) {
        SpecimenWorkflowQueryModels.ApplicationPage result =
            specimenWorkflowAppService.listApplications(
                new SpecimenWorkflowQueryModels.ApplicationListQuery(
                    page,
                    size,
                    applicationNo,
                    patientName,
                    submittingDepartmentId,
                    applicationType,
                    applicationFormStatus,
                    dateFrom,
                    dateTo));
        return new ApplicationPageResponse(
            result.items().stream().map(this::toListItemResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "重复申请预警", description = "按患者标识、外部申请号或同日同部位条件检查疑似重复申请。")
    @RequirePermission(M2PermissionCodes.APPLICATION_CREATE)
    @GetMapping("/duplicate-check")
    public ApplicationDuplicateCheckResponse duplicateCheck(
        @Parameter(description = "患者 ID") @RequestParam(required = false) String patientId,
        @Parameter(description = "患者姓名") @RequestParam(required = false) String patientName,
        @Parameter(description = "外部申请号") @RequestParam(required = false) String externalOrderNo,
        @Parameter(description = "申请日期") @RequestParam(required = false) LocalDate applicationDate,
        @Parameter(description = "申请类型") @RequestParam(required = false) String applicationType,
        @Parameter(description = "送检部位") @RequestParam(required = false) String specimenSite
    ) {
        SpecimenWorkflowQueryModels.DuplicateCheckResult result = specimenWorkflowAppService.checkApplicationDuplicate(
            new SpecimenWorkflowQueryModels.DuplicateCheckCommand(
                patientId,
                patientName,
                externalOrderNo,
                applicationDate == null ? null : applicationDate.toString(),
                applicationType,
                specimenSite));
        List<ApplicationDuplicateCheckItemResponse> items = result.items().stream()
            .map(item -> new ApplicationDuplicateCheckItemResponse(
                item.id(),
                item.applicationNo(),
                item.patientName(),
                stringify(item.applicationDate()),
                item.specimenSite(),
                item.status(),
                item.currentNode(),
                item.matchedBy()))
            .toList();
        return new ApplicationDuplicateCheckResponse(items, result.suggestedAction());
    }

    @Operation(summary = "查询申请单详情", description = "按申请单 ID 查询申请单、标本与最近追踪事件。")
    @RequirePermission(M2PermissionCodes.APPLICATION_DETAIL_QUERY)
    @GetMapping("/{id}")
    public ApplicationDetailResponse getById(@Parameter(description = "申请单 ID") @PathVariable("id") String id) {
        ApplicationTracking tracking = specimenWorkflowAppService.getApplicationTracking(id);
        return toDetailResponse(tracking);
    }

    @Operation(summary = "按患者标识查询患者", description = "按患者编号、住院号、门诊号或患者主键查询患者基础信息。")
    @RequirePermission(M2PermissionCodes.APPLICATION_CREATE)
    @GetMapping("/patient-lookup")
    public ApplicationPatientLookupResponse lookupPatient(
        @Parameter(description = "患者编号、住院号、门诊号或患者主键 ID")
        @RequestParam("identifier") String identifier
    ) {
        ApplicationPatientIdentityResolver.PatientSummary patient = patientIdentityResolver.lookup(identifier)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "未找到对应患者"));
        return new ApplicationPatientLookupResponse(
            patient.patientId(),
            patient.patientIdentifier(),
            patient.patientName(),
            patient.patientGender(),
            patient.patientAge());
    }

    @Operation(summary = "查询申请单追踪", description = "查询申请单当前节点、标本列表与追踪事件。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_TRACKING_QUERY)
    @GetMapping("/{id}/tracking")
    public ApplicationDetailResponse getTracking(@Parameter(description = "申请单 ID") @PathVariable("id") String id) {
        return toDetailResponse(specimenWorkflowAppService.getApplicationTracking(id));
    }

    private ApplicationDetailResponse toDetailResponse(ApplicationTracking tracking) {
        List<SpecimenSummaryResponse> specimenSummaries = tracking.specimens().stream().map(this::toSpecimenSummary).toList();
        Map<String, SpecimenSummaryResponse> specimenMap = specimenSummaries.stream()
            .collect(Collectors.toMap(SpecimenSummaryResponse::id, Function.identity()));
        SpecimenWorkflowQueryModels.ApplicationOperationState operationState =
            specimenWorkflowAppService.resolveApplicationOperationState(tracking.application());
        String patientIdentifier = patientIdentityResolver.lookup(tracking.application().getPatientId())
            .map(ApplicationPatientIdentityResolver.PatientSummary::patientIdentifier)
            .orElse(tracking.application().getPatientId());
        return new ApplicationDetailResponse(
            tracking.application().getId().value(),
            tracking.application().getApplicationNo(),
            tracking.application().getPatientId(),
            patientIdentifier,
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

    private ApplicationListItemResponse toListItemResponse(SpecimenWorkflowQueryModels.ApplicationListItem item) {
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

    private List<String> splitCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .toList();
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
        if (specimen.specimenStatus() != null && ("REJECTED".equals(specimen.specimenStatus().name()) || "RETURNED".equals(specimen.specimenStatus().name()))) {
            return specimen.specimenStatus().name();
        }
        if (specimen.fixationStatus() != null && "ABNORMAL".equals(specimen.fixationStatus().name())) {
            return "FIXATION_ABNORMAL";
        }
        if (specimen.qualityCheckResult() != null && "FAILED".equalsIgnoreCase(specimen.qualityCheckResult())) {
            return "QUALITY_CHECK_FAILED";
        }
        if (specimen.unqualifiedReason() != null && !specimen.unqualifiedReason().isBlank()) {
            return "QUALITY_EXCEPTION";
        }
        return null;
    }
}
