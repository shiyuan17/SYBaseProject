package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
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
import java.util.Optional;
import java.util.UUID;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;

@Service
class SpecimenReceiptAndRemovalService {

    private static final String APPLICATION_TYPE_FROZEN = "FROZEN";
    private static final String FROZEN_REQUEST_NODE = "APPOINTMENT";
    private static final String FROZEN_REQUEST_EVENT = "FROZEN_REQUESTED";

    private final SpecimenWorkflowCommandRepository specimenWorkflowRepository;
    private final SpecimenWorkflowSupport specimenWorkflowSupport;
    private final NumberingService numberingService;

    SpecimenReceiptAndRemovalService(SpecimenWorkflowCommandRepository specimenWorkflowRepository,
                                     SpecimenWorkflowSupport specimenWorkflowSupport,
                                     NumberingService numberingService) {
        this.specimenWorkflowRepository = specimenWorkflowRepository;
        this.specimenWorkflowSupport = specimenWorkflowSupport;
        this.numberingService = numberingService;
    }

    @Transactional
    @ObservedOperation(
        operation = "receive_specimens",
        successCounter = "specimen_receive_total",
        failureCounter = "specimen_receive_failed_total",
        durationMetric = "specimen_receive_duration")
    ReceiptResult receiveSpecimens(ReceiveSpecimensCommand command) {
        TransportOrder order = specimenWorkflowSupport.getTransportOrder(command.transportOrderId());
        if (!specimenWorkflowSupport.isTransportOrderReadyForReceipt(order.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Transport order is not ready for receipt");
        }
        Application application = specimenWorkflowSupport.getApplication(order.applicationId());
        return processReceipt(
            application,
            order,
            command.receivedByUserId(),
            command.receivedByName(),
            command.logisticsStaffName(),
            command.terminalCode(),
            command.items(),
            false);
    }

    @Transactional
    @ObservedOperation(
        operation = "receive_specimens_by_barcodes",
        successCounter = "specimen_direct_receive_total",
        failureCounter = "specimen_direct_receive_failed_total",
        durationMetric = "specimen_direct_receive_duration")
    ReceiptResult receiveSpecimensByBarcodes(DirectReceiveSpecimensCommand command) {
        if (command.items().isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Receipt items are required");
        }
        List<Specimen> specimens = command.items().stream()
            .map(this::resolveReceiptSpecimen)
            .toList();
        String applicationId = specimens.get(0).applicationId();
        boolean hasMultipleApplications = specimens.stream().anyMatch(specimen -> !applicationId.equals(specimen.applicationId()));
        if (hasMultipleApplications) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Direct receipt items must belong to the same application");
        }
        Application application = specimenWorkflowSupport.getApplication(applicationId);
        return processReceipt(
            application,
            null,
            command.receivedByUserId(),
            specimenWorkflowSupport.defaultIfBlank(command.receivedByName(), command.receivedByUserId()),
            null,
            command.terminalCode(),
            command.items(),
            true);
    }

    @Transactional
    @ObservedOperation(
        operation = "confirm_specimen_removal",
        successCounter = "specimen_removal_confirm_total",
        failureCounter = "specimen_removal_confirm_failed_total",
        durationMetric = "specimen_removal_confirm_duration")
    SpecimenRemovalResult confirmSpecimenRemoval(SpecimenRemovalCommand command) {
        Specimen specimen = specimenWorkflowSupport.getSpecimen(command.specimenBarcode());
        return confirmResolvedSpecimenRemoval(specimen, command);
    }

    private SpecimenRemovalResult confirmResolvedSpecimenRemoval(Specimen specimen, SpecimenRemovalCommand command) {
        if (specimen.specimenRemovalAt() != null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen already confirmed for removal");
        }
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.confirmSpecimenRemoval(
            specimen.id(),
            now,
            command.operatorUserId(),
            command.operatorName());
        specimenWorkflowRepository.completeSpecimenVerificationFromRemoval(
            specimen.applicationId(),
            specimen.id(),
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            command.remarks());
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "REMOVAL",
            "COMPLETED",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Specimen removal time confirmed", null));
        return new SpecimenRemovalResult(
            specimen.id(),
            specimen.barcode(),
            now,
            command.operatorName());
    }

    @Transactional
    @ObservedOperation(
        operation = "quick_confirm_specimen_removal",
        successCounter = "specimen_removal_quick_confirm_total",
        failureCounter = "specimen_removal_quick_confirm_failed_total",
        durationMetric = "specimen_removal_quick_confirm_duration")
    SpecimenRemovalResult quickConfirmSpecimenRemoval(SpecimenRemovalQuickConfirmCommand command) {
        Specimen specimen = specimenWorkflowSupport.resolveSpecimenForRemoval(command.identifierType(), command.identifier());
        return confirmResolvedSpecimenRemoval(specimen, new SpecimenRemovalCommand(
            specimen.barcode(),
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            command.remarks()));
    }

