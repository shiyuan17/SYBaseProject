package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.MedicalOrderJdbcRepository;
import com.company.bl.masterdata.infrastructure.MedicalOrderPageJdbcRepository;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final MedicalOrderChargeService medicalOrderChargeService;
    private final MedicalOrderPackageService medicalOrderPackageService;

    public MedicalOrderService(MedicalOrderJdbcRepository repository,
                               MedicalOrderPageJdbcRepository pageRepository,
                               NumberingService numberingService,
                               OperationAuditService operationAuditService,
                               MedicalOrderChargeService medicalOrderChargeService,
                               MedicalOrderPackageService medicalOrderPackageService) {
        this.repository = repository;
        this.pageRepository = pageRepository;
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
        this.medicalOrderChargeService = medicalOrderChargeService;
        this.medicalOrderPackageService = medicalOrderPackageService;
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
        String categoryCode = resolveCreateCode(command.categoryCode(), numberingService::generateOrderCategoryCode);
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
    public MedicalOrderCategoryNode updateMedicalOrderCategory(String id, UpdateMedicalOrderCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_CATEGORY", "update_order_category", () -> {
            MedicalOrderJdbcRepository.OrderCategoryRow current = repository.findOrderCategoryById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order category not found");
            }
            String categoryCode = resolveExistingCode(command.categoryCode(), current.categoryCode(), "Medical order category code");
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
        String orderItemCode = resolveCreateCode(command.orderItemCode(), numberingService::generateOrderItemCode);
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
    public MedicalOrderItemView updateMedicalOrderItem(String id, UpdateMedicalOrderItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_ITEM", "update_order_item", () -> {
            MedicalOrderJdbcRepository.OrderItemRow current = repository.findOrderItemById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order item not found");
            }
            String orderItemCode = resolveExistingCode(command.orderItemCode(), current.orderItemCode(), "Medical order item code");
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
        return medicalOrderChargeService.listChargeItems();
    }

    @Transactional(readOnly = true)
    public PagedResult<ChargeItemView> listChargeItemsPage(int page, int size, Boolean enabled, String keyword, String orderDictItemId) {
        return medicalOrderChargeService.listChargeItemsPage(page, size, enabled, keyword, orderDictItemId);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView createChargeItem(CreateChargeItemCommand command) {
        return medicalOrderChargeService.createChargeItem(command);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView updateChargeItem(String id, UpdateChargeItemCommand command) {
        return medicalOrderChargeService.updateChargeItem(id, command);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public ChargeItemView updateChargeItemEnabled(String id, boolean enabled) {
        return medicalOrderChargeService.updateChargeItemEnabled(id, enabled);
    }

    @CacheEvict(value = "medicalOrderChargeItems", allEntries = true)
    @Transactional
    public void deleteChargeItem(String id) {
        medicalOrderChargeService.deleteChargeItem(id);
    }

    @Transactional(readOnly = true)
    public byte[] exportChargeItems(Boolean enabled, String keyword, String orderDictItemId) {
        return medicalOrderChargeService.exportChargeItems(enabled, keyword, orderDictItemId);
    }

    @Transactional
    public ImportResult importChargeItems(byte[] content) {
        return medicalOrderChargeService.importChargeItems(content);
    }

    @Transactional(readOnly = true)
    public List<PackageView> listPackages() {
        return medicalOrderPackageService.listPackages();
    }

    @Transactional(readOnly = true)
    public PagedResult<PackageView> listPackagesPage(int page, int size, Boolean enabled, String keyword, String packageType) {
        return medicalOrderPackageService.listPackagesPage(page, size, enabled, keyword, packageType);
    }

    @Transactional
    public PackageView createPackage(CreatePackageCommand command) {
        return medicalOrderPackageService.createPackage(command);
    }

    @Transactional
    public PackageView updatePackage(String id, UpdatePackageCommand command) {
        return medicalOrderPackageService.updatePackage(id, command);
    }

    @Transactional
    public PackageView updatePackageEnabled(String id, boolean enabled) {
        return medicalOrderPackageService.updatePackageEnabled(id, enabled);
    }

    @Transactional
    public void deletePackage(String id) {
        medicalOrderPackageService.deletePackage(id);
    }

    private MedicalOrderCategoryNode toCategoryNode(MedicalOrderJdbcRepository.OrderCategoryRow row) {
        return new MedicalOrderCategoryNode(
            row.id(),
            row.parentId(),
            row.categoryCode(),
            row.categoryName(),
            row.sortOrder(),
            row.enabled(),
            new ArrayList<>(),
            new ArrayList<>());
    }

    private MedicalOrderItemView toItemView(MedicalOrderJdbcRepository.OrderItemRow row) {
        return new MedicalOrderItemView(
            row.id(),
            row.categoryId(),
            row.orderItemCode(),
            row.orderItemName(),
            row.orderType(),
            row.defaultContent(),
            row.executionScope(),
            row.sortOrder(),
            row.enabled());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    @Schema(name = "MedicalOrderCategoryNode")
    public record MedicalOrderCategoryNode(
        String id,
        String parentId,
        String categoryCode,
        String categoryName,
        int sortOrder,
        boolean enabled,
        List<MedicalOrderCategoryNode> children,
        List<MedicalOrderItemView> items
    ) {
    }

    @Schema(name = "MedicalOrderItemView")
    public record MedicalOrderItemView(
        String id,
        String categoryId,
        String orderItemCode,
        String orderItemName,
        String orderType,
        String defaultContent,
        String executionScope,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record CreateMedicalOrderCategoryCommand(String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {
    }

    public record UpdateMedicalOrderCategoryCommand(String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {
    }

    public record CreateMedicalOrderItemCommand(
        String categoryId,
        String orderItemCode,
        String orderItemName,
        String orderType,
        String defaultContent,
        String executionScope,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record UpdateMedicalOrderItemCommand(
        String categoryId,
        String orderItemCode,
        String orderItemName,
        String orderType,
        String defaultContent,
        String executionScope,
        int sortOrder,
        boolean enabled
    ) {
    }

    @Schema(name = "ChargeItemView")
    public record ChargeItemView(
        String id,
        String orderDictItemId,
        String orderItemName,
        String chargeItemCode,
        String chargeItemName,
        String specification,
        String unit,
        BigDecimal price,
        int sortOrder,
        boolean enabled
    ) {
    }

    @Schema(name = "MedicalOrderPagedResult")
    public record PagedResult<T>(List<T> items, int page, int size, long total) {
    }

    public record CreateChargeItemCommand(
        String orderDictItemId,
        String chargeItemCode,
        String chargeItemName,
        String specification,
        String unit,
        BigDecimal price,
        int sortOrder,
        boolean enabled
    ) {
    }

    public record UpdateChargeItemCommand(
        String orderDictItemId,
        String chargeItemCode,
        String chargeItemName,
        String specification,
        String unit,
        BigDecimal price,
        int sortOrder,
        boolean enabled
    ) {
    }

    @Schema(name = "PackageView")
    public record PackageView(
        String id,
        String packageCode,
        String packageName,
        String packageType,
        String ownerUserId,
        boolean enabled,
        String remarks,
        List<PackageItemView> items
    ) {
    }

    @Schema(name = "PackageItemView")
    public record PackageItemView(
        String id,
        String packageId,
        String orderItemId,
        String orderItemCode,
        String orderItemName,
        int sortOrder,
        String remarks
    ) {
    }

    public record CreatePackageCommand(
        String packageCode,
        String packageName,
        String packageType,
        String ownerUserId,
        boolean enabled,
        String remarks,
        List<String> itemIds
    ) {
    }

    public record UpdatePackageCommand(
        String packageCode,
        String packageName,
        String packageType,
        String ownerUserId,
        boolean enabled,
        String remarks,
        List<String> itemIds
    ) {
    }

    @Schema(name = "MedicalOrderImportResult")
    public record ImportResult(int successCount, int failureCount) {
    }
}
