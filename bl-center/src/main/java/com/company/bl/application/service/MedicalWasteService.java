package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.MedicalWasteRepository;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MedicalWasteService {

    private static final List<MedicalWasteModels.OptionItemView> GROSSING_PERIODS = List.of(
        new MedicalWasteModels.OptionItemView("上午", "AM"),
        new MedicalWasteModels.OptionItemView("下午", "PM"));

    private final MedicalWasteRepository medicalWasteRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;
    private final OperationAuditService operationAuditService;

    public MedicalWasteService(MedicalWasteRepository medicalWasteRepository,
                               DiagnosticReportSupport diagnosticReportSupport,
                               OperationAuditService operationAuditService) {
        this.medicalWasteRepository = medicalWasteRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
        this.operationAuditService = operationAuditService;
    }

    @Transactional(readOnly = true)
    public List<MedicalWasteModels.SpecimenBatchView> listSpecimenBatches(String keyword,
                                                                          String createdByName,
                                                                          LocalDate dateFrom,
                                                                          LocalDate dateTo) {
        return medicalWasteRepository.findSpecimenBatches(keyword, createdByName, dateFrom, dateTo)
            .stream()
            .map(this::toSpecimenBatchView)
            .toList();
    }

    @Transactional(readOnly = true)
    public MedicalWasteModels.SpecimenOptionsView getSpecimenOptions() {
        return new MedicalWasteModels.SpecimenOptionsView(
            GROSSING_PERIODS,
            medicalWasteRepository.findGrossingStations().stream().map(this::toOptionItemView).toList(),
            medicalWasteRepository.findGrossingOperators().stream().map(this::toOptionItemView).toList());
    }

    @Transactional(readOnly = true)
    public List<MedicalWasteModels.SpecimenPreviewLabelView> previewSpecimenLabels(
        MedicalWasteModels.SpecimenPreviewRequest request
    ) {
        validateGrossingPeriod(request.grossingPeriod());
        return medicalWasteRepository.findSpecimenPreviewLabels(
                request.grossingStationName(),
                request.grossingOperatorName(),
                request.grossingDate(),
                request.grossingPeriod())
            .stream()
            .map(this::toSpecimenPreviewLabelView)
            .toList();
    }

    @Transactional
    public MedicalWasteModels.PrintSpecimenBatchResult printSpecimenBatch(
        MedicalWasteModels.PrintSpecimenBatchCommand command
    ) {
        return operationAuditService.audit("M5_SUPPORT", "MEDICAL_WASTE_SPECIMEN", "print_medical_waste_specimen", () -> {
            List<MedicalWasteModels.SpecimenPreviewLabelView> labels = previewSpecimenLabels(
                new MedicalWasteModels.SpecimenPreviewRequest(
                    command.bagName(),
                    command.grossingOperatorName(),
                    command.grossingStationName(),
                    command.grossingDate(),
                    command.grossingPeriod()));
            if (labels.isEmpty()) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "No labels found for the selected grossing context");
            }
            String batchId = diagnosticReportSupport.nextId("MWB");
            LocalDateTime now = LocalDateTime.now();
            medicalWasteRepository.insertSpecimenBatch(new MedicalWasteRepository.CreateSpecimenBatchCommand(
                batchId,
                command.bagName(),
                null,
                command.grossingStationName(),
                null,
                command.grossingOperatorName(),
                command.grossingDate(),
                command.grossingPeriod(),
                defaultDecimal(command.weightKg()),
                labels.size(),
                now,
                command.operatorUserId(),
                command.operatorName()));
            medicalWasteRepository.insertSpecimenBatchLabels(labels.stream()
                .map(label -> new MedicalWasteRepository.CreateSpecimenBatchLabelCommand(
                    diagnosticReportSupport.nextId("MWL"),
                    batchId,
                    label.sourceLabelId(),
                    label.patientId(),
                    label.patientName(),
                    label.pathologyNo(),
                    label.specimenName()))
                .toList());
            MedicalWasteRepository.SpecimenBatch batch = medicalWasteRepository.findSpecimenBatchById(batchId)
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Printed specimen batch not found"));
            return new MedicalWasteModels.PrintSpecimenBatchResult(
                toSpecimenBatchView(batch),
                labels,
                command.bagName(),
                command.grossingDate() + " " + formatGrossingPeriod(command.grossingPeriod()) + " " + command.grossingOperatorName());
        }, result -> result.batch().id(), null, command.operatorUserId(), command.operatorName(), command::bagName);
    }

    @Transactional
    public MedicalWasteModels.SpecimenBatchView destroySpecimenBatch(MedicalWasteModels.DestroySpecimenBatchCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "MEDICAL_WASTE_SPECIMEN", "destroy_medical_waste_specimen", () -> {
            MedicalWasteRepository.SpecimenBatch current = medicalWasteRepository.findSpecimenBatchById(command.batchId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen batch not found"));
            if (current.destroyedAt() != null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen batch has already been destroyed");
            }
            medicalWasteRepository.destroySpecimenBatch(
                command.batchId(),
                command.operatorUserId(),
                command.operatorName(),
                LocalDateTime.now());
            return medicalWasteRepository.findSpecimenBatchById(command.batchId())
                .map(this::toSpecimenBatchView)
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Destroyed specimen batch not found"));
        }, MedicalWasteModels.SpecimenBatchView::id, command::batchId, command.operatorUserId(), command.operatorName(), command::batchId);
    }

    @Transactional(readOnly = true)
    public List<MedicalWasteModels.ReagentBagView> listReagentBags(String keyword, LocalDate dateFrom, LocalDate dateTo) {
        return medicalWasteRepository.findReagentBags(keyword, dateFrom, dateTo)
            .stream()
            .map(this::toReagentBagView)
            .toList();
    }

    @Transactional
    public MedicalWasteModels.ReagentBagView saveReagentBag(MedicalWasteModels.SaveReagentBagCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "MEDICAL_WASTE_REAGENT", "save_medical_waste_reagent_bag", () -> {
            LocalDateTime now = LocalDateTime.now();
            String wasteType = blankToNull(command.wasteType()) == null ? "DRUG" : command.wasteType().trim();
            validateWasteType(wasteType);
            if (blankToNull(command.id()) == null) {
                String bagId = diagnosticReportSupport.nextId("MWR");
                medicalWasteRepository.insertReagentBag(new MedicalWasteRepository.CreateReagentBagCommand(
                    bagId,
                    command.bagName(),
                    wasteType,
                    defaultDecimal(command.weightKg()),
                    defaultDecimal(command.volumeMl()),
                    command.source(),
                    command.remarks(),
                    now,
                    command.operatorUserId(),
                    command.operatorName(),
                    now,
                    command.operatorUserId(),
                    command.operatorName(),
                    now));
                return medicalWasteRepository.findReagentBagById(bagId)
                    .map(this::toReagentBagView)
                    .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent waste bag not found"));
            }
            MedicalWasteRepository.ReagentBag current = medicalWasteRepository.findReagentBagById(command.id())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent waste bag not found"));
            if (current.handedOverAt() != null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Handed over reagent waste bag cannot be updated");
            }
            medicalWasteRepository.updateReagentBag(new MedicalWasteRepository.UpdateReagentBagCommand(
                current.id(),
                command.bagName(),
                wasteType,
                defaultDecimal(command.weightKg()),
                defaultDecimal(command.volumeMl()),
                command.source(),
                command.remarks(),
                now,
                command.operatorUserId(),
                command.operatorName(),
                now));
            return medicalWasteRepository.findReagentBagById(current.id())
                .map(this::toReagentBagView)
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Updated reagent waste bag not found"));
        }, MedicalWasteModels.ReagentBagView::id, command::id, command.operatorUserId(), command.operatorName(), command::bagName);
    }

    @Transactional
    public MedicalWasteModels.ReagentBagView handoverReagentBag(MedicalWasteModels.HandoverReagentBagCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "MEDICAL_WASTE_REAGENT", "handover_medical_waste_reagent_bag", () -> {
            MedicalWasteRepository.ReagentBag current = medicalWasteRepository.findReagentBagById(command.bagId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent waste bag not found"));
            if (current.handedOverAt() != null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Reagent waste bag has already been handed over");
            }
            medicalWasteRepository.handoverReagentBag(
                current.id(),
                command.operatorUserId(),
                command.handedOverByName(),
                command.handedOverAt(),
                command.handoverRemarks(),
                LocalDateTime.now());
            return medicalWasteRepository.findReagentBagById(current.id())
                .map(this::toReagentBagView)
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Handed over reagent waste bag not found"));
        }, MedicalWasteModels.ReagentBagView::id, command::bagId, command.operatorUserId(), command.operatorName(), command::bagId);
    }

    private MedicalWasteModels.OptionItemView toOptionItemView(MedicalWasteRepository.OptionItem item) {
        return new MedicalWasteModels.OptionItemView(item.label(), item.value());
    }

    private MedicalWasteModels.SpecimenPreviewLabelView toSpecimenPreviewLabelView(MedicalWasteRepository.SpecimenPreviewLabel item) {
        return new MedicalWasteModels.SpecimenPreviewLabelView(
            item.sourceLabelId(),
            item.patientId(),
            item.patientName(),
            item.pathologyNo(),
            item.specimenName());
    }

    private MedicalWasteModels.SpecimenBatchView toSpecimenBatchView(MedicalWasteRepository.SpecimenBatch item) {
        return new MedicalWasteModels.SpecimenBatchView(
            item.id(),
            item.bagName(),
            joinInfo(item.grossingOperatorName(), stringify(item.printedAt())),
            joinInfo(item.destroyedByName(), stringify(item.destroyedAt())),
            item.grossingStationName(),
            item.weightKg(),
            item.labelCount(),
            item.grossingOperatorName(),
            item.grossingDate() == null ? null : item.grossingDate().toString(),
            item.grossingPeriod(),
            stringify(item.printedAt()),
            item.printedByName(),
            stringify(item.destroyedAt()),
            item.destroyedByName());
    }

    private MedicalWasteModels.ReagentBagView toReagentBagView(MedicalWasteRepository.ReagentBag item) {
        return new MedicalWasteModels.ReagentBagView(
            item.id(),
            item.bagName(),
            item.wasteType(),
            item.weightKg(),
            item.volumeMl(),
            item.source(),
            joinInfo(item.createdByName(), stringify(item.createdAt())),
            joinInfo(item.handedOverByName(), stringify(item.handedOverAt())),
            item.remarks(),
            stringify(item.createdAt()),
            item.createdByName(),
            stringify(item.printedAt()),
            item.printedByName(),
            stringify(item.handedOverAt()),
            item.handedOverByName(),
            item.handoverRemarks());
    }

    private void validateGrossingPeriod(String grossingPeriod) {
        if (!"AM".equals(grossingPeriod) && !"PM".equals(grossingPeriod)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "grossingPeriod must be AM or PM");
        }
    }

    private void validateWasteType(String wasteType) {
        if (!"CHEMICAL".equals(wasteType) && !"DRUG".equals(wasteType)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "wasteType must be DRUG or CHEMICAL");
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatGrossingPeriod(String grossingPeriod) {
        return "PM".equals(grossingPeriod) ? "下午" : "上午";
    }

    private String joinInfo(String name, String timestamp) {
        if (blankToNull(name) == null && blankToNull(timestamp) == null) {
            return "";
        }
        if (blankToNull(name) == null) {
            return timestamp;
        }
        if (blankToNull(timestamp) == null) {
            return name;
        }
        return name + " / " + timestamp;
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
