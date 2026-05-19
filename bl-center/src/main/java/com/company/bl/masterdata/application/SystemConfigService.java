package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.SystemConfigJdbcRepository;
import com.company.bl.support.application.OperationAuditService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SystemConfigService {

    private final SystemConfigJdbcRepository repository;
    private final OperationAuditService operationAuditService;

    public SystemConfigService(SystemConfigJdbcRepository repository,
                               OperationAuditService operationAuditService) {
        this.repository = repository;
        this.operationAuditService = operationAuditService;
    }

    @Cacheable("systemConfigTree")
    @Transactional(readOnly = true)
    public List<ConfigCategoryNode> listSystemConfigs() {
        var categories = repository.findConfigCategories();
        var items = repository.findConfigItems();
        var nodes = categories.stream().map(this::toNode)
            .collect(Collectors.toMap(ConfigCategoryNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        items.forEach(item -> {
            ConfigCategoryNode parent = nodes.get(item.categoryId());
            if (parent != null) {
                parent.items().add(toItemView(item));
            }
        });
        List<ConfigCategoryNode> roots = new ArrayList<>();
        nodes.values().forEach(node -> {
            if (node.parentId() == null) {
                roots.add(node);
            } else {
                ConfigCategoryNode parent = nodes.get(node.parentId());
                if (parent != null) {
                    parent.children().add(node);
                } else {
                    roots.add(node);
                }
            }
        });
        return roots;
    }

    @CacheEvict(value = "systemConfigTree", allEntries = true)
    @Transactional
    public ConfigCategoryNode createConfigCategory(CreateConfigCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "CONFIG_CATEGORY", "create_config_category", () -> {
            try {
                return toNode(repository.insertConfigCategory(new SystemConfigJdbcRepository.CreateConfigCategoryRow(
                    "SCC-" + UUID.randomUUID(), command.parentId(), command.categoryCode(), command.categoryName(),
                    command.categoryType(), command.sortOrder(), command.enabled(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Config category code already exists");
            }
        }, ConfigCategoryNode::id, command::categoryCode);
    }

    @CacheEvict(value = "systemConfigTree", allEntries = true)
    @Transactional
    public ConfigItemView createConfigItem(CreateConfigItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "CONFIG_ITEM", "create_config_item", () -> {
            try {
                return toItemView(repository.insertConfigItem(new SystemConfigJdbcRepository.CreateConfigItemRow(
                    "SCI-" + UUID.randomUUID(), command.categoryId(), command.configKey(), command.configName(),
                    command.configValue(), command.valueType(), command.sortOrder(), command.enabled(),
                    command.remarks(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Config key already exists");
            }
        }, ConfigItemView::id, command::configKey);
    }

    @CacheEvict(value = "systemConfigTree", allEntries = true)
    @Transactional
    public ConfigItemView updateConfigItem(String id, UpdateConfigItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "CONFIG_ITEM", "update_config_item", () -> {
            if (repository.findConfigItemById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Config item not found");
            }
            repository.updateConfigItem(id, new SystemConfigJdbcRepository.UpdateConfigItemRow(
                command.configValue(), command.enabled(), command.remarks()));
            return toItemView(repository.findConfigItemById(id));
        }, ConfigItemView::id, () -> id);
    }

    private ConfigCategoryNode toNode(SystemConfigJdbcRepository.ConfigCategoryRow row) {
        return new ConfigCategoryNode(row.id(), row.parentId(), row.categoryCode(), row.categoryName(),
            row.categoryType(), row.sortOrder(), row.enabled(), new ArrayList<>(), new ArrayList<>());
    }

    private ConfigItemView toItemView(SystemConfigJdbcRepository.ConfigItemRow row) {
        return new ConfigItemView(row.id(), row.categoryId(), row.configKey(), row.configName(), row.configValue(),
            row.valueType(), row.sortOrder(), row.enabled(), row.remarks());
    }

    public record ConfigCategoryNode(String id, String parentId, String categoryCode, String categoryName,
                                     String categoryType, int sortOrder, boolean enabled,
                                     List<ConfigCategoryNode> children, List<ConfigItemView> items) {
    }

    public record ConfigItemView(String id, String categoryId, String configKey, String configName,
                                 String configValue, String valueType, int sortOrder, boolean enabled,
                                 String remarks) {
    }

    public record CreateConfigCategoryCommand(String parentId, String categoryCode, String categoryName,
                                              String categoryType, int sortOrder, boolean enabled) {
    }

    public record CreateConfigItemCommand(String categoryId, String configKey, String configName,
                                          String configValue, String valueType, int sortOrder,
                                          boolean enabled, String remarks) {
    }

    public record UpdateConfigItemCommand(String configValue, boolean enabled, String remarks) {
    }
}
