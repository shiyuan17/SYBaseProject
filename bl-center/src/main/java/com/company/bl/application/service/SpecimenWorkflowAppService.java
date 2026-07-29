package com.company.bl.application.service;

import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TransportOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;
import static com.company.bl.application.service.SpecimenWorkflowQueryModels.*;
import static com.company.bl.application.service.SpecimenWorkflowTransportModels.*;

@Service
@RequiredArgsConstructor
public class SpecimenWorkflowAppService {

    private final SpecimenRegistrationService specimenRegistrationService;
    private final SpecimenBarcodeBindingService specimenBarcodeBindingService;
    private final SpecimenFixationService specimenFixationService;
    private final SpecimenVerificationService specimenVerificationService;
    private final SpecimenTransportService specimenTransportService;
    private final SpecimenReceiptAndRemovalService specimenReceiptAndRemovalService;
    private final SpecimenWorkflowQueryService specimenWorkflowQueryService;

    public SpecimenRegistrationResult registerSpecimens(RegisterSpecimensCommand command) {
        return specimenRegistrationService.registerSpecimens(command);
    }

    public FixationResult startFixation(FixationCommand command) {
        return specimenFixationService.startFixation(command);
    }

    public Specimen bindSpecimenBarcode(SpecimenBarcodeBindingCommand command) {
        return specimenBarcodeBindingService.bindSpecimenBarcode(command);
    }

    public Specimen rebindSpecimenBarcode(SpecimenBarcodeBindingCommand command) {
        return specimenBarcodeBindingService.rebindSpecimenBarcode(command);
    }

    public Specimen unbindSpecimenBarcode(SpecimenBarcodeUnbindCommand command) {
        return specimenBarcodeBindingService.unbindSpecimenBarcode(command);
    }

    public FixationResult completeFixation(FixationCommand command) {
        return specimenFixationService.completeFixation(command);
    }

    public SpecimenVerificationResult startSpecimenVerification(SpecimenVerificationCommand command) {
        return specimenVerificationService.startSpecimenVerification(command);
    }

    public SpecimenVerificationResult completeSpecimenVerification(SpecimenVerificationCommand command) {
        return specimenVerificationService.completeSpecimenVerification(command);
    }

    public Specimen confirmSpecimen(ConfirmSpecimenCommand command) {
        return specimenVerificationService.confirmSpecimen(command);
    }

    public Specimen checkInSpecimen(CheckInSpecimenCommand command) {
        return specimenVerificationService.checkInSpecimen(command);
    }

    public TransportOrder createTransportOrder(CreateTransportOrderCommand command) {
        return specimenTransportService.createTransportOrder(command);
    }

    public TransportOrder printTransportOrder(String transportOrderId, OperatorCommand command) {
        return specimenTransportService.printTransportOrder(transportOrderId, command);
    }

    public TransportOrder handoverTransportOrder(String transportOrderId, HandoverTransportOrderCommand command) {
        return specimenTransportService.handoverTransportOrder(transportOrderId, command);
    }

    public TransportOrder outboundTransportOrder(String transportOrderId, OutboundTransportOrderCommand command) {
        return specimenTransportService.outboundTransportOrder(transportOrderId, command);
    }

    public TransportOrder quickOutboundTransportOrder(QuickOutboundTransportOrderCommand command) {
        return specimenTransportService.quickOutboundTransportOrder(command);
    }

    public ReceiptResult receiveSpecimens(ReceiveSpecimensCommand command) {
        return specimenReceiptAndRemovalService.receiveSpecimens(command);
    }

    public ReceiptResult receiveSpecimensByBarcodes(DirectReceiveSpecimensCommand command) {
        return specimenReceiptAndRemovalService.receiveSpecimensByBarcodes(command);
    }

    public LabelPrintRetryResult retryLabelPrint(RetryLabelPrintCommand command) {
        return specimenRegistrationService.retryLabelPrint(command);
    }

    public PendingSpecimenPage listPendingFixations(PendingSpecimenQuery query) {
        return specimenWorkflowQueryService.listPendingFixations(query);
    }

    public PendingSpecimenPage listPendingReceipts(PendingSpecimenQuery query) {
        return specimenWorkflowQueryService.listPendingReceipts(query);
    }

    public List<SpecimenVerificationRecord> listSpecimenVerificationRecords(String barcode) {
        return specimenWorkflowQueryService.listSpecimenVerificationRecords(barcode);
    }

    public PendingTransportOrderPage listPendingTransportOrders(PendingTransportOrderQuery query) {
        return specimenWorkflowQueryService.listPendingTransportOrders(query);
    }

    public SpecimenOutboundPage listSpecimenOutbounds(SpecimenOutboundListQuery query) {
        return specimenWorkflowQueryService.listSpecimenOutbounds(query);
    }

    public ApplicationPage listApplications(ApplicationListQuery query) {
        return specimenWorkflowQueryService.listApplications(query);
    }

    public DuplicateCheckResult checkApplicationDuplicate(DuplicateCheckCommand command) {
        return specimenWorkflowQueryService.checkApplicationDuplicate(command);
    }

    public SpecimenManagementListPage listSpecimenManagementItems(SpecimenManagementListQuery query) {
        return specimenWorkflowQueryService.listSpecimenManagementItems(query);
    }

    public byte[] exportSpecimenManagementItems(SpecimenManagementListQuery query) {
        return specimenWorkflowQueryService.exportSpecimenManagementItems(query);
    }

    public SpecimenRemovalListPage listSpecimenRemovalItems(SpecimenRemovalQuery query) {
        return specimenWorkflowQueryService.listSpecimenRemovalItems(query);
    }

    public SpecimenRemovalResult confirmSpecimenRemoval(SpecimenRemovalCommand command) {
        return specimenReceiptAndRemovalService.confirmSpecimenRemoval(command);
    }

    public SpecimenRemovalResult quickConfirmSpecimenRemoval(SpecimenRemovalQuickConfirmCommand command) {
        return specimenReceiptAndRemovalService.quickConfirmSpecimenRemoval(command);
    }

    public byte[] exportSpecimenRemovalItems(SpecimenRemovalQuery query) {
        return specimenWorkflowQueryService.exportSpecimenRemovalItems(query);
    }

    public ApplicationTracking getApplicationTracking(String applicationId) {
        return specimenWorkflowQueryService.getApplicationTracking(applicationId);
    }

    public ApplicationTracking getTrackingByBarcode(String barcode) {
        return specimenWorkflowQueryService.getTrackingByBarcode(barcode);
    }

    public LatestSpecimenRegistrationResult getLatestRegistrationResult(String applicationId) {
        return specimenWorkflowQueryService.getLatestRegistrationResult(applicationId);
    }

    public ApplicationListItem getRegistrationApplicationByApplicationNo(String applicationNo) {
        return specimenWorkflowQueryService.getRegistrationApplicationByApplicationNo(applicationNo);
    }

    public ApplicationOperationState resolveApplicationOperationState(Application application) {
        return specimenWorkflowQueryService.resolveApplicationOperationState(application);
    }
}
