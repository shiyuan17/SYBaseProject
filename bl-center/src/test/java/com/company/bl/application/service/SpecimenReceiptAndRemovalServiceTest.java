package com.company.bl.application.service;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.support.application.NumberingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.company.bl.application.service.SpecimenWorkflowModels.DirectReceiveSpecimensCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.ReceiveSpecimensCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.ReceiptItem;
import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenRemovalQuickConfirmCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenReceiptAndRemovalServiceTest {

    @Mock
    private SpecimenWorkflowCommandRepository commandRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NumberingService numberingService;

    @Test
    void directReceiptShouldRejectCrossApplicationItems() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SP-1",
                "BC-1",
                SpecimenStatus.FIXED,
                FixationStatus.COMPLETED,
                "VERIFIED",
                LocalDateTime.now(),
                "CHECKED_IN",
                null)));
        when(queryRepository.findSpecimenByBarcode("BC-2"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-2",
                "SP-2",
                "BC-2",
                SpecimenStatus.FIXED,
                FixationStatus.COMPLETED,
                "VERIFIED",
                LocalDateTime.now(),
                "CHECKED_IN",
                null)));
        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(commandRepository, support, numberingService);

        DirectReceiveSpecimensCommand command = new DirectReceiveSpecimensCommand(
            "receiver-1",
            "Receiver",
            "TERM-1",
            List.of(
                new ReceiptItem(null, "BC-1", null, ReceiptStatus.RECEIVED, 1, "PASSED", List.of(), null, "ok"),
                new ReceiptItem(null, "BC-2", null, ReceiptStatus.RECEIVED, 1, "PASSED", List.of(), null, "ok")));

        assertThatThrownBy(() -> service.receiveSpecimensByBarcodes(command))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("same application");
    }

    @Test
    void receiptShouldRejectTransportOrdersThatAreNotReady() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findTransportOrderById("TO-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.transportOrder("TO-1", "APP-1", TransportOrderStatus.COMPLETED)));
        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(commandRepository, support, numberingService);

        ReceiveSpecimensCommand command = new ReceiveSpecimensCommand(
            "TO-1",
            "receiver-1",
            "Receiver",
            "Logistics",
            "TERM-1",
            List.of(new ReceiptItem(null, "BC-1", null, ReceiptStatus.RECEIVED, 1, "PASSED", List.of(), null, "ok")));

        assertThatThrownBy(() -> service.receiveSpecimens(command))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("not ready for receipt");
    }

    @Test
    void directReceiptShouldCreateCaseWithoutImmediatePathologyNumber() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        Specimen receivedSpecimen = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-1",
            "BC-1",
            SpecimenStatus.RECEIVED,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SP-1",
                "BC-1",
                SpecimenStatus.FIXED,
                FixationStatus.COMPLETED,
                "VERIFIED",
                LocalDateTime.now(),
                "CHECKED_IN",
                null)));
        when(queryRepository.findPathologyCaseByApplicationId("APP-1")).thenReturn(Optional.empty());
        when(queryRepository.findSpecimensByApplicationId("APP-1")).thenReturn(List.of(receivedSpecimen));
        when(applicationRepository.findById(any()))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.application(
                "APP-1",
                com.company.bl.domain.enums.ApplicationStatus.SUBMITTED)));
        when(commandRepository.insertPathologyCase(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(commandRepository, support, numberingService);

        var result = service.receiveSpecimensByBarcodes(new DirectReceiveSpecimensCommand(
            "receiver-1",
            "Receiver",
            "TERM-1",
            List.of(new ReceiptItem(null, "BC-1", null, ReceiptStatus.RECEIVED, 1, "PASSED", List.of(), null, "ok"))));

        assertThat(result.caseId()).isNotBlank();
        assertThat(result.pathologyNo()).isNull();
        verify(commandRepository).insertPathologyCase(any());
        verify(numberingService, never()).generatePathologyNo();
    }

    @Test
    void directReceiptShouldResolveUnboundSpecimenBySpecimenId() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        Specimen receivedSpecimen = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-1",
            null,
            SpecimenStatus.RECEIVED,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        when(queryRepository.findSpecimenById("SP-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SP-1",
                null,
                SpecimenStatus.FIXED,
                FixationStatus.COMPLETED,
                "VERIFIED",
                LocalDateTime.now(),
                "CHECKED_IN",
                null)));
        when(queryRepository.findPathologyCaseByApplicationId("APP-1")).thenReturn(Optional.empty());
        when(queryRepository.findSpecimensByApplicationId("APP-1")).thenReturn(List.of(receivedSpecimen));
        when(applicationRepository.findById(any()))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.application(
                "APP-1",
                com.company.bl.domain.enums.ApplicationStatus.SUBMITTED)));
        when(commandRepository.insertPathologyCase(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(commandRepository, support, numberingService);

        var result = service.receiveSpecimensByBarcodes(new DirectReceiveSpecimensCommand(
            "receiver-1",
            "Receiver",
            "TERM-1",
            List.of(new ReceiptItem("SP-1", null, null, ReceiptStatus.RECEIVED, 1, "PASSED", List.of(), null, "ok"))));

        assertThat(result.caseId()).isNotBlank();
        verify(queryRepository, never()).findSpecimenByBarcode(any());
        verify(commandRepository).insertSpecimenReceipt(
            eq("APP-1"),
            any(),
            eq("SP-1"),
            isNull(),
            eq(ReceiptStatus.RECEIVED),
            eq(1),
            eq("PASSED"),
            isNull(),
            isNull(),
            eq("receiver-1"),
            eq("Receiver"),
            isNull(),
            any(LocalDateTime.class),
            eq("TERM-1"),
            isNull(),
            isNull(),
            eq("ok"));
    }

    @Test
    void quickRemovalConfirmationShouldUseResolvedSpecimen() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        Specimen specimen = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-1",
            "BC-1",
            SpecimenStatus.RECEIVED,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        when(queryRepository.findSpecimensBySpecimenNo("SP-NO-1")).thenReturn(List.of(specimen));
        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(commandRepository, support, numberingService);

        var result = service.quickConfirmSpecimenRemoval(
            new SpecimenRemovalQuickConfirmCommand("SPECIMEN_NO", "SP-NO-1", "u1", "Operator", "TERM-1", "remark"));

        assertThat(result.specimenId()).isEqualTo("SP-1");
        verify(commandRepository).confirmSpecimenRemoval(eq("SP-1"), any(LocalDateTime.class), eq("u1"), eq("Operator"));
        verify(commandRepository).completeSpecimenVerificationFromRemoval(
            eq("APP-1"),
            eq("SP-1"),
            any(LocalDateTime.class),
            eq("u1"),
            eq("Operator"),
            eq("TERM-1"),
            eq("remark"));
    }

    @Test
    void quickRemovalConfirmationShouldSupportSpecimenNumberBeforeBarcodeBinding() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        Specimen specimen = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-1",
            null,
            SpecimenStatus.RECEIVED,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        when(queryRepository.findSpecimensBySpecimenNo("SP-NO-1")).thenReturn(List.of(specimen));
        SpecimenReceiptAndRemovalService service = new SpecimenReceiptAndRemovalService(commandRepository, support, numberingService);

        var result = service.quickConfirmSpecimenRemoval(
            new SpecimenRemovalQuickConfirmCommand("SPECIMEN_NO", "SP-NO-1", "u1", "Operator", "TERM-1", "remark"));

        assertThat(result.specimenId()).isEqualTo("SP-1");
        assertThat(result.barcode()).isNull();
        verify(queryRepository, never()).findSpecimenByBarcode(any());
        verify(commandRepository).confirmSpecimenRemoval(eq("SP-1"), any(LocalDateTime.class), eq("u1"), eq("Operator"));
        verify(commandRepository).completeSpecimenVerificationFromRemoval(
            eq("APP-1"),
            eq("SP-1"),
            any(LocalDateTime.class),
            eq("u1"),
            eq("Operator"),
            eq("TERM-1"),
            eq("remark"));
    }
}
