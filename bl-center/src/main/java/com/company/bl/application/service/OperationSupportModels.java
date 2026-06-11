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
        String operatorUserId,
        String operatorName,
        String remarks
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
}
