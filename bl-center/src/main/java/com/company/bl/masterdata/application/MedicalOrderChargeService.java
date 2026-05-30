package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.MedicalOrderChargeJdbcRepository;
import com.company.bl.masterdata.infrastructure.MedicalOrderPageJdbcRepository;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class MedicalOrderChargeService {

    private final MedicalOrderChargeJdbcRepository repository;
    private final MedicalOrderPageJdbcRepository pageRepository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    public MedicalOrderChargeService(MedicalOrderChargeJdbcRepository repository,
                                     MedicalOrderPageJdbcRepository pageRepository,
                                     NumberingService numberingService,
                                     OperationAuditService operationAuditService) {
        this.repository = repository;
        this.pageRepository = pageRepository;
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
    }

    @Cacheable("medicalOrderChargeItems")
    @Transactional(readOnly = true)
    public List<MedicalOrderService.ChargeItemView> listChargeItems() {
        return repository.findChargeItems().stream().map(this::toChargeItemView).toList();
    }

    @Transactional(readOnly = true)
    public MedicalOrderService.PagedResult<MedicalOrderService.ChargeItemView> listChargeItemsPage(
        int page,
        int size,
        Boolean enabled,
        String keyword,
        String orderDictItemId
    ) {
        MedicalOrderPageJdbcRepository.PagedChargeItems pagedItems =
            pageRepository.findChargeItemsPage(page, size, enabled, keyword, orderDictItemId);
        return new MedicalOrderService.PagedResult<>(
            pagedItems.items().stream().map(this::toChargeItemView).toList(),
            page,
            size,
            pagedItems.total());
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public MedicalOrderService.ChargeItemView createChargeItem(MedicalOrderService.CreateChargeItemCommand command) {
        String chargeItemCode = resolveCreateCode(command.chargeItemCode(), numberingService::generateChargeItemCode);
        return operationAuditService.audit("MASTERDATA", "ORDER_CHARGE", "create_charge_item", () -> {
            try {
                return toChargeItemView(repository.insertChargeItem(new MedicalOrderChargeJdbcRepository.CreateChargeItemRow(
                    "OCI-" + UUID.randomUUID(),
                    command.orderDictItemId(),
                    chargeItemCode,
                    command.chargeItemName(),
                    command.specification(),
                    command.unit(),
                    command.price(),
                    command.sortOrder(),
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Charge item code already exists");
            }
        }, MedicalOrderService.ChargeItemView::id, () -> chargeItemCode);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public MedicalOrderService.ChargeItemView updateChargeItem(String id, MedicalOrderService.UpdateChargeItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_CHARGE", "update_charge_item", () -> {
            MedicalOrderChargeJdbcRepository.ChargeItemRow current = repository.findChargeItemById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Charge item not found");
            }
            String chargeItemCode = resolveExistingCode(command.chargeItemCode(), current.chargeItemCode(), "Charge item code");
            try {
                repository.updateChargeItem(id, new MedicalOrderChargeJdbcRepository.UpdateChargeItemRow(
                    command.orderDictItemId(),
                    chargeItemCode,
                    command.chargeItemName(),
                    command.specification(),
                    command.unit(),
                    command.price(),
                    command.sortOrder(),
                    command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Charge item code already exists");
            }
            return toChargeItemView(repository.findChargeItemById(id));
        }, MedicalOrderService.ChargeItemView::id, () -> id);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public MedicalOrderService.ChargeItemView updateChargeItemEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "ORDER_CHARGE", "update_charge_item_enabled", () -> {
            if (repository.findChargeItemById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Charge item not found");
            }
            repository.updateChargeItemEnabled(id, enabled);
            return toChargeItemView(repository.findChargeItemById(id));
        }, MedicalOrderService.ChargeItemView::id, () -> id);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public void deleteChargeItem(String id) {
        operationAuditService.audit("MASTERDATA", "ORDER_CHARGE", "delete_charge_item", () -> {
            if (repository.findChargeItemById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Charge item not found");
            }
            repository.deleteChargeItem(id);
            return id;
        }, value -> id, () -> id);
    }

    @Transactional(readOnly = true)
    public byte[] exportChargeItems(Boolean enabled, String keyword, String orderDictItemId) {
        List<MedicalOrderService.ChargeItemView> items = listChargeItemsPage(1, Integer.MAX_VALUE, enabled, keyword, orderDictItemId).items();
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("orderDictItemId,chargeItemCode,chargeItemName,specification,unit,price,sortOrder,enabled\r\n");
        for (MedicalOrderService.ChargeItemView item : items) {
            appendCsvRow(builder, List.of(
                safe(item.orderDictItemId()),
                safe(item.chargeItemCode()),
                safe(item.chargeItemName()),
                safe(item.specification()),
                safe(item.unit()),
                item.price() == null ? "" : item.price().toPlainString(),
                String.valueOf(item.sortOrder()),
                item.enabled() ? "true" : "false"));
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public MedicalOrderService.ImportResult importChargeItems(byte[] content) {
        List<Map<String, String>> rows = parseCsv(content);
        int successCount = 0;
        int failureCount = 0;
        for (Map<String, String> row : rows) {
            String chargeItemCode = trimToNull(row.get("chargeItemCode"));
            String chargeItemName = trimToNull(row.get("chargeItemName"));
            String orderDictItemId = trimToNull(row.get("orderDictItemId"));
            if (chargeItemName == null || orderDictItemId == null) {
                failureCount++;
                continue;
            }
            try {
                MedicalOrderChargeJdbcRepository.ChargeItemRow existing = repository.findChargeItemByCode(chargeItemCode);
                BigDecimal price = parseDecimal(row.get("price"));
                int sortOrder = parseInteger(row.get("sortOrder"), 0);
                boolean enabled = parseBoolean(row.get("enabled"), true);
                if (existing == null) {
                    createChargeItem(new MedicalOrderService.CreateChargeItemCommand(
                        orderDictItemId,
                        chargeItemCode,
                        chargeItemName,
                        trimToNull(row.get("specification")),
                        trimToNull(row.get("unit")),
                        price,
                        sortOrder,
                        enabled));
                } else {
                    updateChargeItem(existing.id(), new MedicalOrderService.UpdateChargeItemCommand(
                        orderDictItemId,
                        chargeItemCode,
                        chargeItemName,
                        trimToNull(row.get("specification")),
                        trimToNull(row.get("unit")),
                        price,
                        sortOrder,
                        enabled));
                }
                successCount++;
            } catch (RuntimeException exception) {
                failureCount++;
            }
        }
        return new MedicalOrderService.ImportResult(successCount, failureCount);
    }

    private MedicalOrderService.ChargeItemView toChargeItemView(MedicalOrderChargeJdbcRepository.ChargeItemRow row) {
        return new MedicalOrderService.ChargeItemView(
            row.id(),
            row.orderDictItemId(),
            row.orderItemName(),
            row.chargeItemCode(),
            row.chargeItemName(),
            row.specification(),
            row.unit(),
            row.price(),
            row.sortOrder(),
            row.enabled());
    }

    private MedicalOrderService.ChargeItemView toChargeItemView(MedicalOrderPageJdbcRepository.ChargeItemRow row) {
        return new MedicalOrderService.ChargeItemView(
            row.id(),
            row.orderDictItemId(),
            row.orderItemName(),
            row.chargeItemCode(),
            row.chargeItemName(),
            row.specification(),
            row.unit(),
            row.price(),
            row.sortOrder(),
            row.enabled());
    }

    private void appendCsvRow(StringBuilder builder, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(escapeCsv(values.get(index)));
        }
        builder.append("\r\n");
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean quoted = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return quoted ? "\"" + escaped + "\"" : escaped;
    }

    private List<Map<String, String>> parseCsv(byte[] content) {
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

    private List<String> parseCsvLine(String line) {
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        return "1".equals(normalized) || "true".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized);
    }

    private int parseInteger(String value, int defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        return Integer.parseInt(normalized);
    }

    private BigDecimal parseDecimal(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : new BigDecimal(normalized);
    }

    private String resolveCreateCode(String requestedCode, Supplier<String> generator) {
        String normalizedCode = trimToNull(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    private String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = trimToNull(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, fieldLabel + " cannot be changed once created");
    }
}
