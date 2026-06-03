package com.company.bl.application.service;

import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;
import static com.company.bl.application.service.SpecimenWorkflowQueryModels.*;

@Component
class SpecimenWorkflowSupport {

    private final SpecimenWorkflowLookupSupport lookupSupport;
    private final SpecimenWorkflowApplicationPolicy applicationPolicy;
    private final SpecimenWorkflowStatusPolicy statusPolicy;
    private final SpecimenWorkflowModelAssembler modelAssembler;
    private final SpecimenWorkflowInputNormalizer inputNormalizer;
    private final SpecimenRemovalExportBuilder removalExportBuilder;

    SpecimenWorkflowSupport(ApplicationRepository applicationRepository,
                            ApplicationRegistrationWorkbenchRepository workbenchRepository,
                            SpecimenWorkflowQueryRepository specimenWorkflowRepository) {
        this.lookupSupport = new SpecimenWorkflowLookupSupport(applicationRepository, specimenWorkflowRepository);
        this.applicationPolicy = new SpecimenWorkflowApplicationPolicy(workbenchRepository);
        this.statusPolicy = new SpecimenWorkflowStatusPolicy();
        this.inputNormalizer = new SpecimenWorkflowInputNormalizer();
        this.modelAssembler = new SpecimenWorkflowModelAssembler(applicationPolicy, statusPolicy);
        this.removalExportBuilder = new SpecimenRemovalExportBuilder();
    }

    Application getApplication(String applicationId) {
        return lookupSupport.getApplication(applicationId);
    }

    Specimen getSpecimen(String barcode) {
        return lookupSupport.getSpecimen(barcode);
    }

    Specimen getSpecimenById(String specimenId) {
        return lookupSupport.getSpecimenById(specimenId);
    }

    TransportOrder getTransportOrder(String transportOrderId) {
        return lookupSupport.getTransportOrder(transportOrderId);
    }

    java.util.Optional<TransportOrder> findActiveTransportOrderBySpecimenId(String specimenId) {
        return lookupSupport.findActiveTransportOrderBySpecimenId(specimenId);
    }

    List<Specimen> getSpecimensByApplicationId(String applicationId) {
        return lookupSupport.getSpecimensByApplicationId(applicationId);
    }

    List<Specimen> getSpecimensByLabelPrintBatchNoAndStatuses(String labelPrintBatchNo, List<String> labelPrintStatuses) {
        return lookupSupport.getSpecimensByLabelPrintBatchNoAndStatuses(labelPrintBatchNo, labelPrintStatuses);
    }

    Optional<PathologyCase> findPathologyCaseByApplicationId(String applicationId) {
        return lookupSupport.findPathologyCaseByApplicationId(applicationId);
    }

    List<TransportOrderItem> getTransportOrderItems(String transportOrderId) {
        return lookupSupport.getTransportOrderItems(transportOrderId);
    }

    Specimen resolveSpecimenForRemoval(String identifierType, String identifier) {
        return lookupSupport.resolveSpecimenForRemoval(identifierType, identifier);
    }

    Specimen resolveSpecimenByIdentifier(String identifierType, String identifier) {
        return lookupSupport.resolveSpecimenByIdentifier(identifierType, identifier);
    }

    void validateApplicationCanRegister(Application application) {
        applicationPolicy.validateApplicationCanRegister(application);
    }

    void ensureBarcodeAvailable(String barcode) {
        lookupSupport.ensureBarcodeAvailable(barcode);
    }

    boolean isReceiptTerminalStatus(SpecimenStatus status) {
        return statusPolicy.isReceiptTerminalStatus(status);
    }

    boolean isTransportOrderReadyForReceipt(TransportOrderStatus status) {
        return statusPolicy.isTransportOrderReadyForReceipt(status);
    }

    boolean isTransportItemTerminal(TransportItemStatus status) {
        return statusPolicy.isTransportItemTerminal(status);
    }

