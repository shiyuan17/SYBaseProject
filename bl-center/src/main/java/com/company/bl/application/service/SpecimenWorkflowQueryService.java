package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import com.company.common.web.observability.ObservedOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;

@Service
class SpecimenWorkflowQueryService {

    private final ApplicationRepository applicationRepository;
    private final SpecimenWorkflowQueryRepository specimenWorkflowRepository;
    private final SpecimenWorkflowSupport specimenWorkflowSupport;

    SpecimenWorkflowQueryService(ApplicationRepository applicationRepository,
                                 SpecimenWorkflowQueryRepository specimenWorkflowRepository,
                                 SpecimenWorkflowSupport specimenWorkflowSupport) {
        this.applicationRepository = applicationRepository;
        this.specimenWorkflowRepository = specimenWorkflowRepository;
        this.specimenWorkflowSupport = specimenWorkflowSupport;
    }

    @Transactional(readOnly = true)
    PendingSpecimenPage listPendingFixations(PendingSpecimenQuery query) {
        SpecimenWorkflowRepository.PagedPendingSpecimens page = specimenWorkflowRepository.findPendingFixations(
            new SpecimenWorkflowRepository.PendingSpecimenQuery(
                specimenWorkflowSupport.normalizePage(query.page()),
                specimenWorkflowSupport.normalizeSize(query.size()),
                specimenWorkflowSupport.trim(query.applicationId()),
                specimenWorkflowSupport.trim(query.specimenNo()),
                specimenWorkflowSupport.trim(query.departmentId()),
                specimenWorkflowSupport.trim(query.fixationStatus()),
                specimenWorkflowSupport.trim(query.verificationStatus()),
                specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return new PendingSpecimenPage(
            page.items().stream().map(specimenWorkflowSupport::toPendingItem).toList(),
            specimenWorkflowSupport.normalizePage(query.page()),
            specimenWorkflowSupport.normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    PendingSpecimenPage listPendingReceipts(PendingSpecimenQuery query) {
        SpecimenWorkflowRepository.PagedPendingSpecimens page = specimenWorkflowRepository.findPendingReceipts(
            new SpecimenWorkflowRepository.PendingSpecimenQuery(
                specimenWorkflowSupport.normalizePage(query.page()),
                specimenWorkflowSupport.normalizeSize(query.size()),
                specimenWorkflowSupport.trim(query.applicationId()),
                specimenWorkflowSupport.trim(query.specimenNo()),
                specimenWorkflowSupport.trim(query.departmentId()),
                null,
                null,
                specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return new PendingSpecimenPage(
            page.items().stream().map(specimenWorkflowSupport::toPendingItem).toList(),
            specimenWorkflowSupport.normalizePage(query.page()),
            specimenWorkflowSupport.normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    List<SpecimenVerificationRecord> listSpecimenVerificationRecords(String barcode) {
        if (specimenWorkflowSupport.blank(barcode)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen barcode is required");
        }
        return specimenWorkflowRepository.listSpecimenVerificationRecords(specimenWorkflowSupport.trim(barcode)).stream()
            .map(row -> new SpecimenVerificationRecord(
                row.applicationId(),
                row.specimenId(),
                row.barcode(),
                row.verificationType(),
                row.result(),
                row.operatorName(),
                row.terminalCode(),
                row.remarks(),
                row.verifiedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    PendingTransportOrderPage listPendingTransportOrders(PendingTransportOrderQuery query) {
        SpecimenWorkflowRepository.PagedPendingTransportOrders page =
            specimenWorkflowRepository.findPendingTransportOrders(
                new SpecimenWorkflowRepository.PendingTransportOrderQuery(
                    specimenWorkflowSupport.normalizePage(query.page()),
                    specimenWorkflowSupport.normalizeSize(query.size()),
                    specimenWorkflowSupport.trim(query.applicationId()),
                    specimenWorkflowSupport.trim(query.specimenNo()),
                    specimenWorkflowSupport.trim(query.departmentId()),
                    specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseDateTo(query.dateTo()),
                    specimenWorkflowSupport.normalizeStatus(query.status())));
        return new PendingTransportOrderPage(
            page.items().stream().map(item -> new PendingTransportOrderItem(
                item.id(),
                item.transportOrderNo(),
                item.applicationId(),
                item.applicationNo(),
                item.patientName(),
                item.handoverDepartmentName(),
                item.receiverDepartmentName(),
                item.status(),
                item.toBeTransportedAt(),
                item.handedOverAt(),
                specimenWorkflowRepository.findTransportOrderSpecimenBarcodes(item.id())))
                .toList(),
            specimenWorkflowSupport.normalizePage(query.page()),
            specimenWorkflowSupport.normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    ApplicationPage listApplications(ApplicationListQuery query) {
        int page = specimenWorkflowSupport.normalizePage(query.page());
        int size = specimenWorkflowSupport.normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedApplications result =
            specimenWorkflowRepository.findApplications(
                new SpecimenWorkflowRepository.ApplicationListQuery(
                    page,
                    size,
                    specimenWorkflowSupport.trim(query.applicationNo()),
                    specimenWorkflowSupport.trim(query.patientName()),
                    specimenWorkflowSupport.trim(query.submittingDepartmentId()),
                    specimenWorkflowSupport.normalizeStatus(query.applicationType()),
                    specimenWorkflowSupport.normalizeStatus(query.applicationFormStatus()),
                    specimenWorkflowSupport.parseLocalDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseLocalDateTo(query.dateTo())));
        return new ApplicationPage(
            result.items().stream().map(item -> new ApplicationListItem(
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
                item.applicationDate(),
                item.submissionDate(),
                item.createdAt(),
                item.updatedAt()))
                .toList(),
            page,
            size,
            result.total());
    }

    @Transactional(readOnly = true)
    DuplicateCheckResult checkApplicationDuplicate(DuplicateCheckCommand command) {
        String patientId = specimenWorkflowSupport.trim(command.patientId());
        String patientName = specimenWorkflowSupport.trim(command.patientName());
        String externalOrderNo = specimenWorkflowSupport.trim(command.externalOrderNo());
        LocalDate applicationDate = specimenWorkflowSupport.parseLocalDate(command.applicationDate());
        String applicationType = specimenWorkflowSupport.normalizeStatus(command.applicationType());
        String specimenSite = specimenWorkflowSupport.trim(command.specimenSite());
        if (patientId == null && patientName == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Patient id or patient name is required");
        }
        if (externalOrderNo == null && applicationDate == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "External order number or application date is required");
        }
        List<DuplicateCheckItem> items = specimenWorkflowRepository.findDuplicateApplications(
                new SpecimenWorkflowRepository.DuplicateApplicationQuery(
                    patientId,
                    patientName,
                    externalOrderNo,
                    applicationDate,
                    applicationType,
                    specimenSite))
            .stream()
            .map(item -> new DuplicateCheckItem(
                item.id(),
                item.applicationNo(),
                item.patientName(),
                item.applicationDate(),
                item.specimenSite(),
                item.status(),
                item.currentNode(),
                resolveMatchedBy(item.externalOrderMatched(), item.sameDaySiteMatched())))
            .toList();
        String suggestedAction = items.stream().anyMatch(item -> item.matchedBy().contains("EXTERNAL_ORDER_NO"))
            ? "BLOCK"
            : items.isEmpty() ? "ALLOW" : "CONFIRM";
        return new DuplicateCheckResult(items, suggestedAction);
    }

    @Transactional(readOnly = true)
    SpecimenManagementListPage listSpecimenManagementItems(SpecimenManagementListQuery query) {
        int page = specimenWorkflowSupport.normalizePage(query.page());
        int size = specimenWorkflowSupport.normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedSpecimenManagementItems result =
            specimenWorkflowRepository.findSpecimenManagementItems(
                new SpecimenWorkflowRepository.SpecimenManagementListQuery(
                    page,
                    size,
                    specimenWorkflowSupport.trim(query.keyword()),
                    specimenWorkflowSupport.trim(query.applicationNo()),
                    specimenWorkflowSupport.trim(query.departmentId()),
                    specimenWorkflowSupport.normalizeStatus(query.specimenStatus()),
                    specimenWorkflowSupport.normalizeStatus(query.labelPrintStatus()),
                    query.abnormalFlag(),
                    specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return new SpecimenManagementListPage(
            result.items().stream().map(item -> new SpecimenManagementListItem(
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
                item.fixationStartedAt(),
                item.fixationCompletedAt(),
                item.fixationLiquidType(),
                item.fixationOperatorUserId(),
                item.fixationOperatorName(),
                item.verificationStatus(),
                item.specimenConfirmedAt(),
                item.checkInStatus(),
                item.checkedInAt(),
                item.checkedInByName(),
                item.labelPrintStatus(),
                item.labelPrintBatchNo(),
                item.registeredAt(),
                item.latestTrackingAt(),
                item.abnormalFlag()))
                .toList(),
            page,
            size,
            result.total(),
            new SpecimenManagementSummary(
                result.summary().totalCount(),
                result.summary().labelPrintedCount(),
                result.summary().pendingLabelCount(),
                result.summary().abnormalCount()));
    }

    @Transactional(readOnly = true)
    SpecimenRemovalListPage listSpecimenRemovalItems(SpecimenRemovalQuery query) {
        int page = specimenWorkflowSupport.normalizePage(query.page());
        int size = specimenWorkflowSupport.normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedSpecimenRemovalItems result =
            specimenWorkflowRepository.findSpecimenRemovalItems(
                new SpecimenWorkflowRepository.SpecimenRemovalListQuery(
                    page,
                    size,
                    specimenWorkflowSupport.trim(query.keyword()),
                    specimenWorkflowSupport.trim(query.applicationNo()),
                    specimenWorkflowSupport.trim(query.departmentId()),
                    specimenWorkflowSupport.normalizeStatus(query.specimenStatus()),
                    query.abnormalFlag(),
                    specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return new SpecimenRemovalListPage(
            result.items().stream().map(item -> new SpecimenRemovalListItem(
                item.specimenId(),
                item.specimenNo(),
                item.barcode(),
                item.applicationId(),
                item.applicationNo(),
                item.patientName(),
                item.patientGender(),
                item.inpatientNo(),
                item.surgeryName(),
                item.submittingDepartmentId(),
                item.submittingDepartmentName(),
                item.specimenName(),
                item.specimenType(),
                item.specimenCount(),
                item.containerName(),
                item.containerCount(),
                item.specimenStatus(),
                item.fixationStatus(),
                item.verificationStatus(),
                item.specimenRemovalAt(),
                item.specimenRemovalOperatorName(),
                item.registeredAt(),
                item.labelPrintBatchNo(),
                item.registeredByName(),
                item.latestTrackingAt(),
                item.abnormalFlag()))
                .toList(),
            page,
            size,
            result.total(),
            new SpecimenRemovalSummary(
                result.summary().totalCount(),
                result.summary().confirmedCount(),
                result.summary().pendingCount(),
                result.summary().abnormalCount()));
    }

    @Transactional(readOnly = true)
    byte[] exportSpecimenRemovalItems(SpecimenRemovalQuery query) {
        List<SpecimenWorkflowRepository.SpecimenRemovalListRow> rows = specimenWorkflowRepository.listSpecimenRemovalExportRows(
            new SpecimenWorkflowRepository.SpecimenRemovalListQuery(
                specimenWorkflowSupport.normalizePage(query.page()),
                specimenWorkflowSupport.normalizeSize(query.size()),
                specimenWorkflowSupport.trim(query.keyword()),
                specimenWorkflowSupport.trim(query.applicationNo()),
                specimenWorkflowSupport.trim(query.departmentId()),
                specimenWorkflowSupport.normalizeStatus(query.specimenStatus()),
                query.abnormalFlag(),
                specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return specimenWorkflowSupport.buildSpecimenRemovalExport(rows);
    }

    @Transactional(readOnly = true)
    @ObservedOperation(
        operation = "get_application",
        successCounter = "application_query_total",
        failureCounter = "application_query_failed_total",
        durationMetric = "application_query_duration")
    ApplicationTracking getApplicationTracking(String applicationId) {
        Application application = specimenWorkflowSupport.getApplication(applicationId);
        return specimenWorkflowRepository.getApplicationTracking(applicationId, application);
    }

    @Transactional(readOnly = true)
    ApplicationTracking getTrackingByBarcode(String barcode) {
        String applicationId = specimenWorkflowRepository.findApplicationIdByBarcode(barcode)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen barcode not found"));
        return getApplicationTracking(applicationId);
    }

    @Transactional(readOnly = true)
    LatestSpecimenRegistrationResult getLatestRegistrationResult(String applicationId) {
        specimenWorkflowSupport.getApplication(applicationId);
        List<Specimen> specimens = specimenWorkflowRepository.findSpecimensByApplicationId(applicationId);
        if (specimens.isEmpty()) {
            return new LatestSpecimenRegistrationResult(applicationId, List.of(), null, false, null, null);
        }
        String latestBatchNo = specimenWorkflowSupport.resolveLatestLabelPrintBatchNo(specimens);
        if (latestBatchNo == null) {
            return new LatestSpecimenRegistrationResult(applicationId, List.of(), null, false, null, null);
        }
        List<Specimen> batchSpecimens = specimens.stream()
            .filter(specimen -> latestBatchNo.equals(specimen.labelPrintBatchNo()))
            .toList();
        List<TrackingEvent> trackingEvents = specimenWorkflowRepository.findTrackingEventsByApplicationId(applicationId);
        String latestPrintMessage = specimenWorkflowSupport.resolveLatestBatchLabelPrintMessage(trackingEvents, batchSpecimens);
        RegistrationSnapshot snapshot = specimenWorkflowRepository
            .findRegistrationSnapshotByApplicationIdAndBatchNo(applicationId, latestBatchNo)
            .map(item -> new RegistrationSnapshot(
                item.collectionScene(),
                item.operatorUserId(),
                item.operatorName(),
                item.printerCode(),
                item.terminalCode(),
                item.remarks()))
            .orElse(null);
        boolean labelPrintSuccess = batchSpecimens.stream()
            .allMatch(specimen -> "SUCCESS".equalsIgnoreCase(specimen.labelPrintStatus()));
        return new LatestSpecimenRegistrationResult(
            applicationId,
            batchSpecimens,
            latestBatchNo,
            labelPrintSuccess,
            latestPrintMessage,
            snapshot);
    }

    @Transactional(readOnly = true)
    ApplicationListItem getRegistrationApplicationByApplicationNo(String applicationNo) {
        if (specimenWorkflowSupport.blank(applicationNo)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Application number is required");
        }
        return applicationRepository.findByApplicationNo(applicationNo.trim())
            .map(application -> specimenWorkflowSupport.toApplicationListItem(specimenWorkflowRepository.getApplicationTracking(
                application.getId().value(),
                application)))
            .orElseThrow(() -> new ApplicationDomainException(com.company.bl.domain.enums.ApplicationErrorCode.APPLICATION_NOT_FOUND, 404));
    }

    ApplicationOperationState resolveApplicationOperationState(Application application) {
        return specimenWorkflowSupport.resolveApplicationOperationState(application);
    }

    private List<String> resolveMatchedBy(boolean externalOrderMatched, boolean sameDaySiteMatched) {
        List<String> matchedBy = new ArrayList<>();
        if (externalOrderMatched) {
            matchedBy.add("EXTERNAL_ORDER_NO");
        }
        if (sameDaySiteMatched) {
            matchedBy.add("SAME_DAY_SAME_SITE");
        }
        return List.copyOf(matchedBy);
    }
}
