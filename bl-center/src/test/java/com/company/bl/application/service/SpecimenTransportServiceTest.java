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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
            eq(null),
            any(LocalDateTime.class)))
            .thenReturn(SpecimenWorkflowServiceTestFixtures.transportOrder("TO-1", "APP-1", TransportOrderStatus.HANDED_OVER));
        when(queryRepository.findTransportOrderItems("TO-1")).thenReturn(List.of(
            new TransportOrderItem("TOI-1", "TO-1", "APP-1", "SP-1", TransportItemStatus.PENDING, "MATCHED", null, null, null, null),
            new TransportOrderItem("TOI-2", "TO-1", "APP-1", "SP-2", TransportItemStatus.PENDING, "MATCHED", null, null, null, null)));
        SpecimenTransportService service = new SpecimenTransportService(commandRepository, support, numberingService);

        TransportOrder updated = service.handoverTransportOrder(
            "TO-1",
            new HandoverTransportOrderCommand("receiver-1", "Receiver", "TERM-1", "remark"));

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
}