    SpecimenVerificationResult buildSpecimenVerificationResult(Specimen specimen) {
        return modelAssembler.buildSpecimenVerificationResult(specimen);
    }

    PendingSpecimenItem toPendingItem(SpecimenWorkflowRepository.PendingSpecimenRow row) {
        return modelAssembler.toPendingItem(row);
    }

    ApplicationListItem toApplicationListItem(ApplicationTracking tracking) {
        return modelAssembler.toApplicationListItem(tracking);
    }

    ApplicationOperationState resolveApplicationOperationState(Application application) {
        return applicationPolicy.resolveApplicationOperationState(application);
    }

    String resolveLatestLabelPrintBatchNo(List<Specimen> specimens) {
        return statusPolicy.resolveLatestLabelPrintBatchNo(specimens);
    }

    String resolveLatestBatchLabelPrintStatus(List<Specimen> specimens, String batchNo) {
        return statusPolicy.resolveLatestBatchLabelPrintStatus(specimens, batchNo);
    }

    String resolveLatestBatchLabelPrintMessage(List<TrackingEvent> events, List<Specimen> batchSpecimens) {
        return statusPolicy.resolveLatestBatchLabelPrintMessage(events, batchSpecimens);
    }

    Specimen copyWithLabelPrintStatus(Specimen specimen, String labelPrintStatus) {
        return modelAssembler.copyWithLabelPrintStatus(specimen, labelPrintStatus);
    }

    byte[] buildSpecimenRemovalExport(List<SpecimenWorkflowRepository.SpecimenRemovalListRow> rows) {
        return removalExportBuilder.buildSpecimenRemovalExport(rows);
    }

    String trim(String value) {
        return inputNormalizer.trim(value);
    }

    boolean blank(String value) {
        return inputNormalizer.blank(value);
    }

    String defaultIfBlank(String value, String fallback) {
        return inputNormalizer.defaultIfBlank(value, fallback);
    }

    String normalizeQualityCheckResult(String value) {
        return inputNormalizer.normalizeQualityCheckResult(value);
    }

    List<String> normalizeQualityIssueCodes(List<String> values) {
        return inputNormalizer.normalizeQualityIssueCodes(values);
    }

    String joinQualityIssueCodes(List<String> values) {
        return inputNormalizer.joinQualityIssueCodes(values);
    }

    int normalizePage(int page) {
        return inputNormalizer.normalizePage(page);
    }

    int normalizeSize(int size) {
        return inputNormalizer.normalizeSize(size);
    }

    LocalDateTime parseDateFrom(String value) {
        return inputNormalizer.parseDateFrom(value);
    }

    LocalDateTime parseDateTo(String value) {
        return inputNormalizer.parseDateTo(value);
    }

    LocalDate parseLocalDateFrom(String value) {
        return inputNormalizer.parseLocalDateFrom(value);
    }

    LocalDate parseLocalDate(String value) {
        return inputNormalizer.parseLocalDate(value);
    }

    LocalDate parseLocalDateTo(String value) {
        return inputNormalizer.parseLocalDateTo(value);
    }

    String normalizeStatus(String value) {
        return inputNormalizer.normalizeStatus(value);
    }

    String commandCheckInStatus(Specimen specimen) {
        return statusPolicy.commandCheckInStatus(specimen);
    }

    boolean isVerificationCompleted(Specimen specimen) {
        return statusPolicy.isVerificationCompleted(specimen);
    }

    boolean canCheckInSpecimen(Specimen specimen) {
        return statusPolicy.canCheckInSpecimen(specimen);
    }

    boolean canCheckInApplication(String applicationId) {
        return statusPolicy.canCheckInApplication(getSpecimensByApplicationId(applicationId));
    }

    boolean canTransportApplication(String applicationId) {
        return statusPolicy.canTransportApplication(getSpecimensByApplicationId(applicationId));
    }
}
