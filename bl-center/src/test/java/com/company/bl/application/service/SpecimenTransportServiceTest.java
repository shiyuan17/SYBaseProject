package com.company.bl.application.service;

import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.support.application.NumberingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.company.bl.application.service.SpecimenWorkflowTransportModels.CreateTransportOrderCommand;
import static com.company.bl.application.service.SpecimenWorkflowTransportModels.HandoverTransportOrderCommand;
import static com.company.bl.application.service.SpecimenWorkflowTransportModels.OutboundTransportOrderCommand;
import static com.company.bl.application.service.SpecimenWorkflowTransportModels.QuickOutboundTransportOrderCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenTransportServiceTest {

    @Mock
    private SpecimenWorkflowCommandRepository commandRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NumberingService numberingService;

    @Test
    void transportShouldRejectSpecimenFromDifferentApplication() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(applicationRepository.findById(any(ApplicationId.class)))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.application("APP-1", ApplicationStatus.SUBMITTED)));
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-2",
                "SP-1",
                "BC-1",
                SpecimenStatus.FIXED,
                FixationStatus.COMPLETED,
                "VERIFIED",
                LocalDateTime.now(),
                "CHECKED_IN",
                null)));
        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

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
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        TransportOrder order = SpecimenWorkflowServiceTestFixtures.transportOrder("TO-1", "APP-1", TransportOrderStatus.PRINTED);
        when(queryRepository.findTransportOrderById("TO-1")).thenReturn(Optional.of(order));
        when(commandRepository.updateTransportOrderStatus(
            eq("TO-1"),
            eq(TransportOrderStatus.HANDED_OVER),
            eq("receiver-1"),
            eq("Receiver"),
            eq("outbound-1"),
            eq("Outbound User"),
            eq(null),
            any(LocalDateTime.class)))
            .thenReturn(SpecimenWorkflowServiceTestFixtures.transportOrder("TO-1", "APP-1", TransportOrderStatus.HANDED_OVER));
        when(queryRepository.findTransportOrderItems("TO-1")).thenReturn(List.of(
            new TransportOrderItem("TOI-1", "TO-1", "APP-1", "SP-1", TransportItemStatus.PENDING, "MATCHED", null, null, null, null),
            new TransportOrderItem("TOI-2", "TO-1", "APP-1", "SP-2", TransportItemStatus.PENDING, "MATCHED", null, null, null, null)));
        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

        TransportOrder updated = service.handoverTransportOrder(
            "TO-1",
            new HandoverTransportOrderCommand("outbound-1", "Outbound User", "receiver-1", "Receiver", "TERM-1", "remark"));

        assertThat(updated.status()).isEqualTo(TransportOrderStatus.HANDED_OVER);
        verify(commandRepository, times(2)).updateTransportOrderItemStatus(
            eq("TO-1"),
            any(String.class),
            eq(TransportItemStatus.HANDED_OVER),
            eq("MATCHED"),
            eq("receiver-1"),
            eq("Receiver"),
            any(LocalDateTime.class),
            eq("remark"));
        verify(commandRepository, times(2)).updateSpecimenStatus(
            any(String.class),
            eq(SpecimenStatus.IN_TRANSIT),
            eq(FixationStatus.COMPLETED),
            eq(null),
            eq("remark"),
            eq(null));
        verify(commandRepository).updateApplicationStatus("APP-1", "IN_TRANSIT");
    }

    @Test
    void transportOutboundShouldAdvanceItemAndApplicationStatuses() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        TransportOrder order = SpecimenWorkflowServiceTestFixtures.transportOrder("TO-2", "APP-2", TransportOrderStatus.PRINTED);
        var checkedInSpecimenA = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-2",
            "SP-3",
            "BC-3",
            SpecimenStatus.CHECKED_IN,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        var checkedInSpecimenB = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-2",
            "SP-4",
            "BC-4",
            SpecimenStatus.CHECKED_IN,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        when(queryRepository.findTransportOrderById("TO-2")).thenReturn(Optional.of(order));
        when(queryRepository.findSpecimensByApplicationId("APP-2"))
            .thenReturn(List.of(checkedInSpecimenA, checkedInSpecimenB));
        when(commandRepository.updateTransportOrderStatus(
            eq("TO-2"),
            eq(TransportOrderStatus.HANDED_OVER),
            eq(null),
            eq(null),
            eq("outbound-2"),
            eq("Outbound User 2"),
            eq(null),
            any(LocalDateTime.class)))
            .thenReturn(SpecimenWorkflowServiceTestFixtures.transportOrder("TO-2", "APP-2", TransportOrderStatus.HANDED_OVER));
        when(queryRepository.findTransportOrderItems("TO-2")).thenReturn(List.of(
            new TransportOrderItem("TOI-3", "TO-2", "APP-2", "SP-3", TransportItemStatus.PENDING, "MATCHED", null, null, null, null),
            new TransportOrderItem("TOI-4", "TO-2", "APP-2", "SP-4", TransportItemStatus.PENDING, "MATCHED", null, null, null, null)));
        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

        TransportOrder updated = service.outboundTransportOrder(
            "TO-2",
            new OutboundTransportOrderCommand("outbound-2", "Outbound User 2", "TERM-2", "scan outbound"));

        assertThat(updated.status()).isEqualTo(TransportOrderStatus.HANDED_OVER);
        verify(commandRepository, times(2)).updateTransportOrderItemStatus(
            eq("TO-2"),
            any(String.class),
            eq(TransportItemStatus.HANDED_OVER),
            eq("MATCHED"),
            eq("outbound-2"),
            eq("Outbound User 2"),
            any(LocalDateTime.class),
            eq("scan outbound"));
        verify(commandRepository, times(2)).updateSpecimenStatus(
            any(String.class),
            eq(SpecimenStatus.IN_TRANSIT),
            eq(FixationStatus.COMPLETED),
            eq(null),
            eq("scan outbound"),
            eq(null));
        verify(commandRepository).updateApplicationStatus("APP-2", "IN_TRANSIT");
    }

    @Test
    void createTransportOrderShouldRejectSpecimenWithActiveTransportOrder() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(applicationRepository.findById(any(ApplicationId.class)))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.application("APP-1", ApplicationStatus.SUBMITTED)));
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SP-1",
                "BC-1",
                SpecimenStatus.CHECKED_IN,
                FixationStatus.COMPLETED,
                "VERIFIED",
                LocalDateTime.now(),
                "CHECKED_IN",
                null)));
        when(queryRepository.findActiveTransportOrderBySpecimenId("SP-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.transportOrder("TO-ACTIVE", "APP-1", TransportOrderStatus.PENDING)));

        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

        assertThatThrownBy(() -> service.createTransportOrder(
            new CreateTransportOrderCommand(
                "APP-1",
                List.of("BC-1"),
                "handover-1",
                "Handover User",
                "dept-1",
                "Grossing",
                "dept-2",
                "Lab",
                "TERM-1",
                "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("active transport order");
    }

    @Test
    void createTransportOrderShouldRejectWhenApplicationHasUncheckedInSpecimens() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(applicationRepository.findById(any(ApplicationId.class)))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.application("APP-1", ApplicationStatus.SUBMITTED)));
        var checkedInSpecimen = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-1",
            "BC-1",
            SpecimenStatus.CHECKED_IN,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        var pendingSibling = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-2",
            "BC-2",
            SpecimenStatus.FIXED,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            null,
            null);
        when(queryRepository.findSpecimenByBarcode("BC-1")).thenReturn(Optional.of(checkedInSpecimen));
        when(queryRepository.findSpecimensByApplicationId("APP-1"))
            .thenReturn(List.of(checkedInSpecimen, pendingSibling));

        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

        assertThatThrownBy(() -> service.createTransportOrder(
            new CreateTransportOrderCommand(
                "APP-1",
                List.of("BC-1"),
                "handover-1",
                "Handover User",
                "dept-1",
                "Grossing",
                "dept-2",
                "Lab",
                "TERM-1",
                "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("All specimens of the application must be checked in");
    }

    @Test
    void quickOutboundShouldCreateTransportOrderWhenSpecimenHasNoActiveOrder() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        var checkedInSpecimen = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-1",
            "BC-1",
            SpecimenStatus.CHECKED_IN,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        when(applicationRepository.findById(any(ApplicationId.class)))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.application("APP-1", ApplicationStatus.SUBMITTED)));
        when(queryRepository.findSpecimensBySpecimenNo("SP-NO-1"))
            .thenReturn(List.of(checkedInSpecimen));
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(checkedInSpecimen));
        when(queryRepository.findSpecimensByApplicationId("APP-1"))
            .thenReturn(List.of(checkedInSpecimen));
        when(queryRepository.findActiveTransportOrderBySpecimenId("SP-1"))
            .thenReturn(Optional.empty());
        when(numberingService.generateTransportOrderNo()).thenReturn("TR-NEW-001");
        when(commandRepository.insertTransportOrder(any(TransportOrder.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(commandRepository.updateTransportOrderStatus(
            any(String.class),
            eq(TransportOrderStatus.HANDED_OVER),
            eq(null),
            eq(null),
            eq("outbound-1"),
            eq("Outbound User"),
            eq(null),
            any(LocalDateTime.class)))
            .thenAnswer(invocation -> SpecimenWorkflowServiceTestFixtures.transportOrder(
                invocation.getArgument(0),
                "APP-1",
                TransportOrderStatus.HANDED_OVER));
        when(queryRepository.findTransportOrderById(any(String.class)))
            .thenAnswer(invocation -> Optional.of(SpecimenWorkflowServiceTestFixtures.transportOrder(
                invocation.getArgument(0),
                "APP-1",
                TransportOrderStatus.PENDING)));
        when(queryRepository.findTransportOrderItems(any(String.class)))
            .thenReturn(List.of(new TransportOrderItem(
                "TOI-1",
                "TO-1",
                "APP-1",
                "SP-1",
                TransportItemStatus.PENDING,
                "MATCHED",
                null,
                null,
                null,
                null)));

        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

        TransportOrder result = service.quickOutboundTransportOrder(
            new QuickOutboundTransportOrderCommand(
                "SPECIMEN_NO",
                "SP-NO-1",
                "outbound-1",
                "Outbound User",
                "TERM-1",
                "remark"));

        assertThat(result.status()).isEqualTo(TransportOrderStatus.HANDED_OVER);
        verify(commandRepository).insertTransportOrder(any(TransportOrder.class));
        verify(commandRepository).updateApplicationStatus("APP-1", "IN_TRANSIT");
    }

    @Test
    void quickOutboundShouldReuseExistingActiveTransportOrder() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        TransportOrder existingOrder = SpecimenWorkflowServiceTestFixtures.transportOrder("TO-EXIST", "APP-1", TransportOrderStatus.PRINTED);
        var checkedInSpecimen = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-1",
            "BC-1",
            SpecimenStatus.CHECKED_IN,
            FixationStatus.COMPLETED,
            "VERIFIED",
            LocalDateTime.now(),
            "CHECKED_IN",
            null);
        when(queryRepository.findSpecimensBySpecimenNo("SP-NO-1"))
            .thenReturn(List.of(checkedInSpecimen));
        when(queryRepository.findSpecimensByApplicationId("APP-1"))
            .thenReturn(List.of(checkedInSpecimen));
        when(queryRepository.findActiveTransportOrderBySpecimenId("SP-1"))
            .thenReturn(Optional.of(existingOrder));
        when(queryRepository.findTransportOrderById("TO-EXIST"))
            .thenReturn(Optional.of(existingOrder));
        when(commandRepository.updateTransportOrderStatus(
            eq("TO-EXIST"),
            eq(TransportOrderStatus.HANDED_OVER),
            eq(null),
            eq(null),
            eq("outbound-2"),
            eq("Outbound User 2"),
            eq(null),
            any(LocalDateTime.class)))
            .thenReturn(SpecimenWorkflowServiceTestFixtures.transportOrder("TO-EXIST", "APP-1", TransportOrderStatus.HANDED_OVER));
        when(queryRepository.findTransportOrderItems("TO-EXIST"))
            .thenReturn(List.of(new TransportOrderItem(
                "TOI-1",
                "TO-EXIST",
                "APP-1",
                "SP-1",
                TransportItemStatus.PENDING,
                "MATCHED",
                null,
                null,
                null,
                null)));

        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

        TransportOrder result = service.quickOutboundTransportOrder(
            new QuickOutboundTransportOrderCommand(
                "SPECIMEN_NO",
                "SP-NO-1",
                "outbound-2",
                "Outbound User 2",
                "TERM-2",
                "remark"));

        assertThat(result.status()).isEqualTo(TransportOrderStatus.HANDED_OVER);
        verify(commandRepository, never()).insertTransportOrder(any(TransportOrder.class));
        verify(commandRepository).updateTransportOrderStatus(
            eq("TO-EXIST"),
            eq(TransportOrderStatus.HANDED_OVER),
            eq(null),
            eq(null),
            eq("outbound-2"),
            eq("Outbound User 2"),
            eq(null),
            any(LocalDateTime.class));
    }

    @Test
    void outboundTransportOrderShouldRejectWhenApplicationHasUncheckedInSpecimens() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        TransportOrder order = SpecimenWorkflowServiceTestFixtures.transportOrder("TO-3", "APP-3", TransportOrderStatus.PRINTED);
        when(queryRepository.findTransportOrderById("TO-3")).thenReturn(Optional.of(order));
        when(queryRepository.findSpecimensByApplicationId("APP-3"))
            .thenReturn(List.of(
                SpecimenWorkflowServiceTestFixtures.specimen(
                    "APP-3",
                    "SP-3",
                    "BC-3",
                    SpecimenStatus.CHECKED_IN,
                    FixationStatus.COMPLETED,
                    "VERIFIED",
                    LocalDateTime.now(),
                    "CHECKED_IN",
                    null),
                SpecimenWorkflowServiceTestFixtures.specimen(
                    "APP-3",
                    "SP-4",
                    "BC-4",
                    SpecimenStatus.FIXED,
                    FixationStatus.COMPLETED,
                    "VERIFIED",
                    LocalDateTime.now(),
                    null,
                    null)));

        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

        assertThatThrownBy(() -> service.outboundTransportOrder(
            "TO-3",
            new OutboundTransportOrderCommand("outbound-3", "Outbound User 3", "TERM-3", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("All specimens of the application must be checked in");
        verify(commandRepository, never()).updateTransportOrderStatus(
            any(String.class),
            any(TransportOrderStatus.class),
            any(),
            any(),
            any(),
            any(),
            any(),
            any());
    }
}
