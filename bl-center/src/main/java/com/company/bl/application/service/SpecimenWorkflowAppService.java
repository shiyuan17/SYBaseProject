package com.company.bl.application.service;

import com.company.bl.application.gateway.LabelPrintGateway;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.ApplicationStatus;
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
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
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
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class SpecimenWorkflowAppService {
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    private final ApplicationRepository applicationRepository;
    private final ApplicationRegistrationWorkbenchRepository workbenchRepository;
    private final SpecimenWorkflowRepository specimenWorkflowRepository;
    private final NumberingService numberingService;
    private final LabelPrintGateway labelPrintGateway;

    @Transactional
    public SpecimenRegistrationResult registerSpecimens(RegisterSpecimensCommand command) {
        Application application = getApplication(command.applicationId());
        validateApplicationCanRegister(application);
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
                trim(item.containerName()),
                item.containerCount(),
                SpecimenStatus.REGISTERED,
                FixationStatus.PENDING,
                "UNVERIFIED",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                null,
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
                trim(command.printerCode()),
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
        if (!"VERIFIED".equals(specimen.verificationStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be verified before fixation");
        }
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.upsertFixationRecord(
            specimen.applicationId(),
            specimen.id(),
            FixationStatus.FIXING,
            command.fixationLiquidType(),
            now,
            null,
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
    public SpecimenVerificationResult startSpecimenVerification(SpecimenVerificationCommand command) {
        Specimen specimen = getSpecimen(command.specimenBarcode());
        if (isReceiptTerminalStatus(specimen.specimenStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen already reached receipt terminal status");
        }
        if ("VERIFYING".equals(specimen.verificationStatus()) || "VERIFIED".equals(specimen.verificationStatus())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen verification already started");
        }
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.startSpecimenVerification(
            specimen.applicationId(),
            specimen.id(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.terminalCode(),
            command.remarks());
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "VERIFICATION",
            "STARTED",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Specimen verification started"));
        return buildSpecimenVerificationResult(getSpecimen(command.specimenBarcode()));
    }

    @Transactional
    public SpecimenVerificationResult completeSpecimenVerification(SpecimenVerificationCommand command) {
        Specimen specimen = getSpecimen(command.specimenBarcode());
        if (!"VERIFYING".equals(specimen.verificationStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen verification must be started before completion");
        }
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.completeSpecimenVerification(
            specimen.id(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.terminalCode(),
            command.remarks());
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "VERIFICATION",
            "COMPLETED",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Specimen verification completed"));
        return buildSpecimenVerificationResult(getSpecimen(command.specimenBarcode()));
    }

    @Transactional
    public Specimen confirmSpecimen(ConfirmSpecimenCommand command) {
        Specimen specimen = getSpecimen(command.specimenBarcode());
        if (isReceiptTerminalStatus(specimen.specimenStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen already reached receipt terminal status");
        }
        if (specimen.fixationStatus() != FixationStatus.COMPLETED) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must complete fixation before confirmation");
        }
        if (specimen.specimenConfirmedAt() != null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen already confirmed");
        }
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.confirmSpecimen(specimen.id(), now);
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "CONFIRMATION",
            "COMPLETED",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Specimen confirmation completed"));
        return getSpecimen(command.specimenBarcode());
    }

    @Transactional
    public Specimen checkInSpecimen(CheckInSpecimenCommand command) {
        Specimen specimen = getSpecimen(command.specimenBarcode());
        if (isReceiptTerminalStatus(specimen.specimenStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen already reached receipt terminal status");
        }
        if (specimen.specimenConfirmedAt() == null) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be confirmed before check-in");
        }
        if ("CHECKED_IN".equalsIgnoreCase(commandCheckInStatus(specimen))) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen already checked in");
        }
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.checkInSpecimen(
            specimen.id(),
            "CHECKED_IN",
            now,
            command.operatorUserId(),
            command.operatorName());
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "CHECK_IN",
            "CHECKED_IN",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Specimen check-in completed"));
        return getSpecimen(command.specimenBarcode());
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
            if (specimen.specimenConfirmedAt() == null) {
                throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be confirmed before transport");
            }
            if (!"CHECKED_IN".equalsIgnoreCase(commandCheckInStatus(specimen))) {
                throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be checked in before transport");
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
        if (!isTransportOrderReadyForReceipt(order.status())) {
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
        List<Specimen> failedSpecimens = specimenWorkflowRepository.findSpecimensByLabelPrintBatchNoAndStatuses(
            command.labelPrintBatchNo(),
            List.of("FAILED", "PENDING"));
        if (failedSpecimens.isEmpty()) {
            return new LabelPrintRetryResult(command.labelPrintBatchNo(), 0, 0, 0, true, "No pending or failed labels found for retry");
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
                trim(query.specimenNo()),
                trim(query.departmentId()),
                trim(query.fixationStatus()),
                trim(query.verificationStatus()),
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
                trim(query.specimenNo()),
                trim(query.departmentId()),
                null,
                null,
                parseDateFrom(query.dateFrom()),
                parseDateTo(query.dateTo())));
        return new PendingSpecimenPage(
            page.items().stream().map(this::toPendingItem).toList(),
            normalizePage(query.page()),
            normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    public List<SpecimenVerificationRecord> listSpecimenVerificationRecords(String barcode) {
        if (blank(barcode)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen barcode is required");
        }
        return specimenWorkflowRepository.listSpecimenVerificationRecords(trim(barcode)).stream()
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
    public PendingTransportOrderPage listPendingTransportOrders(PendingTransportOrderQuery query) {
        SpecimenWorkflowRepository.PagedPendingTransportOrders page =
            specimenWorkflowRepository.findPendingTransportOrders(
                new SpecimenWorkflowRepository.PendingTransportOrderQuery(
                    normalizePage(query.page()),
                    normalizeSize(query.size()),
                    trim(query.applicationId()),
                    trim(query.specimenNo()),
                    trim(query.departmentId()),
                    parseDateFrom(query.dateFrom()),
                    parseDateTo(query.dateTo()),
                    normalizeStatus(query.status())));
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
            normalizePage(query.page()),
            normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    public ApplicationPage listApplications(ApplicationListQuery query) {
        int page = normalizePage(query.page());
        int size = normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedApplications result =
            specimenWorkflowRepository.findApplications(
                new SpecimenWorkflowRepository.ApplicationListQuery(
                    page,
                    size,
                    trim(query.applicationNo()),
                    trim(query.patientName()),
                    trim(query.submittingDepartmentId()),
                    normalizeStatus(query.applicationType()),
                    normalizeStatus(query.applicationFormStatus()),
                    parseLocalDateFrom(query.dateFrom()),
                    parseLocalDateTo(query.dateTo())));
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
    public DuplicateCheckResult checkApplicationDuplicate(DuplicateCheckCommand command) {
        String patientId = trim(command.patientId());
        String patientName = trim(command.patientName());
        String externalOrderNo = trim(command.externalOrderNo());
        LocalDate applicationDate = parseLocalDate(command.applicationDate());
        String applicationType = normalizeStatus(command.applicationType());
        String specimenSite = trim(command.specimenSite());
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
    public SpecimenManagementListPage listSpecimenManagementItems(SpecimenManagementListQuery query) {
        int page = normalizePage(query.page());
        int size = normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedSpecimenManagementItems result =
            specimenWorkflowRepository.findSpecimenManagementItems(
                new SpecimenWorkflowRepository.SpecimenManagementListQuery(
                    page,
                    size,
                    trim(query.keyword()),
                    trim(query.applicationNo()),
                    trim(query.departmentId()),
                    normalizeStatus(query.specimenStatus()),
                    normalizeStatus(query.labelPrintStatus()),
                    query.abnormalFlag(),
                    parseDateFrom(query.dateFrom()),
                    parseDateTo(query.dateTo())));
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
    public SpecimenRemovalListPage listSpecimenRemovalItems(SpecimenRemovalQuery query) {
        int page = normalizePage(query.page());
        int size = normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedSpecimenRemovalItems result =
            specimenWorkflowRepository.findSpecimenRemovalItems(
                new SpecimenWorkflowRepository.SpecimenRemovalListQuery(
                    page,
                    size,
                    trim(query.keyword()),
                    trim(query.applicationNo()),
                    trim(query.departmentId()),
                    normalizeStatus(query.specimenStatus()),
                    query.abnormalFlag(),
                    parseDateFrom(query.dateFrom()),
                    parseDateTo(query.dateTo())));
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

    @Transactional
    public SpecimenRemovalResult confirmSpecimenRemoval(SpecimenRemovalCommand command) {
        Specimen specimen = getSpecimen(command.specimenBarcode());
        if (specimen.specimenRemovalAt() != null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen already confirmed for removal");
        }
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.confirmSpecimenRemoval(
            specimen.id(),
            now,
            command.operatorUserId(),
            command.operatorName());
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
            "Specimen removal time confirmed"));
        return new SpecimenRemovalResult(
            specimen.id(),
            specimen.barcode(),
            now,
            command.operatorName());
    }

    @Transactional(readOnly = true)
    public byte[] exportSpecimenRemovalItems(SpecimenRemovalQuery query) {
        List<SpecimenWorkflowRepository.SpecimenRemovalListRow> rows = specimenWorkflowRepository.listSpecimenRemovalExportRows(
            new SpecimenWorkflowRepository.SpecimenRemovalListQuery(
                normalizePage(query.page()),
                normalizeSize(query.size()),
                trim(query.keyword()),
                trim(query.applicationNo()),
                trim(query.departmentId()),
                normalizeStatus(query.specimenStatus()),
                query.abnormalFlag(),
                parseDateFrom(query.dateFrom()),
                parseDateTo(query.dateTo())));
        return buildSpecimenRemovalExport(rows);
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

    @Transactional(readOnly = true)
    public LatestSpecimenRegistrationResult getLatestRegistrationResult(String applicationId) {
        getApplication(applicationId);
        List<Specimen> specimens = specimenWorkflowRepository.findSpecimensByApplicationId(applicationId);
        if (specimens.isEmpty()) {
            return new LatestSpecimenRegistrationResult(applicationId, List.of(), null, false, null, null);
        }
        String latestBatchNo = resolveLatestLabelPrintBatchNo(specimens);
        if (latestBatchNo == null) {
            return new LatestSpecimenRegistrationResult(applicationId, List.of(), null, false, null, null);
        }
        List<Specimen> batchSpecimens = specimens.stream()
            .filter(specimen -> latestBatchNo.equals(specimen.labelPrintBatchNo()))
            .toList();
        List<TrackingEvent> trackingEvents = specimenWorkflowRepository.findTrackingEventsByApplicationId(applicationId);
        String latestPrintMessage = resolveLatestBatchLabelPrintMessage(trackingEvents, batchSpecimens);
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
    public ApplicationListItem getRegistrationApplicationByApplicationNo(String applicationNo) {
        if (blank(applicationNo)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Application number is required");
        }
        return applicationRepository.findByApplicationNo(applicationNo.trim())
            .map(application -> toApplicationListItem(specimenWorkflowRepository.getApplicationTracking(
                application.getId().value(),
                application)))
            .orElseThrow(() -> new ApplicationDomainException(com.company.bl.domain.enums.ApplicationErrorCode.APPLICATION_NOT_FOUND, 404));
    }

    private Application getApplication(String applicationId) {
        return applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new ApplicationDomainException(com.company.bl.domain.enums.ApplicationErrorCode.APPLICATION_NOT_FOUND, 404));
    }

    private Specimen getSpecimen(String barcode) {
        return specimenWorkflowRepository.findSpecimenByBarcode(barcode)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen barcode not found"));
    }

    private SpecimenVerificationResult buildSpecimenVerificationResult(Specimen specimen) {
        return new SpecimenVerificationResult(
            specimen.id(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenNameStandardized(),
            specimen.specimenType(),
            specimen.specimenSite(),
            specimen.collectionMode(),
            specimen.clinicalSymptom(),
            specimen.specimenCount(),
            specimen.containerName(),
            specimen.containerCount(),
            specimen.specimenStatus() == null ? null : specimen.specimenStatus().name(),
            specimen.fixationStatus() == null ? null : specimen.fixationStatus().name(),
            specimen.verificationStatus(),
            specimen.verificationStartedAt(),
            specimen.verificationCompletedAt(),
            specimen.labelPrintStatus(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            specimen.unqualifiedReason());
    }

    private TransportOrder getTransportOrder(String transportOrderId) {
        return specimenWorkflowRepository.findTransportOrderById(transportOrderId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Transport order not found"));
    }

    private void validateApplicationCanRegister(Application application) {
        ApplicationStatus status = application.getStatus();
        if (status == ApplicationStatus.DRAFT || status == ApplicationStatus.SUBMITTED) {
            return;
        }
        throw new BlBusinessException(
            BlErrorCode.OPERATION_NOT_ALLOWED,
            409,
            "Application status does not allow specimen registration: " + status.name());
    }

    private String resolveLatestLabelPrintBatchNo(List<Specimen> specimens) {
        String latestBatchNo = null;
        for (Specimen specimen : specimens) {
            if (!blank(specimen.labelPrintBatchNo())) {
                latestBatchNo = specimen.labelPrintBatchNo().trim();
            }
        }
        return latestBatchNo;
    }

    private String resolveLatestBatchLabelPrintStatus(List<Specimen> specimens, String batchNo) {
        String resolvedStatus = null;
        int resolvedPriority = 0;
        for (Specimen specimen : specimens) {
            if (!batchNo.equals(specimen.labelPrintBatchNo())) {
                continue;
            }
            String status = normalizeStatus(specimen.labelPrintStatus());
            int priority = labelPrintStatusPriority(status);
            if (priority > resolvedPriority) {
                resolvedStatus = status;
                resolvedPriority = priority;
            }
        }
        return resolvedStatus;
    }

    private int labelPrintStatusPriority(String status) {
        if ("FAILED".equals(status)) {
            return 3;
        }
        if ("PENDING".equals(status)) {
            return 2;
        }
        if ("SUCCESS".equals(status)) {
            return 1;
        }
        return 0;
    }

    private String resolveLatestBatchLabelPrintMessage(List<TrackingEvent> events, List<Specimen> batchSpecimens) {
        if (batchSpecimens.isEmpty()) {
            return null;
        }
        Set<String> batchSpecimenIds = new HashSet<>();
        for (Specimen specimen : batchSpecimens) {
            batchSpecimenIds.add(specimen.id());
        }
        String latestMessage = null;
        for (TrackingEvent event : events) {
            if (!"LABEL_PRINT".equals(event.nodeCode())) {
                continue;
            }
            if (event.specimenId() == null || !batchSpecimenIds.contains(event.specimenId())) {
                continue;
            }
            latestMessage = event.eventContent();
        }
        return latestMessage;
    }

    private byte[] buildSpecimenRemovalExport(List<SpecimenWorkflowRepository.SpecimenRemovalListRow> rows) {
        List<List<String>> sheetRows = new ArrayList<>();
        sheetRows.add(List.of(
            "标本ID",
            "申请单",
            "标本编号",
            "姓名",
            "住院号",
            "性别",
            "手术间",
            "标本名称",
            "标本状态",
            "类型",
            "离体时间",
            "离体操作人",
            "添加时间",
            "添加人"
        ));
        for (SpecimenWorkflowRepository.SpecimenRemovalListRow row : rows) {
            sheetRows.add(List.of(
                defaultString(row.barcode()),
                defaultString(row.applicationNo()),
                defaultString(row.specimenNo()),
                defaultString(row.patientName()),
                defaultString(row.inpatientNo()),
                defaultString(row.patientGender()),
                defaultString(row.surgeryName()),
                defaultString(row.specimenName()),
                defaultString(row.specimenStatus()),
                defaultString(row.specimenType()),
                formatExportDateTime(row.specimenRemovalAt()),
                defaultString(row.specimenRemovalOperatorName()),
                formatExportDateTime(row.registeredAt()),
                defaultString(row.registeredByName())
            ));
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            writeZipEntry(zipOutputStream, "[Content_Types].xml", contentTypesXml());
            writeZipEntry(zipOutputStream, "_rels/.rels", rootRelsXml());
            writeZipEntry(zipOutputStream, "xl/workbook.xml", workbookXml());
            writeZipEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            writeZipEntry(zipOutputStream, "xl/worksheets/sheet1.xml", worksheetXml(sheetRows));
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build specimen removal export", exception);
        }
    }

    private void writeZipEntry(ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String contentTypesXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
            """;
    }

    private String rootRelsXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
            """;
    }

    private String workbookXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="离体时间设置" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
            """;
    }

    private String workbookRelsXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
            """;
    }

    private String worksheetXml(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
            """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            builder.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<String> cells = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++) {
                builder.append("<c r=\"")
                    .append(excelColumnName(columnIndex))
                    .append(rowIndex + 1)
                    .append("\" t=\"inlineStr\"><is><t>")
                    .append(escapeXml(cells.get(columnIndex)))
                    .append("</t></is></c>");
            }
            builder.append("</row>");
        }
        builder.append("""
              </sheetData>
            </worksheet>
            """);
        return builder.toString();
    }

    private String excelColumnName(int columnIndex) {
        StringBuilder builder = new StringBuilder();
        int current = columnIndex;
        do {
            builder.insert(0, (char) ('A' + (current % 26)));
            current = current / 26 - 1;
        } while (current >= 0);
        return builder.toString();
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private String formatExportDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXPORT_TIME_FORMATTER);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private ApplicationListItem toApplicationListItem(ApplicationTracking tracking) {
        String latestBatchNo = resolveLatestLabelPrintBatchNo(tracking.specimens());
        String latestLabelPrintStatus = latestBatchNo == null
            ? null
            : resolveLatestBatchLabelPrintStatus(tracking.specimens(), latestBatchNo);
        ApplicationOperationState operationState = resolveApplicationOperationState(tracking.application());
        return new ApplicationListItem(
            tracking.application().getId().value(),
            tracking.application().getApplicationNo(),
            tracking.application().getPatientName(),
            tracking.application().getPatientGender(),
            tracking.application().getPatientAge(),
            tracking.application().getStatus().name(),
            tracking.application().getSubmittingDepartmentName(),
            tracking.application().getSubmittingDoctorName(),
            tracking.application().getApplicationType(),
            tracking.application().getApplicationFormStatus().name(),
            tracking.currentNode(),
            tracking.abnormal(),
            tracking.specimens().size(),
            latestLabelPrintStatus,
            operationState.editable(),
            operationState.deletable(),
            operationState.voided(),
            operationState.disabledReason(),
            tracking.application().getApplicationDate(),
            tracking.application().getSubmissionDate(),
            tracking.application().getCreatedAt(),
            tracking.application().getUpdatedAt());
    }

    public ApplicationOperationState resolveApplicationOperationState(Application application) {
        if (application.getStatus() == ApplicationStatus.VOIDED) {
            return new ApplicationOperationState(false, false, true, "申请单已作废，不能再编辑或作废");
        }
        if (workbenchRepository.hasStartedDownstreamWorkflow(application.getId().value())) {
            return new ApplicationOperationState(false, false, false, "申请单已进入下游流程，不能再编辑或作废");
        }
        return new ApplicationOperationState(true, true, false, null);
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
                normalizeQualityCheckResult(item.qualityCheckResult()),
                joinQualityIssueCodes(item.qualityIssueCodes()),
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
                .filter(transportOrderItem -> isTransportItemTerminal(transportOrderItem.status()))
                .count();
            boolean orderCompleted = terminalTransportItemCount + processedCount >= transportOrderItems.size();
            specimenWorkflowRepository.updateTransportOrderStatus(
                order.id(),
                orderCompleted ? TransportOrderStatus.COMPLETED : TransportOrderStatus.PARTIALLY_RECEIVED,
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
        String qualityCheckResult = normalizeQualityCheckResult(item.qualityCheckResult());
        List<String> qualityIssueCodes = normalizeQualityIssueCodes(item.qualityIssueCodes());
        if (item.receiptStatus() == ReceiptStatus.RECEIVED && !"PASSED".equals(qualityCheckResult)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Received specimens must pass quality check");
        }
        if (item.receiptStatus() != ReceiptStatus.RECEIVED && blank(item.reason())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Rejected or returned specimens must provide a reason");
        }
        if ("FAILED".equals(qualityCheckResult) && qualityIssueCodes.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Failed quality checks must provide issue codes");
        }
    }

    private boolean isTransportItemTerminal(TransportItemStatus status) {
        return status == TransportItemStatus.COMPLETED || status == TransportItemStatus.RETURNED;
    }

    private boolean isTransportOrderReadyForReceipt(TransportOrderStatus status) {
        return status == TransportOrderStatus.PRINTED
            || status == TransportOrderStatus.HANDED_OVER
            || status == TransportOrderStatus.PARTIALLY_RECEIVED;
    }

    private boolean isReceiptTerminalStatus(SpecimenStatus status) {
        return status == SpecimenStatus.RECEIVED
            || status == SpecimenStatus.REJECTED
            || status == SpecimenStatus.RETURNED;
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
            row.transportOrderId(),
            row.specimenId(),
            row.specimenNo(),
            row.barcode(),
            row.containerName(),
            row.containerCount(),
            row.specimenStatus(),
            row.fixationStatus(),
            row.verificationStatus(),
            row.verificationStartedAt(),
            row.verificationCompletedAt(),
            row.specimenConfirmedAt(),
            row.checkInStatus(),
            row.checkedInAt(),
            row.checkedInByName(),
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
            specimen.containerName(),
            specimen.containerCount(),
            specimen.specimenStatus(),
            specimen.fixationStatus(),
            specimen.verificationStatus(),
            specimen.verificationStartedAt(),
            specimen.verificationCompletedAt(),
            specimen.specimenRemovalAt(),
            specimen.specimenRemovalOperatorUserId(),
            specimen.specimenRemovalOperatorName(),
            specimen.specimenConfirmedAt(),
            specimen.checkInStatus(),
            specimen.checkedInAt(),
            specimen.checkedInByName(),
            specimen.qualified(),
            specimen.unqualifiedReason(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            specimen.qualityIssueCodes(),
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

    private String normalizeQualityCheckResult(String value) {
        String normalized = normalizeStatus(value);
        if (normalized == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Quality check result is required");
        }
        if (!"PASSED".equals(normalized) && !"FAILED".equals(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported quality check result");
        }
        return normalized;
    }

    private List<String> normalizeQualityIssueCodes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String code = trim(value);
            if (code != null) {
                normalized.add(code.toUpperCase());
            }
        }
        return List.copyOf(normalized);
    }

    private String joinQualityIssueCodes(List<String> values) {
        List<String> normalized = normalizeQualityIssueCodes(values);
        return normalized.isEmpty() ? null : String.join(",", normalized);
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

    private LocalDate parseLocalDateFrom(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private LocalDate parseLocalDate(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private LocalDate parseLocalDateTo(String value) {
        if (blank(value)) {
            return null;
        }
        return LocalDate.parse(value.trim()).plusDays(1);
    }

    private String normalizeStatus(String value) {
        return blank(value) ? null : value.trim().toUpperCase();
    }

    private String commandCheckInStatus(Specimen specimen) {
        return blank(specimen.checkInStatus()) ? "NOT_CHECKED_IN" : specimen.checkInStatus().trim().toUpperCase();
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
        String containerName,
        Integer containerCount,
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

    public record RegistrationSnapshot(
        String collectionScene,
        String operatorUserId,
        String operatorName,
        String printerCode,
        String terminalCode,
        String remarks
    ) {
    }

    public record LatestSpecimenRegistrationResult(
        String applicationId,
        List<Specimen> specimens,
        String labelPrintBatchNo,
        boolean labelPrintSuccess,
        String labelPrintMessage,
        RegistrationSnapshot registrationSnapshot
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

    public record SpecimenVerificationCommand(
        String specimenBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ConfirmSpecimenCommand(
        String specimenBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record CheckInSpecimenCommand(
        String specimenBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record SpecimenVerificationResult(
        String id,
        String specimenNo,
        String barcode,
        String specimenName,
        String specimenType,
        String specimenSite,
        String collectionMode,
        String clinicalSymptom,
        Integer specimenCount,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        String verificationStatus,
        LocalDateTime verificationStartedAt,
        LocalDateTime verificationCompletedAt,
        String labelPrintStatus,
        String receiptStatus,
        String qualityCheckResult,
        String abnormalReason
    ) {
    }

    public record SpecimenVerificationRecord(
        String applicationId,
        String specimenId,
        String barcode,
        String verificationType,
        String result,
        String operatorName,
        String terminalCode,
        String remarks,
        LocalDateTime verifiedAt
    ) {
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
        String qualityCheckResult,
        List<String> qualityIssueCodes,
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
        String specimenNo,
        String departmentId,
        String fixationStatus,
        String verificationStatus,
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
        String transportOrderId,
        String specimenId,
        String specimenNo,
        String barcode,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        String verificationStatus,
        LocalDateTime verificationStartedAt,
        LocalDateTime verificationCompletedAt,
        LocalDateTime specimenConfirmedAt,
        String checkInStatus,
        LocalDateTime checkedInAt,
        String checkedInByName,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    public record PendingTransportOrderQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo,
        String departmentId,
        String dateFrom,
        String dateTo,
        String status
    ) {
    }

    public record PendingTransportOrderPage(
        List<PendingTransportOrderItem> items,
        int page,
        int size,
        long total
    ) {
    }

    public record PendingTransportOrderItem(
        String id,
        String transportOrderNo,
        String applicationId,
        String applicationNo,
        String patientName,
        String handoverDepartmentName,
        String receiverDepartmentName,
        String status,
        LocalDateTime toBeTransportedAt,
        LocalDateTime handedOverAt,
        List<String> specimenBarcodes
    ) {
    }

    public record ApplicationListQuery(
        int page,
        int size,
        String applicationNo,
        String patientName,
        String submittingDepartmentId,
        String applicationType,
        String applicationFormStatus,
        String dateFrom,
        String dateTo
    ) {
    }

    public record ApplicationPage(
        List<ApplicationListItem> items,
        int page,
        int size,
        long total
    ) {
    }

    public record DuplicateCheckCommand(
        String patientId,
        String patientName,
        String externalOrderNo,
        String applicationDate,
        String applicationType,
        String specimenSite
    ) {
    }

    public record DuplicateCheckItem(
        String id,
        String applicationNo,
        String patientName,
        LocalDate applicationDate,
        String specimenSite,
        String status,
        String currentNode,
        List<String> matchedBy
    ) {
    }

    public record DuplicateCheckResult(
        List<DuplicateCheckItem> items,
        String suggestedAction
    ) {
    }

    public record ApplicationListItem(
        String id,
        String applicationNo,
        String patientName,
        String patientGender,
        String patientAge,
        String status,
        String submittingDepartmentName,
        String submittingDoctorName,
        String applicationType,
        String applicationFormStatus,
        String currentNode,
        boolean abnormalFlag,
        int registeredSpecimenCount,
        String latestLabelPrintStatus,
        boolean editable,
        boolean deletable,
        boolean voided,
        String operationDisabledReason,
        LocalDate applicationDate,
        LocalDate submissionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record ApplicationOperationState(
        boolean editable,
        boolean deletable,
        boolean voided,
        String disabledReason
    ) {
    }

    public record SpecimenManagementListQuery(
        int page,
        int size,
        String keyword,
        String applicationNo,
        String departmentId,
        String specimenStatus,
        String labelPrintStatus,
        Boolean abnormalFlag,
        String dateFrom,
        String dateTo
    ) {
    }

    public record SpecimenManagementListPage(
        List<SpecimenManagementListItem> items,
        int page,
        int size,
        long total,
        SpecimenManagementSummary summary
    ) {
    }

    public record SpecimenManagementListItem(
        String specimenId,
        String specimenNo,
        String barcode,
        String applicationId,
        String applicationNo,
        String patientName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String specimenName,
        String specimenType,
        String specimenSite,
        Integer specimenCount,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        String verificationStatus,
        LocalDateTime specimenConfirmedAt,
        String checkInStatus,
        LocalDateTime checkedInAt,
        String checkedInByName,
        String labelPrintStatus,
        String labelPrintBatchNo,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    public record SpecimenManagementSummary(
        long totalCount,
        long labelPrintedCount,
        long pendingLabelCount,
        long abnormalCount
    ) {
    }

    public record SpecimenRemovalQuery(
        int page,
        int size,
        String keyword,
        String applicationNo,
        String departmentId,
        String specimenStatus,
        Boolean abnormalFlag,
        String dateFrom,
        String dateTo
    ) {
    }

    public record SpecimenRemovalCommand(
        String specimenBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record SpecimenRemovalResult(
        String specimenId,
        String barcode,
        LocalDateTime specimenRemovalAt,
        String operatorName
    ) {
    }

    public record SpecimenRemovalListPage(
        List<SpecimenRemovalListItem> items,
        int page,
        int size,
        long total,
        SpecimenRemovalSummary summary
    ) {
    }

    public record SpecimenRemovalListItem(
        String specimenId,
        String specimenNo,
        String barcode,
        String applicationId,
        String applicationNo,
        String patientName,
        String patientGender,
        String inpatientNo,
        String surgeryName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String specimenName,
        String specimenType,
        Integer specimenCount,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        String verificationStatus,
        LocalDateTime specimenRemovalAt,
        String specimenRemovalOperatorName,
        LocalDateTime registeredAt,
        String labelPrintBatchNo,
        String registeredByName,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    public record SpecimenRemovalSummary(
        long totalCount,
        long confirmedCount,
        long pendingCount,
        long abnormalCount
    ) {
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