    private ReceiptResult processReceipt(Application application,
                                         TransportOrder order,
                                         String receivedByUserId,
                                         String receivedByName,
                                         String logisticsStaffName,
                                         String terminalCode,
                                         List<ReceiptItem> items,
                                         boolean directReceive) {
        Optional<PathologyCase> existingCase = specimenWorkflowSupport.findPathologyCaseByApplicationId(application.getId().value());
        PathologyCase pathologyCase = existingCase.orElse(null);
        LocalDateTime now = LocalDateTime.now();
        List<TransportOrderItem> transportOrderItems = order == null ? List.of() : specimenWorkflowSupport.getTransportOrderItems(order.id());
        int receivedCount = 0;
        int processedCount = 0;
        for (ReceiptItem item : items) {
            Specimen specimen = resolveReceiptSpecimen(item);
            validateReceiptSpecimen(application, order, transportOrderItems, specimen, item, directReceive);
            if (pathologyCase == null && item.receiptStatus() == ReceiptStatus.RECEIVED) {
                String pathologyNo = APPLICATION_TYPE_FROZEN.equalsIgnoreCase(application.getApplicationType())
                    ? numberingService.generatePathologyNo(APPLICATION_TYPE_FROZEN)
                    : null;
                String initialCaseStatus = APPLICATION_TYPE_FROZEN.equalsIgnoreCase(application.getApplicationType())
                    ? "REQUESTED"
                    : "RECEIVED";
                pathologyCase = specimenWorkflowRepository.insertPathologyCase(new PathologyCase(
                    "CASE-" + UUID.randomUUID(),
                    application.getId().value(),
                    pathologyNo,
                    initialCaseStatus,
                    application.getSourceHospitalId(),
                    application.getSourceHospitalName(),
                    application.getSubmittingDepartmentId(),
                    application.getSubmittingDepartmentName(),
                    receivedByUserId,
                    receivedByName,
                    now));
                if (APPLICATION_TYPE_FROZEN.equalsIgnoreCase(application.getApplicationType())) {
                    specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
                        "EVT-" + UUID.randomUUID(),
                        application.getId().value(),
                        specimen.id(),
                        pathologyCase.id(),
                        order == null ? null : order.id(),
                        FROZEN_REQUEST_NODE,
                        FROZEN_REQUEST_EVENT,
                        "SUCCESS",
                        resolveFrozenRequestedAt(application, now),
                        application.getSubmittingDoctorUserId(),
                        application.getSubmittingDoctorName(),
                        null,
                        "创建冰冻术中申请",
                        null));
                }
            }
            String caseId = pathologyCase == null ? null : pathologyCase.id();
            specimenWorkflowRepository.insertSpecimenReceipt(
                application.getId().value(),
                caseId,
                specimen.id(),
                order == null ? null : order.id(),
                item.receiptStatus(),
                item.containerCount(),
                specimenWorkflowSupport.normalizeQualityCheckResult(item.qualityCheckResult()),
                specimenWorkflowSupport.joinQualityIssueCodes(item.qualityIssueCodes()),
                specimen.barcode(),
                receivedByUserId,
                receivedByName,
                logisticsStaffName,
                now,
                terminalCode,
                item.receiptStatus() == ReceiptStatus.REJECTED ? item.reason() : null,
                item.receiptStatus() == ReceiptStatus.RETURNED ? item.reason() : null,
                item.remarks());
            if (item.receiptStatus() == ReceiptStatus.RECEIVED) {
                receivedCount++;
                specimenWorkflowRepository.updateSpecimenStatus(specimen.id(), SpecimenStatus.RECEIVED, FixationStatus.COMPLETED, null, item.remarks(), caseId);
                if (order != null) {
                    specimenWorkflowRepository.updateTransportOrderItemStatus(
                        order.id(),
                        specimen.id(),
                        TransportItemStatus.COMPLETED,
                        "MATCHED",
                        receivedByUserId,
                        receivedByName,
                        now,
                        item.remarks());
                }
            } else {
                SpecimenStatus status = item.receiptStatus() == ReceiptStatus.REJECTED ? SpecimenStatus.REJECTED : SpecimenStatus.RETURNED;
                specimenWorkflowRepository.updateSpecimenStatus(specimen.id(), status, specimen.fixationStatus(), item.reason(), item.remarks(), null);
                if (order != null) {
                    specimenWorkflowRepository.updateTransportOrderItemStatus(
                        order.id(),
                        specimen.id(),
                        TransportItemStatus.RETURNED,
                        item.receiptStatus().name(),
                        receivedByUserId,
                        receivedByName,
                        now,
                        item.reason());
                }
            }
            processedCount++;
            specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
                "EVT-" + UUID.randomUUID(),
                application.getId().value(),
                specimen.id(),
                caseId,
                order == null ? null : order.id(),
                "RECEPTION",
                directReceive ? "DIRECT_RECEIVE" : item.receiptStatus().name(),
                directReceive ? item.receiptStatus().name() : "SUCCESS",
                now,
                receivedByUserId,
                receivedByName,
                terminalCode,
                item.reason() == null ? (directReceive ? "Specimen directly received" : "Specimen received") : item.reason(), null));
        }

        List<Specimen> allSpecimens = specimenWorkflowSupport.getSpecimensByApplicationId(application.getId().value());
        long receivedSpecimenCount = allSpecimens.stream()
            .filter(specimen -> specimen.specimenStatus() == SpecimenStatus.RECEIVED)
            .count();
        long unreceivedCount = allSpecimens.stream().filter(specimen -> specimen.specimenStatus() != SpecimenStatus.RECEIVED).count();
        String applicationStatus = unreceivedCount == 0
            ? "RECEIVED"
            : receivedSpecimenCount > 0 ? "PARTIALLY_RECEIVED" : "REJECTED";
        specimenWorkflowRepository.updateApplicationStatus(application.getId().value(), applicationStatus);
        if (order != null) {
            long terminalTransportItemCount = transportOrderItems.stream()
                .filter(transportOrderItem -> specimenWorkflowSupport.isTransportItemTerminal(transportOrderItem.status()))
                .count();
            boolean orderCompleted = terminalTransportItemCount + processedCount >= transportOrderItems.size();
            specimenWorkflowRepository.updateTransportOrderStatus(
                order.id(),
                orderCompleted ? TransportOrderStatus.COMPLETED : TransportOrderStatus.PARTIALLY_RECEIVED,
                receivedByUserId,
                receivedByName,
                null,
                null,
                null,
                order.handedOverAt());
        }
        if (pathologyCase != null) {
            specimenWorkflowRepository.ensureTechnicalSpecimenRegistrationPending(
                application.getId().value(),
                pathologyCase.id());
        }
        return new ReceiptResult(
            pathologyCase == null ? null : pathologyCase.id(),
            pathologyCase == null ? null : pathologyCase.pathologyNo(),
            applicationStatus,
            (int) unreceivedCount);
    }

    private Specimen resolveReceiptSpecimen(ReceiptItem item) {
        return specimenWorkflowSupport.resolveSpecimenByPreferredIdentifier(
            item.specimenId(),
            item.specimenBarcode(),
            item.specimenNo());
    }

    private void validateReceiptSpecimen(Application application,
                                         TransportOrder order,
                                         List<TransportOrderItem> transportOrderItems,
                                         Specimen specimen,
                                         ReceiptItem item,
                                         boolean directReceive) {
        if (!specimen.applicationId().equals(application.getId().value())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Receipt specimen does not belong to the same application");
        }
        if (order != null) {
            boolean includedInOrder = transportOrderItems.stream().anyMatch(transportOrderItem -> transportOrderItem.specimenId().equals(specimen.id()));
            if (!includedInOrder) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen does not belong to transport order");
            }
        }
        if (specimen.specimenStatus() == SpecimenStatus.RECEIVED && item.receiptStatus() == ReceiptStatus.RECEIVED) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen already received");
        }
        if (directReceive && specimen.fixationStatus() != FixationStatus.COMPLETED) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be fixed before direct receipt");
        }
        String qualityCheckResult = specimenWorkflowSupport.normalizeQualityCheckResult(item.qualityCheckResult());
        List<String> qualityIssueCodes = specimenWorkflowSupport.normalizeQualityIssueCodes(item.qualityIssueCodes());
        if (item.receiptStatus() == ReceiptStatus.RECEIVED && !"PASSED".equals(qualityCheckResult)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Received specimens must pass quality check");
        }
        if (item.receiptStatus() != ReceiptStatus.RECEIVED && specimenWorkflowSupport.blank(item.reason())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Rejected or returned specimens must provide a reason");
        }
        if ("FAILED".equals(qualityCheckResult) && qualityIssueCodes.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Failed quality checks must provide issue codes");
        }
    }

    private LocalDateTime resolveFrozenRequestedAt(Application application, LocalDateTime fallback) {
        if (application.getSubmissionDate() != null) {
            return application.getSubmissionDate().atStartOfDay();
        }
        if (application.getCreatedAt() != null) {
            return application.getCreatedAt();
        }
        return fallback;
    }
}
