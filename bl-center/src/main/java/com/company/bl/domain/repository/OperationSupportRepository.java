package com.company.bl.domain.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OperationSupportRepository {

    List<Reagent> findReagents(String keyword, Boolean enabled, String reagentType, String templateStatus);

    Optional<Reagent> findReagentById(String reagentId);

    Optional<Reagent> findReagentByCodeOrName(String reagentCode, String reagentName);

    boolean existsMedicalOrderItem(String orderDictItemId);

    void insertReagent(CreateReagentCommand command);

    void updateReagent(UpdateReagentCommand command);

    List<ReagentStock> findReagentStocks(String keyword, String stockStatus, String reagentType, LocalDate dateFrom, LocalDate dateTo);

    Optional<ReagentStock> findReagentStockById(String stockId);

    void insertReagentStock(CreateReagentStockCommand command);

    void updateReagentStock(UpdateReagentStockCommand command);

    void updateReagentStockState(UpdateReagentStockStateCommand command);

    void insertReagentStockEvent(CreateReagentStockEventCommand command);

    List<ReagentStockEvent> findReagentStockEvents(String stockId);

    List<ReagentWarning> findReagentWarnings(LocalDate today);

    List<EquipmentRecord> findEquipmentRecords(String keyword, String equipmentStatus);

    Optional<EquipmentRecord> findEquipmentRecordById(String equipmentId);

    void insertEquipmentRecord(CreateEquipmentRecordCommand command);

    void updateEquipmentRecord(UpdateEquipmentRecordCommand command);

    void updateEquipmentStatusBatch(List<String> equipmentIds, String equipmentStatus, LocalDateTime updatedAt);

    List<EquipmentMaintenanceLog> findEquipmentMaintenanceLogs(String equipmentId);

    void insertEquipmentMaintenanceLog(CreateEquipmentMaintenanceLogCommand command);

    List<EquipmentWarning> findEquipmentWarnings(LocalDateTime now, LocalDateTime dueSoonThreshold);

    List<EquipmentRecord> findCommonlyUsedEquipmentRecords();

    void insertEquipmentUsageRecord(CreateEquipmentUsageRecordCommand command);

    Optional<EquipmentUsageRecord> findEquipmentUsageRecordById(String usageRecordId);

    List<WhiteSlideStock> findWhiteSlideStocks(String keyword, String status);

    Optional<WhiteSlideStock> findWhiteSlideStockById(String stockId);

    void updateWhiteSlideStockQuantities(UpdateWhiteSlideStockQuantitiesCommand command);

    List<WhiteSlideLoan> findWhiteSlideLoans(String keyword, String loanStatus);

    Optional<WhiteSlideLoan> findWhiteSlideLoanById(String loanId);

    void insertWhiteSlideLoan(CreateWhiteSlideLoanCommand command);

    void updateWhiteSlideLoanReturned(UpdateWhiteSlideLoanReturnedCommand command);

    record Reagent(
        String id,
        String reagentCode,
        String reagentName,
        String specification,
        String unit,
        String manufacturer,
        String reagentType,
        String reagentUsage,
        String orderDictItemId,
        String orderItemName,
        String cloneNo,
        String recommendedDilution,
        String applicationDilution,
        String templateStatus,
        Integer validityDays,
        BigDecimal defaultLowStockThreshold,
        BigDecimal defaultStockThreshold,
        Integer defaultNearExpiryDays,
        BigDecimal stainCapacity,
        BigDecimal stainThreshold,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdByUserId,
        String createdByName,
        String updatedByUserId,
        String updatedByName,
        String remarks
    ) {
    }

    record CreateReagentCommand(
        String id,
        String reagentCode,
        String reagentName,
        String specification,
        String unit,
        String manufacturer,
        String reagentType,
        String reagentUsage,
        String orderDictItemId,
        String cloneNo,
        String recommendedDilution,
        String applicationDilution,
        String templateStatus,
        Integer validityDays,
        BigDecimal defaultLowStockThreshold,
        BigDecimal defaultStockThreshold,
        Integer defaultNearExpiryDays,
        BigDecimal stainCapacity,
        BigDecimal stainThreshold,
        boolean enabled,
        String remarks,
        String createdByUserId,
        String createdByName,
        String updatedByUserId,
        String updatedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateReagentCommand(
        String id,
        String reagentName,
        String specification,
        String unit,
        String manufacturer,
        String reagentType,
        String reagentUsage,
        String orderDictItemId,
        String cloneNo,
        String recommendedDilution,
        String applicationDilution,
        String templateStatus,
        Integer validityDays,
        BigDecimal defaultLowStockThreshold,
        BigDecimal defaultStockThreshold,
        Integer defaultNearExpiryDays,
        BigDecimal stainCapacity,
        BigDecimal stainThreshold,
        boolean enabled,
        String remarks,
        String updatedByUserId,
        String updatedByName,
        LocalDateTime updatedAt
    ) {
    }

    record ReagentStock(
        String id,
        String reagentId,
        String reagentCode,
        String reagentName,
        String reagentType,
        String orderDictItemId,
        String orderItemName,
        String batchNo,
        BigDecimal initialQuantity,
        BigDecimal stockQuantity,
        BigDecimal remainingQuantity,
        String stockStatus,
        LocalDate productionDate,
        LocalDateTime inboundAt,
        LocalDate expiryDate,
        String storageLocation,
        BigDecimal lowStockThreshold,
        Integer nearExpiryDays,
        Integer testReminderThreshold,
        Integer expiryReminderThreshold,
        String recommendedDilution,
        String applicationDilution,
        BigDecimal stainCapacity,
        BigDecimal stainThreshold,
        Integer validityDays,
        LocalDateTime testedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdByUserId,
        String createdByName,
        String updatedByUserId,
        String updatedByName,
        String remarks
    ) {
    }

    record CreateReagentStockCommand(
        String id,
        String reagentId,
        String batchNo,
        BigDecimal initialQuantity,
        BigDecimal stockQuantity,
        BigDecimal remainingQuantity,
        String stockStatus,
        LocalDate productionDate,
        LocalDateTime inboundAt,
        LocalDate expiryDate,
        String storageLocation,
        BigDecimal lowStockThreshold,
        Integer nearExpiryDays,
        Integer testReminderThreshold,
        Integer expiryReminderThreshold,
        String recommendedDilution,
        String applicationDilution,
        BigDecimal stainCapacity,
        BigDecimal stainThreshold,
        Integer validityDays,
        String remarks,
        String createdByUserId,
        String createdByName,
        String updatedByUserId,
        String updatedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateReagentStockCommand(
        String id,
        BigDecimal initialQuantity,
        BigDecimal stockQuantity,
        BigDecimal remainingQuantity,
        String stockStatus,
        LocalDate productionDate,
        LocalDateTime inboundAt,
        LocalDate expiryDate,
        String storageLocation,
        BigDecimal lowStockThreshold,
        Integer nearExpiryDays,
        Integer testReminderThreshold,
        Integer expiryReminderThreshold,
        String recommendedDilution,
        String applicationDilution,
        BigDecimal stainCapacity,
        BigDecimal stainThreshold,
        Integer validityDays,
        String remarks,
        String updatedByUserId,
        String updatedByName,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateReagentStockStateCommand(
        String id,
        BigDecimal stockQuantity,
        BigDecimal remainingQuantity,
        String stockStatus,
        LocalDateTime testedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String remarks,
        String updatedByUserId,
        String updatedByName,
        LocalDateTime updatedAt
    ) {
    }

    record CreateReagentStockEventCommand(
        String id,
        String stockId,
        String eventType,
        BigDecimal quantityDelta,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        LocalDateTime occurredAt,
        String operatorUserId,
        String operatorName,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    record ReagentStockEvent(
        String id,
        String stockId,
        String eventType,
        BigDecimal quantityDelta,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        LocalDateTime occurredAt,
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    record ReagentWarning(
        String stockId,
        String reagentCode,
        String reagentName,
        String batchNo,
        String warningType,
        BigDecimal stockQuantity,
        BigDecimal lowStockThreshold,
        LocalDate expiryDate,
        Integer nearExpiryDays
    ) {
    }

    record EquipmentRecord(
        String id,
        String equipmentCode,
        String equipmentName,
        String equipmentCategory,
        String modelNo,
        String equipmentStatus,
        String locationDescription,
        LocalDateTime enabledAt,
        LocalDateTime nextMaintenanceAt,
        Integer quantity,
        LocalDate purchaseDate,
        String purchaserName,
        String purchaserCode,
        String managementUnit,
        String managementCode,
        String useUnit,
        String principalCode,
        String principalName,
        String userName,
        LocalDate productionDate,
        LocalDate warrantyEndDate,
        String factoryNo,
        String depreciationMethod,
        Integer serviceLifeYears,
        BigDecimal price,
        String manufacturer,
        String portNo,
        String ipAddress,
        String commonStartupTime,
        String commonShutdownTime,
        String commonUsageContent,
        boolean commonlyUsed,
        BigDecimal setTemperature,
        BigDecimal currentTemperature,
        String rfid,
        String remarks
    ) {
    }

    record CreateEquipmentRecordCommand(
        String id,
        String equipmentCode,
        String equipmentName,
        String equipmentCategory,
        String modelNo,
        String equipmentStatus,
        String locationDescription,
        LocalDateTime enabledAt,
        LocalDateTime nextMaintenanceAt,
        Integer quantity,
        LocalDate purchaseDate,
        String purchaserName,
        String purchaserCode,
        String managementUnit,
        String managementCode,
        String useUnit,
        String principalCode,
        String principalName,
        String userName,
        LocalDate productionDate,
        LocalDate warrantyEndDate,
        String factoryNo,
        String depreciationMethod,
        Integer serviceLifeYears,
        BigDecimal price,
        String manufacturer,
        String portNo,
        String ipAddress,
        String commonStartupTime,
        String commonShutdownTime,
        String commonUsageContent,
        boolean commonlyUsed,
        BigDecimal setTemperature,
        BigDecimal currentTemperature,
        String rfid,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateEquipmentRecordCommand(
        String id,
        String equipmentName,
        String equipmentCategory,
        String modelNo,
        String equipmentStatus,
        String locationDescription,
        LocalDateTime enabledAt,
        LocalDateTime nextMaintenanceAt,
        Integer quantity,
        LocalDate purchaseDate,
        String purchaserName,
        String purchaserCode,
        String managementUnit,
        String managementCode,
        String useUnit,
        String principalCode,
        String principalName,
        String userName,
        LocalDate productionDate,
        LocalDate warrantyEndDate,
        String factoryNo,
        String depreciationMethod,
        Integer serviceLifeYears,
        BigDecimal price,
        String manufacturer,
        String portNo,
        String ipAddress,
        String commonStartupTime,
        String commonShutdownTime,
        String commonUsageContent,
        boolean commonlyUsed,
        BigDecimal setTemperature,
        BigDecimal currentTemperature,
        String rfid,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }

    record EquipmentMaintenanceLog(
        String id,
        String equipmentId,
        String maintenanceType,
        String maintenanceStatus,
        LocalDateTime performedAt,
        String performedByUserId,
        String performedByName,
        String description,
        LocalDateTime nextMaintenanceAt,
        String remarks
    ) {
    }

    record CreateEquipmentMaintenanceLogCommand(
        String id,
        String equipmentId,
        String maintenanceType,
        String maintenanceStatus,
        LocalDateTime performedAt,
        String performedByUserId,
        String performedByName,
        String description,
        LocalDateTime nextMaintenanceAt,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record EquipmentWarning(
        String equipmentId,
        String equipmentCode,
        String equipmentName,
        String warningType,
        LocalDateTime nextMaintenanceAt,
        String equipmentStatus
    ) {
    }

    record EquipmentUsageRecord(
        String id,
        String equipmentId,
        String equipmentCategorySnapshot,
        String equipmentNameSnapshot,
        boolean commonlyUsed,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        BigDecimal runtimeHours,
        Integer diagnosisCount,
        String equipmentCondition,
        String operatorUserId,
        String operatorName,
        String usageContent,
        String remarks
    ) {
    }

    record CreateEquipmentUsageRecordCommand(
        String id,
        String equipmentId,
        String equipmentCategorySnapshot,
        String equipmentNameSnapshot,
        boolean commonlyUsed,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        BigDecimal runtimeHours,
        Integer diagnosisCount,
        String equipmentCondition,
        String operatorUserId,
        String operatorName,
        String usageContent,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record WhiteSlideStock(
        String id,
        String stockNo,
        String stockCode,
        String specification,
        Integer quantityAvailable,
        Integer quantityBorrowed,
        String status,
        String remarks
    ) {
    }

    record UpdateWhiteSlideStockQuantitiesCommand(
        String id,
        Integer quantityAvailable,
        Integer quantityBorrowed,
        LocalDateTime updatedAt
    ) {
    }

    record WhiteSlideLoan(
        String id,
        String loanNo,
        String stockId,
        String stockNo,
        String stockCode,
        Integer quantity,
        String caseId,
        String pathologyNo,
        String patientName,
        String embeddingBoxNo,
        String slicePurpose,
        String sliceThickness,
        String borrowerName,
        String borrowerIdentityNo,
        String borrowerUnit,
        String borrowerPhone,
        BigDecimal unitPrice,
        BigDecimal amount,
        boolean saveDirectPrint,
        String loanStatus,
        String waxBlockUsage,
        String operatorUserId,
        String operatorName,
        LocalDateTime loanedAt,
        LocalDateTime returnedAt,
        String returnedByUserId,
        String returnedByName,
        String remarks
    ) {
    }

    record CreateWhiteSlideLoanCommand(
        String id,
        String loanNo,
        String stockId,
        Integer quantity,
        String caseId,
        String pathologyNo,
        String patientName,
        String embeddingBoxNo,
        String slicePurpose,
        String sliceThickness,
        String borrowerName,
        String borrowerIdentityNo,
        String borrowerUnit,
        String borrowerPhone,
        BigDecimal unitPrice,
        BigDecimal amount,
        boolean saveDirectPrint,
        String loanStatus,
        String waxBlockUsage,
        String operatorUserId,
        String operatorName,
        LocalDateTime loanedAt,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateWhiteSlideLoanReturnedCommand(
        String id,
        String loanStatus,
        LocalDateTime returnedAt,
        String returnedByUserId,
        String returnedByName,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }
}
