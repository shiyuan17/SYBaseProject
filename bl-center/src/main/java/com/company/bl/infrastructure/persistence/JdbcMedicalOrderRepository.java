package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.MedicalOrderRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcMedicalOrderRepository implements MedicalOrderRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcMedicalOrderRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertMedicalOrder(CreateMedicalOrderCommand command) {
        jdbcTemplate.update("""
            insert into medical_orders
                (id, case_id, order_number, order_content, order_type,
                 order_item_id, order_item_code, order_item_name,
                 order_category_id, order_category_code, order_category_name,
                 execution_scope, billing_status, status,
                 doctor_user_id, doctor_name, order_date, remarks, created_at, updated_at)
            values
                (:id, :caseId, :orderNumber, :orderContent, :orderType,
                 :orderItemId, :orderItemCode, :orderItemName,
                 :orderCategoryId, :orderCategoryCode, :orderCategoryName,
                 :executionScope, :billingStatus, :status,
                 :doctorUserId, :doctorName, :orderDate, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("orderNumber", command.orderNumber())
            .addValue("orderContent", command.orderContent())
            .addValue("orderType", command.orderType())
            .addValue("orderItemId", command.orderItemId())
            .addValue("orderItemCode", command.orderItemCode())
            .addValue("orderItemName", command.orderItemName())
            .addValue("orderCategoryId", command.orderCategoryId())
            .addValue("orderCategoryCode", command.orderCategoryCode())
            .addValue("orderCategoryName", command.orderCategoryName())
            .addValue("executionScope", command.executionScope())
            .addValue("billingStatus", command.billingStatus())
            .addValue("status", command.status())
            .addValue("doctorUserId", command.doctorUserId())
            .addValue("doctorName", command.doctorName())
            .addValue("orderDate", command.orderDate())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.orderDate())
            .addValue("updatedAt", command.orderDate()));
    }

    @Override
    public Optional<MedicalOrderItemSnapshot> findMedicalOrderItemSnapshotById(String orderItemId) {
        if (orderItemId == null || orderItemId.isBlank()) {
            return Optional.empty();
        }
        List<MedicalOrderItemSnapshot> rows = jdbcTemplate.query("""
            select
                item.id as order_item_id,
                item.order_item_code,
                item.order_item_name,
                category.id as order_category_id,
                category.category_code,
                category.category_name,
                item.order_type,
                item.default_content,
                item.execution_scope
            from medical_order_dict_items item
            join medical_order_dict_categories category on category.id = item.category_id
            where item.id = :orderItemId
            """, new MapSqlParameterSource().addValue("orderItemId", orderItemId), this::mapMedicalOrderItemSnapshot);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<MedicalOrder> findMedicalOrderById(String orderId) {
        List<MedicalOrder> rows = jdbcTemplate.query(selectSql() + """
            where mo.id = :orderId
            """, Map.of("orderId", orderId), this::mapMedicalOrder);
        return rows.stream().findFirst();
    }

    @Override
    public List<MedicalOrder> findMedicalOrdersByCaseId(String caseId) {
        return jdbcTemplate.query(selectSql() + """
            where mo.case_id = :caseId
            order by mo.created_at desc, mo.id desc
            """, Map.of("caseId", caseId), this::mapMedicalOrder);
    }

    @Override
    public PagedMedicalOrders findMedicalOrders(PendingMedicalOrderQuery query) {
        String where = " where 1 = 1 " + buildFilters(query);
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from medical_orders mo
            join pathology_cases pc on pc.id = mo.case_id
            join applications a on a.id = pc.application_id
            """ + where, filterParams(query), Long.class);
        List<MedicalOrder> items = jdbcTemplate.query(selectSql() + where + """
            order by mo.created_at desc, mo.id desc
            offset :offset rows fetch next :limit rows only
            """, pageParams(query), this::mapMedicalOrder);
        return new PagedMedicalOrders(items, total == null ? 0 : total);
    }

    @Override
    public void acceptMedicalOrder(String orderId,
                                   String executorUserId,
                                   String executorName,
                                   String remarks,
                                   LocalDateTime acceptedAt) {
        jdbcTemplate.update("""
            update medical_orders
            set status = 'IN_PROGRESS',
                executor_user_id = :executorUserId,
                executor_name = :executorName,
                accepted_at = :acceptedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :orderId
            """, new MapSqlParameterSource()
            .addValue("orderId", orderId)
            .addValue("executorUserId", executorUserId)
            .addValue("executorName", executorName)
            .addValue("acceptedAt", acceptedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", acceptedAt));
    }

    @Override
    public void completeMedicalOrder(String orderId, String remarks, LocalDateTime completedAt) {
        jdbcTemplate.update("""
            update medical_orders
            set status = 'COMPLETED',
                completed_at = :completedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :orderId
            """, new MapSqlParameterSource()
            .addValue("orderId", orderId)
            .addValue("completedAt", completedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", completedAt));
    }

    @Override
    public void cancelMedicalOrder(String orderId, String remarks, LocalDateTime cancelledAt) {
        jdbcTemplate.update("""
            update medical_orders
            set status = 'CANCELLED',
                cancelled_at = :cancelledAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :orderId
            """, new MapSqlParameterSource()
            .addValue("orderId", orderId)
            .addValue("cancelledAt", cancelledAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", cancelledAt));
    }

    private String selectSql() {
        return """
            select
                mo.*,
                pc.pathology_no,
                a.application_no,
                a.patient_name
            from medical_orders mo
            join pathology_cases pc on pc.id = mo.case_id
            join applications a on a.id = pc.application_id
            """;
    }

    private String buildFilters(PendingMedicalOrderQuery query) {
        StringBuilder builder = new StringBuilder();
        if (query.pathologyNo() != null && !query.pathologyNo().isBlank()) {
            builder.append(" and pc.pathology_no = :pathologyNo");
        }
        if (query.status() != null && !query.status().isBlank()) {
            builder.append(" and mo.status = :status");
        } else {
            builder.append(" and mo.status in ('PENDING', 'IN_PROGRESS')");
        }
        List<String> categoryCodes = parseOrderCategoryCodes(query.orderCategoryCode());
        if (!categoryCodes.isEmpty()) {
            builder.append(" and (upper(mo.order_category_code) in (:orderCategoryCodes)");
            String fallback = buildLegacyCategoryFallback(categoryCodes);
            if (!fallback.isBlank()) {
                builder.append(" or (mo.order_category_code is null and (").append(fallback).append("))");
            }
            builder.append(")");
        }
        return builder.toString();
    }

    private MapSqlParameterSource filterParams(PendingMedicalOrderQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (query.pathologyNo() != null && !query.pathologyNo().isBlank()) {
            params.addValue("pathologyNo", query.pathologyNo());
        }
        if (query.status() != null && !query.status().isBlank()) {
            params.addValue("status", query.status());
        }
        List<String> categoryCodes = parseOrderCategoryCodes(query.orderCategoryCode());
        if (!categoryCodes.isEmpty()) {
            params.addValue("orderCategoryCodes", categoryCodes);
        }
        return params;
    }

    private MapSqlParameterSource pageParams(PendingMedicalOrderQuery query) {
        return filterParams(query)
            .addValue("offset", Math.max(query.page() - 1, 0) * query.size())
            .addValue("limit", query.size());
    }

    private MedicalOrder mapMedicalOrder(ResultSet rs, int rowNum) throws SQLException {
        return new MedicalOrder(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("order_number"),
            rs.getString("order_content"),
            rs.getString("order_type"),
            rs.getString("order_item_id"),
            rs.getString("order_item_code"),
            rs.getString("order_item_name"),
            rs.getString("order_category_id"),
            rs.getString("order_category_code"),
            rs.getString("order_category_name"),
            rs.getString("execution_scope"),
            rs.getString("billing_status"),
            rs.getString("status"),
            rs.getString("doctor_user_id"),
            rs.getString("doctor_name"),
            rs.getString("executor_user_id"),
            rs.getString("executor_name"),
            toLocalDateTime(rs.getTimestamp("order_date")),
            toLocalDateTime(rs.getTimestamp("accepted_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            toLocalDateTime(rs.getTimestamp("cancelled_at")),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private MedicalOrderItemSnapshot mapMedicalOrderItemSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new MedicalOrderItemSnapshot(
            rs.getString("order_item_id"),
            rs.getString("order_item_code"),
            rs.getString("order_item_name"),
            rs.getString("order_category_id"),
            rs.getString("category_code"),
            rs.getString("category_name"),
            rs.getString("order_type"),
            rs.getString("default_content"),
            rs.getString("execution_scope"));
    }

    private List<String> parseOrderCategoryCodes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .map(String::toUpperCase)
            .distinct()
            .toList();
    }

    private String buildLegacyCategoryFallback(List<String> categoryCodes) {
        List<String> filters = new ArrayList<>();
        for (String categoryCode : categoryCodes) {
            switch (categoryCode) {
                case "EXAM", "CGRS", "BLOCK", "QP" -> filters.add("""
                    upper(mo.order_type) in ('ROUTINE', 'RE_STAIN', 'RESTAIN', 'DEEP_CUT', 'RECUT', 'SLICE', 'SECTION')
                    """);
                case "TSRS" -> filters.add("""
                    upper(mo.order_type) in ('SPECIAL_STAIN', 'SPECIAL_STAINING')
                    or lower(mo.order_content) like '%特殊染色%'
                    """);
                case "IHC" -> filters.add("""
                    upper(mo.order_type) in ('IHC', 'IMMUNOHISTOCHEMISTRY')
                    or lower(mo.order_content) like '%免疫组化%'
                    """);
                case "CYTOLOGY" -> filters.add("""
                    upper(mo.order_type) in ('CYTOLOGY', 'CYTOLOGY_CONSULTATION', 'CYTOLOGY_SMEAR')
                    or lower(mo.order_content) like '%细胞学%'
                    """);
                case "LIQUID_CYTOLOGY" -> filters.add("""
                    upper(mo.order_type) in ('LIQUID_CYTOLOGY', 'GYNECOLOGY_LBC_CYTOLOGY', 'NON_GYNECOLOGY_LBC_CYTOLOGY')
                    or lower(mo.order_content) like '%液基%'
                    """);
                default -> {
                    // Unknown category codes cannot be inferred safely from legacy fields.
                }
            }
        }
        return String.join(" or ", filters);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
