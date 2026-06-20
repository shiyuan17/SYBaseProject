package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.common.web.observability.ObservedOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;

@Service
class SpecimenVerificationService {

    private final SpecimenWorkflowCommandRepository specimenWorkflowRepository;
    private final SpecimenWorkflowSupport specimenWorkflowSupport;

    SpecimenVerificationService(SpecimenWorkflowCommandRepository specimenWorkflowRepository,
                                SpecimenWorkflowSupport specimenWorkflowSupport) {
        this.specimenWorkflowRepository = specimenWorkflowRepository;
        this.specimenWorkflowSupport = specimenWorkflowSupport;
    }

    @Transactional
    @ObservedOperation(
        operation = "start_specimen_verification",
        successCounter = "specimen_verification_start_total",
        failureCounter = "specimen_verification_start_failed_total",
        durationMetric = "specimen_verification_start_duration")
    SpecimenVerificationResult startSpecimenVerification(SpecimenVerificationCommand command) {
        Specimen specimen = specimenWorkflowSupport.getSpecimen(command.specimenBarcode());
        if (specimenWorkflowSupport.isReceiptTerminalStatus(specimen.specimenStatus())) {
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
            "Specimen verification started", null));
        return specimenWorkflowSupport.buildSpecimenVerificationResult(specimenWorkflowSupport.getSpecimen(command.specimenBarcode()));
    }

    @Transactional
    @ObservedOperation(
        operation = "complete_specimen_verification",
        successCounter = "specimen_verification_complete_total",
        failureCounter = "specimen_verification_complete_failed_total",
        durationMetric = "specimen_verification_complete_duration")
    SpecimenVerificationResult completeSpecimenVerification(SpecimenVerificationCommand command) {
        Specimen specimen = specimenWorkflowSupport.getSpecimen(command.specimenBarcode());
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
            "Specimen verification completed", null));
        return specimenWorkflowSupport.buildSpecimenVerificationResult(specimenWorkflowSupport.getSpecimen(command.specimenBarcode()));
    }

    @Transactional
    @ObservedOperation(
        operation = "confirm_specimen",
        successCounter = "specimen_confirm_total",
        failureCounter = "specimen_confirm_failed_total",
        durationMetric = "specimen_confirm_duration")
    Specimen confirmSpecimen(ConfirmSpecimenCommand command) {
        Specimen specimen = specimenWorkflowSupport.resolveSpecimenByPreferredIdentifier(
            command.specimenId(),
            command.specimenBarcode(),
            command.specimenNo());
        if (specimenWorkflowSupport.isReceiptTerminalStatus(specimen.specimenStatus())) {
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
            "Specimen confirmation completed", null));
        return specimenWorkflowSupport.getSpecimenById(specimen.id());
    }

    @Transactional
    @ObservedOperation(
        operation = "check_in_specimen",
        successCounter = "specimen_check_in_total",
        failureCounter = "specimen_check_in_failed_total",
        durationMetric = "specimen_check_in_duration")
    Specimen checkInSpecimen(CheckInSpecimenCommand command) {
        Specimen specimen = specimenWorkflowSupport.resolveSpecimenByPreferredIdentifier(
            command.specimenId(),
            command.specimenBarcode(),
            command.specimenNo());
        if (specimenWorkflowSupport.isReceiptTerminalStatus(specimen.specimenStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen already reached receipt terminal status");
        }
        if ("CHECKED_IN".equalsIgnoreCase(specimenWorkflowSupport.commandCheckInStatus(specimen))) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen already checked in");
        }
        if (!specimenWorkflowSupport.isVerificationCompleted(specimen)) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must complete verification before check-in");
        }
        if (specimen.fixationStatus() != FixationStatus.COMPLETED) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must complete fixation before check-in");
        }
        if (specimen.specimenConfirmedAt() == null) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be confirmed before check-in");
        }
        if (!specimenWorkflowSupport.canCheckInSpecimen(specimen)) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen cannot be checked in");
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
            "Specimen check-in completed", null));
        return specimenWorkflowSupport.getSpecimenById(specimen.id());
    }
}
