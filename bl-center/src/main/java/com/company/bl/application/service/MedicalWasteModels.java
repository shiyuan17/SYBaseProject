package com.company.bl.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class MedicalWasteModels {

    private MedicalWasteModels() {
    }

    public record OptionItemView(
        String label,
        String value
    ) {
    }

    public record SpecimenOptionsView(
        List<OptionItemView> grossingPeriods,
        List<OptionItemView> grossingStations,
        List<OptionItemView> grossingOperators
    ) {
    }

    public record SpecimenPreviewRequest(
        String bagName,
        String grossingOperatorName,
        String grossingStationName,
        LocalDate grossingDate,
        String grossingPeriod
    ) {
    }

    public record SpecimenPreviewLabelView(
        String sourceLabelId,
        String patientId,
        String patientName,
        String pathologyNo,
        String specimenName
    ) {
    }

    public record SpecimenBatchView(
        String id,
        String bagName,
        String grossingAction,
        String destroyAction,
        String grossingStationName,
        BigDecimal weightKg,
        Integer labelCount,
        String grossingOperatorName,
        String grossingDate,
        String grossingPeriod,
        String printedAt,
        String printedByName,
        String destroyedAt,
        String destroyedByName
    ) {
    }

    public record PrintSpecimenBatchCommand(
        String bagName,
        String grossingOperatorName,
        String grossingStationName,
        LocalDate grossingDate,
        String grossingPeriod,
        BigDecimal weightKg,
        String operatorUserId,
        String operatorName
    ) {
    }

    public record PrintSpecimenBatchResult(
        SpecimenBatchView batch,
        List<SpecimenPreviewLabelView> labels,
        String printTitle,
        String printSubtitle
    ) {
    }

    public record DestroySpecimenBatchCommand(
        String batchId,
        String operatorUserId,
        String operatorName
    ) {
    }

    public record ReagentBagView(
        String id,
        String bagName,
        String wasteType,
        BigDecimal weightKg,
        BigDecimal volumeMl,
        String source,
        String createdInfo,
        String handoverInfo,
        String remarks,
        String createdAt,
        String createdByName,
        String printedAt,
        String printedByName,
        String handedOverAt,
        String handedOverByName,
        String handoverRemarks
    ) {
    }

    public record SaveReagentBagCommand(
        String id,
        String bagName,
        String wasteType,
        BigDecimal weightKg,
        BigDecimal volumeMl,
        String source,
        String remarks,
        String operatorUserId,
        String operatorName
    ) {
    }

    public record HandoverReagentBagCommand(
        String bagId,
        String handedOverByName,
        LocalDateTime handedOverAt,
        String handoverRemarks,
        String operatorUserId,
        String operatorName
    ) {
    }
}
