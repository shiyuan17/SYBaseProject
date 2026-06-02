package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.support.application.NumberingService;
import com.company.common.web.observability.ObservedOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;
import static com.company.bl.application.service.SpecimenWorkflowTransportModels.*;

@Service
class SpecimenTransportService {

    private static final String PATHOLOGY_DEPARTMENT_ID = "DEPT_PATH";
    private static final String PATHOLOGY_DEPARTMENT_NAME = "病理科";

    private final SpecimenWorkflowCommandRepository specimenWorkflowRepository;
    private final SpecimenWorkflowSupport specimenWorkflowSupport;
    private final NumberingService numberingService;

    SpecimenTransportService(SpecimenWorkflowCommandRepository specimenWorkflowRepository,
                             SpecimenWorkflowSupport specimenWorkflowSupport,
                             NumberingService numberingService) {
        this.specimenWorkflowRepository = specimenWorkflowRepository;
        this.specimenWorkflowSupport = specimenWorkflowSupport;
        this.numberingService = numberingService;
    }

    @Transactional
    @ObservedOperation(
        operation = "create_transport_order",
        successCounter = "transport_order_create_total",
        failureCounter = "transport_order_create_failed_total",
        durationMetric = "transport_order_create_duration")
    TransportOrder createTransportOrder(CreateTransportOrderCommand command) {
        Application application = specimenWorkflowSupport.getApplication(command.applicationId());
        List<Specimen> specimens = command.specimenBarcodes().stream()
            .map(specimenWorkflowSupport::getSpecimen)
            .toList();
        specimens.forEach(specimen -> {
            requireTransportReadySpecimen(specimen, command.applicationId());
            requireNoActiveTransportOrder(specimen);
        });
        if (!specimenWorkflowSupport.canTransportApplication(command.applicationId())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "All specimens of the application must be checked in before transport");
        }
        LocalDateTime now = LocalDateTime.now();
        TransportOrder order = new TransportOrder(
            "TO-" + UUID.randomUUID(),
            numberingService.generateTransportOrderNo(),
            application.getId().value(),
            TransportOrderStatus.PENDING,
            command.handoverUserId(),
            command.handoverUserName(),
            command.handoverDepartmentId(),
            command.handoverDepartmentName(),
            command.receiverDepartmentId(),
            command.receiverDepartmentName(),
            null,
            null,
            null,
            null,
            null,
            now,
            null,
            command.terminalCode(),
            command.remarks());
        specimenWorkflowRepository.insertTransportOrder(order);
        for (Specimen specimen : specimens) {
            specimenWorkflowRepository.insertTransportOrderItem(new TransportOrderItem(
                "TOI-" + UUID.randomUUID(),
                order.id(),
                command.applicationId(),
                specimen.id(),
                TransportItemStatus.PENDING,
                "MATCHED",
                command.handoverUserId(),
                command.handoverUserName(),
                now,
                command.remarks()));
            specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
                "EVT-" + UUID.randomUUID(),
                command.applicationId(),
                specimen.id(),
                null,
                order.id(),
                "TRANSPORT",
                "ORDER_CREATED",
                "SUCCESS",
                now,
                command.handoverUserId(),
                command.handoverUserName(),
                command.terminalCode(),
                "Transport order " + order.transportOrderNo() + " created"));
        }
        return order;
    }

    @Transactional
    @ObservedOperation(
        operation = "print_transport_order",
        successCounter = "transport_order_print_total",
        failureCounter = "transport_order_print_failed_total",
        durationMetric = "transport_order_print_duration")
    TransportOrder printTransportOrder(String transportOrderId, OperatorCommand command) {
        TransportOrder order = specimenWorkflowSupport.getTransportOrder(transportOrderId);
        LocalDateTime now = LocalDateTime.now();
        TransportOrder updated = specimenWorkflowRepository.updateTransportOrderStatus(
            order.id(),
            TransportOrderStatus.PRINTED,
            null,
            null,
            null,
            null,
            now,
            null);
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            updated.applicationId(),
            null,
            null,
            updated.id(),
            "TRANSPORT",
            "ORDER_PRINTED",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Transport order printed"));
        return updated;
    }

    @Transactional
    @ObservedOperation(
        operation = "handover_transport_order",
        successCounter = "transport_order_handover_total",
        failureCounter = "transport_order_handover_failed_total",
        durationMetric = "transport_order_handover_duration")
    TransportOrder handoverTransportOrder(String transportOrderId, HandoverTransportOrderCommand command) {
        TransportOrder order = specimenWorkflowSupport.getTransportOrder(transportOrderId);
        LocalDateTime now = LocalDateTime.now();
        TransportOrder updated = specimenWorkflowRepository.updateTransportOrderStatus(
            order.id(),
            TransportOrderStatus.HANDED_OVER,
            command.receiverUserId(),
            command.receiverUserName(),
            command.outboundUserId(),
            command.outboundUserName(),
            null,
            now);
        List<TransportOrderItem> items = specimenWorkflowSupport.getTransportOrderItems(transportOrderId);
        for (TransportOrderItem item : items) {
            specimenWorkflowRepository.updateTransportOrderItemStatus(
                order.id(),
                item.specimenId(),
                TransportItemStatus.HANDED_OVER,
                "MATCHED",
                command.receiverUserId(),
                command.receiverUserName(),
                now,
                command.remarks());
            specimenWorkflowRepository.updateSpecimenStatus(item.specimenId(), SpecimenStatus.IN_TRANSIT, FixationStatus.COMPLETED, null, command.remarks(), null);
            specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
                "EVT-" + UUID.randomUUID(),
                order.applicationId(),
                item.specimenId(),
                null,
                order.id(),
                "TRANSPORT",
                "HANDED_OVER",
                "SUCCESS",
                now,
                command.receiverUserId(),
                command.receiverUserName(),
                command.terminalCode(),
                "Transport handover completed"));
        }
        specimenWorkflowRepository.updateApplicationStatus(order.applicationId(), "IN_TRANSIT");
        return updated;
    }

    @Transactional
    @ObservedOperation(
        operation = "outbound_transport_order",
        successCounter = "transport_order_outbound_total",
        failureCounter = "transport_order_outbound_failed_total",
        durationMetric = "transport_order_outbound_duration")
    TransportOrder outboundTransportOrder(String transportOrderId, OutboundTransportOrderCommand command) {
        TransportOrder order = specimenWorkflowSupport.getTransportOrder(transportOrderId);
        if (!specimenWorkflowSupport.canTransportApplication(order.applicationId())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "All specimens of the application must be checked in before transport");
        }
        LocalDateTime now = LocalDateTime.now();
        TransportOrder updated = specimenWorkflowRepository.updateTransportOrderStatus(
            order.id(),
            TransportOrderStatus.HANDED_OVER,
            null,
            null,
            command.outboundUserId(),
            command.outboundUserName(),
            null,
            now);
        List<TransportOrderItem> items = specimenWorkflowSupport.getTransportOrderItems(transportOrderId);
        for (TransportOrderItem item : items) {
            specimenWorkflowRepository.updateTransportOrderItemStatus(
                order.id(),
                item.specimenId(),
                TransportItemStatus.HANDED_OVER,
                "MATCHED",
                command.outboundUserId(),
                command.outboundUserName(),
                now,
                command.remarks());
            specimenWorkflowRepository.updateSpecimenStatus(item.specimenId(), SpecimenStatus.IN_TRANSIT, FixationStatus.COMPLETED, null, command.remarks(), null);
            specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
                "EVT-" + UUID.randomUUID(),
                order.applicationId(),
                item.specimenId(),
                null,
                order.id(),
                "TRANSPORT",
                "HANDED_OVER",
                "SUCCESS",
                now,
                command.outboundUserId(),
                command.outboundUserName(),
                command.terminalCode(),
                "Transport outbound completed"));
        }
        specimenWorkflowRepository.updateApplicationStatus(order.applicationId(), "IN_TRANSIT");
        return updated;
    }

    @Transactional
    @ObservedOperation(
        operation = "quick_outbound_transport_order",
        successCounter = "transport_order_quick_outbound_total",
        failureCounter = "transport_order_quick_outbound_failed_total",
        durationMetric = "transport_order_quick_outbound_duration")
    TransportOrder quickOutboundTransportOrder(QuickOutboundTransportOrderCommand command) {
        Specimen specimen = specimenWorkflowSupport.resolveSpecimenByIdentifier(
            command.identifierType(),
            command.identifier());
        requireTransportReadySpecimen(specimen, specimen.applicationId());
        if (!specimenWorkflowSupport.canTransportApplication(specimen.applicationId())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "All specimens of the application must be checked in before transport");
        }

        TransportOrder activeOrder = specimenWorkflowSupport.findActiveTransportOrderBySpecimenId(specimen.id())
            .orElse(null);
        if (activeOrder != null) {
            if (activeOrder.status() == TransportOrderStatus.HANDED_OVER
                || activeOrder.status() == TransportOrderStatus.PARTIALLY_RECEIVED) {
                return activeOrder;
            }
            return outboundTransportOrder(
                activeOrder.id(),
                new OutboundTransportOrderCommand(
                    command.outboundUserId(),
                    command.outboundUserName(),
                    command.terminalCode(),
                    command.remarks()));
        }

        Application application = specimenWorkflowSupport.getApplication(specimen.applicationId());
        TransportOrder createdOrder = createTransportOrder(
            new CreateTransportOrderCommand(
                application.getId().value(),
                List.of(specimen.barcode()),
                null,
                resolveHandoverUserName(specimen, command.outboundUserName()),
                application.getSubmittingDepartmentId(),
                application.getSubmittingDepartmentName(),
                PATHOLOGY_DEPARTMENT_ID,
                PATHOLOGY_DEPARTMENT_NAME,
                command.terminalCode(),
                command.remarks()));
        return outboundTransportOrder(
            createdOrder.id(),
            new OutboundTransportOrderCommand(
                command.outboundUserId(),
                command.outboundUserName(),
                command.terminalCode(),
                command.remarks()));
    }

    private void requireTransportReadySpecimen(Specimen specimen, String applicationId) {
        if (!specimen.applicationId().equals(applicationId)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen does not belong to application");
        }
        if (specimenWorkflowSupport.isReceiptTerminalStatus(specimen.specimenStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen already reached receipt terminal state");
        }
        if (specimen.fixationStatus() != FixationStatus.COMPLETED) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be fixed before transport");
        }
        if (specimen.specimenConfirmedAt() == null) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be confirmed before transport");
        }
        if (!"CHECKED_IN".equalsIgnoreCase(specimenWorkflowSupport.commandCheckInStatus(specimen))) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be checked in before transport");
        }
    }

    private void requireNoActiveTransportOrder(Specimen specimen) {
        if (specimenWorkflowSupport.findActiveTransportOrderBySpecimenId(specimen.id()).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen already has an active transport order");
        }
    }

    private String resolveHandoverUserName(Specimen specimen, String outboundUserName) {
        return specimenWorkflowSupport.defaultIfBlank(specimen.checkedInByName(), outboundUserName);
    }
}
