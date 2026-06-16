package com.company.bl.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class OperationSupportModels {

    private OperationSupportModels() {
    }

    public record ReagentView(
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
        String createdAt,
        String updatedAt,
        String createdByName,
        String updatedByName,
        String remarks
    ) {
    }

    public record CreateReagentCommand(
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
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record UpdateReagentCommand(
        String reagentId,
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
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record ReagentStockView(
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
        String inboundAt,
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
        String testedAt,
        String startedAt,
        String finishedAt,
        String createdAt,
        String updatedAt,
        String createdByName,
        String updatedByName,
        String remarks
    ) {
    }

    public record CreateReagentStockCommand(
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
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record UpdateReagentStockCommand(
        String stockId,
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
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record ReagentStockActionCommand(
        String stockId,
        BigDecimal quantity,
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record ReagentStockEventView(
        String id,
        String stockId,
        String eventType,
        BigDecimal quantityDelta,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        String occurredAt,
        String operatorName,
        String remarks
    ) {
    }

    public record ReagentStockImportResult(
        int successCount,
        int failureCount,
        List<ReagentStockImportError> errors
    ) {
    }

    public record ReagentStockImportError(
        int rowNumber,
        String field,
        String rejectedValue,
        String message
    ) {
    }

    public record ReagentWarningView(
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

    public record EquipmentRecordView(
        String id,
        String equipmentCode,
        String equipmentName,
        String equipmentCategory,
        String modelNo,
        String equipmentStatus,
        String locationDescription,
        String enabledAt,
        String nextMaintenanceAt,
        Integer quantity,
        String purchaseDate,
        String purchaserName,
        String purchaserCode,
        String managementUnit,
        String managementCode,
        String useUnit,
        String principalCode,
        String principalName,
        String userName,
        String productionDate,
        String warrantyEndDate,
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

    public record CreateEquipmentRecordCommand(
        String equipmentCode,
        String equipmentName,
        String equipmentCategory,
        String modelNo,
        String equipmentStatus,
        String locationDescription,
        String enabledAt,
        String nextMaintenanceAt,
        Integer quantity,
        String purchaseDate,
        String purchaserName,
        String purchaserCode,
        String managementUnit,
        String managementCode,
        String useUnit,
        String principalCode,
        String principalName,
        String userName,
        String productionDate,
        String warrantyEndDate,
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
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record UpdateEquipmentRecordCommand(
        String equipmentId,
        String equipmentName,
        String equipmentCategory,
        String modelNo,
        String equipmentStatus,
        String locationDescription,
        String enabledAt,
        String nextMaintenanceAt,
        Integer quantity,
        String purchaseDate,
        String purchaserName,
        String purchaserCode,
        String managementUnit,
        String managementCode,
        String useUnit,
        String principalCode,
        String principalName,
        String userName,
        String productionDate,
        String warrantyEndDate,
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
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record BatchUpdateEquipmentStatusCommand(
        List<String> equipmentIds,
        String equipmentStatus,
        String operatorUserId,
        String operatorName
    ) {
    }

    public record EquipmentMaintenanceLogView(
        String id,
        String equipmentId,
        String maintenanceType,
        String maintenanceStatus,
        String performedAt,
        String performedByName,
        String description,
        String nextMaintenanceAt,
        String remarks
    ) {
    }

    public record CreateEquipmentMaintenanceLogCommand(
        String equipmentId,
        String maintenanceType,
        String maintenanceStatus,
        String performedAt,
        String nextMaintenanceAt,
        String operatorUserId,
        String operatorName,
        String description,
        String remarks
    ) {
    }

    public record EquipmentWarningView(
        String equipmentId,
        String equipmentCode,
        String equipmentName,
        String warningType,
        String nextMaintenanceAt,
        String equipmentStatus
    ) {
    }

    public record WhiteSlideStockView(
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

    public record WhiteSlideLoanView(
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
        String operatorName,
        String loanedAt,
        String returnedAt,
        String returnedByName,
        String remarks
    ) {
    }

    public record CreateWhiteSlideLoanCommand(
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
        String waxBlockUsage,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ReturnWhiteSlideLoanCommand(
        String loanId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }
}
