package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.MedicalOrderRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
                (id, case_id, order_number, order_content, order_type, execution_scope, billing_status, status,
                 doctor_user_id, doctor_name, order_date, remarks, created_at, updated_at)
            values
                (:id, :caseId, :orderNumber, :orderContent, :orderType, :executionScope, :billingStatus, :status,
                 :doctorUserId, :doctorName, :orderDate, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("orderNumber", command.orderNumber())
            .addValue("orderContent", command.orderContent())
            .addValue("orderType", command.orderType())
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
