package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.common.web.observability.ObservedOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenBarcodeBindingCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenBarcodeUnbindCommand;

@Service
class SpecimenBarcodeBindingService {

    private final SpecimenWorkflowCommandRepository specimenWorkflowRepository;
    private final SpecimenWorkflowSupport specimenWorkflowSupport;

    SpecimenBarcodeBindingService(
        SpecimenWorkflowCommandRepository specimenWorkflowRepository,
        SpecimenWorkflowSupport specimenWorkflowSupport
    ) {
        this.specimenWorkflowRepository = specimenWorkflowRepository;
        this.specimenWorkflowSupport = specimenWorkflowSupport;
    }

    @Transactional
    @ObservedOperation(
        operation = "bind_specimen_barcode",
        successCounter = "specimen_barcode_bind_total",
        failureCounter = "specimen_barcode_bind_failed_total",
        durationMetric = "specimen_barcode_bind_duration")
    Specimen bindSpecimenBarcode(SpecimenBarcodeBindingCommand command) {
        Specimen specimen = specimenWorkflowSupport.getSpecimenById(command.specimenId());
        if (!specimenWorkflowSupport.blank(specimen.barcode())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen barcode already bound");
        }

        String targetBarcode = normalizeTargetBarcode(command.targetBarcode(), specimen.id());
        specimenWorkflowSupport.ensureBarcodeAvailable(targetBarcode);

        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.bindSpecimenBarcode(specimen.id(), targetBarcode);
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "BARCODE_BINDING",
            "BOUND",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Specimen barcode bound to " + targetBarcode));
        return specimenWorkflowSupport.getSpecimenById(specimen.id());
    }

    @Transactional
    @ObservedOperation(
        operation = "rebind_specimen_barcode",
        successCounter = "specimen_barcode_rebind_total",
        failureCounter = "specimen_barcode_rebind_failed_total",
        durationMetric = "specimen_barcode_rebind_duration")
    Specimen rebindSpecimenBarcode(SpecimenBarcodeBindingCommand command) {
        Specimen specimen = specimenWorkflowSupport.getSpecimenById(command.specimenId());
        validateWorkflowLockedSpecimen(specimen, "重绑条码");
        if (specimenWorkflowSupport.blank(specimen.barcode())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen barcode must be bound before rebinding");
        }

        String targetBarcode = normalizeTargetBarcode(command.targetBarcode(), specimen.id());
        if (!targetBarcode.equals(specimen.barcode())) {
            specimenWorkflowSupport.ensureBarcodeAvailable(targetBarcode);
        }

        String previousBarcode = specimen.barcode();
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.bindSpecimenBarcode(specimen.id(), targetBarcode);
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "BARCODE_BINDING",
            "REBOUND",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Specimen barcode rebound from " + previousBarcode + " to " + targetBarcode));
        return specimenWorkflowSupport.getSpecimenById(specimen.id());
    }

    @Transactional
    @ObservedOperation(
        operation = "unbind_specimen_barcode",
        successCounter = "specimen_barcode_unbind_total",
        failureCounter = "specimen_barcode_unbind_failed_total",
        durationMetric = "specimen_barcode_unbind_duration")
    Specimen unbindSpecimenBarcode(SpecimenBarcodeUnbindCommand command) {
        Specimen specimen = specimenWorkflowSupport.getSpecimenById(command.specimenId());
        validateWorkflowLockedSpecimen(specimen, "取消绑定条码");
        if (specimenWorkflowSupport.blank(specimen.barcode())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen barcode is not bound");
        }

        String previousBarcode = specimen.barcode();
        LocalDateTime now = LocalDateTime.now();
        specimenWorkflowRepository.unbindSpecimenBarcode(specimen.id());
        specimenWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            "EVT-" + UUID.randomUUID(),
            specimen.applicationId(),
            specimen.id(),
            null,
            null,
            "BARCODE_BINDING",
            "UNBOUND",
            "SUCCESS",
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Specimen barcode unbound from " + previousBarcode));
        return specimenWorkflowSupport.getSpecimenById(specimen.id());
    }

    private void validateWorkflowLockedSpecimen(Specimen specimen, String operationLabel) {
        if (specimenWorkflowSupport.isReceiptTerminalStatus(specimen.specimenStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Specimen already reached receipt terminal status");
        }
        if ("CHECKED_IN".equalsIgnoreCase(specimenWorkflowSupport.commandCheckInStatus(specimen))) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, operationLabel + "前标本不能已入库");
        }
    }

    private String normalizeTargetBarcode(String targetBarcode, String specimenId) {
        String normalizedBarcode = specimenWorkflowSupport.trim(targetBarcode);
        if (normalizedBarcode == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Target barcode is required");
        }
        if (normalizedBarcode.equals(specimenId)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Target barcode cannot equal specimen id");
        }
        return normalizedBarcode;
    }
}
