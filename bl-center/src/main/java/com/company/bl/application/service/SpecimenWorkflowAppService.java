package com.company.bl.application.service;

import com.company.bl.application.gateway.LabelPrintGateway;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.infrastructure.observability.ObservedOperation;
import com.company.bl.support.application.NumberingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpecimenWorkflowAppService {

    private final ApplicationRepository applicationRepository;
    private final SpecimenWorkflowRepository specimenWorkflowRepository;
    private final NumberingService numberingService;
    private final LabelPrintGateway labelPrintGateway;

    @Transactional
    public SpecimenRegistrationResult registerSpecimens(RegisterSpecimensCommand command) {
        Application application = getApplication(command.applicationId());
        LocalDateTime now = LocalDateTime.now();
        String labelPrintBatchNo = "LP-" + UUID.randomUUID();
        List<Specimen> specimens = new ArrayList<>();
        for (SpecimenRegistrationItem item : command.items()) {
            String specimenNo = numberingService.generateSpecimenNo(command.applicationId());
            String barcode = blank(item.barcode()) ? application.getApplicationNo() + "-" + specimenNo : item.barcode().trim();
            ensureBarcodeAvailable(barcode);
            Specimen specimen = new Specimen(
                "SP-" + UUID.randomUUID(),
                command.applicationId(),
                null,
                specimenNo,
                barcode,
                trim(item.specimenType()),
                trim(item.specimenNameStandardized()),
                trim(item.specimenSite()),
                trim(item.collectionMode()),
                item.specimenCount(),
                SpecimenStatus.REGISTERED,
                FixationStatus.PENDING,
                true,
                null,
                defaultIfBlank(item.clinicalSymptom(), application.getClinicalSymptom()),
                application.getSubmittingDepartmentId(),
                application.getSubmittingDepartmentName(),
                application.getSubmittingDoctorUserId(),
                application.getSubmittingDoctorName(),
                application.getSubmissionDate(),
                labelPrintBatchNo,
                "PENDING",
                trim(command.operatorUserId()),
                trim(command.operatorName()),
                now,
                trim(command.terminalCode()),
                trim(command.remarks()));
            specimenWorkflowRepository.insertSpecimen(specimen);
            specimenWorkflowRepository.insertCollectionRecord(
                command.applicationId(),
                specimen.id(),
                "COLLECTED",
                defaultIfBlank(command.collectionScene(), "OPERATING_ROOM"),
                specimen.collectionMode(),
                labelPrintBatchNo,
                command.operatorUserId(),
                command.operatorName(),
                now,
                command.terminalCode(),
                command.remarks());
            specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
                "EVT-" + UUID.randomUUID(),
                command.applicationId(),
                specimen.id(),
                null,
                null,
                "SPECIMEN_COLLECTION",
                "REGISTERED",
                "SUCCESS",
                now,
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                "Registered specimen " + specimen.barcode()));
            specimens.add(specimen);
        }

        LabelPrintGateway.LabelPrintResult printResult = labelPrintGateway.print(
            new LabelPrintGateway.LabelPrintRequest(
                command.applicationId(),
                labelPrintBatchNo,
                command.printerCode(),
                specimens.stream().map(Specimen::barcode).toList()));
        String labelPrintStatus = printResult.success() ? "SUCCESS" : "FAILED";
        List<Specimen> updatedSpecimens = new ArrayList<>();
        for (Specimen specimen : specimens) {
            specimenWorkflowRepository.updateSpecimenLabelPrintStatus(specimen.id(), labelPrintStatus);
            specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
                "EVT-" + UUID.randomUUID(),
                command.applicationId(),
                specimen.id(),
                null,
                null,
                "LABEL_PRINT",
                "PRINTED",
                printResult.success() ? "SUCCESS" : "FAILED",
                now,
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                printResult.message()));
            updatedSpecimens.add(copyWithLabelPrintStatus(specimen, labelPrintStatus));
        }
        specimenWorkflowRepository.updateApplicationStatus(command.applicationId(), "SUBMITTED");
        return new SpecimenRegistrationResult(updatedSpecimens, labelPrintBatchNo, printResult.success(), printResult.message());
    }

    @Transactional
    public FixationResult startFixation(FixationCommand command) {
        Specimen specimen = getSpecimen(command.specimenBarcode());
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.upsertFixationRecord(
            specimen.applicationId(),
            specimen.id(),
            FixationStatus.FIXING,
            command.fixationLiquidType(),
            now,
            null,
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.terminalCode(),
            command.remarks());
        specimenWorkflowRepository.updateSpecimenStatus(specimen.id(), SpecimenStatus.FIXING, FixationStatus.FIXING, null, command.remarks(), null);
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "FIXATION",
            "STARTED",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Fixation started"));
        return new FixationResult(specimen.id(), specimen.barcode(), FixationStatus.FIXING.name());
    }

    @Transactional
    public FixationResult completeFixation(FixationCommand command) {
        Specimen specimen = getSpecimen(command.specimenBarcode());
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.upsertFixationRecord(
            specimen.applicationId(),
            specimen.id(),
            FixationStatus.COMPLETED,
            command.fixationLiquidType(),
            null,
            now,
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.terminalCode(),
            command.remarks());
        specimenWorkflowRepository.updateSpecimenStatus(specimen.id(), SpecimenStatus.FIXED, FixationStatus.COMPLETED, null, command.remarks(), null);
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "FIXATION",
            "COMPLETED",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Fixation completed"));
        return new FixationResult(specimen.id(), specimen.barcode(), FixationStatus.COMPLETED.name());
    }

    @Transactional
    public TransportOrder createTransportOrder(CreateTransportOrderCommand command) {
        Application application = getApplication(command.applicationId());
        List<Specimen> specimens = command.specimenBarcodes().stream()
            .map(this::getSpecimen)
            .toList();
        specimens.forEach(specimen -> {
            if (!specimen.applicationId().equals(command.applicationId())) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen does not belong to application");
            }
            if (specimen.fixationStatus() != FixationStatus.COMPLETED) {
                throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be fixed before transport");
            }
        });
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
    public TransportOrder printTransportOrder(String transportOrderId, OperatorCommand command) {
        TransportOrder order = getTransportOrder(transportOrderId);
        LocalDateTime now = LocalDateTime.now();
        TransportOrder updated = specimenWorkflowRepository.updateTransportOrderStatus(
            order.id(),
            TransportOrderStatus.PRINTED,
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
    public TransportOrder handoverTransportOrder(String transportOrderId, HandoverTransportOrderCommand command) {
        TransportOrder order = getTransportOrder(transportOrderId);
        LocalDateTime now = LocalDateTime.now();
        TransportOrder updated = specimenWorkflowRepository.updateTransportOrderStatus(
            order.id(),
            TransportOrderStatus.HANDED_OVER,
            command.receiverUserId(),
            command.receiverUserName(),
            null,
            now);
        List<TransportOrderItem> items = specimenWorkflowRepository.findTransportOrderItems(transportOrderId);
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
    public ReceiptResult receiveSpecimens(ReceiveSpecimensCommand command) {
        TransportOrder order = getTransportOrder(command.transportOrderId());
        if (order.status() != TransportOrderStatus.HANDED_OVER && order.status() != TransportOrderStatus.PRINTED) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Transport order is not ready for receipt");
        }
        Application application = getApplication(order.applicationId());
        return processReceipt(
            application,
            order,
            command.receivedByUserId(),
            command.receivedByName(),
            command.terminalCode(),
            command.items(),
            false);
    }

    @Transactional
    public ReceiptResult receiveSpecimensByBarcodes(DirectReceiveSpecimensCommand command) {
        if (command.items().isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Receipt items are required");
        }
        List<Specimen> specimens = command.items().stream()
            .map(item -> getSpecimen(item.specimenBarcode()))
            .toList();
        String applicationId = specimens.get(0).applicationId();
        boolean hasMultipleApplications = specimens.stream().anyMatch(specimen -> !applicationId.equals(specimen.applicationId()));
        if (hasMultipleApplications) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Direct receipt items must belong to the same application");
        }
        Application application = getApplication(applicationId);
        return processReceipt(
            application,
            null,
            command.receivedByUserId(),
            defaultIfBlank(command.receivedByName(), command.receivedByUserId()),
            command.terminalCode(),
            command.items(),
            true);
    }

    @Transactional
    public LabelPrintRetryResult retryLabelPrint(RetryLabelPrintCommand command) {
        List<Specimen> failedSpecimens = specimenWorkflowRepository.findSpecimensByLabelPrintBatchNoAndStatus(
            command.labelPrintBatchNo(),
            "FAILED");
        if (failedSpecimens.isEmpty()) {
            return new LabelPrintRetryResult(command.labelPrintBatchNo(), 0, 0, 0, true, "No failed labels found for retry");
        }
        LabelPrintGateway.LabelPrintResult printResult = labelPrintGateway.print(
            new LabelPrintGateway.LabelPrintRequest(
                failedSpecimens.get(0).applicationId(),
                command.labelPrintBatchNo(),
                command.printerCode(),
                failedSpecimens.stream().map(Specimen::barcode).toList()));
        LocalDateTime now = LocalDateTime.now();
        String labelPrintStatus = printResult.success() ? "SUCCESS" : "FAILED";
        for (Specimen specimen : failedSpecimens) {
            specimenWorkflowRepository.updateSpecimenLabelPrintStatus(specimen.id(), labelPrintStatus);
            specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
                "EVT-" + UUID.randomUUID(),
                specimen.applicationId(),
                specimen.id(),
                specimen.caseId(),
                null,
                "LABEL_PRINT",
                "RETRY",
                labelPrintStatus,
                now,
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                printResult.message()));
        }
        int total = failedSpecimens.size();
        return new LabelPrintRetryResult(
            command.labelPrintBatchNo(),
            total,
            printResult.success() ? total : 0,
            printResult.success() ? 0 : total,
            printResult.success(),
            printResult.message());
    }

    @Transactional(readOnly = true)
    public PendingSpecimenPage listPendingFixations(PendingSpecimenQuery query) {
        SpecimenWorkflowRepository.PagedPendingSpecimens page = specimenWorkflowRepository.findPendingFixations(
            new SpecimenWorkflowRepository.PendingSpecimenQuery(
                normalizePage(query.page()),
                normalizeSize(query.size()),
                trim(query.applicationId()),
                trim(query.departmentId()),
                parseDateFrom(query.dateFrom()),
                parseDateTo(query.dateTo())));
        return new PendingSpecimenPage(
            page.items().stream().map(this::toPendingItem).toList(),
            normalizePage(query.page()),
            normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    public PendingSpecimenPage listPendingReceipts(PendingSpecimenQuery query) {
        SpecimenWorkflowRepository.PagedPendingSpecimens page = specimenWorkflowRepository.findPendingReceipts(
            new SpecimenWorkflowRepository.PendingSpecimenQuery(
                normalizePage(query.page()),
                normalizeSize(query.size()),
                trim(query.applicationId()),
                trim(query.departmentId()),
                parseDateFrom(query.dateFrom()),
                parseDateTo(query.dateTo())));
        return new PendingSpecimenPage(
            page.items().stream().map(this::toPendingItem).toList(),
            normalizePage(query.page()),
            normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    @ObservedOperation(
        operation = "get_application",
        successCounter = "application_query_total",
        failureCounter = "application_query_failed_total",
        durationMetric = "application_query_duration")
    public ApplicationTracking getApplicationTracking(String applicationId) {
        Application application = getApplication(applicationId);
        return specimenWorkflowRepository.getApplicationTracking(applicationId, application);
    }

    @Transactional(readOnly = true)
    public ApplicationTracking getTrackingByBarcode(String barcode) {
        String applicationId = specimenWorkflowRepository.findApplicationIdByBarcode(barcode)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen barcode not found"));
        return getApplicationTracking(applicationId);
    }

    private Application getApplication(String applicationId) {
        return applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new ApplicationDomainException(com.company.bl.domain.enums.ApplicationErrorCode.APPLICATION_NOT_FOUND, 404));
    }

    private Specimen getSpecimen(String barcode) {
        return specimenWorkflowRepository.findSpecimenByBarcode(barcode)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen barcode not found"));
    }

    private TransportOrder getTransportOrder(String transportOrderId) {
        return specimenWorkflowRepository.findTransportOrderById(transportOrderId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Transport order not found"));
    }

    private ReceiptResult processReceipt(Application application,
                                         TransportOrder order,
                                         String receivedByUserId,
                                         String receivedByName,
                                         String terminalCode,
                                         List<ReceiptItem> items,
                                         boolean directReceive) {
        Optional<PathologyCase> existingCase = specimenWorkflowRepository.findPathologyCaseByApplicationId(application.getId().value());
        PathologyCase pathologyCase = existingCase.orElse(null);
        LocalDateTime now = LocalDateTime.now();
        List<TransportOrderItem> transportOrderItems = order == null ? List.of() : specimenWorkflowRepository.findTransportOrderItems(order.id());
        int receivedCount = 0;
        int processedCount = 0;
        for (ReceiptItem item : items) {
            Specimen specimen = getSpecimen(item.specimenBarcode());
            validateReceiptSpecimen(application, order, transportOrderItems, specimen, item, directReceive);
            if (pathologyCase == null && item.receiptStatus() == ReceiptStatus.RECEIVED) {
                pathologyCase = specimenWorkflowRepository.insertPathologyCase(new PathologyCase(
                    "CASE-" + UUID.randomUUID(),
                    application.getId().value(),
                    numberingService.generatePathologyNo(),
                    "RECEIVED",
                    application.getSourceHospitalId(),
                    application.getSourceHospitalName(),
                    application.getSubmittingDepartmentId(),
                    application.getSubmittingDepartmentName(),
                    receivedByUserId,
                    receivedByName,
                    now));
            }
            String caseId = pathologyCase == null ? null : pathologyCase.id();
            specimenWorkflowRepository.insertSpecimenReceipt(
                application.getId().value(),
                caseId,
                specimen.id(),
                order == null ? null : order.id(),
                item.receiptStatus(),
                item.containerCount(),
                specimen.barcode(),
                receivedByUserId,
                receivedByName,
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
                item.reason() == null ? (directReceive ? "Specimen directly received" : "Specimen received") : item.reason()));
        }

        List<Specimen> allSpecimens = specimenWorkflowRepository.findSpecimensByApplicationId(application.getId().value());
        long unreceivedCount = allSpecimens.stream().filter(specimen -> specimen.specimenStatus() != SpecimenStatus.RECEIVED).count();
        String applicationStatus = unreceivedCount == 0
            ? "RECEIVED"
            : receivedCount > 0 ? "PARTIALLY_RECEIVED" : "REJECTED";
        specimenWorkflowRepository.updateApplicationStatus(application.getId().value(), applicationStatus);
        if (order != null) {
            specimenWorkflowRepository.updateTransportOrderStatus(
                order.id(),
                unreceivedCount == 0 ? TransportOrderStatus.COMPLETED : TransportOrderStatus.PARTIALLY_RECEIVED,
                receivedByUserId,
                receivedByName,
                null,
                order.handedOverAt());
        }
        if (pathologyCase != null) {
            specimenWorkflowRepository.upsertTechnicalPendingTask(
                application.getId().value(),
                pathologyCase.id(),
                "pathologyNo=" + pathologyCase.pathologyNo() + ";receivedCount=" + receivedCount + ";processedCount=" + processedCount);
        }
        return new ReceiptResult(
            pathologyCase == null ? null : pathologyCase.id(),
            pathologyCase == null ? null : pathologyCase.pathologyNo(),
            applicationStatus,
            (int) unreceivedCount);
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
    }

    private void ensureBarcodeAvailable(String barcode) {
        if (specimenWorkflowRepository.findSpecimenByBarcode(barcode).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen barcode already exists");
        }
    }

    private PendingSpecimenItem toPendingItem(SpecimenWorkflowRepository.PendingSpecimenRow row) {
        return new PendingSpecimenItem(
            row.applicationId(),
            row.applicationNo(),
            row.patientName(),
            row.submittingDepartmentId(),
            row.submittingDepartmentName(),
            row.specimenId(),
            row.specimenNo(),
            row.barcode(),
            row.specimenStatus(),
            row.fixationStatus(),
            row.registeredAt(),
            row.latestTrackingAt(),
            row.abnormalFlag());
    }

    private Specimen copyWithLabelPrintStatus(Specimen specimen, String labelPrintStatus) {
        return new Specimen(
            specimen.id(),
            specimen.applicationId(),
            specimen.caseId(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenType(),
            specimen.specimenNameStandardized(),
            specimen.specimenSite(),
            specimen.collectionMode(),
            specimen.specimenCount(),
            specimen.specimenStatus(),
            specimen.fixationStatus(),
            specimen.qualified(),
            specimen.unqualifiedReason(),
            specimen.clinicalSymptom(),
            specimen.applicantDepartmentId(),
            specimen.applicantDepartmentName(),
            specimen.applicantDoctorUserId(),
            specimen.applicantDoctorName(),
            specimen.submissionDate(),
            specimen.labelPrintBatchNo(),
            labelPrintStatus,
            specimen.registeredByUserId(),
            specimen.registeredByName(),
            specimen.registeredAt(),
            specimen.terminalCode(),
            specimen.remarks());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String defaultIfBlank(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 200);
    }

    private LocalDateTime parseDateFrom(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    private LocalDateTime parseDateTo(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay();
    }

    public record RegisterSpecimensCommand(
        String applicationId,
        String printerCode,
        String collectionScene,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        List<SpecimenRegistrationItem> items
    ) {
    }

    public record SpecimenRegistrationItem(
        String specimenNameStandardized,
        String specimenType,
        String specimenSite,
        String collectionMode,
        Integer specimenCount,
        String barcode,
        String clinicalSymptom
    ) {
    }

    public record SpecimenRegistrationResult(
        List<Specimen> specimens,
        String labelPrintBatchNo,
        boolean labelPrintSuccess,
        String labelPrintMessage
    ) {
    }

    public record FixationCommand(
        String specimenBarcode,
        String fixationLiquidType,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record FixationResult(String specimenId, String barcode, String fixationStatus) {
    }

    public record CreateTransportOrderCommand(
        String applicationId,
        List<String> specimenBarcodes,
        String handoverUserId,
        String handoverUserName,
        String handoverDepartmentId,
        String handoverDepartmentName,
        String receiverDepartmentId,
        String receiverDepartmentName,
        String terminalCode,
        String remarks
    ) {
    }

    public record OperatorCommand(String operatorUserId, String operatorName, String terminalCode) {
    }

    public record HandoverTransportOrderCommand(
        String receiverUserId,
        String receiverUserName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ReceiveSpecimensCommand(
        String transportOrderId,
        String receivedByUserId,
        String receivedByName,
        String terminalCode,
        List<ReceiptItem> items
    ) {
    }

    public record DirectReceiveSpecimensCommand(
        String receivedByUserId,
        String receivedByName,
        String terminalCode,
        List<ReceiptItem> items
    ) {
    }

    public record ReceiptItem(
        String specimenBarcode,
        ReceiptStatus receiptStatus,
        Integer containerCount,
        String reason,
        String remarks
    ) {
    }

    public record ReceiptResult(
        String caseId,
        String pathologyNo,
        String receiptStatus,
        int unreceivedCount
    ) {
    }

    public record RetryLabelPrintCommand(
        String labelPrintBatchNo,
        String operatorUserId,
        String operatorName,
        String printerCode,
        String terminalCode,
        String remarks
    ) {
    }

    public record LabelPrintRetryResult(
        String labelPrintBatchNo,
        int retriedCount,
        int successCount,
        int failedCount,
        boolean allSuccessful,
        String message
    ) {
    }

    public record PendingSpecimenQuery(
        int page,
        int size,
        String applicationId,
        String departmentId,
        String dateFrom,
        String dateTo
    ) {
    }

    public record PendingSpecimenPage(
        List<PendingSpecimenItem> items,
        int page,
        int size,
        long total
    ) {
    }

    public record PendingSpecimenItem(
        String applicationId,
        String applicationNo,
        String patientName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String specimenId,
        String specimenNo,
        String barcode,
        String specimenStatus,
        String fixationStatus,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }
}
