package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
import com.company.bl.masterdata.infrastructure.MedicalOrderPageJdbcRepository;
import com.company.bl.masterdata.infrastructure.MedicalOrderJdbcRepository;
import com.company.bl.support.application.OperationAuditService;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class MedicalOrderService {

    private final MedicalOrderJdbcRepository repository;
    private final MedicalOrderPageJdbcRepository pageRepository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    public MedicalOrderService(MedicalOrderJdbcRepository repository,
                               MedicalOrderPageJdbcRepository pageRepository,
                               NumberingService numberingService,
                               OperationAuditService operationAuditService) {
        this.repository = repository;
        this.pageRepository = pageRepository;
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
    }

    @Cacheable("medicalOrderDictTree")
    @Transactional(readOnly = true)
    public List<MedicalOrderCategoryNode> listMedicalOrderDicts() {
        List<MedicalOrderJdbcRepository.OrderCategoryRow> categories = repository.findOrderCategories();
        List<MedicalOrderJdbcRepository.OrderItemRow> items = repository.findOrderItems();
        var nodes = categories.stream().map(this::toCategoryNode)
            .collect(Collectors.toMap(MedicalOrderCategoryNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        items.forEach(item -> {
            MedicalOrderCategoryNode parent = nodes.get(item.categoryId());
            if (parent != null) {
                parent.items().add(toItemView(item));
            }
        });
        List<MedicalOrderCategoryNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                MedicalOrderCategoryNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderCategoryNode createMedicalOrderCategory(CreateMedicalOrderCategoryCommand command) {
        String categoryCode = resolveCreateCode(
            command.categoryCode(),
            numberingService::generateOrderCategoryCode);
        return operationAuditService.audit("MASTERDATA", "ORDER_CATEGORY", "create_order_category", () -> {
            try {
                return toCategoryNode(repository.insertOrderCategory(new MedicalOrderJdbcRepository.CreateOrderCategoryRow(
                    "ODC-" + UUID.randomUUID(),
                    command.parentId(),
                    categoryCode,
                    command.categoryName(),
                    command.sortOrder(),
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Medical order category code already exists");
            }
        }, MedicalOrderCategoryNode::id, () -> categoryCode);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderCategoryNode updateMedicalOrderCategory(String id, MedicalOrderService.UpdateMedicalOrderCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_CATEGORY", "update_order_category", () -> {
            MedicalOrderJdbcRepository.OrderCategoryRow current = repository.findOrderCategoryById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order category not found");
            }
            String categoryCode = resolveExistingCode(
                command.categoryCode(),
                current.categoryCode(),
                "Medical order category code");
            try {
                repository.updateOrderCategory(id, new MedicalOrderJdbcRepository.UpdateOrderCategoryRow(
                    command.parentId(),
                    categoryCode,
                    command.categoryName(),
                    command.sortOrder(),
                    command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Medical order category code already exists");
            }
            return toCategoryNode(repository.findOrderCategoryById(id));
        }, MedicalOrderCategoryNode::id, () -> id);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderItemView createMedicalOrderItem(CreateMedicalOrderItemCommand command) {
        String orderItemCode = resolveCreateCode(
            command.orderItemCode(),
            numberingService::generateOrderItemCode);
        return operationAuditService.audit("MASTERDATA", "ORDER_ITEM", "create_order_item", () -> {
            try {
                return toItemView(repository.insertOrderItem(new MedicalOrderJdbcRepository.CreateOrderItemRow(
                    "ODI-" + UUID.randomUUID(),
                    command.categoryId(),
                    orderItemCode,
                    command.orderItemName(),
                    command.orderType(),
                    command.defaultContent(),
                    command.executionScope(),
                    command.sortOrder(),
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Medical order item code already exists");
            }
        }, MedicalOrderItemView::id, () -> orderItemCode);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderItemView updateMedicalOrderItem(String id, MedicalOrderService.UpdateMedicalOrderItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_ITEM", "update_order_item", () -> {
            MedicalOrderJdbcRepository.OrderItemRow current = repository.findOrderItemById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order item not found");
            }
            String orderItemCode = resolveExistingCode(
                command.orderItemCode(),
                current.orderItemCode(),
                "Medical order item code");
            try {
                repository.updateOrderItem(id, new MedicalOrderJdbcRepository.UpdateOrderItemRow(
                    command.categoryId(),
                    orderItemCode,
                    command.orderItemName(),
                    command.orderType(),
                    command.defaultContent(),
                    command.executionScope(),
                    command.sortOrder(),
                    command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Medical order item code already exists");
            }
            return toItemView(repository.findOrderItemById(id));
        }, MedicalOrderItemView::id, () -> id);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderItemView updateMedicalOrderItemEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "ORDER_ITEM", "update_order_item_enabled", () -> {
            if (repository.findOrderItemById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order item not found");
            }
            repository.updateOrderItemEnabled(id, enabled);
            return toItemView(repository.findOrderItemById(id));
        }, MedicalOrderItemView::id, () -> id);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public void deleteMedicalOrderCategory(String id) {
        operationAuditService.audit("MASTERDATA", "ORDER_CATEGORY", "delete_order_category", () -> {
            if (repository.findOrderCategoryById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order category not found");
            }
            if (repository.countOrderCategoryChildren(id) > 0 || repository.countOrderCategoryItems(id) > 0) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Medical order category still has children or items");
            }
            repository.deleteOrderCategory(id);
            return id;
        }, value -> id, () -> id);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public void deleteMedicalOrderItem(String id) {
        operationAuditService.audit("MASTERDATA", "ORDER_ITEM", "delete_order_item", () -> {
            if (repository.findOrderItemById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order item not found");
            }
            if (repository.countChargeItemsByOrderItem(id) > 0 || repository.countPackageItemsByOrderItem(id) > 0) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Medical order item is referenced by charges or packages");
            }
            repository.deleteOrderItem(id);
            return id;
        }, value -> id, () -> id);
    }

    @Cacheable("medicalOrderChargeItems")
    @Transactional(readOnly = true)
    public List<ChargeItemView> listChargeItems() {
        return repository.findChargeItems().stream().map(this::toChargeItemView).toList();
    }

    @Transactional(readOnly = true)
    public PagedResult<ChargeItemView> listChargeItemsPage(int page,
                                                           int size,
                                                           Boolean enabled,
                                                           String keyword,
                                                           String orderDictItemId) {
        MedicalOrderPageJdbcRepository.PagedChargeItems pagedItems =
            pageRepository.findChargeItemsPage(page, size, enabled, keyword, orderDictItemId);
        return new PagedResult<>(
            pagedItems.items().stream().map(this::toChargeItemView).toList(),
            page,
            size,
            pagedItems.total());
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView createChargeItem(CreateChargeItemCommand command) {
        String chargeItemCode = resolveCreateCode(
            command.chargeItemCode(),
            numberingService::generateChargeItemCode);
        return operationAuditService.audit("MASTERDATA", "ORDER_CHARGE", "create_charge_item", () -> {
            try {
                return toChargeItemView(repository.insertChargeItem(new MedicalOrderJdbcRepository.CreateChargeItemRow(
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
        }, ChargeItemView::id, () -> chargeItemCode);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView updateChargeItem(String id, MedicalOrderService.UpdateChargeItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_CHARGE", "update_charge_item", () -> {
            MedicalOrderJdbcRepository.ChargeItemRow current = repository.findChargeItemById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Charge item not found");
            }
            String chargeItemCode = resolveExistingCode(
                command.chargeItemCode(),
                current.chargeItemCode(),
                "Charge item code");
            try {
                repository.updateChargeItem(id, new MedicalOrderJdbcRepository.UpdateChargeItemRow(
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
        }, ChargeItemView::id, () -> id);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView updateChargeItemEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "ORDER_CHARGE", "update_charge_item_enabled", () -> {
            if (repository.findChargeItemById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Charge item not found");
            }
            repository.updateChargeItemEnabled(id, enabled);
            return toChargeItemView(repository.findChargeItemById(id));
        }, ChargeItemView::id, () -> id);
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
        List<ChargeItemView> items = listChargeItemsPage(1, Integer.MAX_VALUE, enabled, keyword, orderDictItemId).items();
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("orderDictItemId,chargeItemCode,chargeItemName,specification,unit,price,sortOrder,enabled\r\n");
        for (ChargeItemView item : items) {
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
                MedicalOrderJdbcRepository.ChargeItemRow existing = repository.findChargeItemByCode(chargeItemCode);
                BigDecimal price = parseDecimal(row.get("price"));
                int sortOrder = parseInteger(row.get("sortOrder"), 0);
                boolean enabled = parseBoolean(row.get("enabled"), true);
                if (existing == null) {
                    createChargeItem(new CreateChargeItemCommand(
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

    @Transactional(readOnly = true)
    public List<PackageView> listPackages() {
        var itemMap = repository.findPackageItems().stream().map(this::toPackageItemView)
            .collect(Collectors.groupingBy(PackageItemView::packageId, LinkedHashMap::new, Collectors.toList()));
        return repository.findPackages().stream().map(row -> new PackageView(
            row.id(), row.packageCode(), row.packageName(), row.packageType(), row.ownerUserId(),
            row.enabled(), row.remarks(), itemMap.getOrDefault(row.id(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public PagedResult<PackageView> listPackagesPage(int page,
                                                     int size,
                                                     Boolean enabled,
                                                     String keyword,
                                                     String packageType) {
        MedicalOrderPageJdbcRepository.PagedPackages pagedPackages =
            pageRepository.findPackagesPage(page, size, enabled, keyword, packageType);
        var packageIds = pagedPackages.items().stream().map(MedicalOrderPageJdbcRepository.PackageRow::id).toList();
        var itemMap = pageRepository.findPackageItemsByPackageIds(packageIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(this::toPackageItemView).toList(),
                (left, right) -> left,
                LinkedHashMap::new));
        return new PagedResult<>(
            pagedPackages.items().stream().map(row -> new PackageView(
                row.id(), row.packageCode(), row.packageName(), row.packageType(), row.ownerUserId(),
                row.enabled(), row.remarks(), itemMap.getOrDefault(row.id(), List.of()))).toList(),
            page,
            size,
            pagedPackages.total());
    }

    @Transactional
    public PackageView createPackage(CreatePackageCommand command) {
        String packageCode = resolveCreateCode(command.packageCode(), numberingService::generatePackageCode);
        return operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "create_package", () -> {
            try {
                repository.insertPackage(new MedicalOrderJdbcRepository.CreatePackageRow(
                    "PKG-" + UUID.randomUUID(),
                    packageCode,
                    command.packageName(),
                    command.packageType(),
                    command.ownerUserId(),
                    command.enabled(),
                    command.remarks(),
                    command.itemIds(),
                    LocalDateTime.now(),
                    LocalDateTime.now()));
                return listPackages().stream().filter(item -> item.packageCode().equals(packageCode)).findFirst()
                    .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found after create"));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Package code already exists");
            }
        }, PackageView::id, () -> packageCode);
    }

    @Transactional
    public PackageView updatePackage(String id, MedicalOrderService.UpdatePackageCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "update_package", () -> {
            MedicalOrderJdbcRepository.PackageRow current = repository.findPackageById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found");
            }
            String packageCode = resolveExistingCode(command.packageCode(), current.packageCode(), "Package code");
            try {
                repository.updatePackage(id, new MedicalOrderJdbcRepository.UpdatePackageRow(
                    packageCode,
                    command.packageName(),
                    command.packageType(),
                    command.ownerUserId(),
                    command.enabled(),
                    command.remarks(),
                    command.itemIds()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Package code already exists");
            }
            return listPackages().stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found"));
        }, PackageView::id, () -> id);
    }

    @Transactional
    public PackageView updatePackageEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "update_package_enabled", () -> {
            if (repository.findPackageById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found");
            }
            repository.updatePackageEnabled(id, enabled);
            return listPackages().stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found"));
        }, PackageView::id, () -> id);
    }

    @Transactional
    public void deletePackage(String id) {
        operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "delete_package", () -> {
            if (repository.findPackageById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found");
            }
            repository.deletePackage(id);
            return id;
        }, value -> id, () -> id);
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

    private MedicalOrderCategoryNode toCategoryNode(MedicalOrderJdbcRepository.OrderCategoryRow row) {
        return new MedicalOrderCategoryNode(row.id(), row.parentId(), row.categoryCode(), row.categoryName(),
            row.sortOrder(), row.enabled(), new ArrayList<>(), new ArrayList<>());
    }

    private MedicalOrderItemView toItemView(MedicalOrderJdbcRepository.OrderItemRow row) {
        return new MedicalOrderItemView(row.id(), row.categoryId(), row.orderItemCode(), row.orderItemName(),
            row.orderType(), row.defaultContent(), row.executionScope(), row.sortOrder(), row.enabled());
    }

    private ChargeItemView toChargeItemView(MedicalOrderJdbcRepository.ChargeItemRow row) {
        return new ChargeItemView(row.id(), row.orderDictItemId(), row.orderItemName(), row.chargeItemCode(),
            row.chargeItemName(), row.specification(), row.unit(), row.price(), row.sortOrder(), row.enabled());
    }

    private PackageItemView toPackageItemView(MedicalOrderJdbcRepository.PackageItemRow row) {
        return new PackageItemView(row.id(), row.packageId(), row.orderItemId(), row.orderItemCode(),
            row.orderItemName(), row.sortOrder(), row.remarks());
    }

    private ChargeItemView toChargeItemView(MedicalOrderPageJdbcRepository.ChargeItemRow row) {
        return new ChargeItemView(row.id(), row.orderDictItemId(), row.orderItemName(), row.chargeItemCode(),
            row.chargeItemName(), row.specification(), row.unit(), row.price(), row.sortOrder(), row.enabled());
    }

    private PackageItemView toPackageItemView(MedicalOrderPageJdbcRepository.PackageItemRow row) {
        return new PackageItemView(row.id(), row.packageId(), row.orderItemId(), row.orderItemCode(),
            row.orderItemName(), row.sortOrder(), row.remarks());
    }

    @Schema(name = "MedicalOrderCategoryNode", description = "医嘱字典分类树节点")
    public record MedicalOrderCategoryNode(
        @Schema(description = "分类 ID") String id,
        @Schema(description = "父级分类 ID") String parentId,
        @Schema(description = "分类编码") String categoryCode,
        @Schema(description = "分类名称") String categoryName,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "子分类列表") List<MedicalOrderCategoryNode> children,
        @Schema(description = "分类下的医嘱条目") List<MedicalOrderItemView> items) {}
    @Schema(name = "MedicalOrderItemView", description = "医嘱字典条目")
    public record MedicalOrderItemView(
        @Schema(description = "条目 ID") String id,
        @Schema(description = "所属分类 ID") String categoryId,
        @Schema(description = "医嘱条目编码") String orderItemCode,
        @Schema(description = "医嘱条目名称") String orderItemName,
        @Schema(description = "医嘱类型") String orderType,
        @Schema(description = "默认内容") String defaultContent,
        @Schema(description = "执行范围") String executionScope,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled) {}
    public record CreateMedicalOrderCategoryCommand(String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {}
    public record UpdateMedicalOrderCategoryCommand(String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {}
    public record CreateMedicalOrderItemCommand(String categoryId, String orderItemCode, String orderItemName,
                                                String orderType, String defaultContent, String executionScope, int sortOrder, boolean enabled) {}
    public record UpdateMedicalOrderItemCommand(String categoryId, String orderItemCode, String orderItemName,
                                                String orderType, String defaultContent, String executionScope, int sortOrder, boolean enabled) {}
    @Schema(name = "ChargeItemView", description = "收费项目")
    public record ChargeItemView(
        @Schema(description = "收费项目 ID") String id,
        @Schema(description = "关联医嘱条目 ID") String orderDictItemId,
        @Schema(description = "医嘱条目名称") String orderItemName,
        @Schema(description = "收费项目编码") String chargeItemCode,
        @Schema(description = "收费项目名称") String chargeItemName,
        @Schema(description = "规格") String specification,
        @Schema(description = "计量单位") String unit,
        @Schema(description = "价格") BigDecimal price,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled) {}
    @Schema(name = "MedicalOrderPagedResult", description = "医嘱模块分页结果")
    public record PagedResult<T>(
        @Schema(description = "当前页数据") List<T> items,
        @Schema(description = "页码，从 1 开始") int page,
        @Schema(description = "每页条数") int size,
        @Schema(description = "总记录数") long total) {}
    public record CreateChargeItemCommand(String orderDictItemId, String chargeItemCode, String chargeItemName,
                                          String specification, String unit, BigDecimal price, int sortOrder, boolean enabled) {}
    public record UpdateChargeItemCommand(String orderDictItemId, String chargeItemCode, String chargeItemName,
                                          String specification, String unit, BigDecimal price, int sortOrder, boolean enabled) {}
    @Schema(name = "PackageView", description = "医嘱套餐")
    public record PackageView(
        @Schema(description = "套餐 ID") String id,
        @Schema(description = "套餐编码") String packageCode,
        @Schema(description = "套餐名称") String packageName,
        @Schema(description = "套餐类型") String packageType,
        @Schema(description = "负责人用户 ID") String ownerUserId,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "备注") String remarks,
        @Schema(description = "套餐明细") List<PackageItemView> items) {}
    @Schema(name = "PackageItemView", description = "套餐条目")
    public record PackageItemView(
        @Schema(description = "条目 ID") String id,
        @Schema(description = "套餐 ID") String packageId,
        @Schema(description = "医嘱条目 ID") String orderItemId,
        @Schema(description = "医嘱条目编码") String orderItemCode,
        @Schema(description = "医嘱条目名称") String orderItemName,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "备注") String remarks) {}
    public record CreatePackageCommand(String packageCode, String packageName, String packageType,
                                       String ownerUserId, boolean enabled, String remarks, List<String> itemIds) {}
    public record UpdatePackageCommand(String packageCode, String packageName, String packageType,
                                       String ownerUserId, boolean enabled, String remarks, List<String> itemIds) {}

    @Schema(name = "MedicalOrderImportResult", description = "导入结果")
    public record ImportResult(
        @Schema(description = "成功数量") int successCount,
        @Schema(description = "失败数量") int failureCount) {}

    private String resolveCreateCode(String requestedCode, Supplier<String> generator) {
        String normalizedCode = trimToNull(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    private String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = trimToNull(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(
            BlErrorCode.INVALID_ARGUMENT,
            400,
            fieldLabel + " cannot be changed once created");
    }
}
