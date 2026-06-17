package com.company.bl.domain.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicalWasteRepository {

    List<SpecimenBatch> findSpecimenBatches(String keyword,
                                           String createdByName,
                                           LocalDate dateFrom,
                                           LocalDate dateTo);

    List<SpecimenPreviewLabel> findSpecimenPreviewLabels(String grossingStationName,
                                                         String grossingOperatorName,
                                                         LocalDate grossingDate,
                                                         String grossingPeriod);

    List<OptionItem> findGrossingStations();

    List<OptionItem> findGrossingOperators();

    void insertSpecimenBatch(CreateSpecimenBatchCommand command);

    void insertSpecimenBatchLabels(List<CreateSpecimenBatchLabelCommand> commands);

    Optional<SpecimenBatch> findSpecimenBatchById(String batchId);

    void destroySpecimenBatch(String batchId,
                              String destroyedByUserId,
                              String destroyedByName,
                              LocalDateTime destroyedAt);

    List<ReagentBag> findReagentBags(String keyword, LocalDate dateFrom, LocalDate dateTo);

    Optional<ReagentBag> findReagentBagById(String bagId);

    void insertReagentBag(CreateReagentBagCommand command);

    void updateReagentBag(UpdateReagentBagCommand command);

    void handoverReagentBag(String bagId,
                            String handedOverByUserId,
                            String handedOverByName,
                            LocalDateTime handedOverAt,
                            String handoverRemarks,
                            LocalDateTime updatedAt);

    record OptionItem(
        String label,
        String value
    ) {
    }

    record SpecimenBatch(
        String id,
        String bagName,
        String grossingStationId,
        String grossingStationName,
        String grossingOperatorId,
        String grossingOperatorName,
        LocalDate grossingDate,
        String grossingPeriod,
        BigDecimal weightKg,
        Integer labelCount,
        LocalDateTime printedAt,
        String printedByUserId,
        String printedByName,
        LocalDateTime destroyedAt,
        String destroyedByUserId,
        String destroyedByName
    ) {
    }

    record SpecimenPreviewLabel(
        String sourceLabelId,
        String patientId,
        String patientIdDisplay,
        String patientName,
        String pathologyNo,
        String specimenName
    ) {
    }

    record CreateSpecimenBatchCommand(
        String id,
        String bagName,
        String grossingStationId,
        String grossingStationName,
        String grossingOperatorId,
        String grossingOperatorName,
        LocalDate grossingDate,
        String grossingPeriod,
        BigDecimal weightKg,
        Integer labelCount,
        LocalDateTime printedAt,
        String printedByUserId,
        String printedByName
    ) {
    }

    record CreateSpecimenBatchLabelCommand(
        String id,
        String batchId,
        String sourceLabelId,
        String patientId,
        String patientName,
        String pathologyNo,
        String specimenName
    ) {
    }

    record ReagentBag(
        String id,
        String bagName,
        String wasteType,
        BigDecimal weightKg,
        BigDecimal volumeMl,
        String source,
        String remarks,
        LocalDateTime createdAt,
        String createdByUserId,
        String createdByName,
        LocalDateTime printedAt,
        String printedByUserId,
        String printedByName,
        LocalDateTime handedOverAt,
        String handedOverByUserId,
        String handedOverByName,
        String handoverRemarks,
        LocalDateTime updatedAt
    ) {
    }

    record CreateReagentBagCommand(
        String id,
        String bagName,
        String wasteType,
        BigDecimal weightKg,
        BigDecimal volumeMl,
        String source,
        String remarks,
        LocalDateTime createdAt,
        String createdByUserId,
        String createdByName,
        LocalDateTime printedAt,
        String printedByUserId,
        String printedByName,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateReagentBagCommand(
        String id,
        String bagName,
        String wasteType,
        BigDecimal weightKg,
        BigDecimal volumeMl,
        String source,
        String remarks,
        LocalDateTime printedAt,
        String printedByUserId,
        String printedByName,
        LocalDateTime updatedAt
    ) {
    }
}
