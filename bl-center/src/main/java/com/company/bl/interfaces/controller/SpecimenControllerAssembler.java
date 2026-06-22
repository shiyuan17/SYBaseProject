package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ApplicationPatientIdentityResolver;
import com.company.bl.application.service.OperatorVerificationService;
import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.application.service.SpecimenWorkflowModels;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.interfaces.dto.RegisterSpecimensRequest;
import com.company.bl.interfaces.dto.RetryLabelPrintRequest;
import com.company.bl.interfaces.dto.SpecimenBarcodeBindingRequest;
import com.company.bl.interfaces.dto.SpecimenCheckInRequest;
import com.company.bl.interfaces.dto.SpecimenConfirmRequest;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;
import static com.company.bl.application.service.SpecimenWorkflowQueryModels.*;

@Component
@RequiredArgsConstructor
class SpecimenControllerAssembler {

    private final ApplicationPatientIdentityResolver patientIdentityResolver;
    private final ApplicationRegistrationWorkbenchRepository workbenchRepository;
    private final OperatorVerificationService operatorVerificationService;

    RegisterSpecimensCommand toRegisterSpecimensCommand(RegisterSpecimensRequest request, HttpServletRequest httpServletRequest) {
        return new RegisterSpecimensCommand(
            request.getApplicationId(),
            request.getPrinterCode(),
            request.getCollectionScene(),
            resolveUserId(null, httpServletRequest),
            resolveOperatorName(null, httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks(),
            request.getItems().stream().map(item -> new SpecimenRegistrationItem(
                item.getSpecimenNameStandardized(),
                item.getSpecimenType(),
                item.getSpecimenSite(),
                item.getCollectionMode(),
                item.getSpecimenCount(),
                item.getContainerName(),
                item.getContainerCount(),
                item.getBarcode(),
                item.getClinicalSymptom())).toList());
    }

    RetryLabelPrintCommand toRetryLabelPrintCommand(String batchNo, RetryLabelPrintRequest request, HttpServletRequest httpServletRequest) {
        return new RetryLabelPrintCommand(
            batchNo,
            resolveUserId(null, httpServletRequest),
            resolveOperatorName(null, httpServletRequest),
            request.getPrinterCode(),
            request.getTerminalCode(),
            request.getRemarks());
    }

    SpecimenManagementListQuery toSpecimenManagementListQuery(int page,
                                                               int size,
                                                               String keyword,
                                                               String applicationNo,
                                                               String departmentId,
                                                               String buildingId,
                                                               String roomId,
                                                               String barcodeBindingStatus,
                                                               String specimenStatus,
                                                               String labelPrintStatus,
                                                               Boolean abnormalFlag,
                                                               String dateFrom,
                                                               String dateTo) {
        return new SpecimenManagementListQuery(
            page,
            size,
            keyword,
            applicationNo,
            departmentId,
            buildingId,
            roomId,
            barcodeBindingStatus,
            specimenStatus,
            labelPrintStatus,
            abnormalFlag,
            dateFrom,
            dateTo);
    }

    SpecimenBarcodeBindingCommand toBindSpecimenBarcodeCommand(String specimenId,
                                                               SpecimenBarcodeBindingRequest request,
                                                               HttpServletRequest httpServletRequest) {
        return new SpecimenBarcodeBindingCommand(
            specimenId,
            request.getTargetBarcode(),
            resolveUserId(null, httpServletRequest),
            resolveOperatorName(null, httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks());
    }

    SpecimenBarcodeUnbindCommand toUnbindSpecimenBarcodeCommand(String specimenId,
                                                                String terminalCode,
                                                                String remarks,
                                                                HttpServletRequest httpServletRequest) {
        return new SpecimenBarcodeUnbindCommand(
            specimenId,
            resolveUserId(null, httpServletRequest),
            resolveOperatorName(null, httpServletRequest),
            terminalCode,
            remarks);
    }

    ConfirmSpecimenCommand toConfirmSpecimenCommand(String barcode, SpecimenConfirmRequest request, HttpServletRequest httpServletRequest) {
        OperatorVerificationService.VerifiedOperator operator = resolveVerifiedOrCurrentOperator(
            request.getOperatorVerificationToken(),
            request.getOperatorUserId(),
            request.getOperatorName(),
            httpServletRequest);
        return new ConfirmSpecimenCommand(
            request.getSpecimenId(),
            request.getSpecimenBarcode() == null || request.getSpecimenBarcode().isBlank()
                ? barcode
                : request.getSpecimenBarcode(),
            request.getSpecimenNo(),
            operator.operatorUserId(),
            operator.operatorName(),
            request.getTerminalCode(),
            request.getRemarks());
    }

    CheckInSpecimenCommand toCheckInSpecimenCommand(String barcode, SpecimenCheckInRequest request, HttpServletRequest httpServletRequest) {
        OperatorVerificationService.VerifiedOperator operator = resolveVerifiedOrCurrentOperator(
            request.getOperatorVerificationToken(),
            request.getOperatorUserId(),
            request.getOperatorName(),
            httpServletRequest);
        return new CheckInSpecimenCommand(
            request.getSpecimenId(),
            request.getSpecimenBarcode() == null || request.getSpecimenBarcode().isBlank()
                ? barcode
                : request.getSpecimenBarcode(),
            request.getSpecimenNo(),
            operator.operatorUserId(),
            operator.operatorName(),
            request.getTerminalCode(),
            request.getRemarks());
    }

    SpecimenRegistrationResponse toSpecimenRegistrationResponse(SpecimenRegistrationResult result) {
        return new SpecimenRegistrationResponse(
            result.labelPrintBatchNo(),
            result.labelPrintSuccess(),
            result.labelPrintMessage(),
            result.specimens().stream().map(this::toSpecimenSummaryResponse).toList());
    }

    LabelPrintRetryResponse toLabelPrintRetryResponse(LabelPrintRetryResult result) {
        return new LabelPrintRetryResponse(
            result.labelPrintBatchNo(),
            result.retriedCount(),
            result.successCount(),
            result.failedCount(),
            result.allSuccessful(),
            result.message());
    }

    SpecimenManagementPageResponse toSpecimenManagementPageResponse(SpecimenManagementListPage result) {
        return new SpecimenManagementPageResponse(
            result.items().stream().map(this::toSpecimenManagementItemResponse).toList(),
            result.page(),
            result.size(),
            result.total(),
            new SpecimenManagementSummaryResponse(
                result.summary().totalCount(),
                result.summary().labelPrintedCount(),
                result.summary().pendingLabelCount(),
                result.summary().abnormalCount(),
                result.summary().unboundCount()));
    }

    ApplicationListItemResponse toApplicationListItemResponse(ApplicationListItem item) {
        return new ApplicationListItemResponse(
            item.id(),
            item.applicationNo(),
            item.pathologyNo(),
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
            item.specimenNos(),
            
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

    LatestSpecimenRegistrationResponse toLatestSpecimenRegistrationResponse(LatestSpecimenRegistrationResult result) {
        return new LatestSpecimenRegistrationResponse(
            result.applicationId(),
            result.labelPrintBatchNo(),
            result.labelPrintSuccess(),
            result.labelPrintMessage(),
            toRegistrationSnapshotResponse(result.registrationSnapshot()),
            result.specimens().stream().map(this::toSpecimenSummaryResponse).toList());
    }

    ApplicationDetailResponse toApplicationDetailResponse(ApplicationTracking tracking,
                                                          ApplicationOperationState operationState) {
        List<SpecimenSummaryResponse> specimenSummaries = tracking.specimens().stream().map(this::toSpecimenSummaryResponse).toList();
        Map<String, SpecimenSummaryResponse> specimenMap = specimenSummaries.stream()
            .collect(Collectors.toMap(SpecimenSummaryResponse::id, Function.identity()));
        String patientIdentifier = patientIdentityResolver.lookup(tracking.application().getPatientId())
            .map(ApplicationPatientIdentityResolver.PatientSummary::patientIdentifier)
            .orElse(tracking.application().getPatientId());
        String patientIdDisplay = workbenchRepository.findExtensionByApplicationId(tracking.application().getId().value())
            .map(ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData::idNo)
            .orElse(null);
        return new ApplicationDetailResponse(
            tracking.application().getId().value(),
            tracking.application().getApplicationNo(),
            tracking.application().getPatientId(),
            patientIdDisplay,
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

    List<SpecimenVerificationRecordResponse> toSpecimenVerificationRecordResponses(List<SpecimenWorkflowModels.SpecimenVerificationRecord> records) {
        return records.stream()
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

    SpecimenSummaryResponse toSpecimenSummaryResponse(Specimen specimen) {
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
            specimen.specimenSize(),
            specimen.registrationEvaluationItems(),
            specimen.containerName(),
            specimen.containerCount(),
            specimen.specimenStatus().name(),
            specimen.fixationStatus().name(),
            resolveVerificationStatus(specimen),
            stringify(specimen.verificationStartedAt()),
            stringify(specimen.verificationCompletedAt()),
            specimen.verifiedByName(),
            resolveBarcodeBindingStatus(specimen),
            specimen.labelPrintStatus(),
            stringify(specimen.specimenRemovalAt()),
            specimen.specimenRemovalOperatorName(),
            stringify(specimen.specimenConfirmedAt()),
            resolveCheckInStatus(specimen),
            stringify(specimen.checkedInAt()),
            specimen.checkedInByName(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            splitCommaSeparated(specimen.qualityIssueCodes()),
            specimen.registeredByName(),
            stringify(specimen.registeredAt()),
            specimen.terminalCode(),
            specimen.remarks(),
            resolveAbnormalType(specimen),
            specimen.unqualifiedReason());
    }

    private RegistrationSnapshotResponse toRegistrationSnapshotResponse(RegistrationSnapshot snapshot) {
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

    private SpecimenManagementItemResponse toSpecimenManagementItemResponse(SpecimenManagementListItem item) {
        return new SpecimenManagementItemResponse(
            item.specimenId(),
            item.specimenNo(),
            item.barcode(),
            item.applicationId(),
            item.applicationNo(),
            item.patientId(),
            item.patientIdDisplay(),
            item.patientName(),
            item.patientGender(),
            item.inpatientNo(),
            item.wardName(),
            item.submittingDepartmentId(),
            item.submittingDepartmentName(),
            item.buildingId(),
            item.roomId(),
            item.surgeryName(),
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
            item.specimenConfirmedByUserId(),
            item.specimenConfirmedByName(),
            stringify(item.specimenRemovalAt()),
            item.specimenRemovalOperatorName(),
            item.checkInStatus(),
            stringify(item.checkedInAt()),
            item.checkedInByName(),
            item.barcode() == null || item.barcode().isBlank() ? "UNBOUND" : "BOUND",
            item.labelPrintStatus(),
            item.labelPrintBatchNo(),
            resolveAbnormalType(item.specimenStatus(), item.fixationStatus(), item.abnormalFlag()),
            item.specimenStatus(),
            item.registrationOperatorName(),
            stringify(item.registeredAt()),
            stringify(item.latestTrackingAt()),
            item.abnormalFlag());
    }

    private TrackingEventResponse toTrackingEventResponse(com.company.bl.domain.model.TrackingEvent event,
                                                          Map<String, SpecimenSummaryResponse> specimenMap) {
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
            event.eventContent(),
            event.operatorIp(),
            event.operatorDevice());
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        if (bodyUserId != null && !bodyUserId.isBlank()) {
            return bodyUserId;
        }
        return RequestOperatorContext.currentUserId(request);
    }

    private String resolveOperatorName(String bodyOperatorName, HttpServletRequest request) {
        if (bodyOperatorName != null && !bodyOperatorName.isBlank()) {
            return bodyOperatorName;
        }
        return RequestOperatorContext.currentOperatorName(request);
    }

    private OperatorVerificationService.VerifiedOperator resolveVerifiedOperator(
        String operatorVerificationToken,
        HttpServletRequest request
    ) {
        return operatorVerificationService.resolveVerifiedOperator(
            operatorVerificationToken,
            RequestOperatorContext.currentUserId(request));
    }

    private OperatorVerificationService.VerifiedOperator resolveVerifiedOrCurrentOperator(
        String operatorVerificationToken,
        String bodyUserId,
        String bodyOperatorName,
        HttpServletRequest request
    ) {
        if (operatorVerificationToken == null || operatorVerificationToken.isBlank()) {
            String currentUserId = resolveUserId(null, request);
            String operatorName = currentUserId != null
                && bodyUserId != null
                && currentUserId.equals(bodyUserId.trim())
                ? resolveOperatorName(bodyOperatorName, request)
                : resolveOperatorName(null, request);
            return new OperatorVerificationService.VerifiedOperator(
                currentUserId,
                null,
                operatorName);
        }
        return resolveVerifiedOperator(operatorVerificationToken, request);
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

    private List<String> splitCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .toList();
    }
}
