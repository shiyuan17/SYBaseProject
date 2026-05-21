package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.masterdata.infrastructure.SystemConfigJdbcRepository;
import com.company.bl.support.application.OperationAuditService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.cache.annotation.Caching;
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

    @Cacheable(cacheNames = "systemConfigItems", key = "#configKey", unless = "#result == null")
    @Transactional(readOnly = true)
    public ConfigItemView findConfigItemByKey(String configKey) {
        SystemConfigJdbcRepository.ConfigItemRow row = repository.findConfigItemByKey(configKey);
        return row == null ? null : toItemView(row);
    }

    @Transactional(readOnly = true)
    public String getConfigValue(String configKey, String defaultValue) {
        ConfigItemView item = findConfigItemByKey(configKey);
        return item == null || item.configValue() == null || item.configValue().isBlank() ? defaultValue : item.configValue();
    }

    @Caching(evict = {
        @CacheEvict(value = "systemConfigTree", allEntries = true),
        @CacheEvict(value = "systemConfigItems", allEntries = true)
    })
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

    @Caching(evict = {
        @CacheEvict(value = "systemConfigTree", allEntries = true),
        @CacheEvict(value = "systemConfigItems", allEntries = true)
    })
    @Transactional
    public ConfigCategoryNode updateConfigCategory(String id, UpdateConfigCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "CONFIG_CATEGORY", "update_config_category", () -> {
            if (repository.findConfigCategoryById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Config category not found");
            }
            try {
                repository.updateConfigCategory(id, new SystemConfigJdbcRepository.UpdateConfigCategoryRow(
                    command.parentId(), command.categoryCode(), command.categoryName(), command.categoryType(),
                    command.sortOrder(), command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Config category code already exists");
            }
            return toNode(repository.findConfigCategoryById(id));
        }, ConfigCategoryNode::id, () -> id);
    }

    @Caching(evict = {
        @CacheEvict(value = "systemConfigTree", allEntries = true),
        @CacheEvict(value = "systemConfigItems", allEntries = true)
    })
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

    @Caching(evict = {
        @CacheEvict(value = "systemConfigTree", allEntries = true),
        @CacheEvict(value = "systemConfigItems", allEntries = true)
    })
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

    @Caching(evict = {
        @CacheEvict(value = "systemConfigTree", allEntries = true),
        @CacheEvict(value = "systemConfigItems", allEntries = true)
    })
    @Transactional
    public void deleteConfigCategory(String id) {
        operationAuditService.audit("MASTERDATA", "CONFIG_CATEGORY", "delete_config_category", () -> {
            if (repository.findConfigCategoryById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Config category not found");
            }
            if (repository.countCategoryChildren(id) > 0 || repository.countItemsByCategory(id) > 0) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Config category still has children or items");
            }
            repository.deleteConfigCategory(id);
            return id;
        }, value -> id, () -> id);
    }

    @Caching(evict = {
        @CacheEvict(value = "systemConfigTree", allEntries = true),
        @CacheEvict(value = "systemConfigItems", allEntries = true)
    })
    @Transactional
    public void deleteConfigItem(String id) {
        operationAuditService.audit("MASTERDATA", "CONFIG_ITEM", "delete_config_item", () -> {
            if (repository.findConfigItemById(id) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Config item not found");
            }
            repository.deleteConfigItem(id);
            return id;
        }, value -> id, () -> id);
    }

    private ConfigCategoryNode toNode(SystemConfigJdbcRepository.ConfigCategoryRow row) {
        return new ConfigCategoryNode(row.id(), row.parentId(), row.categoryCode(), row.categoryName(),
            row.categoryType(), row.sortOrder(), row.enabled(), new ArrayList<>(), new ArrayList<>());
    }

    private ConfigItemView toItemView(SystemConfigJdbcRepository.ConfigItemRow row) {
        return new ConfigItemView(row.id(), row.categoryId(), row.configKey(), row.configName(), row.configValue(),
            row.valueType(), row.sortOrder(), row.enabled(), row.remarks());
    }

    @Schema(name = "ConfigCategoryNode", description = "系统配置分类树节点")
    public record ConfigCategoryNode(
        @Schema(description = "分类 ID") String id,
        @Schema(description = "父级分类 ID") String parentId,
        @Schema(description = "分类编码") String categoryCode,
        @Schema(description = "分类名称") String categoryName,
        @Schema(description = "分类类型") String categoryType,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "子分类列表") List<ConfigCategoryNode> children,
        @Schema(description = "分类下配置项") List<ConfigItemView> items) {
    }

    @Schema(name = "ConfigItemView", description = "系统配置项")
    public record ConfigItemView(
        @Schema(description = "配置项 ID") String id,
        @Schema(description = "分类 ID") String categoryId,
        @Schema(description = "配置键") String configKey,
        @Schema(description = "配置名称") String configName,
        @Schema(description = "配置值") String configValue,
        @Schema(description = "值类型") String valueType,
        @Schema(description = "排序号") int sortOrder,
        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "备注") String remarks) {
    }

    public record CreateConfigCategoryCommand(String parentId, String categoryCode, String categoryName,
                                              String categoryType, int sortOrder, boolean enabled) {
    }

    public record UpdateConfigCategoryCommand(String parentId, String categoryCode, String categoryName,
                                              String categoryType, int sortOrder, boolean enabled) {
    }

    public record CreateConfigItemCommand(String categoryId, String configKey, String configName,
                                          String configValue, String valueType, int sortOrder,
                                          boolean enabled, String remarks) {
    }

    public record UpdateConfigItemCommand(String configValue, boolean enabled, String remarks) {
    }
}
