package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.OperationSupportRepository;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return listReagents(keyword, enabled, null, null);
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.ReagentView> listReagents(String keyword, Boolean enabled, String reagentType, String templateStatus) {
        return operationSupportRepository.findReagents(keyword, enabled, blankToNull(reagentType), blankToNull(templateStatus))
            .stream()
            .map(item -> OperationSupportServiceSupport.toReagentView(item, this::stringify))
            .toList();
    }

    @Transactional
    public OperationSupportModels.ReagentView createReagent(OperationSupportModels.CreateReagentCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "REAGENT", "create_reagent", () -> {
            try {
                String reagentId = diagnosticReportSupport.nextId("REAG");
                LocalDateTime now = LocalDateTime.now();
                validateOrderDictItem(command.orderDictItemId());
                String templateStatus = resolveTemplateStatus(command.templateStatus(), command.enabled());
                BigDecimal defaultStockThreshold = coalesce(command.defaultStockThreshold(), command.defaultLowStockThreshold());
                BigDecimal defaultLowStockThreshold = coalesce(command.defaultLowStockThreshold(), defaultStockThreshold);
                operationSupportRepository.insertReagent(new OperationSupportRepository.CreateReagentCommand(
                    reagentId,
                    command.reagentCode(),
                    command.reagentName(),
                    command.specification(),
                    command.unit(),
                    command.manufacturer(),
                    command.reagentType(),
                    command.reagentUsage(),
                    command.orderDictItemId(),
                    command.cloneNo(),
                    command.recommendedDilution(),
                    command.applicationDilution(),
                    templateStatus,
                    command.validityDays(),
                    defaultLowStockThreshold,
                    defaultStockThreshold,
                    command.defaultNearExpiryDays(),
                    command.stainCapacity(),
                    command.stainThreshold(),
                    isEnabledTemplateStatus(templateStatus),
                    command.remarks(),
                    command.operatorUserId(),
                    command.operatorName(),
                    command.operatorUserId(),
                    command.operatorName(),
                    now,
                    now));
                return OperationSupportServiceSupport.toReagentView(
                    operationSupportRepository.findReagentById(reagentId).orElseThrow(),
                    this::stringify);
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
            validateOrderDictItem(command.orderDictItemId());
            String templateStatus = resolveTemplateStatus(command.templateStatus(), command.enabled());
            BigDecimal defaultStockThreshold = coalesce(command.defaultStockThreshold(), command.defaultLowStockThreshold());
            BigDecimal defaultLowStockThreshold = coalesce(command.defaultLowStockThreshold(), defaultStockThreshold);
            operationSupportRepository.updateReagent(new OperationSupportRepository.UpdateReagentCommand(
                command.reagentId(),
                command.reagentName(),
                command.specification(),
                command.unit(),
                command.manufacturer(),
                command.reagentType(),
                command.reagentUsage(),
                command.orderDictItemId(),
                command.cloneNo(),
                command.recommendedDilution(),
                command.applicationDilution(),
                templateStatus,
                command.validityDays(),
                defaultLowStockThreshold,
                defaultStockThreshold,
                command.defaultNearExpiryDays(),
                command.stainCapacity(),
                command.stainThreshold(),
                isEnabledTemplateStatus(templateStatus),
                command.remarks(),
                command.operatorUserId(),
                command.operatorName(),
                LocalDateTime.now()));
            return OperationSupportServiceSupport.toReagentView(
                operationSupportRepository.findReagentById(command.reagentId()).orElseThrow(),
                this::stringify);
        }, OperationSupportModels.ReagentView::id, () -> command.reagentId(), command.operatorUserId(), command.operatorName(), command::reagentId);
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.ReagentStockView> listReagentStocks(String keyword, String stockStatus) {
        return listReagentStocks(keyword, stockStatus, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.ReagentStockView> listReagentStocks(String keyword,
                                                                           String stockStatus,
                                                                           String reagentType,
                                                                           LocalDate dateFrom,
                                                                           LocalDate dateTo) {
        return operationSupportRepository.findReagentStocks(keyword, blankToNull(stockStatus), blankToNull(reagentType), dateFrom, dateTo)
            .stream()
            .map(item -> OperationSupportServiceSupport.toReagentStockView(item, this::stringify))
            .toList();
    }

    @Transactional
    public OperationSupportModels.ReagentStockView createReagentStock(OperationSupportModels.CreateReagentStockCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "REAGENT_STOCK", "create_reagent_stock", () -> {
            OperationSupportRepository.Reagent reagent = operationSupportRepository.findReagentById(command.reagentId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent not found"));
            try {
                String stockId = diagnosticReportSupport.nextId("RST");
                LocalDateTime now = LocalDateTime.now();
                BigDecimal remainingQuantity = resolveQuantity(command.remainingQuantity(), command.stockQuantity(), command.initialQuantity());
                BigDecimal initialQuantity = resolveQuantity(command.initialQuantity(), command.stockQuantity(), remainingQuantity);
                String stockStatus = resolveStockStatus(command.stockStatus(), "IN_STOCK");
                LocalDate expiryDate = resolveExpiryDate(command.expiryDate(), command.productionDate(), coalesce(command.validityDays(), reagent.validityDays()));
                LocalDateTime inboundAt = command.inboundAt() == null ? now : command.inboundAt();
                operationSupportRepository.insertReagentStock(new OperationSupportRepository.CreateReagentStockCommand(
                    stockId,
                    command.reagentId(),
                    command.batchNo(),
                    initialQuantity,
                    remainingQuantity,
                    remainingQuantity,
                    stockStatus,
                    command.productionDate(),
                    inboundAt,
                    expiryDate,
                    command.storageLocation(),
                    coalesce(command.lowStockThreshold(), reagent.defaultStockThreshold(), reagent.defaultLowStockThreshold()),
                    coalesce(command.nearExpiryDays(), reagent.defaultNearExpiryDays()),
                    command.testReminderThreshold(),
                    coalesce(command.expiryReminderThreshold(), command.nearExpiryDays(), reagent.defaultNearExpiryDays()),
                    coalesce(command.recommendedDilution(), reagent.recommendedDilution()),
                    coalesce(command.applicationDilution(), reagent.applicationDilution()),
                    coalesce(command.stainCapacity(), reagent.stainCapacity()),
                    coalesce(command.stainThreshold(), reagent.stainThreshold()),
                    coalesce(command.validityDays(), reagent.validityDays()),
                    command.remarks(),
                    command.operatorUserId(),
                    command.operatorName(),
                    command.operatorUserId(),
                    command.operatorName(),
                    now,
                    now));
                appendStockEvent(stockId, "INBOUND", initialQuantity, BigDecimal.ZERO, remainingQuantity, now,
                    command.operatorUserId(), command.operatorName(), command.remarks());
                return OperationSupportServiceSupport.toReagentStockView(
                    operationSupportRepository.findReagentStockById(stockId).orElseThrow(),
                    this::stringify);
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Reagent stock batch already exists");
            }
        }, OperationSupportModels.ReagentStockView::id, null, command.operatorUserId(), command.operatorName(), command::batchNo);
    }

    @Transactional
    public OperationSupportModels.ReagentStockView updateReagentStock(OperationSupportModels.UpdateReagentStockCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "REAGENT_STOCK", "update_reagent_stock", () -> {
            OperationSupportRepository.ReagentStock current = operationSupportRepository.findReagentStockById(command.stockId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent stock not found"));
            BigDecimal remainingQuantity = resolveQuantity(command.remainingQuantity(), command.stockQuantity(), current.remainingQuantity(), current.stockQuantity());
            BigDecimal initialQuantity = resolveQuantity(command.initialQuantity(), current.initialQuantity(), remainingQuantity);
            String stockStatus = resolveStockStatus(command.stockStatus(), current.stockStatus());
            operationSupportRepository.updateReagentStock(new OperationSupportRepository.UpdateReagentStockCommand(
                command.stockId(),
                initialQuantity,
                remainingQuantity,
                remainingQuantity,
                stockStatus,
                command.productionDate(),
                command.inboundAt() == null ? current.inboundAt() : command.inboundAt(),
                command.expiryDate(),
                command.storageLocation(),
                command.lowStockThreshold(),
                command.nearExpiryDays(),
                command.testReminderThreshold(),
                command.expiryReminderThreshold(),
                command.recommendedDilution(),
                command.applicationDilution(),
                command.stainCapacity(),
                command.stainThreshold(),
                command.validityDays(),
                command.remarks(),
                command.operatorUserId(),
                command.operatorName(),
                LocalDateTime.now()));
            return OperationSupportServiceSupport.toReagentStockView(
                operationSupportRepository.findReagentStockById(command.stockId()).orElseThrow(),
                this::stringify);
        }, OperationSupportModels.ReagentStockView::id, () -> command.stockId(), command.operatorUserId(), command.operatorName(), command::stockId);
    }

    @Transactional
    public OperationSupportModels.ReagentStockView testReagentStock(OperationSupportModels.ReagentStockActionCommand command) {
        return adjustReagentStock(command, "TEST", "TESTED", true);
    }

    @Transactional
    public OperationSupportModels.ReagentStockView consumeReagentStock(OperationSupportModels.ReagentStockActionCommand command) {
        return adjustReagentStock(command, "CONSUME", null, true);
    }

    @Transactional
    public OperationSupportModels.ReagentStockView startUsingReagentStock(OperationSupportModels.ReagentStockActionCommand command) {
        return adjustReagentStock(command, "START_USE", "IN_USE", false);
    }

    @Transactional
    public OperationSupportModels.ReagentStockView finishUsingReagentStock(OperationSupportModels.ReagentStockActionCommand command) {
        return adjustReagentStock(command, "FINISH_USE", "FINISHED", false);
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.ReagentStockEventView> listReagentStockEvents(String stockId) {
        operationSupportRepository.findReagentStockById(stockId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent stock not found"));
        return operationSupportRepository.findReagentStockEvents(stockId).stream()
            .map(item -> new OperationSupportModels.ReagentStockEventView(
                item.id(),
                item.stockId(),
                item.eventType(),
                item.quantityDelta(),
                item.quantityBefore(),
                item.quantityAfter(),
                stringify(item.occurredAt()),
                item.operatorName(),
                item.remarks()))
            .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportReagentStocks(String keyword,
                                      String stockStatus,
                                      String reagentType,
                                      LocalDate dateFrom,
                                      LocalDate dateTo) {
        List<OperationSupportModels.ReagentStockView> stocks =
            listReagentStocks(keyword, stockStatus, reagentType, dateFrom, dateTo);
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("试剂编码,试剂名称,试剂类型,对应医嘱,批号,初始数量,当前剩余量,库存状态,生产日期,入库时间,有效期,库位,推荐稀释度,应用稀释度,预计染色总量,染色阈值,备注\r\n");
        for (OperationSupportModels.ReagentStockView stock : stocks) {
            appendCsvRow(builder, List.of(
                safe(stock.reagentCode()),
                safe(stock.reagentName()),
                safe(stock.reagentType()),
                safe(stock.orderItemName()),
                safe(stock.batchNo()),
                decimal(stock.initialQuantity()),
                decimal(stock.remainingQuantity()),
                safe(stock.stockStatus()),
                stock.productionDate() == null ? "" : stock.productionDate().toString(),
                safe(stock.inboundAt()),
                stock.expiryDate() == null ? "" : stock.expiryDate().toString(),
                safe(stock.storageLocation()),
                safe(stock.recommendedDilution()),
                safe(stock.applicationDilution()),
                decimal(stock.stainCapacity()),
                decimal(stock.stainThreshold()),
                safe(stock.remarks())));
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public OperationSupportModels.ReagentStockImportResult importReagentStocks(byte[] content,
                                                                               String operatorUserId,
                                                                               String operatorName) {
        List<Map<String, String>> rows = parseCsv(content);
        int successCount = 0;
        int failureCount = 0;
        List<OperationSupportModels.ReagentStockImportError> errors = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            Map<String, String> row = rows.get(index);
            int rowNumber = index + 2;
            String reagentCode = firstPresent(row, "试剂编码", "reagentCode");
            String reagentName = firstPresent(row, "试剂名称", "reagentName");
            String batchNo = firstPresent(row, "批号", "batchNo");
            if (blankToNull(batchNo) == null) {
                failureCount++;
                errors.add(importError(rowNumber, "batchNo", batchNo, "Batch no must not be blank"));
                continue;
            }
            OperationSupportRepository.Reagent reagent = operationSupportRepository.findReagentByCodeOrName(reagentCode, reagentName).orElse(null);
            if (reagent == null) {
                failureCount++;
                errors.add(importError(rowNumber, "reagentCode", reagentCode, "Reagent template not found"));
                continue;
            }
            BigDecimal initialQuantity = parseDecimal(firstPresent(row, "初始数量", "initialQuantity"), rowNumber, "initialQuantity", errors);
            BigDecimal remainingQuantity = parseDecimal(firstPresent(row, "当前剩余量", "remainingQuantity", "stockQuantity"), rowNumber, "remainingQuantity", errors);
            if (hasRowError(errors, rowNumber)) {
                failureCount++;
                continue;
            }
            BigDecimal lowStockThreshold = parseDecimal(firstPresent(row, "库存阈值", "lowStockThreshold"), rowNumber, "lowStockThreshold", errors);
            Integer nearExpiryDays = parseInteger(firstPresent(row, "过期提醒阈值", "nearExpiryDays"), null, rowNumber, "nearExpiryDays", errors);
            Integer testReminderThreshold = parseInteger(firstPresent(row, "测试提醒阈值", "testReminderThreshold"), null, rowNumber, "testReminderThreshold", errors);
            Integer expiryReminderThreshold = parseInteger(firstPresent(row, "过期提醒阈值", "expiryReminderThreshold"), null, rowNumber, "expiryReminderThreshold", errors);
            BigDecimal stainCapacity = parseDecimal(firstPresent(row, "预计染色总量", "stainCapacity"), rowNumber, "stainCapacity", errors);
            BigDecimal stainThreshold = parseDecimal(firstPresent(row, "染色阈值", "stainThreshold"), rowNumber, "stainThreshold", errors);
            Integer validityDays = parseInteger(firstPresent(row, "有效期天数", "validityDays"), null, rowNumber, "validityDays", errors);
            LocalDate productionDate = parseDate(firstPresent(row, "生产日期", "productionDate"));
            LocalDateTime inboundAt = parseDateTime(firstPresent(row, "入库时间", "inboundAt"));
            LocalDate expiryDate = parseDate(firstPresent(row, "有效期", "expiryDate"));
            if (hasRowError(errors, rowNumber)) {
                failureCount++;
                continue;
            }
            try {
                createReagentStock(new OperationSupportModels.CreateReagentStockCommand(
                    reagent.id(),
                    batchNo,
                    initialQuantity,
                    remainingQuantity,
                    remainingQuantity,
                    resolveStockStatus(firstPresent(row, "库存状态", "stockStatus"), "IN_STOCK"),
                    productionDate,
                    inboundAt,
                    expiryDate,
                    firstPresent(row, "库位", "storageLocation"),
                    lowStockThreshold,
                    nearExpiryDays,
                    testReminderThreshold,
                    expiryReminderThreshold,
                    firstPresent(row, "推荐稀释度", "recommendedDilution"),
                    firstPresent(row, "应用稀释度", "applicationDilution"),
                    stainCapacity,
                    stainThreshold,
                    validityDays,
                    operatorUserId,
                    operatorName,
                    firstPresent(row, "备注", "remarks")));
                successCount++;
            } catch (BlBusinessException exception) {
                failureCount++;
                errors.add(importError(rowNumber, "batchNo", batchNo, exception.getMessage()));
            } catch (RuntimeException exception) {
                failureCount++;
                errors.add(importError(rowNumber, "batchNo", batchNo,
                    exception.getMessage() == null ? "Reagent stock import failed" : exception.getMessage()));
            }
        }
        return new OperationSupportModels.ReagentStockImportResult(successCount, failureCount, errors);
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
        return operationSupportRepository.findEquipmentRecords(keyword, equipmentStatus).stream().map(item -> OperationSupportServiceSupport.toEquipmentRecordView(item, this::stringify, this::stringifyDate)).toList();
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
                    command.quantity(),
                    parseDate(command.purchaseDate()),
                    blankToNull(command.purchaserName()),
                    blankToNull(command.purchaserCode()),
                    blankToNull(command.managementUnit()),
                    blankToNull(command.managementCode()),
                    blankToNull(command.useUnit()),
                    blankToNull(command.principalCode()),
                    blankToNull(command.principalName()),
                    blankToNull(command.userName()),
                    parseDate(command.productionDate()),
                    parseDate(command.warrantyEndDate()),
                    blankToNull(command.factoryNo()),
                    blankToNull(command.depreciationMethod()),
                    command.serviceLifeYears(),
                    command.price(),
                    blankToNull(command.manufacturer()),
                    blankToNull(command.portNo()),
                    blankToNull(command.ipAddress()),
                    parseTime(command.commonStartupTime()),
                    parseTime(command.commonShutdownTime()),
                    blankToNull(command.commonUsageContent()),
                    command.commonlyUsed(),
                    command.setTemperature(),
                    command.currentTemperature(),
                    blankToNull(command.rfid()),
                    command.remarks(),
                    now,
                    now));
                return OperationSupportServiceSupport.toEquipmentRecordView(
                    operationSupportRepository.findEquipmentRecordById(equipmentId).orElseThrow(),
                    this::stringify,
                    this::stringifyDate);
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
                command.quantity(),
                parseDate(command.purchaseDate()),
                blankToNull(command.purchaserName()),
                blankToNull(command.purchaserCode()),
                blankToNull(command.managementUnit()),
                blankToNull(command.managementCode()),
                blankToNull(command.useUnit()),
                blankToNull(command.principalCode()),
                blankToNull(command.principalName()),
                blankToNull(command.userName()),
                parseDate(command.productionDate()),
                parseDate(command.warrantyEndDate()),
                blankToNull(command.factoryNo()),
                blankToNull(command.depreciationMethod()),
                command.serviceLifeYears(),
                command.price(),
                blankToNull(command.manufacturer()),
                blankToNull(command.portNo()),
                blankToNull(command.ipAddress()),
                parseTime(command.commonStartupTime()),
                parseTime(command.commonShutdownTime()),
                blankToNull(command.commonUsageContent()),
                command.commonlyUsed(),
                command.setTemperature(),
                command.currentTemperature(),
                blankToNull(command.rfid()),
                command.remarks(),
                LocalDateTime.now()));
            return OperationSupportServiceSupport.toEquipmentRecordView(
                operationSupportRepository.findEquipmentRecordById(command.equipmentId()).orElseThrow(),
                this::stringify,
                this::stringifyDate);
        }, OperationSupportModels.EquipmentRecordView::id, () -> command.equipmentId(), command.operatorUserId(), command.operatorName(), command::equipmentId);
    }

    @Transactional
    public List<OperationSupportModels.EquipmentRecordView> batchUpdateEquipmentStatus(OperationSupportModels.BatchUpdateEquipmentStatusCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "EQUIPMENT", "batch_update_equipment_status", () -> {
            List<String> equipmentIds = normalizeIds(command.equipmentIds());
            if (equipmentIds.isEmpty()) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Equipment IDs are required");
            }
            String equipmentStatus = blankToNull(command.equipmentStatus());
            if (!"ACTIVE".equals(equipmentStatus) && !"DISABLED".equals(equipmentStatus)) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported equipment status");
            }
            for (String equipmentId : equipmentIds) {
                operationSupportRepository.findEquipmentRecordById(equipmentId)
                    .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Equipment record not found"));
            }
            operationSupportRepository.updateEquipmentStatusBatch(equipmentIds, equipmentStatus, LocalDateTime.now());
            return equipmentIds.stream()
                .map(id -> OperationSupportServiceSupport.toEquipmentRecordView(
                    operationSupportRepository.findEquipmentRecordById(id).orElseThrow(),
                    this::stringify,
                    this::stringifyDate))
                .toList();
        }, item -> item.isEmpty() ? null : item.get(0).id(), null, command.operatorUserId(), command.operatorName(), () -> String.join(",", command.equipmentIds()));
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

    @Transactional(readOnly = true)
    public List<OperationSupportModels.EquipmentCommonDeviceView> listEquipmentCommonDevices() {
        return operationSupportRepository.findCommonlyUsedEquipmentRecords().stream().map(OperationSupportServiceSupport::toEquipmentCommonDeviceView).toList();
    }

    @Transactional
    public OperationSupportModels.EquipmentUsageRecordView createEquipmentUsageRecord(
        OperationSupportModels.CreateEquipmentUsageRecordCommand command
    ) {
        return operationAuditService.audit("M5_SUPPORT", "EQUIPMENT_USAGE", "create_equipment_usage_record", () -> {
            LocalDateTime startedAt = parseDateTime(command.startedAt());
            LocalDateTime endedAt = parseDateTime(command.endedAt());
            OperationSupportServiceSupport.validateEquipmentUsageRecordCommand(command, startedAt, endedAt);

            OperationSupportRepository.EquipmentRecord equipment = null;
            if (blankToNull(command.equipmentId()) != null) {
                equipment = operationSupportRepository.findEquipmentRecordById(command.equipmentId())
                    .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Equipment record not found"));
            }

            String usageRecordId = diagnosticReportSupport.nextId("EQU");
            LocalDateTime now = LocalDateTime.now();
            operationSupportRepository.insertEquipmentUsageRecord(OperationSupportServiceSupport.toCreateEquipmentUsageRecordCommand(
                usageRecordId,
                command,
                equipment,
                startedAt,
                endedAt,
                now,
                this::blankToNull));
            return OperationSupportServiceSupport.toEquipmentUsageRecordView(
                operationSupportRepository.findEquipmentUsageRecordById(usageRecordId).orElseThrow(),
                this::stringify);
        }, OperationSupportModels.EquipmentUsageRecordView::id, null, command.operatorUserId(), command.operatorName(), command::equipmentName);
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
                    equipment.quantity(),
                    equipment.purchaseDate(),
                    equipment.purchaserName(),
                    equipment.purchaserCode(),
                    equipment.managementUnit(),
                    equipment.managementCode(),
                    equipment.useUnit(),
                    equipment.principalCode(),
                    equipment.principalName(),
                    equipment.userName(),
                    equipment.productionDate(),
                    equipment.warrantyEndDate(),
                    equipment.factoryNo(),
                    equipment.depreciationMethod(),
                    equipment.serviceLifeYears(),
                    equipment.price(),
                    equipment.manufacturer(),
                    equipment.portNo(),
                    equipment.ipAddress(),
                    equipment.commonStartupTime(),
                    equipment.commonShutdownTime(),
                    equipment.commonUsageContent(),
                    equipment.commonlyUsed(),
                    equipment.setTemperature(),
                    equipment.currentTemperature(),
                    equipment.rfid(),
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

    @Transactional(readOnly = true)
    public List<OperationSupportModels.WhiteSlideStockView> listWhiteSlideStocks(String keyword, String status) {
        return operationSupportRepository.findWhiteSlideStocks(keyword, blankToNull(status)).stream()
            .map(OperationSupportServiceSupport::toWhiteSlideStockView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OperationSupportModels.WhiteSlideLoanView> listWhiteSlideLoans(String keyword, String loanStatus) {
        String normalizedLoanStatus = blankToNull(loanStatus);
        return operationSupportRepository.findWhiteSlideLoans(keyword, normalizedLoanStatus == null ? "BORROWED" : normalizedLoanStatus).stream().map(item -> OperationSupportServiceSupport.toWhiteSlideLoanView(item, this::stringify)).toList();
    }

    @Transactional
    public OperationSupportModels.WhiteSlideLoanView createWhiteSlideLoan(OperationSupportModels.CreateWhiteSlideLoanCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "WHITE_SLIDE_LOAN", "create_white_slide_loan", () -> {
            OperationSupportRepository.WhiteSlideStock stock = operationSupportRepository.findWhiteSlideStockById(command.stockId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "White slide stock not found"));
            if (!"ACTIVE".equals(stock.status())) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "White slide stock is not active");
            }
            int quantity = requirePositiveInt(command.quantity(), "Quantity must be greater than zero");
            int available = stock.quantityAvailable() == null ? 0 : stock.quantityAvailable();
            int borrowed = stock.quantityBorrowed() == null ? 0 : stock.quantityBorrowed();
            if (available < quantity) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "White slide stock is insufficient");
            }

            String loanId = diagnosticReportSupport.nextId("WSL");
            String loanNo = "WS-" + System.currentTimeMillis();
            LocalDateTime now = LocalDateTime.now();
            BigDecimal unitPrice = command.unitPrice();
            BigDecimal amount = resolveWhiteSlideAmount(command.amount(), unitPrice, quantity);

            operationSupportRepository.insertWhiteSlideLoan(new OperationSupportRepository.CreateWhiteSlideLoanCommand(
                loanId,
                loanNo,
                stock.id(),
                quantity,
                blankToNull(command.caseId()),
                blankToNull(command.pathologyNo()),
                blankToNull(command.patientName()),
                blankToNull(command.embeddingBoxNo()),
                blankToNull(command.slicePurpose()),
                blankToNull(command.sliceThickness()),
                command.borrowerName(),
                blankToNull(command.borrowerIdentityNo()),
                blankToNull(command.borrowerUnit()),
                blankToNull(command.borrowerPhone()),
                unitPrice,
                amount,
                command.saveDirectPrint(),
                "BORROWED",
                blankToNull(command.waxBlockUsage()),
                command.operatorUserId(),
                command.operatorName(),
                now,
                blankToNull(command.remarks()),
                now,
                now));

            operationSupportRepository.updateWhiteSlideStockQuantities(new OperationSupportRepository.UpdateWhiteSlideStockQuantitiesCommand(
                stock.id(),
                available - quantity,
                borrowed + quantity,
                now));

            return OperationSupportServiceSupport.toWhiteSlideLoanView(
                operationSupportRepository.findWhiteSlideLoanById(loanId).orElseThrow(),
                this::stringify);
        }, OperationSupportModels.WhiteSlideLoanView::id, null, command.operatorUserId(), command.operatorName(), command::stockId);
    }

    @Transactional
    public OperationSupportModels.WhiteSlideLoanView returnWhiteSlideLoan(OperationSupportModels.ReturnWhiteSlideLoanCommand command) {
        return operationAuditService.audit("M5_SUPPORT", "WHITE_SLIDE_LOAN", "return_white_slide_loan", () -> {
            OperationSupportRepository.WhiteSlideLoan loan = operationSupportRepository.findWhiteSlideLoanById(command.loanId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "White slide loan not found"));
            if (!"BORROWED".equals(loan.loanStatus())) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "White slide loan has already been returned");
            }
            OperationSupportRepository.WhiteSlideStock stock = operationSupportRepository.findWhiteSlideStockById(loan.stockId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "White slide stock not found"));

            LocalDateTime now = LocalDateTime.now();
            int available = stock.quantityAvailable() == null ? 0 : stock.quantityAvailable();
            int borrowed = stock.quantityBorrowed() == null ? 0 : stock.quantityBorrowed();
            int quantity = loan.quantity() == null ? 0 : loan.quantity();

            operationSupportRepository.updateWhiteSlideLoanReturned(new OperationSupportRepository.UpdateWhiteSlideLoanReturnedCommand(
                loan.id(),
                "RETURNED",
                now,
                command.operatorUserId(),
                command.operatorName(),
                blankToNull(command.remarks()),
                now));
            operationSupportRepository.updateWhiteSlideStockQuantities(new OperationSupportRepository.UpdateWhiteSlideStockQuantitiesCommand(
                stock.id(),
                available + quantity,
                Math.max(0, borrowed - quantity),
                now));

            return OperationSupportServiceSupport.toWhiteSlideLoanView(
                operationSupportRepository.findWhiteSlideLoanById(loan.id()).orElseThrow(),
                this::stringify);
        }, OperationSupportModels.WhiteSlideLoanView::id, command::loanId, command.operatorUserId(), command.operatorName(), command::loanId);
    }

    private OperationSupportModels.ReagentStockView adjustReagentStock(OperationSupportModels.ReagentStockActionCommand command,
                                                                       String eventType,
                                                                       String nextStatus,
                                                                       boolean subtractQuantity) {
        return operationAuditService.audit("M5_SUPPORT", "REAGENT_STOCK", eventType.toLowerCase(), () -> {
            OperationSupportRepository.ReagentStock current = operationSupportRepository.findReagentStockById(command.stockId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Reagent stock not found"));
            if (isClosedStockStatus(current.stockStatus())) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Reagent stock cannot be operated in current status");
            }
            BigDecimal before = coalesce(current.remainingQuantity(), current.stockQuantity(), BigDecimal.ZERO);
            BigDecimal quantity = subtractQuantity ? requirePositiveQuantity(command.quantity()) : BigDecimal.ZERO;
            BigDecimal after = subtractQuantity ? before.subtract(quantity) : before;
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Reagent stock cannot be consumed below zero");
            }
            LocalDateTime now = LocalDateTime.now();
            String status = nextStatus == null ? current.stockStatus() : nextStatus;
            operationSupportRepository.updateReagentStockState(new OperationSupportRepository.UpdateReagentStockStateCommand(
                current.id(),
                after,
                after,
                status,
                "TEST".equals(eventType) ? now : null,
                "START_USE".equals(eventType) ? now : null,
                "FINISH_USE".equals(eventType) ? now : null,
                command.remarks(),
                command.operatorUserId(),
                command.operatorName(),
                now));
            appendStockEvent(current.id(), eventType, subtractQuantity ? quantity.negate() : null, before, after,
                now, command.operatorUserId(), command.operatorName(), command.remarks());
            return OperationSupportServiceSupport.toReagentStockView(
                operationSupportRepository.findReagentStockById(current.id()).orElseThrow(),
                this::stringify);
        }, OperationSupportModels.ReagentStockView::id, () -> command.stockId(), command.operatorUserId(), command.operatorName(), command::stockId);
    }

    private void appendStockEvent(String stockId,
                                  String eventType,
                                  BigDecimal quantityDelta,
                                  BigDecimal quantityBefore,
                                  BigDecimal quantityAfter,
                                  LocalDateTime occurredAt,
                                  String operatorUserId,
                                  String operatorName,
                                  String remarks) {
        operationSupportRepository.insertReagentStockEvent(new OperationSupportRepository.CreateReagentStockEventCommand(
            diagnosticReportSupport.nextId("RSE"),
            stockId,
            eventType,
            quantityDelta,
            quantityBefore,
            quantityAfter,
            occurredAt,
            operatorUserId,
            operatorName,
            remarks,
            occurredAt));
    }

    private void validateOrderDictItem(String orderDictItemId) {
        if (!operationSupportRepository.existsMedicalOrderItem(orderDictItemId)) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order item not found");
        }
    }

    private String resolveTemplateStatus(String requestedStatus, boolean enabled) {
        String normalized = blankToNull(requestedStatus);
        if (normalized == null) {
            return enabled ? "ENABLED" : "DISABLED";
        }
        return normalized;
    }

    private boolean isEnabledTemplateStatus(String templateStatus) {
        return !"DISABLED".equals(templateStatus) && !"DELETED".equals(templateStatus);
    }

    private String resolveStockStatus(String requestedStatus, String defaultStatus) {
        String normalized = blankToNull(requestedStatus);
        return normalized == null ? defaultStatus : normalized;
    }

    private BigDecimal resolveQuantity(BigDecimal... candidates) {
        for (BigDecimal candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal requirePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Quantity must be greater than zero");
        }
        return quantity;
    }

    private int requirePositiveInt(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value;
    }

    private BigDecimal resolveWhiteSlideAmount(BigDecimal requestedAmount, BigDecimal unitPrice, int quantity) {
        if (requestedAmount != null) {
            return requestedAmount;
        }
        if (unitPrice == null) {
            return null;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private boolean isClosedStockStatus(String stockStatus) {
        return "DISABLED".equals(stockStatus) || "FINISHED".equals(stockStatus) || "DEPLETED".equals(stockStatus);
    }

    private LocalDate resolveExpiryDate(LocalDate requestedExpiryDate, LocalDate productionDate, Integer validityDays) {
        if (requestedExpiryDate != null) {
            return requestedExpiryDate;
        }
        if (productionDate != null && validityDays != null) {
            return productionDate.plusDays(validityDays);
        }
        return null;
    }

    private String firstPresent(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (blankToNull(value) != null) {
                return value;
            }
        }
        return null;
    }

    private boolean hasRowError(List<OperationSupportModels.ReagentStockImportError> errors, int rowNumber) {
        return errors.stream().anyMatch(error -> error.rowNumber() == rowNumber);
    }

    private OperationSupportModels.ReagentStockImportError importError(int rowNumber, String field, String rejectedValue, String message) {
        return new OperationSupportModels.ReagentStockImportError(rowNumber, field, rejectedValue, message);
    }

    private void appendCsvRow(StringBuilder builder, List<String> values) {
        OperationSupportServiceSupport.appendCsvRow(builder, values);
    }

    private List<Map<String, String>> parseCsv(byte[] content) {
        return OperationSupportServiceSupport.parseCsv(content);
    }

    private BigDecimal parseDecimal(String value,
                                    int rowNumber,
                                    String field,
                                    List<OperationSupportModels.ReagentStockImportError> errors) {
        return OperationSupportServiceSupport.parseDecimal(
            value,
            rowNumber,
            field,
            errors,
            this::blankToNull,
            args -> importError(args.rowNumber(), args.field(), args.rejectedValue(), args.message()));
    }

    private Integer parseInteger(String value,
                                 Integer defaultValue,
                                 int rowNumber,
                                 String field,
                                 List<OperationSupportModels.ReagentStockImportError> errors) {
        return OperationSupportServiceSupport.parseInteger(
            value,
            defaultValue,
            rowNumber,
            field,
            errors,
            this::blankToNull,
            args -> importError(args.rowNumber(), args.field(), args.rejectedValue(), args.message()));
    }

    private LocalDate parseDate(String value) { String normalized = blankToNull(value); return normalized == null ? null : LocalDate.parse(normalized); }

    private String decimal(BigDecimal value) { return OperationSupportServiceSupport.decimal(value); }

    private String safe(String value) { return OperationSupportServiceSupport.safe(value); }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> T coalesce(T first, T second) { return first != null ? first : second; }

    private <T> T coalesce(T first, T second, T third) { return first != null ? first : second != null ? second : third; }

    private List<String> normalizeIds(List<String> values) {
        return values == null ? List.of() : values.stream().map(this::blankToNull).filter(item -> item != null).distinct().toList();
    }
    private LocalDateTime parseDateTime(String value) { return value == null || value.isBlank() ? null : LocalDateTime.parse(value); }

    private String parseTime(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : LocalTime.parse(normalized).toString();
    }

    private String stringifyDate(LocalDate value) { return value == null ? null : value.toString(); }

    private String stringify(LocalDateTime value) { return value == null ? null : value.toString(); }
}
