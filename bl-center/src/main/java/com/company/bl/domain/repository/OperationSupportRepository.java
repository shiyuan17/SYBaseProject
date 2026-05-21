package com.company.bl.domain.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OperationSupportRepository {

    List<Reagent> findReagents(String keyword, Boolean enabled);

    Optional<Reagent> findReagentById(String reagentId);

    void insertReagent(CreateReagentCommand command);

    void updateReagent(UpdateReagentCommand command);

    List<ReagentStock> findReagentStocks(String keyword, String stockStatus);

    Optional<ReagentStock> findReagentStockById(String stockId);

    void insertReagentStock(CreateReagentStockCommand command);

    void updateReagentStock(UpdateReagentStockCommand command);

    List<ReagentWarning> findReagentWarnings(LocalDate today);

    List<EquipmentRecord> findEquipmentRecords(String keyword, String equipmentStatus);

    Optional<EquipmentRecord> findEquipmentRecordById(String equipmentId);

    void insertEquipmentRecord(CreateEquipmentRecordCommand command);

    void updateEquipmentRecord(UpdateEquipmentRecordCommand command);

    List<EquipmentMaintenanceLog> findEquipmentMaintenanceLogs(String equipmentId);

    void insertEquipmentMaintenanceLog(CreateEquipmentMaintenanceLogCommand command);

    List<EquipmentWarning> findEquipmentWarnings(LocalDateTime now, LocalDateTime dueSoonThreshold);

    record Reagent(
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

    record CreateReagentCommand(
        String id,
        String reagentCode,
        String reagentName,
        String specification,
        String unit,
        String manufacturer,
        BigDecimal defaultLowStockThreshold,
        Integer defaultNearExpiryDays,
        boolean enabled,
        String remarks,
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
        BigDecimal defaultLowStockThreshold,
        Integer defaultNearExpiryDays,
        boolean enabled,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }

    record ReagentStock(
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

    record CreateReagentStockCommand(
        String id,
        String reagentId,
        String batchNo,
        BigDecimal stockQuantity,
        String stockStatus,
        LocalDate expiryDate,
        String storageLocation,
        BigDecimal lowStockThreshold,
        Integer nearExpiryDays,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateReagentStockCommand(
        String id,
        BigDecimal stockQuantity,
        String stockStatus,
        LocalDate expiryDate,
        String storageLocation,
        BigDecimal lowStockThreshold,
        Integer nearExpiryDays,
        String remarks,
        LocalDateTime updatedAt
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
}
