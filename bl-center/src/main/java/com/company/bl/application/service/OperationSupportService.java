package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.OperationSupportRepository;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationSupportService {

    private final OperationSupportRepository operationSupportRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;
    private final OperationAuditService operationAuditService;

    public OperationSupportService(OperationSupportRepository operationSupportRepository,
                                   DiagnosticReportSupport diagnosticReportSupport,
                                   OperationAuditService operationAuditService) {
        this.operationSupportRepository = operationSupportRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
        this.operationAuditService = operationAuditService;
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.ReagentView> listReagents(String keyword, Boolean enabled) {
        return operationSupportRepository.findReagents(keyword, enabled).stream().map(this::toReagentView).toList();
    }

    @Transactional
    public OperationSupportModels.ReagentView createReagent(OperationSupportModels.CreateReagentCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "REAGENT", "create_reagent", () -> {
            try {
                String reagentId = diagnosticReportSupport.nextId("REAG");
                LocalDateTime now = LocalDateTime.now();
                operationSupportRepository.insertReagent(new OperationSupportRepository.CreateReagentCommand(
                    reagentId,
                    command.reagentCode(),
                    command.reagentName(),
                    command.specification(),
                    command.unit(),
                    command.manufacturer(),
                    command.defaultLowStockThreshold(),
                    command.defaultNearExpiryDays(),
                    command.enabled(),
                    command.remarks(),
                    now,
                    now));
                return toReagentView(operationSupportRepository.findReagentById(reagentId).orElseThrow());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Reagent code already exists");
            }
        }, OperationSupportModels.ReagentView::id, null, command.operatorUserId(), command.operatorName(), command::reagentCode);
    }

    @Transactional
    public OperationSupportModels.ReagentView updateReagent(OperationSupportModels.UpdateReagentCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "REAGENT", "update_reagent", () -> {
            operationSupportRepository.findReagentById(command.reagentId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent not found"));
            operationSupportRepository.updateReagent(new OperationSupportRepository.UpdateReagentCommand(
                command.reagentId(),
                command.reagentName(),
                command.specification(),
                command.unit(),
                command.manufacturer(),
                command.defaultLowStockThreshold(),
                command.defaultNearExpiryDays(),
                command.enabled(),
                command.remarks(),
                LocalDateTime.now()));
            return toReagentView(operationSupportRepository.findReagentById(command.reagentId()).orElseThrow());
        }, OperationSupportModels.ReagentView::id, () -> command.reagentId(), command.operatorUserId(), command.operatorName(), command::reagentId);
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.ReagentStockView> listReagentStocks(String keyword, String stockStatus) {
        return operationSupportRepository.findReagentStocks(keyword, stockStatus).stream().map(this::toReagentStockView).toList();
    }

    @Transactional
    public OperationSupportModels.ReagentStockView createReagentStock(OperationSupportModels.CreateReagentStockCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "REAGENT_STOCK", "create_reagent_stock", () -> {
            operationSupportRepository.findReagentById(command.reagentId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent not found"));
            try {
                String stockId = diagnosticReportSupport.nextId("RST");
                LocalDateTime now = LocalDateTime.now();
                operationSupportRepository.insertReagentStock(new OperationSupportRepository.CreateReagentStockCommand(
                    stockId,
                    command.reagentId(),
                    command.batchNo(),
                    command.stockQuantity(),
                    command.stockStatus(),
                    command.expiryDate(),
                    command.storageLocation(),
                    command.lowStockThreshold(),
                    command.nearExpiryDays(),
                    command.remarks(),
                    now,
                    now));
                return toReagentStockView(operationSupportRepository.findReagentStockById(stockId).orElseThrow());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Reagent stock batch already exists");
            }
        }, OperationSupportModels.ReagentStockView::id, null, command.operatorUserId(), command.operatorName(), command::batchNo);
    }

    @Transactional
    public OperationSupportModels.ReagentStockView updateReagentStock(OperationSupportModels.UpdateReagentStockCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "REAGENT_STOCK", "update_reagent_stock", () -> {
            operationSupportRepository.findReagentStockById(command.stockId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent stock not found"));
            operationSupportRepository.updateReagentStock(new OperationSupportRepository.UpdateReagentStockCommand(
                command.stockId(),
                command.stockQuantity(),
                command.stockStatus(),
                command.expiryDate(),
                command.storageLocation(),
                command.lowStockThreshold(),
                command.nearExpiryDays(),
                command.remarks(),
                LocalDateTime.now()));
            return toReagentStockView(operationSupportRepository.findReagentStockById(command.stockId()).orElseThrow());
        }, OperationSupportModels.ReagentStockView::id, () -> command.stockId(), command.operatorUserId(), command.operatorName(), command::stockId);
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.ReagentWarningView> listReagentWarnings() {
        LocalDate today = LocalDate.now();
        return operationSupportRepository.findReagentWarnings(today).stream().map(item -> new OperationSupportModels.ReagentWarningView(
            item.stockId(),
            item.reagentCode(),
            item.reagentName(),
            item.batchNo(),
            item.warningType(),
            item.stockQuantity(),
            item.lowStockThreshold(),
            item.expiryDate(),
            item.nearExpiryDays())).toList();
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.EquipmentRecordView> listEquipmentRecords(String keyword, String equipmentStatus) {
        return operationSupportRepository.findEquipmentRecords(keyword, equipmentStatus).stream().map(this::toEquipmentRecordView).toList();
    }

    @Transactional
    public OperationSupportModels.EquipmentRecordView createEquipmentRecord(OperationSupportModels.CreateEquipmentRecordCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "EQUIPMENT", "create_equipment_record", () -> {
            try {
                String equipmentId = diagnosticReportSupport.nextId("EQ");
                LocalDateTime now = LocalDateTime.now();
                operationSupportRepository.insertEquipmentRecord(new OperationSupportRepository.CreateEquipmentRecordCommand(
                    equipmentId,
                    command.equipmentCode(),
                    command.equipmentName(),
                    command.equipmentCategory(),
                    command.modelNo(),
                    command.equipmentStatus(),
                    command.locationDescription(),
                    parseDateTime(command.enabledAt()),
                    parseDateTime(command.nextMaintenanceAt()),
                    command.remarks(),
                    now,
                    now));
                return toEquipmentRecordView(operationSupportRepository.findEquipmentRecordById(equipmentId).orElseThrow());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Equipment code already exists");
            }
        }, OperationSupportModels.EquipmentRecordView::id, null, command.operatorUserId(), command.operatorName(), command::equipmentCode);
    }

    @Transactional
    public OperationSupportModels.EquipmentRecordView updateEquipmentRecord(OperationSupportModels.UpdateEquipmentRecordCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "EQUIPMENT", "update_equipment_record", () -> {
            operationSupportRepository.findEquipmentRecordById(command.equipmentId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Equipment record not found"));
            operationSupportRepository.updateEquipmentRecord(new OperationSupportRepository.UpdateEquipmentRecordCommand(
                command.equipmentId(),
                command.equipmentName(),
                command.equipmentCategory(),
                command.modelNo(),
                command.equipmentStatus(),
                command.locationDescription(),
                parseDateTime(command.enabledAt()),
                parseDateTime(command.nextMaintenanceAt()),
                command.remarks(),
                LocalDateTime.now()));
            return toEquipmentRecordView(operationSupportRepository.findEquipmentRecordById(command.equipmentId()).orElseThrow());
        }, OperationSupportModels.EquipmentRecordView::id, () -> command.equipmentId(), command.operatorUserId(), command.operatorName(), command::equipmentId);
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.EquipmentMaintenanceLogView> listEquipmentMaintenanceLogs(String equipmentId) {
        operationSupportRepository.findEquipmentRecordById(equipmentId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Equipment record not found"));
        return operationSupportRepository.findEquipmentMaintenanceLogs(equipmentId).stream().map(item -> new OperationSupportModels.EquipmentMaintenanceLogView(
            item.id(),
            item.equipmentId(),
            item.maintenanceType(),
            item.maintenanceStatus(),
            stringify(item.performedAt()),
            item.performedByName(),
            item.description(),
            stringify(item.nextMaintenanceAt()),
            item.remarks())).toList();
    }

    @Transactional
    public OperationSupportModels.EquipmentMaintenanceLogView createEquipmentMaintenanceLog(OperationSupportModels.CreateEquipmentMaintenanceLogCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "EQUIPMENT_MAINTENANCE", "create_equipment_maintenance_log", () -> {
            operationSupportRepository.findEquipmentRecordById(command.equipmentId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Equipment record not found"));
            String maintenanceLogId = diagnosticReportSupport.nextId("EQM");
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextMaintenanceAt = parseDateTime(command.nextMaintenanceAt());
            operationSupportRepository.insertEquipmentMaintenanceLog(new OperationSupportRepository.CreateEquipmentMaintenanceLogCommand(
                maintenanceLogId,
                command.equipmentId(),
                command.maintenanceType(),
                command.maintenanceStatus(),
                parseDateTime(command.performedAt()),
                command.operatorUserId(),
                command.operatorName(),
                command.description(),
                nextMaintenanceAt,
                command.remarks(),
                now,
                now));
            if (nextMaintenanceAt != null) {
                OperationSupportRepository.EquipmentRecord equipment = operationSupportRepository.findEquipmentRecordById(command.equipmentId()).orElseThrow();
                operationSupportRepository.updateEquipmentRecord(new OperationSupportRepository.UpdateEquipmentRecordCommand(
                    equipment.id(),
                    equipment.equipmentName(),
                    equipment.equipmentCategory(),
                    equipment.modelNo(),
                    equipment.equipmentStatus(),
                    equipment.locationDescription(),
                    equipment.enabledAt(),
                    nextMaintenanceAt,
                    equipment.remarks(),
                    now));
            }
            return listEquipmentMaintenanceLogs(command.equipmentId()).stream()
                .filter(item -> maintenanceLogId.equals(item.id()))
                .findFirst()
                .orElseThrow();
        }, OperationSupportModels.EquipmentMaintenanceLogView::id, null, command.operatorUserId(), command.operatorName(), command::equipmentId);
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.EquipmentWarningView> listEquipmentWarnings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueSoonThreshold = now.plusDays(7);
        return operationSupportRepository.findEquipmentWarnings(now, dueSoonThreshold).stream().map(item -> new OperationSupportModels.EquipmentWarningView(
            item.equipmentId(),
            item.equipmentCode(),
            item.equipmentName(),
            item.warningType(),
            stringify(item.nextMaintenanceAt()),
            item.equipmentStatus())).toList();
    }

    private OperationSupportModels.ReagentView toReagentView(OperationSupportRepository.Reagent item) {
        return new OperationSupportModels.ReagentView(
            item.id(),
            item.reagentCode(),
            item.reagentName(),
            item.specification(),
            item.unit(),
            item.manufacturer(),
            item.defaultLowStockThreshold(),
            item.defaultNearExpiryDays(),
            item.enabled(),
            item.remarks());
    }

    private OperationSupportModels.ReagentStockView toReagentStockView(OperationSupportRepository.ReagentStock item) {
        return new OperationSupportModels.ReagentStockView(
            item.id(),
            item.reagentId(),
            item.reagentCode(),
            item.reagentName(),
            item.batchNo(),
            item.stockQuantity(),
            item.stockStatus(),
            item.expiryDate(),
            item.storageLocation(),
            item.lowStockThreshold(),
            item.nearExpiryDays(),
            item.remarks());
    }

    private OperationSupportModels.EquipmentRecordView toEquipmentRecordView(OperationSupportRepository.EquipmentRecord item) {
        return new OperationSupportModels.EquipmentRecordView(
            item.id(),
            item.equipmentCode(),
            item.equipmentName(),
            item.equipmentCategory(),
            item.modelNo(),
            item.equipmentStatus(),
            item.locationDescription(),
            stringify(item.enabledAt()),
            stringify(item.nextMaintenanceAt()),
            item.remarks());
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
