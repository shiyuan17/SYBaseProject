package com.company.bl.masterdata.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MedicalOrderChargeJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MedicalOrderChargeJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    private ChargeItemRow mapChargeItem(ResultSet rs, int rowNum) throws SQLException {
        return new ChargeItemRow(
            rs.getString("id"),
            rs.getString("order_dict_item_id"),
            rs.getString("order_item_name"),
            rs.getString("charge_item_code"),
            rs.getString("charge_item_name"),
            rs.getString("specification"),
            rs.getString("unit"),
            rs.getBigDecimal("price"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1);
    }

    public record ChargeItemRow(
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

    public record CreateChargeItemRow(
        String id,
        String orderDictItemId,
        String chargeItemCode,
        String chargeItemName,
        String specification,
        String unit,
        BigDecimal price,
        int sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record UpdateChargeItemRow(
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
}
