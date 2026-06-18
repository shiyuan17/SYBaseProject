package com.company.bl.masterdata.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SystemConfigJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SystemConfigJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ConfigCategoryRow> findConfigCategories() {
        return jdbcTemplate.query("""
            select id, parent_id, category_code, category_name, category_type, sort_order, enabled
            from system_config_categories
            order by sort_order, category_code
            """, this::mapCategory);
    }

    public List<ConfigItemRow> findConfigItems() {
        return jdbcTemplate.query("""
            select id, category_id, config_key, config_name, config_value, value_type, sort_order, enabled, remarks
            from system_config_items
            order by sort_order, config_key
            """, this::mapItem);
    }

    public List<SpecimenDictionaryDepartmentRelationRow> findSpecimenDictionaryDepartmentRelations() {
        return jdbcTemplate.query("""
            select rel.config_item_id, rel.department_id
            from system_config_item_departments rel
            join system_config_items item on item.id = rel.config_item_id
            join system_config_categories category on category.id = item.category_id
            where category.category_type = 'SPECIMEN_DICTIONARY'
            order by rel.config_item_id, rel.department_id
            """, (rs, rowNum) -> new SpecimenDictionaryDepartmentRelationRow(
            rs.getString("config_item_id"),
            rs.getString("department_id")));
    }

    public ConfigCategoryRow insertConfigCategory(CreateConfigCategoryRow row) {
        jdbcTemplate.update("""
            insert into system_config_categories
                (id, parent_id, category_code, category_name, category_type, sort_order, enabled, created_at, updated_at)
            values
                (:id, :parentId, :categoryCode, :categoryName, :categoryType, :sortOrder, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("parentId", row.parentId())
            .addValue("categoryCode", row.categoryCode())
            .addValue("categoryName", row.categoryName())
            .addValue("categoryType", row.categoryType())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findConfigCategoryById(row.id());
    }

    public ConfigItemRow insertConfigItem(CreateConfigItemRow row) {
        jdbcTemplate.update("""
            insert into system_config_items
                (id, category_id, config_key, config_name, config_value, value_type, sort_order,
                 enabled, remarks, created_at, updated_at)
            values
                (:id, :categoryId, :configKey, :configName, :configValue, :valueType, :sortOrder,
                 :enabled, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("categoryId", row.categoryId())
            .addValue("configKey", row.configKey())
            .addValue("configName", row.configName())
            .addValue("configValue", row.configValue())
            .addValue("valueType", row.valueType())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("remarks", row.remarks())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findConfigItemById(row.id());
    }

    public void replaceSpecimenDictionaryItemDepartments(String configItemId, List<String> departmentIds) {
        jdbcTemplate.update("""
            delete from system_config_item_departments
            where config_item_id = :configItemId
            """, new MapSqlParameterSource().addValue("configItemId", configItemId));

        if (departmentIds == null || departmentIds.isEmpty()) {
            return;
        }

        List<Map<String, Object>> batchValues = new ArrayList<>();
        int index = 0;
        for (String departmentId : departmentIds) {
            if (departmentId == null || departmentId.isBlank()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", configItemId + "_" + (++index) + "_" + departmentId.trim());
            row.put("configItemId", configItemId);
            row.put("departmentId", departmentId.trim());
            row.put("createdAt", LocalDateTime.now());
            batchValues.add(row);
        }
        if (batchValues.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
            insert into system_config_item_departments
                (id, config_item_id, department_id, created_at)
            values
                (:id, :configItemId, :departmentId, :createdAt)
            """, batchValues.toArray(Map[]::new));
    }

    public ConfigCategoryRow findConfigCategoryById(String id) {
        List<ConfigCategoryRow> rows = jdbcTemplate.query("""
            select id, parent_id, category_code, category_name, category_type, sort_order, enabled
            from system_config_categories where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapCategory);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ConfigItemRow findConfigItemById(String id) {
        List<ConfigItemRow> rows = jdbcTemplate.query("""
            select id, category_id, config_key, config_name, config_value, value_type, sort_order, enabled, remarks
            from system_config_items where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapItem);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ConfigItemRow findConfigItemByKey(String configKey) {
        List<ConfigItemRow> rows = jdbcTemplate.query("""
            select id, category_id, config_key, config_name, config_value, value_type, sort_order, enabled, remarks
            from system_config_items
            where config_key = :configKey
            """, new MapSqlParameterSource().addValue("configKey", configKey), this::mapItem);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateConfigCategory(String id, UpdateConfigCategoryRow row) {
        jdbcTemplate.update("""
            update system_config_categories
            set parent_id = :parentId,
                category_code = :categoryCode,
                category_name = :categoryName,
                category_type = :categoryType,
                sort_order = :sortOrder,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("parentId", row.parentId())
            .addValue("categoryCode", row.categoryCode())
            .addValue("categoryName", row.categoryName())
            .addValue("categoryType", row.categoryType())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateConfigItem(String id, UpdateConfigItemRow row) {
        jdbcTemplate.update("""
            update system_config_items
            set config_value = :configValue, enabled = :enabled, remarks = :remarks, updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("configValue", row.configValue())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("remarks", row.remarks())
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateSpecimenDictionaryItem(String id, UpdateSpecimenDictionaryItemRow row) {
        jdbcTemplate.update("""
            update system_config_items
            set config_name = :configName,
                sort_order = :sortOrder,
                enabled = :enabled,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("configName", row.configName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("remarks", row.remarks())
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public long countCategoryChildren(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from system_config_categories
            where parent_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public long countItemsByCategory(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from system_config_items
            where category_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public void deleteConfigCategory(String id) {
        jdbcTemplate.update("delete from system_config_categories where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    public void deleteConfigItem(String id) {
        jdbcTemplate.update("delete from system_config_items where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    private ConfigCategoryRow mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new ConfigCategoryRow(rs.getString("id"), rs.getString("parent_id"), rs.getString("category_code"),
            rs.getString("category_name"), rs.getString("category_type"), rs.getInt("sort_order"), rs.getInt("enabled") == 1);
    }

    private ConfigItemRow mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new ConfigItemRow(rs.getString("id"), rs.getString("category_id"), rs.getString("config_key"),
            rs.getString("config_name"), rs.getString("config_value"), rs.getString("value_type"),
            rs.getInt("sort_order"), rs.getInt("enabled") == 1, rs.getString("remarks"));
    }

    public record ConfigCategoryRow(String id, String parentId, String categoryCode, String categoryName,
                                    String categoryType, int sortOrder, boolean enabled) {
    }

    public record ConfigItemRow(String id, String categoryId, String configKey, String configName,
                                String configValue, String valueType, int sortOrder, boolean enabled,
                                String remarks) {
    }

    public record CreateConfigCategoryRow(String id, String parentId, String categoryCode, String categoryName,
                                          String categoryType, int sortOrder, boolean enabled,
                                          LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record CreateConfigItemRow(String id, String categoryId, String configKey, String configName,
                                      String configValue, String valueType, int sortOrder, boolean enabled,
                                      String remarks, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record UpdateConfigCategoryRow(String parentId, String categoryCode, String categoryName,
                                          String categoryType, int sortOrder, boolean enabled) {
    }

    public record UpdateConfigItemRow(String configValue, boolean enabled, String remarks) {
    }

    public record UpdateSpecimenDictionaryItemRow(
        String configName,
        int sortOrder,
        boolean enabled,
        String remarks
    ) {
    }

    public record SpecimenDictionaryDepartmentRelationRow(String configItemId, String departmentId) {
    }
}
