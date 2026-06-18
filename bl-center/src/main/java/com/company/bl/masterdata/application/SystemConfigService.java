package com.company.bl.masterdata.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class SystemConfigService {

    public static final String SPECIMEN_DICTIONARY_CATEGORY_TYPE = "SPECIMEN_DICTIONARY";
    public static final String SPECIMEN_DICTIONARY_ROOT_CODE = "SPECIMEN_DICTIONARY";

    private final SystemConfigJdbcRepository repository;
    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;

    public SystemConfigService(SystemConfigJdbcRepository repository,
                               NumberingService numberingService,
                               OperationAuditService operationAuditService) {
        this.repository = repository;
        this.numberingService = numberingService;
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

    @Transactional(readOnly = true)
    public SpecimenDictionaryTreeView getSpecimenDictionaryTree() {
        ConfigCategoryNode root = findCategoryByCode(listSystemConfigs(), SPECIMEN_DICTIONARY_ROOT_CODE);
        if (root == null) {
            return new SpecimenDictionaryTreeView(List.of(), List.of());
        }

        Map<String, List<String>> departmentIdsByItemId = groupDepartmentIds(
            repository.findSpecimenDictionaryDepartmentRelations());

        List<SpecimenDictionarySystemCategoryView> systems = root.children().stream()
            .map(system -> toSpecimenDictionarySystem(system, departmentIdsByItemId))
            .toList();
        List<SpecimenDictionaryItemView> items = systems.stream()
            .flatMap(system -> system.parts().stream())
            .flatMap(part -> part.items().stream())
            .toList();
        return new SpecimenDictionaryTreeView(systems, items);
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
        String categoryCode = resolveCreateCode(
            command.categoryCode(),
            numberingService::generateConfigCategoryCode);
        return operationAuditService.audit("MASTERDATA", "CONFIG_CATEGORY", "create_config_category", () -> {
            try {
                return toNode(repository.insertConfigCategory(new SystemConfigJdbcRepository.CreateConfigCategoryRow(
                    "SCC-" + UUID.randomUUID(), command.parentId(), categoryCode, command.categoryName(),
                    command.categoryType(), command.sortOrder(), command.enabled(), LocalDateTime.now(), LocalDateTime.now())));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Config category code already exists");
            }
        }, ConfigCategoryNode::id, () -> categoryCode);
    }

    @Caching(evict = {
        @CacheEvict(value = "systemConfigTree", allEntries = true),
        @CacheEvict(value = "systemConfigItems", allEntries = true)
    })
    @Transactional
    public ConfigCategoryNode updateConfigCategory(String id, UpdateConfigCategoryCommand command) {
        return operationAuditService.audit("MASTERDATA", "CONFIG_CATEGORY", "update_config_category", () -> {
            SystemConfigJdbcRepository.ConfigCategoryRow current = repository.findConfigCategoryById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Config category not found");
            }
            String categoryCode = resolveExistingCode(command.categoryCode(), current.categoryCode(), "Config category code");
            try {
                repository.updateConfigCategory(id, new SystemConfigJdbcRepository.UpdateConfigCategoryRow(
                    command.parentId(), categoryCode, command.categoryName(), command.categoryType(),
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
    public SpecimenDictionaryItemView createSpecimenDictionaryItem(CreateSpecimenDictionaryItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "SPECIMEN_DICTIONARY_ITEM", "create_specimen_dictionary_item", () -> {
            SystemConfigJdbcRepository.ConfigItemRow row = repository.insertConfigItem(new SystemConfigJdbcRepository.CreateConfigItemRow(
                "SCI-" + UUID.randomUUID(),
                command.partCategoryId(),
                command.configKey(),
                command.specimenName(),
                null,
                "SPECIMEN_DICTIONARY_ITEM",
                command.sortOrder(),
                command.enabled(),
                command.remarks(),
                LocalDateTime.now(),
                LocalDateTime.now()));
            repository.replaceSpecimenDictionaryItemDepartments(row.id(), normalizeDepartmentIds(command.departmentIds()));
            return toSpecimenDictionaryItemView(
                row,
                normalizeDepartmentIds(command.departmentIds()));
        }, SpecimenDictionaryItemView::id, command::configKey);
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
    public SpecimenDictionaryItemView updateSpecimenDictionaryItem(String id, UpdateSpecimenDictionaryItemCommand command) {
        return operationAuditService.audit("MASTERDATA", "SPECIMEN_DICTIONARY_ITEM", "update_specimen_dictionary_item", () -> {
            SystemConfigJdbcRepository.ConfigItemRow current = repository.findConfigItemById(id);
            if (current == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Config item not found");
            }
            repository.updateSpecimenDictionaryItem(id, new SystemConfigJdbcRepository.UpdateSpecimenDictionaryItemRow(
                command.specimenName(),
                command.sortOrder(),
                command.enabled(),
                command.remarks()));
            repository.replaceSpecimenDictionaryItemDepartments(id, normalizeDepartmentIds(command.departmentIds()));
            SystemConfigJdbcRepository.ConfigItemRow updated = repository.findConfigItemById(id);
            return toSpecimenDictionaryItemView(updated, normalizeDepartmentIds(command.departmentIds()));
        }, SpecimenDictionaryItemView::id, () -> id);
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
            repository.replaceSpecimenDictionaryItemDepartments(id, List.of());
            repository.deleteConfigItem(id);
            return id;
        }, value -> id, () -> id);
    }

    private ConfigCategoryNode findCategoryByCode(List<ConfigCategoryNode> categories, String categoryCode) {
        for (ConfigCategoryNode category : categories) {
            if (categoryCode.equals(category.categoryCode())) {
                return category;
            }
            ConfigCategoryNode nested = findCategoryByCode(category.children(), categoryCode);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private Map<String, List<String>> groupDepartmentIds(List<SystemConfigJdbcRepository.SpecimenDictionaryDepartmentRelationRow> relations) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (SystemConfigJdbcRepository.SpecimenDictionaryDepartmentRelationRow relation : relations) {
            grouped.computeIfAbsent(relation.configItemId(), ignored -> new ArrayList<>())
                .add(relation.departmentId());
        }
        return grouped;
    }

    private SpecimenDictionarySystemCategoryView toSpecimenDictionarySystem(
        ConfigCategoryNode category,
        Map<String, List<String>> departmentIdsByItemId
    ) {
        return new SpecimenDictionarySystemCategoryView(
            category.id(),
            category.categoryCode(),
            category.categoryName(),
            category.sortOrder(),
            category.enabled(),
            category.children().stream()
                .map(part -> toSpecimenDictionaryPart(part, departmentIdsByItemId))
                .toList());
    }

    private SpecimenDictionaryPartCategoryView toSpecimenDictionaryPart(
        ConfigCategoryNode category,
        Map<String, List<String>> departmentIdsByItemId
    ) {
        return new SpecimenDictionaryPartCategoryView(
            category.id(),
            category.parentId(),
            category.categoryCode(),
            category.categoryName(),
            category.sortOrder(),
            category.enabled(),
            category.items().stream()
                .map(item -> toSpecimenDictionaryItemView(
                    item,
                    departmentIdsByItemId.getOrDefault(item.id(), List.of())))
                .toList());
    }

    private SpecimenDictionaryItemView toSpecimenDictionaryItemView(
        ConfigItemView item,
        List<String> departmentIds
    ) {
        return new SpecimenDictionaryItemView(
            item.id(),
            item.categoryId(),
            item.configKey(),
            item.configName(),
            item.sortOrder(),
            item.enabled(),
            item.remarks(),
            List.copyOf(departmentIds));
    }

    private SpecimenDictionaryItemView toSpecimenDictionaryItemView(
        SystemConfigJdbcRepository.ConfigItemRow row,
        List<String> departmentIds
    ) {
        return new SpecimenDictionaryItemView(
            row.id(),
            row.categoryId(),
            row.configKey(),
            row.configName(),
            row.sortOrder(),
            row.enabled(),
            row.remarks(),
            List.copyOf(departmentIds));
    }

    private List<String> normalizeDepartmentIds(Collection<String> departmentIds) {
        if (departmentIds == null) {
            return List.of();
        }
        Set<String> deduped = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String departmentId : departmentIds) {
            if (departmentId == null) {
                continue;
            }
            String trimmed = departmentId.trim();
            if (trimmed.isEmpty() || !deduped.add(trimmed)) {
                continue;
            }
            normalized.add(trimmed);
        }
        return List.copyOf(normalized);
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

    @Schema(name = "SpecimenDictionaryTreeView", description = "标本字典树")
    public record SpecimenDictionaryTreeView(
        @Schema(description = "系统分类列表") List<SpecimenDictionarySystemCategoryView> systems,
        @Schema(description = "扁平标本项列表") List<SpecimenDictionaryItemView> items
    ) {
    }

    @Schema(name = "SpecimenDictionarySystemCategoryView", description = "标本字典系统分类")
    public record SpecimenDictionarySystemCategoryView(
        String id,
        String categoryCode,
        String categoryName,
        int sortOrder,
        boolean enabled,
        List<SpecimenDictionaryPartCategoryView> parts
    ) {
    }

    @Schema(name = "SpecimenDictionaryPartCategoryView", description = "标本字典部位分类")
    public record SpecimenDictionaryPartCategoryView(
        String id,
        String parentId,
        String categoryCode,
        String categoryName,
        int sortOrder,
        boolean enabled,
        List<SpecimenDictionaryItemView> items
    ) {
    }

    @Schema(name = "SpecimenDictionaryItemView", description = "标本字典项")
    public record SpecimenDictionaryItemView(
        String id,
        String partCategoryId,
        String configKey,
        String specimenName,
        int sortOrder,
        boolean enabled,
        String remarks,
        List<String> departmentIds
    ) {
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

    public record CreateSpecimenDictionaryItemCommand(
        String partCategoryId,
        String configKey,
        String specimenName,
        int sortOrder,
        boolean enabled,
        String remarks,
        List<String> departmentIds
    ) {
    }

    public record UpdateSpecimenDictionaryItemCommand(
        String specimenName,
        int sortOrder,
        boolean enabled,
        String remarks,
        List<String> departmentIds
    ) {
    }

    private String resolveCreateCode(String requestedCode, Supplier<String> generator) {
        String normalizedCode = normalizeCode(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    private String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = normalizeCode(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(
            BlErrorCode.INVALID_ARGUMENT,
            400,
            fieldLabel + " cannot be changed once created");
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
