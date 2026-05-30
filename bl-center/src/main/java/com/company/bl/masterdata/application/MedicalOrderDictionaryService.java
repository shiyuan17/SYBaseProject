package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.MedicalOrderJdbcRepository;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class MedicalOrderDictionaryService {

    private final MedicalOrderJdbcRepository repository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    public MedicalOrderDictionaryService(MedicalOrderJdbcRepository repository,
                                         NumberingService numberingService,
                                         OperationAuditService operationAuditService) {
        this.repository = repository;
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
    }

    @Transactional(readOnly = true)
    public List<MedicalOrderService.MedicalOrderCategoryNode> listMedicalOrderDicts() {
        List<MedicalOrderJdbcRepository.OrderCategoryRow> categories = repository.findOrderCategories();
        List<MedicalOrderJdbcRepository.OrderItemRow> items = repository.findOrderItems();
        var nodes = categories.stream().map(this::toCategoryNode)
            .collect(Collectors.toMap(MedicalOrderService.MedicalOrderCategoryNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        items.forEach(item -> {
            MedicalOrderService.MedicalOrderCategoryNode parent = nodes.get(item.categoryId());
            if (parent != null) {
                parent.items().add(toItemView(item));
            }
        });
        List<MedicalOrderService.MedicalOrderCategoryNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                MedicalOrderService.MedicalOrderCategoryNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    @Transactional
    public MedicalOrderService.MedicalOrderCategoryNode createMedicalOrderCategory(MedicalOrderService.CreateMedicalOrderCategoryCommand command) {
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
        }, MedicalOrderService.MedicalOrderCategoryNode::id, () -> categoryCode);
    }

    @Transactional
    public MedicalOrderService.MedicalOrderCategoryNode updateMedicalOrderCategory(String id, MedicalOrderService.UpdateMedicalOrderCategoryCommand command) {
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
        }, MedicalOrderService.MedicalOrderCategoryNode::id, () -> id);
    }

    @Transactional
    public MedicalOrderService.MedicalOrderItemView createMedicalOrderItem(MedicalOrderService.CreateMedicalOrderItemCommand command) {
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
        }, MedicalOrderService.MedicalOrderItemView::id, () -> orderItemCode);
    }

    @Transactional
    public MedicalOrderService.MedicalOrderItemView updateMedicalOrderItem(String id, MedicalOrderService.UpdateMedicalOrderItemCommand command) {
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
        }, MedicalOrderService.MedicalOrderItemView::id, () -> id);
    }

    @Transactional
    public MedicalOrderService.MedicalOrderItemView updateMedicalOrderItemEnabled(String id, boolean enabled) {
        return operationAuditService.audit("MASTERDATA", "ORDER_ITEM", "update_order_item_enabled", () -> {
            if (repository.findOrderItemById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order item not found");
            }
            repository.updateOrderItemEnabled(id, enabled);
            return toItemView(repository.findOrderItemById(id));
        }, MedicalOrderService.MedicalOrderItemView::id, () -> id);
    }

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

    private MedicalOrderService.MedicalOrderCategoryNode toCategoryNode(MedicalOrderJdbcRepository.OrderCategoryRow row) {
        return new MedicalOrderService.MedicalOrderCategoryNode(
            row.id(),
            row.parentId(),
            row.categoryCode(),
            row.categoryName(),
            row.sortOrder(),
            row.enabled(),
            new ArrayList<>(),
            new ArrayList<>());
    }

    private MedicalOrderService.MedicalOrderItemView toItemView(MedicalOrderJdbcRepository.OrderItemRow row) {
        return new MedicalOrderService.MedicalOrderItemView(
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
