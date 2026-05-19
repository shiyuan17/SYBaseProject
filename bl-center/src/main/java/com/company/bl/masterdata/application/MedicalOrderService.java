package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.MedicalOrderPageJdbcRepository;
import com.company.bl.masterdata.infrastructure.MedicalOrderJdbcRepository;
import com.company.bl.support.application.OperationAuditService;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MedicalOrderService {

    private final MedicalOrderJdbcRepository repository;
    private final MedicalOrderPageJdbcRepository pageRepository;
    private final OperationAuditService operationAuditService;

    public MedicalOrderService(MedicalOrderJdbcRepository repository,
                               MedicalOrderPageJdbcRepository pageRepository,
                               OperationAuditService operationAuditService) {
        this.repository = repository;
        this.pageRepository = pageRepository;
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
        return operationAuditService.audit("MASTERDATA", "ORDER_CATEGORY", "create_order_category", () -> {
            try {
                return toCategoryNode(repository.insertOrderCategory(new MedicalOrderJdbcRepository.CreateOrderCategoryRow(
                    "ODC-" + UUID.randomUUID(),
                    command.parentId(),
                    command.categoryCode(),
                    command.categoryName(),
                    command.sortOrder(),
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Medical order category code already exists");
            }
        }, MedicalOrderCategoryNode::id, command::categoryCode);
    }

    @CacheEvict(value = "medicalOrderDictTree", allEntries = true)
    @Transactional
    public MedicalOrderItemView createMedicalOrderItem(CreateMedicalOrderItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "ORDER_ITEM", "create_order_item", () -> {
            try {
                return toItemView(repository.insertOrderItem(new MedicalOrderJdbcRepository.CreateOrderItemRow(
                    "ODI-" + UUID.randomUUID(),
                    command.categoryId(),
                    command.orderItemCode(),
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
        }, MedicalOrderItemView::id, command::orderItemCode);
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
        return operationAuditService.audit("MASTERDATA", "ORDER_CHARGE", "create_charge_item", () -> {
            try {
                return toChargeItemView(repository.insertChargeItem(new MedicalOrderJdbcRepository.CreateChargeItemRow(
                    "OCI-" + UUID.randomUUID(),
                    command.orderDictItemId(),
                    command.chargeItemCode(),
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
        }, ChargeItemView::id, command::chargeItemCode);
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
        return operationAuditService.audit("MASTERDATA", "ORDER_PACKAGE", "create_package", () -> {
            try {
                repository.insertPackage(new MedicalOrderJdbcRepository.CreatePackageRow(
                    "PKG-" + UUID.randomUUID(),
                    command.packageCode(),
                    command.packageName(),
                    command.packageType(),
                    command.ownerUserId(),
                    command.enabled(),
                    command.remarks(),
                    command.itemIds(),
                    LocalDateTime.now(),
                    LocalDateTime.now()));
                return listPackages().stream().filter(item -> item.packageCode().equals(command.packageCode())).findFirst()
                    .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Package not found after create"));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Package code already exists");
            }
        }, PackageView::id, command::packageCode);
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

    public record MedicalOrderCategoryNode(String id, String parentId, String categoryCode, String categoryName,
                                           int sortOrder, boolean enabled, List<MedicalOrderCategoryNode> children, List<MedicalOrderItemView> items) {}
    public record MedicalOrderItemView(String id, String categoryId, String orderItemCode, String orderItemName,
                                       String orderType, String defaultContent, String executionScope, int sortOrder, boolean enabled) {}
    public record CreateMedicalOrderCategoryCommand(String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {}
    public record CreateMedicalOrderItemCommand(String categoryId, String orderItemCode, String orderItemName,
                                                String orderType, String defaultContent, String executionScope, int sortOrder, boolean enabled) {}
    public record ChargeItemView(String id, String orderDictItemId, String orderItemName, String chargeItemCode,
                                 String chargeItemName, String specification, String unit, BigDecimal price, int sortOrder, boolean enabled) {}
    public record PagedResult<T>(List<T> items, int page, int size, long total) {}
    public record CreateChargeItemCommand(String orderDictItemId, String chargeItemCode, String chargeItemName,
                                          String specification, String unit, BigDecimal price, int sortOrder, boolean enabled) {}
    public record PackageView(String id, String packageCode, String packageName, String packageType,
                              String ownerUserId, boolean enabled, String remarks, List<PackageItemView> items) {}
    public record PackageItemView(String id, String packageId, String orderItemId, String orderItemCode, String orderItemName, int sortOrder, String remarks) {}
    public record CreatePackageCommand(String packageCode, String packageName, String packageType,
                                       String ownerUserId, boolean enabled, String remarks, List<String> itemIds) {}
}
