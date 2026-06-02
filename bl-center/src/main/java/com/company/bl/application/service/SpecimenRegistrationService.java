package com.company.bl.application.service;

import com.company.bl.application.gateway.LabelPrintGateway;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.support.application.NumberingService;
import com.company.common.web.observability.ObservedOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;

@Service
class SpecimenRegistrationService {

    private final SpecimenWorkflowCommandRepository specimenWorkflowRepository;
    private final SpecimenWorkflowSupport specimenWorkflowSupport;
    private final NumberingService numberingService;
    private final LabelPrintGateway labelPrintGateway;

    SpecimenRegistrationService(SpecimenWorkflowCommandRepository specimenWorkflowRepository,
                                SpecimenWorkflowSupport specimenWorkflowSupport,
                                NumberingService numberingService,
                                LabelPrintGateway labelPrintGateway) {
        this.specimenWorkflowRepository = specimenWorkflowRepository;
        this.specimenWorkflowSupport = specimenWorkflowSupport;
        this.numberingService = numberingService;
        this.labelPrintGateway = labelPrintGateway;
    }

    @Transactional
    @ObservedOperation(
        operation = "register_specimens",
        successCounter = "specimen_register_total",
        failureCounter = "specimen_register_failed_total",
        durationMetric = "specimen_register_duration")
    SpecimenRegistrationResult registerSpecimens(RegisterSpecimensCommand command) {
        Application application = specimenWorkflowSupport.getApplication(command.applicationId());
        specimenWorkflowSupport.validateApplicationCanRegister(application);
        LocalDateTime now = LocalDateTime.now();
        String labelPrintBatchNo = "LP-" + UUID.randomUUID();
        List<Specimen> specimens = new ArrayList<>();
        for (SpecimenRegistrationItem item : command.items()) {
            String specimenNo = numberingService.generateSpecimenNo(null);
            String barcode = specimenWorkflowSupport.blank(item.barcode())
                ? application.getApplicationNo() + "-" + specimenNo
                : item.barcode().trim();
            specimenWorkflowSupport.ensureBarcodeAvailable(barcode);
            Specimen specimen = new Specimen(
                "SP-" + UUID.randomUUID(),
                command.applicationId(),
                null,
                specimenNo,
                barcode,
                specimenWorkflowSupport.trim(item.specimenType()),
                specimenWorkflowSupport.trim(item.specimenNameStandardized()),
                specimenWorkflowSupport.trim(item.specimenSite()),
                specimenWorkflowSupport.trim(item.collectionMode()),
                item.specimenCount(),
                null,
                false,
                null,
                specimenWorkflowSupport.trim(item.containerName()),
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
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                specimenWorkflowSupport.defaultIfBlank(item.clinicalSymptom(), application.getClinicalSymptom()),
                application.getSubmittingDepartmentId(),
                application.getSubmittingDepartmentName(),
                application.getSubmittingDoctorUserId(),
                application.getSubmittingDoctorName(),
                application.getSubmissionDate(),
                labelPrintBatchNo,
                "PENDING",
                specimenWorkflowSupport.trim(command.operatorUserId()),
                specimenWorkflowSupport.trim(command.operatorName()),
                now,
                specimenWorkflowSupport.trim(command.terminalCode()),
                specimenWorkflowSupport.trim(command.remarks()));
            specimenWorkflowRepository.insertSpecimen(specimen);
            specimenWorkflowRepository.insertCollectionRecord(
                command.applicationId(),
                specimen.id(),
                "COLLECTED",
                specimenWorkflowSupport.defaultIfBlank(command.collectionScene(), "OPERATING_ROOM"),
                specimen.collectionMode(),
                labelPrintBatchNo,
                specimenWorkflowSupport.trim(command.printerCode()),
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
            updatedSpecimens.add(specimenWorkflowSupport.copyWithLabelPrintStatus(specimen, labelPrintStatus));
        }
        specimenWorkflowRepository.updateApplicationStatus(command.applicationId(), "SUBMITTED");
        return new SpecimenRegistrationResult(updatedSpecimens, labelPrintBatchNo, printResult.success(), printResult.message());
    }

    @Transactional
    @ObservedOperation(
        operation = "retry_label_print",
        successCounter = "label_print_retry_total",
        failureCounter = "label_print_retry_failed_total",
        durationMetric = "label_print_retry_duration")
    LabelPrintRetryResult retryLabelPrint(RetryLabelPrintCommand command) {
        List<Specimen> failedSpecimens = specimenWorkflowSupport.getSpecimensByLabelPrintBatchNoAndStatuses(
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
}
