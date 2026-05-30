package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
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
class SpecimenFixationService {

    private final SpecimenWorkflowCommandRepository specimenWorkflowRepository;
    private final SpecimenWorkflowSupport specimenWorkflowSupport;

    SpecimenFixationService(SpecimenWorkflowCommandRepository specimenWorkflowRepository,
                            SpecimenWorkflowSupport specimenWorkflowSupport) {
        this.specimenWorkflowRepository = specimenWorkflowRepository;
        this.specimenWorkflowSupport = specimenWorkflowSupport;
    }

    @Transactional
    @ObservedOperation(
        operation = "start_fixation",
        successCounter = "specimen_fixation_start_total",
        failureCounter = "specimen_fixation_start_failed_total",
        durationMetric = "specimen_fixation_start_duration")
    FixationResult startFixation(FixationCommand command) {
        Specimen specimen = specimenWorkflowSupport.getSpecimen(command.specimenBarcode());
        if (specimenWorkflowSupport.isReceiptTerminalStatus(specimen.specimenStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen already reached receipt terminal status");
        }
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
            null,
            null,
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
        return new FixationResult(
            specimen.id(),
            specimen.barcode(),
            FixationStatus.FIXING.name(),
            null,
            command.operatorUserId(),
            command.operatorName(),
            command.fixationLiquidType());
    }

    @Transactional
    @ObservedOperation(
        operation = "complete_fixation",
        successCounter = "specimen_fixation_complete_total",
        failureCounter = "specimen_fixation_complete_failed_total",
        durationMetric = "specimen_fixation_complete_duration")
    FixationResult completeFixation(FixationCommand command) {
        Specimen specimen = specimenWorkflowSupport.getSpecimen(command.specimenBarcode());
        if (specimenWorkflowSupport.isReceiptTerminalStatus(specimen.specimenStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen already reached receipt terminal status");
        }
        if (!"VERIFIED".equals(specimen.verificationStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen must be verified before fixation");
        }
        if (specimen.fixationStatus() == FixationStatus.COMPLETED) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen fixation already completed");
        }
        if (specimen.fixationStatus() != FixationStatus.PENDING && specimen.fixationStatus() != FixationStatus.FIXING) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen current status does not allow fixation completion");
        }
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.upsertFixationRecord(
            specimen.applicationId(),
            specimen.id(),
            FixationStatus.COMPLETED,
            command.fixationLiquidType(),
            specimen.fixationStatus() == FixationStatus.FIXING ? null : now,
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
        return new FixationResult(
            specimen.id(),
            specimen.barcode(),
            FixationStatus.COMPLETED.name(),
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.fixationLiquidType());
    }
}
