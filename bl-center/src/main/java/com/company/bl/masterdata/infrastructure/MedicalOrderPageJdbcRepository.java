package com.company.bl.masterdata.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class MedicalOrderPageJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MedicalOrderPageJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PagedChargeItems findChargeItemsPage(int page, int size, Boolean enabled, String keyword, String orderDictItemId) {
        int offset = Math.max(0, (page - 1) * size);
        String baseSql = """
            from medical_order_charge_items charge
            join medical_order_dict_items item on item.id = charge.order_dict_item_id
            where 1 = 1
            """;
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("offset", offset).addValue("size", size);
        StringBuilder conditions = new StringBuilder();
        appendChargeItemFilters(conditions, params, enabled, keyword, orderDictItemId);
        List<ChargeItemRow> rows = jdbcTemplate.query("""
            select charge.id, charge.order_dict_item_id, charge.charge_item_code, charge.charge_item_name,
                   charge.specification, charge.unit, charge.price, charge.sort_order, charge.enabled, item.order_item_name
            """ + baseSql + conditions + """

            order by charge.sort_order, charge.charge_item_code
            offset :offset rows fetch next :size rows only
            """, params, this::mapChargeItem);
        Long total = jdbcTemplate.queryForObject("select count(*) " + baseSql + conditions, params, Long.class);
        return new PagedChargeItems(rows, total == null ? 0L : total);
    }

    public PagedPackages findPackagesPage(int page, int size, Boolean enabled, String keyword, String packageType) {
        int offset = Math.max(0, (page - 1) * size);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("offset", offset).addValue("size", size);
        StringBuilder conditions = new StringBuilder(" where 1 = 1");
        if (enabled != null) {
            conditions.append(" and enabled = :enabled");
            params.addValue("enabled", enabled ? 1 : 0);
        }
        if (packageType != null && !packageType.isBlank()) {
            conditions.append(" and package_type = :packageType");
            params.addValue("packageType", packageType);
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.append(" and (package_code like :keyword or package_name like :keyword)");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        List<PackageRow> rows = jdbcTemplate.query("""
            select id, package_code, package_name, package_type, owner_user_id, enabled, remarks
            from medical_order_packages
            """ + conditions + """
            order by package_code
            offset :offset rows fetch next :size rows only
            """, params, this::mapPackage);
        Long total = jdbcTemplate.queryForObject("select count(*) from medical_order_packages" + conditions, params, Long.class);
        return new PagedPackages(rows, total == null ? 0L : total);
    }

    public Map<String, List<PackageItemRow>> findPackageItemsByPackageIds(List<String> packageIds) {
        Map<String, List<PackageItemRow>> result = new HashMap<>();
        if (packageIds.isEmpty()) {
            return result;
        }
        jdbcTemplate.query("""
            select package_items.id, package_items.package_id, package_items.order_item_id, package_items.sort_order,
                   package_items.remarks, items.order_item_code, items.order_item_name
            from medical_order_package_items package_items
            join medical_order_dict_items items on items.id = package_items.order_item_id
            where package_items.package_id in (:packageIds)
            order by package_items.sort_order, package_items.id
            """, new MapSqlParameterSource().addValue("packageIds", packageIds), rs -> {
            PackageItemRow row = mapPackageItem(rs, 0);
            result.computeIfAbsent(row.packageId(), key -> new ArrayList<>()).add(row);
        });
        return result;
    }

    private void appendChargeItemFilters(StringBuilder conditions, MapSqlParameterSource params, Boolean enabled,
                                         String keyword, String orderDictItemId) {
        if (enabled != null) {
            conditions.append(" and charge.enabled = :enabled");
            params.addValue("enabled", enabled ? 1 : 0);
        }
        if (orderDictItemId != null && !orderDictItemId.isBlank()) {
            conditions.append(" and charge.order_dict_item_id = :orderDictItemId");
            params.addValue("orderDictItemId", orderDictItemId);
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.append("""
                 and (
                    charge.charge_item_code like :keyword
                    or charge.charge_item_name like :keyword
                    or item.order_item_name like :keyword
                 )
                """);
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
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

    public record ChargeItemRow(String id, String orderDictItemId, String orderItemName, String chargeItemCode,
                                String chargeItemName, String specification, String unit, BigDecimal price,
                                int sortOrder, boolean enabled) {}
    public record PagedChargeItems(List<ChargeItemRow> items, long total) {}
    public record PackageRow(String id, String packageCode, String packageName, String packageType,
                             String ownerUserId, boolean enabled, String remarks) {}
    public record PackageItemRow(String id, String packageId, String orderItemId, String orderItemCode,
                                 String orderItemName, int sortOrder, String remarks) {}
    public record PagedPackages(List<PackageRow> items, long total) {}
}
