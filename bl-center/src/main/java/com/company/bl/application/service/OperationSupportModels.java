package com.company.bl.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        BigDecimal defaultLowStockThreshold,
        Integer defaultNearExpiryDays,
        boolean enabled,
        String remarks
    ) {
    }

    public record CreateReagentCommand(
        String reagentCode,
        String reagentName,
        String specification,
        String unit,
        String manufacturer,
        BigDecimal defaultLowStockThreshold,
        Integer defaultNearExpiryDays,
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
        BigDecimal defaultLowStockThreshold,
        Integer defaultNearExpiryDays,
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
        String batchNo,
        BigDecimal stockQuantity,
        String stockStatus,
        LocalDate expiryDate,
        String storageLocation,
        BigDecimal lowStockThreshold,
        Integer nearExpiryDays,
        String remarks
    ) {
    }

    public record CreateReagentStockCommand(
        String reagentId,
        String batchNo,
        BigDecimal stockQuantity,
        String stockStatus,
        LocalDate expiryDate,
        String storageLocation,
        BigDecimal lowStockThreshold,
        Integer nearExpiryDays,
        String operatorUserId,
        String operatorName,
        String remarks
    ) {
    }

    public record UpdateReagentStockCommand(
        String stockId,
        BigDecimal stockQuantity,
        String stockStatus,
        LocalDate expiryDate,
        String storageLocation,
        BigDecimal lowStockThreshold,
        Integer nearExpiryDays,
        String operatorUserId,
        String operatorName,
        String remarks
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
