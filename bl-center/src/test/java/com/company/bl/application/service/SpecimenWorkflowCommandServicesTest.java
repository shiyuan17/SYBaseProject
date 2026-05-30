package com.company.bl.application.service;

import com.company.bl.application.gateway.LabelPrintGateway;
import com.company.bl.domain.enums.ApplicationFormStatus;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.support.application.NumberingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.company.bl.application.service.SpecimenWorkflowModels.CheckInSpecimenCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.CreateTransportOrderCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.DirectReceiveSpecimensCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.FixationCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.HandoverTransportOrderCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.ReceiveSpecimensCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.ReceiptItem;
import static com.company.bl.application.service.SpecimenWorkflowModels.RegisterSpecimensCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.RetryLabelPrintCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenRegistrationItem;
import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenRemovalQuickConfirmCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenVerificationCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenWorkflowCommandServicesTest {

    @Mock
    private SpecimenWorkflowCommandRepository specimenWorkflowCommandRepository;

    @Mock
    private SpecimenWorkflowQueryRepository specimenWorkflowQueryRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NumberingService numberingService;

    @Mock
    private LabelPrintGateway labelPrintGateway;

    @Test
    void registrationShouldRejectBarcodeConflict() {
        SpecimenWorkflowSupport support = support();
        when(applicationRepository.findById(any(ApplicationId.class))).thenReturn(Optional.of(application("APP-1", ApplicationStatus.DRAFT)));
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-1")).thenReturn(Optional.of(specimen("APP-1", "SP-EXIST", "BC-1")));
        SpecimenRegistrationService service =
            new SpecimenRegistrationService(specimenWorkflowCommandRepository, support, numberingService, labelPrintGateway);

        RegisterSpecimensCommand command = new RegisterSpecimensCommand(
            "APP-1",
            "PRINTER-1",
            "OPERATING_ROOM",
            "u1",
            "Operator",
            "TERM-1",
            "remark",
            List.of(new SpecimenRegistrationItem("Liver", "TISSUE", "L1", "SURGERY", 1, "Bottle", 1, "BC-1", "pain")));

        assertThatThrownBy(() -> service.registerSpecimens(command))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Specimen barcode already exists");

        verify(labelPrintGateway, never()).print(any());
    }

    @Test
    void retryLabelPrintShouldReturnEmptyResultWhenNoPendingLabels() {
        SpecimenWorkflowSupport support = support();
        when(specimenWorkflowQueryRepository.findSpecimensByLabelPrintBatchNoAndStatuses("LP-1", List.of("FAILED", "PENDING")))
            .thenReturn(List.of());
        SpecimenRegistrationService service =
            new SpecimenRegistrationService(specimenWorkflowCommandRepository, support, numberingService, labelPrintGateway);

        var result = service.retryLabelPrint(new RetryLabelPrintCommand("LP-1", "u1", "Operator", "PRINTER-1", "TERM-1", "remark"));

        assertThat(result.retriedCount()).isZero();
        assertThat(result.allSuccessful()).isTrue();
        verify(labelPrintGateway, never()).print(any());
    }

    @Test
    void fixationShouldRejectUnverifiedSpecimen() {
        SpecimenWorkflowSupport support = support();
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(specimen("APP-1", "SP-1", "BC-1")));
        SpecimenFixationService service = new SpecimenFixationService(specimenWorkflowCommandRepository, support);

        assertThatThrownBy(() -> service.startFixation(new FixationCommand("BC-1", "FORMALIN", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("must be verified before fixation");
    }

    @Test
    void fixationCompletionShouldRejectAlreadyCompletedSpecimen() {
        SpecimenWorkflowSupport support = support();
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(specimen("APP-1", "SP-1", "BC-1", SpecimenStatus.FIXED, FixationStatus.COMPLETED, "VERIFIED", null, null, null)));
        SpecimenFixationService service = new SpecimenFixationService(specimenWorkflowCommandRepository, support);

        assertThatThrownBy(() -> service.completeFixation(new FixationCommand("BC-1", "FORMALIN", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("already completed");
    }

    @Test
    void verificationShouldRejectRepeatedStart() {
        SpecimenWorkflowSupport support = support();
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(specimen("APP-1", "SP-1", "BC-1", SpecimenStatus.REGISTERED, FixationStatus.PENDING, "VERIFYING", null, null, null)));
        SpecimenVerificationService service = new SpecimenVerificationService(specimenWorkflowCommandRepository, support);

        assertThatThrownBy(() -> service.startSpecimenVerification(new SpecimenVerificationCommand("BC-1", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("already started");
    }

    @Test
    void checkInShouldRejectUnconfirmedSpecimen() {
        SpecimenWorkflowSupport support = support();
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(specimen("APP-1", "SP-1", "BC-1", SpecimenStatus.FIXED, FixationStatus.COMPLETED, "VERIFIED", null, null, null)));
        SpecimenVerificationService service = new SpecimenVerificationService(specimenWorkflowCommandRepository, support);

        assertThatThrownBy(() -> service.checkInSpecimen(new CheckInSpecimenCommand("BC-1", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("must be confirmed before check-in");
    }

    @Test
    void transportShouldRejectSpecimenFromDifferentApplication() {
        SpecimenWorkflowSupport support = support();
        when(applicationRepository.findById(any(ApplicationId.class))).thenReturn(Optional.of(application("APP-1", ApplicationStatus.SUBMITTED)));
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(specimen("APP-2", "SP-1", "BC-1", SpecimenStatus.FIXED, FixationStatus.COMPLETED, "VERIFIED", LocalDateTime.now(), "CHECKED_IN", null)));
        SpecimenTransportService service = new SpecimenTransportService(specimenWorkflowCommandRepository, support, numberingService);

        CreateTransportOrderCommand command = new CreateTransportOrderCommand(
            "APP-1",
            List.of("BC-1"),
            "handover-1",
            "Handover User",
            "dept-1",
            "Grossing",
            "dept-2",
            "Lab",
            "TERM-1",
            "remark");

        assertThatThrownBy(() -> service.createTransportOrder(command))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("does not belong to application");
    }

    @Test
    void transportHandoverShouldAdvanceItemAndApplicationStatuses() {
        SpecimenWorkflowSupport support = support();
        TransportOrder order = transportOrder("TO-1", "APP-1", TransportOrderStatus.PRINTED);
        when(specimenWorkflowQueryRepository.findTransportOrderById("TO-1")).thenReturn(Optional.of(order));
        when(specimenWorkflowCommandRepository.updateTransportOrderStatus(
            eq("TO-1"),
            eq(TransportOrderStatus.HANDED_OVER),
            eq("receiver-1"),
            eq("Receiver"),
            eq(null),
            any(LocalDateTime.class)))
            .thenReturn(transportOrder("TO-1", "APP-1", TransportOrderStatus.HANDED_OVER));
        when(specimenWorkflowQueryRepository.findTransportOrderItems("TO-1")).thenReturn(List.of(
            new TransportOrderItem("TOI-1", "TO-1", "APP-1", "SP-1", TransportItemStatus.PENDING, "MATCHED", null, null, null, null),
            new TransportOrderItem("TOI-2", "TO-1", "APP-1", "SP-2", TransportItemStatus.PENDING, "MATCHED", null, null, null, null)));
        SpecimenTransportService service = new SpecimenTransportService(specimenWorkflowCommandRepository, support, numberingService);

        TransportOrder updated = service.handoverTransportOrder(
            "TO-1",
            new HandoverTransportOrderCommand("receiver-1", "Receiver", "TERM-1", "remark"));

        assertThat(updated.status()).isEqualTo(TransportOrderStatus.HANDED_OVER);
        verify(specimenWorkflowCommandRepository, times(2)).updateTransportOrderItemStatus(
            eq("TO-1"),
            any(String.class),
            eq(TransportItemStatus.HANDED_OVER),
            eq("MATCHED"),
            eq("receiver-1"),
            eq("Receiver"),
            any(LocalDateTime.class),
            eq("remark"));
        verify(specimenWorkflowCommandRepository, times(2)).updateSpecimenStatus(
            any(String.class),
            eq(SpecimenStatus.IN_TRANSIT),
            eq(FixationStatus.COMPLETED),
            eq(null),
            eq("remark"),
            eq(null));
        verify(specimenWorkflowCommandRepository).updateApplicationStatus("APP-1", "IN_TRANSIT");
    }

    @Test
    void directReceiptShouldRejectCrossApplicationItems() {
        SpecimenWorkflowSupport support = support();
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(specimen("APP-1", "SP-1", "BC-1", SpecimenStatus.FIXED, FixationStatus.COMPLETED, "VERIFIED", LocalDateTime.now(), "CHECKED_IN", null)));
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-2"))
            .thenReturn(Optional.of(specimen("APP-2", "SP-2", "BC-2", SpecimenStatus.FIXED, FixationStatus.COMPLETED, "VERIFIED", LocalDateTime.now(), "CHECKED_IN", null)));
        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(specimenWorkflowCommandRepository, support, numberingService);

        DirectReceiveSpecimensCommand command = new DirectReceiveSpecimensCommand(
            "receiver-1",
            "Receiver",
            "TERM-1",
            List.of(
                new ReceiptItem("BC-1", ReceiptStatus.RECEIVED, 1, "PASSED", List.of(), null, "ok"),
                new ReceiptItem("BC-2", ReceiptStatus.RECEIVED, 1, "PASSED", List.of(), null, "ok")));

        assertThatThrownBy(() -> service.receiveSpecimensByBarcodes(command))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("same application");
    }

    @Test
    void receiptShouldRejectTransportOrdersThatAreNotReady() {
        SpecimenWorkflowSupport support = support();
        when(specimenWorkflowQueryRepository.findTransportOrderById("TO-1"))
            .thenReturn(Optional.of(transportOrder("TO-1", "APP-1", TransportOrderStatus.COMPLETED)));
        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(specimenWorkflowCommandRepository, support, numberingService);

        ReceiveSpecimensCommand command = new ReceiveSpecimensCommand(
            "TO-1",
            "receiver-1",
            "Receiver",
            "TERM-1",
            List.of(new ReceiptItem("BC-1", ReceiptStatus.RECEIVED, 1, "PASSED", List.of(), null, "ok")));

        assertThatThrownBy(() -> service.receiveSpecimens(command))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("not ready for receipt");
    }

    @Test
    void quickRemovalConfirmationShouldUseResolvedSpecimen() {
        SpecimenWorkflowSupport support = support();
        Specimen specimen = specimen("APP-1", "SP-1", "BC-1", SpecimenStatus.RECEIVED, FixationStatus.COMPLETED, "VERIFIED", LocalDateTime.now(), "CHECKED_IN", null);
        when(specimenWorkflowQueryRepository.findSpecimensBySpecimenNo("SP-NO-1")).thenReturn(List.of(specimen));
        when(specimenWorkflowQueryRepository.findSpecimenByBarcode("BC-1")).thenReturn(Optional.of(specimen));
        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(specimenWorkflowCommandRepository, support, numberingService);

        var result = service.quickConfirmSpecimenRemoval(
            new SpecimenRemovalQuickConfirmCommand("SPECIMEN_NO", "SP-NO-1", "u1", "Operator", "TERM-1", "remark"));

        assertThat(result.specimenId()).isEqualTo("SP-1");
        verify(specimenWorkflowCommandRepository).confirmSpecimenRemoval(eq("SP-1"), any(LocalDateTime.class), eq("u1"), eq("Operator"));
        verify(specimenWorkflowCommandRepository).completeSpecimenVerificationFromRemoval(
            eq("APP-1"),
            eq("SP-1"),
            any(LocalDateTime.class),
            eq("u1"),
            eq("Operator"),
            eq("TERM-1"),
            eq("remark"));
    }

    private SpecimenWorkflowSupport support() {
        ApplicationRegistrationWorkbenchRepository workbenchRepository = mock(ApplicationRegistrationWorkbenchRepository.class);
        return new SpecimenWorkflowSupport(applicationRepository, workbenchRepository, specimenWorkflowQueryRepository);
    }

    private static Application application(String applicationId, ApplicationStatus status) {
        return new Application(
            new ApplicationId(applicationId),
            "APP-NO-1",
            "PAT-1",
            "Patient",
            "F",
            "32",
            "BIOPSY",
            status,
            ApplicationFormStatus.PENDING,
            "EXT-1",
            "HIS",
            "H-1",
            "Hospital",
            "D-1",
            "Dept",
            "DOC-1",
            "Doctor",
            "Dx",
            "Symptom",
            "Liver",
            LocalDate.now(),
            LocalDate.now(),
            null,
            "remark",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now());
    }

    private static Specimen specimen(String applicationId, String specimenId, String barcode) {
        return specimen(applicationId, specimenId, barcode, SpecimenStatus.REGISTERED, FixationStatus.PENDING, "UNVERIFIED", null, null, null);
    }

    private static Specimen specimen(String applicationId,
                                     String specimenId,
                                     String barcode,
                                     SpecimenStatus specimenStatus,
                                     FixationStatus fixationStatus,
                                     String verificationStatus,
                                     LocalDateTime specimenConfirmedAt,
                                     String checkInStatus,
                                     LocalDateTime specimenRemovalAt) {
        return new Specimen(
            specimenId,
            applicationId,
            null,
            "SP-NO-1",
            barcode,
            "TISSUE",
            "Liver",
            "L1",
            "SURGERY",
            1,
            "Bottle",
            1,
            specimenStatus,
            fixationStatus,
            verificationStatus,
            null,
            null,
            specimenRemovalAt,
            null,
            null,
            specimenConfirmedAt,
            checkInStatus,
            checkInStatus == null ? null : LocalDateTime.now(),
            checkInStatus == null ? null : "Operator",
            true,
            null,
            specimenStatus == SpecimenStatus.RECEIVED ? "RECEIVED" : null,
            "PASSED",
            null,
            "pain",
            "D-1",
            "Dept",
            "DOC-1",
            "Doctor",
            LocalDate.now(),
            "LP-1",
            "SUCCESS",
            "u1",
            "Operator",
            LocalDateTime.now().minusHours(1),
            "TERM-1",
            "remark");
    }

    private static TransportOrder transportOrder(String transportOrderId, String applicationId, TransportOrderStatus status) {
        return new TransportOrder(
            transportOrderId,
            "TO-NO-1",
            applicationId,
            status,
            "handover-1",
            "Handover User",
            "dept-1",
            "Grossing",
            "dept-2",
            "Lab",
            null,
            null,
            null,
            LocalDateTime.now().minusHours(1),
            null,
            "TERM-1",
            "remark");
    }
}
