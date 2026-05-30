package com.company.bl.integration.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

final class JdbcM6BillingStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcM6BillingStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insertBillingRecord(M6BillingRows.CreateBillingRecordRow row) {
        jdbcTemplate.update("""
            insert into billing_records
                (id, case_id, order_id, billing_no, billing_stage, item_type, item_name, quantity, amount,
                 billing_status, billed_at, operator_user_id, operator_name, external_bill_no, external_system,
                 remarks, created_at, updated_at)
            values
                (:id, :caseId, :orderId, :billingNo, :billingStage, :itemType, :itemName, :quantity, :amount,
                 :billingStatus, :billedAt, :operatorUserId, :operatorName, :externalBillNo, :externalSystem,
                 :remarks, :createdAt, :updatedAt)
            """, toBillingParams(row));
    }

    void updateBillingRecord(M6BillingRows.BillingRecordRow row) {
        jdbcTemplate.update("""
            update billing_records
            set billing_status = :billingStatus,
                billed_at = :billedAt,
                operator_user_id = :operatorUserId,
                operator_name = :operatorName,
                external_bill_no = :externalBillNo,
                external_system = :externalSystem,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, toBillingParams(new M6BillingRows.CreateBillingRecordRow(
            row.id(), row.caseId(), row.orderId(), row.billingNo(), row.billingStage(), row.itemType(), row.itemName(),
            row.quantity(), row.amount(), row.billingStatus(), row.billedAt(), row.operatorUserId(), row.operatorName(),
            row.externalBillNo(), row.externalSystem(), row.remarks(), row.createdAt(), row.updatedAt())));
    }

    M6BillingRows.BillingRecordRow findBillingRecordById(String id) {
        List<M6BillingRows.BillingRecordRow> rows = jdbcTemplate.query("""
            select id, case_id, order_id, billing_no, billing_stage, item_type, item_name, quantity, amount,
                   billing_status, billed_at, operator_user_id, operator_name, external_bill_no, external_system,
                   remarks, created_at, updated_at
            from billing_records
            where id = :id
            """, Map.of("id", id), this::mapBillingRecord);
        return rows.isEmpty() ? null : rows.get(0);
    }

    List<M6BillingRows.BillingRecordRow> findBillingRecords(String billingStatus,
                                                            String billingStage,
                                                            String externalSystem,
                                                            String caseId,
                                                            String orderId,
                                                            LocalDateTime from,
                                                            LocalDateTime to) {
        return jdbcTemplate.query("""
            select id, case_id, order_id, billing_no, billing_stage, item_type, item_name, quantity, amount,
                   billing_status, billed_at, operator_user_id, operator_name, external_bill_no, external_system,
                   remarks, created_at, updated_at
            from billing_records
            where (:billingStatus is null or billing_status = :billingStatus)
              and (:billingStage is null or billing_stage = :billingStage)
              and (:externalSystem is null or external_system = :externalSystem)
              and (:caseId is null or case_id = :caseId)
              and (:orderId is null or order_id = :orderId)
              and (:fromTime is null or coalesce(billed_at, created_at) >= :fromTime)
              and (:toTime is null or coalesce(billed_at, created_at) <= :toTime)
            order by created_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("billingStatus", blankToNull(billingStatus))
            .addValue("billingStage", blankToNull(billingStage))
            .addValue("externalSystem", blankToNull(externalSystem))
            .addValue("caseId", blankToNull(caseId))
            .addValue("orderId", blankToNull(orderId))
            .addValue("fromTime", from)
            .addValue("toTime", to), this::mapBillingRecord);
    }

    void updateMedicalOrderBillingStatus(String orderId, String billingStatus) {
        jdbcTemplate.update("""
            update medical_orders
            set billing_status = :billingStatus,
                updated_at = :updatedAt
            where id = :orderId
            """, new MapSqlParameterSource()
            .addValue("orderId", orderId)
            .addValue("billingStatus", billingStatus)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    private MapSqlParameterSource toBillingParams(M6BillingRows.CreateBillingRecordRow row) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("caseId", row.caseId())
            .addValue("orderId", row.orderId())
            .addValue("billingNo", row.billingNo())
            .addValue("billingStage", row.billingStage())
            .addValue("itemType", row.itemType())
            .addValue("itemName", row.itemName())
            .addValue("quantity", row.quantity())
            .addValue("amount", row.amount())
            .addValue("billingStatus", row.billingStatus())
            .addValue("billedAt", row.billedAt())
            .addValue("operatorUserId", row.operatorUserId())
            .addValue("operatorName", row.operatorName())
            .addValue("externalBillNo", row.externalBillNo())
            .addValue("externalSystem", row.externalSystem())
            .addValue("remarks", row.remarks())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private M6BillingRows.BillingRecordRow mapBillingRecord(ResultSet rs, int rowNum) throws SQLException {
        return new M6BillingRows.BillingRecordRow(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("order_id"),
            rs.getString("billing_no"),
            rs.getString("billing_stage"),
            rs.getString("item_type"),
            rs.getString("item_name"),
            rs.getBigDecimal("quantity"),
            rs.getBigDecimal("amount"),
            rs.getString("billing_status"),
            toLocalDateTime(rs.getTimestamp("billed_at")),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            rs.getString("external_bill_no"),
            rs.getString("external_system"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
