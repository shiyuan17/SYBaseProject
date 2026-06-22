package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.MedicalOrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public JdbcMedicalOrderRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insertMedicalOrder(CreateMedicalOrderCommand command) {
        jdbcTemplate.update("""
            insert into medical_orders
                (id, case_id, order_number, order_content, order_type,
                 order_item_id, order_item_code, order_item_name,
                 order_category_id, order_category_code, order_category_name,
                 execution_scope, billing_status, status,
                 doctor_user_id, doctor_name,
                 target_type, target_specimen_id, target_specimen_no, target_block_id, target_block_no, target_slide_id, target_slide_no,
                 order_date, remarks, created_at, updated_at)
            values
                (:id, :caseId, :orderNumber, :orderContent, :orderType,
                 :orderItemId, :orderItemCode, :orderItemName,
                 :orderCategoryId, :orderCategoryCode, :orderCategoryName,
                 :executionScope, :billingStatus, :status,
                 :doctorUserId, :doctorName,
                 :targetType, :targetSpecimenId, :targetSpecimenNo, :targetBlockId, :targetBlockNo, :targetSlideId, :targetSlideNo,
                 :orderDate, :remarks, :createdAt, :updatedAt)
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
            .addValue("targetType", command.targetType())
            .addValue("targetSpecimenId", command.targetSpecimenId())
            .addValue("targetSpecimenNo", command.targetSpecimenNo())
            .addValue("targetBlockId", command.targetBlockId())
            .addValue("targetBlockNo", command.targetBlockNo())
            .addValue("targetSlideId", command.targetSlideId())
            .addValue("targetSlideNo", command.targetSlideNo())
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
                released_by_user_id = executor_user_id,
                released_by_name = executor_name,
                released_at = :completedAt,
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
    public void markMedicalOrderPrinted(String orderId,
                                        String printedByUserId,
                                        String printedByName,
                                        String remarks,
                                        LocalDateTime printedAt) {
        jdbcTemplate.update("""
            update medical_orders
            set printed_by_user_id = :printedByUserId,
                printed_by_name = :printedByName,
                printed_at = :printedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :orderId
            """, new MapSqlParameterSource()
            .addValue("orderId", orderId)
            .addValue("printedByUserId", printedByUserId)
            .addValue("printedByName", printedByName)
            .addValue("printedAt", printedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", printedAt));
    }

    @Override
    public void terminateMedicalOrder(String orderId,
                                      String terminatedByUserId,
                                      String terminatedByName,
                                      String terminationReasonCode,
                                      String terminationReasonLabel,
                                      String remarks,
                                      LocalDateTime terminatedAt) {
        jdbcTemplate.update("""
            update medical_orders
            set status = 'TERMINATED',
                terminated_by_user_id = :terminatedByUserId,
                terminated_by_name = :terminatedByName,
                terminated_at = :terminatedAt,
                termination_reason_code = :terminationReasonCode,
                termination_reason_label = :terminationReasonLabel,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :orderId
            """, new MapSqlParameterSource()
            .addValue("orderId", orderId)
            .addValue("terminatedByUserId", terminatedByUserId)
            .addValue("terminatedByName", terminatedByName)
            .addValue("terminatedAt", terminatedAt)
            .addValue("terminationReasonCode", terminationReasonCode)
            .addValue("terminationReasonLabel", terminationReasonLabel)
            .addValue("remarks", remarks)
            .addValue("updatedAt", terminatedAt));
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

    @Override
    public void insertMedicalOrderQcEvaluation(CreateMedicalOrderQcEvaluationCommand command) {
        jdbcTemplate.update("""
            insert into medical_order_qc_evaluations
                (id, order_id, case_id, qc_aspect, total_score, grade, evaluation_reason, processing_action,
                 rework_type, rework_order_id, remarks, evaluator_user_id, evaluator_name, evaluated_at,
                 detail_payload_json, created_at, updated_at)
            values
                (:id, :orderId, :caseId, :qcAspect, :totalScore, :grade, :evaluationReason, :processingAction,
                 :reworkType, :reworkOrderId, :remarks, :evaluatorUserId, :evaluatorName, :evaluatedAt,
                 :detailPayloadJson, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("orderId", command.orderId())
            .addValue("caseId", command.caseId())
            .addValue("qcAspect", command.qcAspect())
            .addValue("totalScore", command.totalScore())
            .addValue("grade", command.grade())
            .addValue("evaluationReason", command.evaluationReason())
            .addValue("processingAction", command.processingAction())
            .addValue("reworkType", command.reworkType())
            .addValue("reworkOrderId", command.reworkOrderId())
            .addValue("remarks", command.remarks())
            .addValue("evaluatorUserId", command.evaluatorUserId())
            .addValue("evaluatorName", command.evaluatorName())
            .addValue("evaluatedAt", command.evaluatedAt())
            .addValue("detailPayloadJson", toJson(command.detailPayload()))
            .addValue("createdAt", command.evaluatedAt())
            .addValue("updatedAt", command.evaluatedAt()));
    }

    @Override
    public Optional<MedicalOrderQcEvaluation> findLatestMedicalOrderQcEvaluation(String orderId) {
        List<MedicalOrderQcEvaluation> rows = jdbcTemplate.query("""
            select *
            from medical_order_qc_evaluations
            where order_id = :orderId
            order by evaluated_at desc, created_at desc
            limit 1
            """, Map.of("orderId", orderId), this::mapMedicalOrderQcEvaluation);
        return rows.stream().findFirst();
    }

    private String selectSql() {
        return """
            select
                mo.*,
                pc.pathology_no,
                a.application_no,
                a.patient_name,
                a.patient_id,
                a.patient_id as patient_id_display
            from medical_orders mo
            join pathology_cases pc on pc.id = mo.case_id
            join applications a on a.id = pc.application_id
            """;
    }

    private String buildFilters(PendingMedicalOrderQuery query) {
        StringBuilder builder = new StringBuilder();
        if (query.pathologyNo() != null && !query.pathologyNo().isBlank()) {
            builder.append(" and pc.pathology_no = :pathologyNo\n");
        }
        if (query.status() != null && !query.status().isBlank()) {
            builder.append(" and mo.status = :status\n");
        } else {
            builder.append(" and mo.status in ('PENDING', 'IN_PROGRESS', 'TERMINATED')\n");
        }
        if (query.orderDateFrom() != null) {
            builder.append(" and mo.order_date >= :orderDateFrom\n");
        }
        if (query.orderDateTo() != null) {
            builder.append(" and mo.order_date < :orderDateTo\n");
        }
        List<String> categoryCodes = parseOrderCategoryCodes(query.orderCategoryCode());
        if (!categoryCodes.isEmpty()) {
            builder.append(" and (upper(mo.order_category_code) in (:orderCategoryCodes)");
            String fallback = buildLegacyCategoryFallback(categoryCodes);
            if (!fallback.isBlank()) {
                builder.append(" or (mo.order_category_code is null and ( ").append(fallback).append(" ))");
            }
            builder.append(")\n");
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
        if (query.orderDateFrom() != null) {
            params.addValue("orderDateFrom", query.orderDateFrom());
        }
        if (query.orderDateTo() != null) {
            params.addValue("orderDateTo", query.orderDateTo());
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
            rs.getString("patient_id"),
            rs.getString("patient_id_display"),
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
            rs.getString("printed_by_user_id"),
            rs.getString("printed_by_name"),
            toLocalDateTime(rs.getTimestamp("printed_at")),
            rs.getString("released_by_user_id"),
            rs.getString("released_by_name"),
            toLocalDateTime(rs.getTimestamp("released_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            toLocalDateTime(rs.getTimestamp("cancelled_at")),
            rs.getString("terminated_by_user_id"),
            rs.getString("terminated_by_name"),
            toLocalDateTime(rs.getTimestamp("terminated_at")),
            rs.getString("termination_reason_code"),
            rs.getString("termination_reason_label"),
            rs.getString("target_type"),
            rs.getString("target_specimen_id"),
            rs.getString("target_specimen_no"),
            rs.getString("target_block_id"),
            rs.getString("target_block_no"),
            rs.getString("target_slide_id"),
            rs.getString("target_slide_no"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private MedicalOrderQcEvaluation mapMedicalOrderQcEvaluation(ResultSet rs, int rowNum) throws SQLException {
        return new MedicalOrderQcEvaluation(
            rs.getString("id"),
            rs.getString("order_id"),
            rs.getString("case_id"),
            rs.getString("qc_aspect"),
            rs.getObject("total_score", Integer.class),
            rs.getString("grade"),
            rs.getString("evaluation_reason"),
            rs.getString("processing_action"),
            rs.getString("rework_type"),
            rs.getString("rework_order_id"),
            rs.getString("remarks"),
            rs.getString("evaluator_user_id"),
            rs.getString("evaluator_name"),
            toLocalDateTime(rs.getTimestamp("evaluated_at")),
            toJsonNode(rs.getString("detail_payload_json")));
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

    private String toJson(JsonNode node) {
        return node == null ? null : node.toString();
    }

    private JsonNode toJsonNode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse JSON payload", ex);
        }
    }
}
