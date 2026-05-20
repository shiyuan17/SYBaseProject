package com.company.bl.masterdata.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class MedicalOrderJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MedicalOrderJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OrderCategoryRow> findOrderCategories() {
        return jdbcTemplate.query("""
            select id, parent_id, category_code, category_name, sort_order, enabled
            from medical_order_dict_categories
            order by sort_order, category_code
            """, this::mapOrderCategory);
    }

    public List<OrderItemRow> findOrderItems() {
        return jdbcTemplate.query("""
            select id, category_id, order_item_code, order_item_name, order_type, default_content, execution_scope,
                   sort_order, enabled
            from medical_order_dict_items
            order by sort_order, order_item_code
            """, this::mapOrderItem);
    }

    public OrderCategoryRow insertOrderCategory(CreateOrderCategoryRow row) {
        jdbcTemplate.update("""
            insert into medical_order_dict_categories
                (id, parent_id, category_code, category_name, sort_order, enabled, created_at, updated_at)
            values
                (:id, :parentId, :categoryCode, :categoryName, :sortOrder, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("parentId", row.parentId())
            .addValue("categoryCode", row.categoryCode())
            .addValue("categoryName", row.categoryName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findOrderCategoryById(row.id());
    }

    public OrderItemRow insertOrderItem(CreateOrderItemRow row) {
        jdbcTemplate.update("""
            insert into medical_order_dict_items
                (id, category_id, order_item_code, order_item_name, order_type, default_content, execution_scope,
                 sort_order, enabled, created_at, updated_at)
            values
                (:id, :categoryId, :orderItemCode, :orderItemName, :orderType, :defaultContent, :executionScope,
                 :sortOrder, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("categoryId", row.categoryId())
            .addValue("orderItemCode", row.orderItemCode())
            .addValue("orderItemName", row.orderItemName())
            .addValue("orderType", row.orderType())
            .addValue("defaultContent", row.defaultContent())
            .addValue("executionScope", row.executionScope())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findOrderItemById(row.id());
    }

    public OrderCategoryRow findOrderCategoryById(String id) {
        List<OrderCategoryRow> rows = jdbcTemplate.query("""
            select id, parent_id, category_code, category_name, sort_order, enabled
            from medical_order_dict_categories where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapOrderCategory);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public OrderItemRow findOrderItemById(String id) {
        List<OrderItemRow> rows = jdbcTemplate.query("""
            select id, category_id, order_item_code, order_item_name, order_type, default_content, execution_scope,
                   sort_order, enabled
            from medical_order_dict_items where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapOrderItem);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateOrderCategory(String id, UpdateOrderCategoryRow row) {
        jdbcTemplate.update("""
            update medical_order_dict_categories
            set parent_id = :parentId,
                category_code = :categoryCode,
                category_name = :categoryName,
                sort_order = :sortOrder,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("parentId", row.parentId())
            .addValue("categoryCode", row.categoryCode())
            .addValue("categoryName", row.categoryName())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateOrderItem(String id, UpdateOrderItemRow row) {
        jdbcTemplate.update("""
            update medical_order_dict_items
            set category_id = :categoryId,
                order_item_code = :orderItemCode,
                order_item_name = :orderItemName,
                order_type = :orderType,
                default_content = :defaultContent,
                execution_scope = :executionScope,
                sort_order = :sortOrder,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("categoryId", row.categoryId())
            .addValue("orderItemCode", row.orderItemCode())
            .addValue("orderItemName", row.orderItemName())
            .addValue("orderType", row.orderType())
            .addValue("defaultContent", row.defaultContent())
            .addValue("executionScope", row.executionScope())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateOrderItemEnabled(String id, boolean enabled) {
        jdbcTemplate.update("""
            update medical_order_dict_items set enabled = :enabled, updated_at = :updatedAt where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("enabled", enabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public long countOrderCategoryChildren(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from medical_order_dict_categories
            where parent_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public long countOrderCategoryItems(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from medical_order_dict_items
            where category_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public void deleteOrderCategory(String id) {
        jdbcTemplate.update("delete from medical_order_dict_categories where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    public long countChargeItemsByOrderItem(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from medical_order_charge_items
            where order_dict_item_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public long countPackageItemsByOrderItem(String id) {
        Long total = jdbcTemplate.queryForObject("""
            select count(*)
            from medical_order_package_items
            where order_item_id = :id
            """, new MapSqlParameterSource().addValue("id", id), Long.class);
        return total == null ? 0L : total;
    }

    public void deleteOrderItem(String id) {
        jdbcTemplate.update("delete from medical_order_dict_items where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    public List<ChargeItemRow> findChargeItems() {
        return jdbcTemplate.query("""
            select charge.id, charge.order_dict_item_id, charge.charge_item_code, charge.charge_item_name,
                   charge.specification, charge.unit, charge.price, charge.sort_order, charge.enabled,
                   item.order_item_name
            from medical_order_charge_items charge
            join medical_order_dict_items item on item.id = charge.order_dict_item_id
            order by charge.sort_order, charge.charge_item_code
            """, this::mapChargeItem);
    }

    public ChargeItemRow insertChargeItem(CreateChargeItemRow row) {
        jdbcTemplate.update("""
            insert into medical_order_charge_items
                (id, order_dict_item_id, charge_item_code, charge_item_name, specification, unit, price,
                 sort_order, enabled, created_at, updated_at)
            values
                (:id, :orderDictItemId, :chargeItemCode, :chargeItemName, :specification, :unit, :price,
                 :sortOrder, :enabled, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("orderDictItemId", row.orderDictItemId())
            .addValue("chargeItemCode", row.chargeItemCode())
            .addValue("chargeItemName", row.chargeItemName())
            .addValue("specification", row.specification())
            .addValue("unit", row.unit())
            .addValue("price", row.price())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        return findChargeItemById(row.id());
    }

    public ChargeItemRow findChargeItemById(String id) {
        List<ChargeItemRow> rows = jdbcTemplate.query("""
            select charge.id, charge.order_dict_item_id, charge.charge_item_code, charge.charge_item_name,
                   charge.specification, charge.unit, charge.price, charge.sort_order, charge.enabled,
                   item.order_item_name
            from medical_order_charge_items charge
            join medical_order_dict_items item on item.id = charge.order_dict_item_id
            where charge.id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapChargeItem);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ChargeItemRow findChargeItemByCode(String chargeItemCode) {
        List<ChargeItemRow> rows = jdbcTemplate.query("""
            select charge.id, charge.order_dict_item_id, charge.charge_item_code, charge.charge_item_name,
                   charge.specification, charge.unit, charge.price, charge.sort_order, charge.enabled,
                   item.order_item_name
            from medical_order_charge_items charge
            join medical_order_dict_items item on item.id = charge.order_dict_item_id
            where charge.charge_item_code = :chargeItemCode
            """, new MapSqlParameterSource().addValue("chargeItemCode", chargeItemCode), this::mapChargeItem);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateChargeItem(String id, UpdateChargeItemRow row) {
        jdbcTemplate.update("""
            update medical_order_charge_items
            set order_dict_item_id = :orderDictItemId,
                charge_item_code = :chargeItemCode,
                charge_item_name = :chargeItemName,
                specification = :specification,
                unit = :unit,
                price = :price,
                sort_order = :sortOrder,
                enabled = :enabled,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("orderDictItemId", row.orderDictItemId())
            .addValue("chargeItemCode", row.chargeItemCode())
            .addValue("chargeItemName", row.chargeItemName())
            .addValue("specification", row.specification())
            .addValue("unit", row.unit())
            .addValue("price", row.price())
            .addValue("sortOrder", row.sortOrder())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void updateChargeItemEnabled(String id, boolean enabled) {
        jdbcTemplate.update("""
            update medical_order_charge_items set enabled = :enabled, updated_at = :updatedAt where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("enabled", enabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void deleteChargeItem(String id) {
        jdbcTemplate.update("delete from medical_order_charge_items where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    public List<PackageRow> findPackages() {
        return jdbcTemplate.query("""
            select id, package_code, package_name, package_type, owner_user_id, enabled, remarks
            from medical_order_packages
            order by package_code
            """, this::mapPackage);
    }

    public List<PackageItemRow> findPackageItems() {
        return jdbcTemplate.query("""
            select package_items.id, package_items.package_id, package_items.order_item_id, package_items.sort_order,
                   package_items.remarks, items.order_item_code, items.order_item_name
            from medical_order_package_items package_items
            join medical_order_dict_items items on items.id = package_items.order_item_id
            order by package_items.sort_order, package_items.id
            """, this::mapPackageItem);
    }

    public PackageRow insertPackage(CreatePackageRow row) {
        jdbcTemplate.update("""
            insert into medical_order_packages
                (id, package_code, package_name, package_type, owner_user_id, enabled, remarks, created_at, updated_at)
            values
                (:id, :packageCode, :packageName, :packageType, :ownerUserId, :enabled, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("packageCode", row.packageCode())
            .addValue("packageName", row.packageName())
            .addValue("packageType", row.packageType())
            .addValue("ownerUserId", row.ownerUserId())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("remarks", row.remarks())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
        replacePackageItems(row.id(), row.itemIds());
        return findPackageById(row.id());
    }

    public PackageRow findPackageById(String id) {
        List<PackageRow> rows = jdbcTemplate.query("""
            select id, package_code, package_name, package_type, owner_user_id, enabled, remarks
            from medical_order_packages where id = :id
            """, new MapSqlParameterSource().addValue("id", id), this::mapPackage);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updatePackage(String id, UpdatePackageRow row) {
        jdbcTemplate.update("""
            update medical_order_packages
            set package_code = :packageCode,
                package_name = :packageName,
                package_type = :packageType,
                owner_user_id = :ownerUserId,
                enabled = :enabled,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("packageCode", row.packageCode())
            .addValue("packageName", row.packageName())
            .addValue("packageType", row.packageType())
            .addValue("ownerUserId", row.ownerUserId())
            .addValue("enabled", row.enabled() ? 1 : 0)
            .addValue("remarks", row.remarks())
            .addValue("updatedAt", LocalDateTime.now()));
        replacePackageItems(id, row.itemIds());
    }

    public void replacePackageItems(String packageId, List<String> itemIds) {
        jdbcTemplate.update("delete from medical_order_package_items where package_id = :packageId",
            new MapSqlParameterSource().addValue("packageId", packageId));
        int sortOrder = 0;
        for (String itemId : itemIds) {
            jdbcTemplate.update("""
                insert into medical_order_package_items
                    (id, package_id, order_item_id, sort_order, created_at)
                values
                    (:id, :packageId, :orderItemId, :sortOrder, :createdAt)
                """, new MapSqlParameterSource()
                .addValue("id", "PKI-" + UUID.randomUUID())
                .addValue("packageId", packageId)
                .addValue("orderItemId", itemId)
                .addValue("sortOrder", ++sortOrder)
                .addValue("createdAt", LocalDateTime.now()));
        }
    }

    public void updatePackageEnabled(String id, boolean enabled) {
        jdbcTemplate.update("""
            update medical_order_packages set enabled = :enabled, updated_at = :updatedAt where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("enabled", enabled ? 1 : 0)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    public void deletePackage(String id) {
        jdbcTemplate.update("delete from medical_order_package_items where package_id = :packageId",
            new MapSqlParameterSource().addValue("packageId", id));
        jdbcTemplate.update("delete from medical_order_packages where id = :id", new MapSqlParameterSource().addValue("id", id));
    }

    private OrderCategoryRow mapOrderCategory(ResultSet rs, int rowNum) throws SQLException {
        return new OrderCategoryRow(rs.getString("id"), rs.getString("parent_id"), rs.getString("category_code"),
            rs.getString("category_name"), rs.getInt("sort_order"), rs.getInt("enabled") == 1);
    }
    private OrderItemRow mapOrderItem(ResultSet rs, int rowNum) throws SQLException {
        return new OrderItemRow(rs.getString("id"), rs.getString("category_id"), rs.getString("order_item_code"),
            rs.getString("order_item_name"), rs.getString("order_type"), rs.getString("default_content"),
            rs.getString("execution_scope"), rs.getInt("sort_order"), rs.getInt("enabled") == 1);
    }
    private ChargeItemRow mapChargeItem(ResultSet rs, int rowNum) throws SQLException {
        return new ChargeItemRow(rs.getString("id"), rs.getString("order_dict_item_id"), rs.getString("order_item_name"),
            rs.getString("charge_item_code"), rs.getString("charge_item_name"), rs.getString("specification"),
            rs.getString("unit"), rs.getBigDecimal("price"), rs.getInt("sort_order"), rs.getInt("enabled") == 1);
    }
    private PackageRow mapPackage(ResultSet rs, int rowNum) throws SQLException {
        return new PackageRow(rs.getString("id"), rs.getString("package_code"), rs.getString("package_name"),
            rs.getString("package_type"), rs.getString("owner_user_id"), rs.getInt("enabled") == 1, rs.getString("remarks"));
    }
    private PackageItemRow mapPackageItem(ResultSet rs, int rowNum) throws SQLException {
        return new PackageItemRow(rs.getString("id"), rs.getString("package_id"), rs.getString("order_item_id"),
            rs.getString("order_item_code"), rs.getString("order_item_name"), rs.getInt("sort_order"), rs.getString("remarks"));
    }
    public record OrderCategoryRow(String id, String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {}
    public record OrderItemRow(String id, String categoryId, String orderItemCode, String orderItemName, String orderType, String defaultContent, String executionScope, int sortOrder, boolean enabled) {}
    public record CreateOrderCategoryRow(String id, String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record UpdateOrderCategoryRow(String parentId, String categoryCode, String categoryName, int sortOrder, boolean enabled) {}
    public record CreateOrderItemRow(String id, String categoryId, String orderItemCode, String orderItemName, String orderType, String defaultContent, String executionScope, int sortOrder, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record UpdateOrderItemRow(String categoryId, String orderItemCode, String orderItemName, String orderType, String defaultContent, String executionScope, int sortOrder, boolean enabled) {}
    public record ChargeItemRow(String id, String orderDictItemId, String orderItemName, String chargeItemCode, String chargeItemName, String specification, String unit, BigDecimal price, int sortOrder, boolean enabled) {}
    public record CreateChargeItemRow(String id, String orderDictItemId, String chargeItemCode, String chargeItemName, String specification, String unit, BigDecimal price, int sortOrder, boolean enabled, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record UpdateChargeItemRow(String orderDictItemId, String chargeItemCode, String chargeItemName, String specification, String unit, BigDecimal price, int sortOrder, boolean enabled) {}
    public record PackageRow(String id, String packageCode, String packageName, String packageType, String ownerUserId, boolean enabled, String remarks) {}
    public record PackageItemRow(String id, String packageId, String orderItemId, String orderItemCode, String orderItemName, int sortOrder, String remarks) {}
    public record CreatePackageRow(String id, String packageCode, String packageName, String packageType, String ownerUserId, boolean enabled, String remarks, List<String> itemIds, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record UpdatePackageRow(String packageCode, String packageName, String packageType, String ownerUserId, boolean enabled, String remarks, List<String> itemIds) {}
}
