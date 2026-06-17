package com.company.bl.application.service;

import com.company.bl.domain.repository.OperationSupportRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OperationSupportServiceSupport {

    private OperationSupportServiceSupport() {
    }

    static void appendCsvRow(StringBuilder builder, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(escapeCsv(values.get(index)));
        }
        builder.append("\r\n");
    }

    static List<Map<String, String>> parseCsv(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        List<String> lines = text.lines().filter(line -> !line.isBlank()).toList();
        if (lines.isEmpty()) {
            return List.of();
        }
        List<String> headers = parseCsvLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            List<String> values = parseCsvLine(lines.get(index));
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                row.put(headers.get(column), column < values.size() ? values.get(column) : null);
            }
            rows.add(row);
        }
        return rows;
    }

    static BigDecimal parseDecimal(String value,
                                   int rowNumber,
                                   String field,
                                   List<OperationSupportModels.ReagentStockImportError> errors,
                                   java.util.function.Function<String, String> blankToNull,
                                   java.util.function.Function<ImportErrorArgs, OperationSupportModels.ReagentStockImportError> importError) {
        String normalized = blankToNull.apply(value);
        if (normalized == null) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            errors.add(importError.apply(new ImportErrorArgs(rowNumber, field, value, field + " must be a valid decimal number")));
            return null;
        }
    }

    static Integer parseInteger(String value,
                                Integer defaultValue,
                                int rowNumber,
                                String field,
                                List<OperationSupportModels.ReagentStockImportError> errors,
                                java.util.function.Function<String, String> blankToNull,
                                java.util.function.Function<ImportErrorArgs, OperationSupportModels.ReagentStockImportError> importError) {
        String normalized = blankToNull.apply(value);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            errors.add(importError.apply(new ImportErrorArgs(rowNumber, field, value, field + " must be a valid integer")));
            return null;
        }
    }

    static String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }

    static OperationSupportModels.ReagentView toReagentView(OperationSupportRepository.Reagent item,
                                                            java.util.function.Function<LocalDateTime, String> stringify) {
        return new OperationSupportModels.ReagentView(
            item.id(),
            item.reagentCode(),
            item.reagentName(),
            item.specification(),
            item.unit(),
            item.manufacturer(),
            item.reagentType(),
            item.reagentUsage(),
            item.orderDictItemId(),
            item.orderItemName(),
            item.cloneNo(),
            item.recommendedDilution(),
            item.applicationDilution(),
            item.templateStatus(),
            item.validityDays(),
            item.defaultLowStockThreshold(),
            item.defaultStockThreshold(),
            item.defaultNearExpiryDays(),
            item.stainCapacity(),
            item.stainThreshold(),
            item.enabled(),
            stringify.apply(item.createdAt()),
            stringify.apply(item.updatedAt()),
            item.createdByName(),
            item.updatedByName(),
            item.remarks());
    }

    static OperationSupportModels.ReagentStockView toReagentStockView(OperationSupportRepository.ReagentStock item,
                                                                      java.util.function.Function<LocalDateTime, String> stringify) {
        return new OperationSupportModels.ReagentStockView(
            item.id(),
            item.reagentId(),
            item.reagentCode(),
            item.reagentName(),
            item.reagentType(),
            item.orderDictItemId(),
            item.orderItemName(),
            item.batchNo(),
            item.initialQuantity(),
            item.stockQuantity(),
            item.remainingQuantity(),
            item.stockStatus(),
            item.productionDate(),
            stringify.apply(item.inboundAt()),
            item.expiryDate(),
            item.storageLocation(),
            item.lowStockThreshold(),
            item.nearExpiryDays(),
            item.testReminderThreshold(),
            item.expiryReminderThreshold(),
            item.recommendedDilution(),
            item.applicationDilution(),
            item.stainCapacity(),
            item.stainThreshold(),
            item.validityDays(),
            stringify.apply(item.testedAt()),
            stringify.apply(item.startedAt()),
            stringify.apply(item.finishedAt()),
            stringify.apply(item.createdAt()),
            stringify.apply(item.updatedAt()),
            item.createdByName(),
            item.updatedByName(),
            item.remarks());
    }

    static OperationSupportModels.EquipmentRecordView toEquipmentRecordView(
        OperationSupportRepository.EquipmentRecord item,
        java.util.function.Function<LocalDateTime, String> stringify,
        java.util.function.Function<LocalDate, String> stringifyDate
    ) {
        return new OperationSupportModels.EquipmentRecordView(
            item.id(),
            item.equipmentCode(),
            item.equipmentName(),
            item.equipmentCategory(),
            item.modelNo(),
            item.equipmentStatus(),
            item.locationDescription(),
            stringify.apply(item.enabledAt()),
            stringify.apply(item.nextMaintenanceAt()),
            item.quantity(),
            stringifyDate.apply(item.purchaseDate()),
            item.purchaserName(),
            item.purchaserCode(),
            item.managementUnit(),
            item.managementCode(),
            item.useUnit(),
            item.principalCode(),
            item.principalName(),
            item.userName(),
            stringifyDate.apply(item.productionDate()),
            stringifyDate.apply(item.warrantyEndDate()),
            item.factoryNo(),
            item.depreciationMethod(),
            item.serviceLifeYears(),
            item.price(),
            item.manufacturer(),
            item.portNo(),
            item.ipAddress(),
            item.commonStartupTime(),
            item.commonShutdownTime(),
            item.commonUsageContent(),
            item.commonlyUsed(),
            item.setTemperature(),
            item.currentTemperature(),
            item.rfid(),
            item.remarks());
    }

    static OperationSupportModels.EquipmentCommonDeviceView toEquipmentCommonDeviceView(
        OperationSupportRepository.EquipmentRecord item
    ) {
        return new OperationSupportModels.EquipmentCommonDeviceView(
            item.id(),
            item.equipmentCode(),
            item.equipmentName(),
            item.equipmentCategory(),
            item.equipmentStatus(),
            item.locationDescription());
    }

    static OperationSupportModels.EquipmentUsageRecordView toEquipmentUsageRecordView(
        OperationSupportRepository.EquipmentUsageRecord item,
        java.util.function.Function<LocalDateTime, String> stringify
    ) {
        return new OperationSupportModels.EquipmentUsageRecordView(
            item.id(),
            item.equipmentId(),
            item.equipmentCategorySnapshot(),
            item.equipmentNameSnapshot(),
            item.commonlyUsed(),
            stringify.apply(item.startedAt()),
            stringify.apply(item.endedAt()),
            item.runtimeHours(),
            item.diagnosisCount(),
            item.equipmentCondition(),
            item.operatorName(),
            item.usageContent(),
            item.remarks());
    }

    static OperationSupportModels.WhiteSlideStockView toWhiteSlideStockView(OperationSupportRepository.WhiteSlideStock item) {
        return new OperationSupportModels.WhiteSlideStockView(
            item.id(),
            item.stockNo(),
            item.stockCode(),
            item.specification(),
            item.quantityAvailable(),
            item.quantityBorrowed(),
            item.status(),
            item.remarks());
    }

    static OperationSupportModels.WhiteSlideLoanView toWhiteSlideLoanView(OperationSupportRepository.WhiteSlideLoan item,
                                                                          java.util.function.Function<LocalDateTime, String> stringify) {
        return new OperationSupportModels.WhiteSlideLoanView(
            item.id(),
            item.loanNo(),
            item.stockId(),
            item.stockNo(),
            item.stockCode(),
            item.quantity(),
            item.caseId(),
            item.pathologyNo(),
            item.patientName(),
            item.embeddingBoxNo(),
            item.slicePurpose(),
            item.sliceThickness(),
            item.borrowerName(),
            item.borrowerIdentityNo(),
            item.borrowerUnit(),
            item.borrowerPhone(),
            item.unitPrice(),
            item.amount(),
            item.saveDirectPrint(),
            item.loanStatus(),
            item.waxBlockUsage(),
            item.operatorName(),
            stringify.apply(item.loanedAt()),
            stringify.apply(item.returnedAt()),
            item.returnedByName(),
            item.remarks());
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean quoted = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return quoted ? "\"" + escaped + "\"" : escaped;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    record ImportErrorArgs(int rowNumber, String field, String rejectedValue, String message) {
    }
}
