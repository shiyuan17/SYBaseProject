package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.DiagnosticReportRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JdbcDiagnosticTaskStore {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "ASSIGNED", "ACCEPTED", "IN_PROGRESS");
    private static final String ROLE_M4_DIAGNOSIS = "M4_DIAGNOSIS";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcDiagnosticTaskStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<DiagnosticReportRepository.DiagnosticTask> findDiagnosticTaskById(String taskId) {
        List<DiagnosticReportRepository.DiagnosticTask> rows = jdbcTemplate.query(diagnosticTaskSelectSql() + """
            where dt.id = :taskId
            """, Map.of("taskId", taskId), this::mapDiagnosticTask);
        return rows.stream().findFirst();
    }

    List<DiagnosticReportRepository.DiagnosticTask> findDiagnosticTasksByCaseId(String caseId) {
        return jdbcTemplate.query(diagnosticTaskSelectSql() + """
            where dt.case_id = :caseId
            order by dt.created_at asc, dt.id asc
            """, Map.of("caseId", caseId), this::mapDiagnosticTask);
    }

    List<DiagnosticReportRepository.DiagnosticTask> findActiveDiagnosticTasksByCaseIdAndType(String caseId, String taskType) {
        return jdbcTemplate.query(diagnosticTaskSelectSql() + """
            where dt.case_id = :caseId
              and dt.task_type = :taskType
              and dt.status in (:statuses)
            order by dt.created_at asc, dt.id asc
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("taskType", taskType)
            .addValue("statuses", ACTIVE_STATUSES), this::mapDiagnosticTask);
    }

    DiagnosticReportRepository.PagedDiagnosticTasks findDiagnosticTasks(DiagnosticReportRepository.PendingDiagnosticTaskQuery query) {
        String where = " where 1 = 1 " + buildDiagnosticTaskFilters(query);
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            """ + where, diagnosticTaskFilterParams(query), Long.class);
        List<DiagnosticReportRepository.DiagnosticTask> items = jdbcTemplate.query(diagnosticTaskSelectSql() + where + """
            
            order by dt.created_at asc, dt.id asc
            offset :offset rows fetch next :limit rows only
            """, diagnosticTaskPageParams(query), this::mapDiagnosticTask);
        return new DiagnosticReportRepository.PagedDiagnosticTasks(items, total == null ? 0 : total);
    }

    void insertDiagnosticTask(DiagnosticReportRepository.CreateDiagnosticTaskCommand command) {
        jdbcTemplate.update("""
            insert into diagnostic_tasks
                (id, case_id, specimen_id, pathology_no, task_type, status, priority, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :pathologyNo, :taskType, :status, :priority, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("pathologyNo", command.pathologyNo())
            .addValue("taskType", command.taskType())
            .addValue("status", command.status())
            .addValue("priority", command.priority())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.createdAt()));
    }

    void assignDiagnosticTask(DiagnosticReportRepository.AssignDiagnosticTaskCommand command) {
        jdbcTemplate.update("""
            update diagnostic_tasks
            set status = 'ASSIGNED',
                assignment_mode = 'MANUAL',
                assigned_by_user_id = :assignedByUserId,
                assigned_by_name = :assignedByName,
                diagnosis_doctor_user_id = :diagnosisDoctorUserId,
                diagnosis_doctor_name = :diagnosisDoctorName,
                primary_doctor_user_id = :primaryDoctorUserId,
                primary_doctor_name = :primaryDoctorName,
                reviewer_user_id = :reviewerUserId,
                reviewer_name = :reviewerName,
                assigned_at = :assignedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", command.taskId())
            .addValue("assignedByUserId", command.assignedByUserId())
            .addValue("assignedByName", command.assignedByName())
            .addValue("diagnosisDoctorUserId", command.diagnosisDoctorUserId())
            .addValue("diagnosisDoctorName", command.diagnosisDoctorName())
            .addValue("primaryDoctorUserId", command.primaryDoctorUserId())
            .addValue("primaryDoctorName", command.primaryDoctorName())
            .addValue("reviewerUserId", command.reviewerUserId())
            .addValue("reviewerName", command.reviewerName())
            .addValue("assignedAt", command.assignedAt())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.assignedAt()));
    }

    void acceptDiagnosticTask(String taskId, String remarks, LocalDateTime acceptedAt) {
        jdbcTemplate.update("""
            update diagnostic_tasks
            set status = 'ACCEPTED',
                accepted_at = :acceptedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("acceptedAt", acceptedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", acceptedAt));
    }

    void startDiagnosticTask(String taskId, String remarks, LocalDateTime startedAt) {
        jdbcTemplate.update("""
            update diagnostic_tasks
            set status = 'IN_PROGRESS',
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("remarks", remarks)
            .addValue("updatedAt", startedAt));
    }

    void markDiagnosticTaskSubmitted(String taskId, String remarks, LocalDateTime primaryDiagnosedAt) {
        jdbcTemplate.update("""
            update diagnostic_tasks
            set remarks = :remarks,
                primary_diagnosed_at = :primaryDiagnosedAt,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("remarks", remarks)
            .addValue("primaryDiagnosedAt", primaryDiagnosedAt)
            .addValue("updatedAt", primaryDiagnosedAt));
    }

    void markDiagnosticTaskReviewed(String taskId,
                                    String reviewerUserId,
                                    String reviewerName,
                                    String remarks,
                                    LocalDateTime reviewedAt) {
        jdbcTemplate.update("""
            update diagnostic_tasks
            set review_completed_at = :reviewedAt,
                reviewer_user_id = :reviewerUserId,
                reviewer_name = :reviewerName,
                reviewed_at = :reviewedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("reviewedAt", reviewedAt)
            .addValue("reviewerUserId", reviewerUserId)
            .addValue("reviewerName", reviewerName)
            .addValue("remarks", remarks)
            .addValue("updatedAt", reviewedAt));
    }

    void revertDiagnosticTaskToInProgress(String taskId, String remarks) {
        jdbcTemplate.update("""
            update diagnostic_tasks
            set status = 'IN_PROGRESS',
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void completeDiagnosticTask(String taskId, String remarks, LocalDateTime completedAt) {
        jdbcTemplate.update("""
            update diagnostic_tasks
            set status = 'COMPLETED',
                completed_at = :completedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("completedAt", completedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", completedAt));
    }

    private String diagnosticTaskSelectSql() {
        return """
            select
                dt.id,
                pc.application_id,
                a.application_no,
                a.patient_name,
                dt.case_id,
                dt.pathology_no,
                dt.specimen_id,
                dt.task_type,
                dt.status,
                dt.priority,
                dt.assignment_mode,
                dt.assigned_by_user_id,
                dt.assigned_by_name,
                dt.diagnosis_doctor_user_id,
                dt.diagnosis_doctor_name,
                dt.primary_doctor_user_id,
                dt.primary_doctor_name,
                dt.reviewer_user_id,
                dt.reviewer_name,
                dt.primary_diagnosed_at,
                dt.reviewed_at,
                dt.assigned_at,
                dt.accepted_at,
                dt.completed_at,
                dt.remarks,
                dt.created_at
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            join applications a on a.id = pc.application_id
            """;
    }

    private String buildDiagnosticTaskFilters(DiagnosticReportRepository.PendingDiagnosticTaskQuery query) {
        StringBuilder builder = new StringBuilder();
        if (hasText(query.taskType())) {
            builder.append(" and dt.task_type = :taskType");
        }
        if (hasText(query.taskStatus())) {
            builder.append(" and dt.status = :taskStatus");
        }
        if (hasText(query.pathologyNo())) {
            builder.append(" and dt.pathology_no = :pathologyNo");
        }
        if (ROLE_M4_DIAGNOSIS.equals(query.currentRoleCode()) && hasText(query.currentUserId())) {
            builder.append(" and (dt.diagnosis_doctor_user_id = :currentUserId or dt.primary_doctor_user_id = :currentUserId)");
        }
        return builder.toString();
    }

    private MapSqlParameterSource diagnosticTaskFilterParams(DiagnosticReportRepository.PendingDiagnosticTaskQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (hasText(query.taskType())) {
            params.addValue("taskType", query.taskType());
        }
        if (hasText(query.taskStatus())) {
            params.addValue("taskStatus", query.taskStatus());
        }
        if (hasText(query.pathologyNo())) {
            params.addValue("pathologyNo", query.pathologyNo());
        }
        if (ROLE_M4_DIAGNOSIS.equals(query.currentRoleCode()) && hasText(query.currentUserId())) {
            params.addValue("currentUserId", query.currentUserId());
        }
        return params;
    }

    private MapSqlParameterSource diagnosticTaskPageParams(DiagnosticReportRepository.PendingDiagnosticTaskQuery query) {
        return diagnosticTaskFilterParams(query)
            .addValue("limit", query.size())
            .addValue("offset", Math.max(query.page() - 1, 0) * query.size());
    }

    private DiagnosticReportRepository.DiagnosticTask mapDiagnosticTask(ResultSet rs, int rowNum) throws SQLException {
        return new DiagnosticReportRepository.DiagnosticTask(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("specimen_id"),
            rs.getString("task_type"),
            rs.getString("status"),
            rs.getString("priority"),
            rs.getString("assignment_mode"),
            rs.getString("assigned_by_user_id"),
            rs.getString("assigned_by_name"),
            rs.getString("diagnosis_doctor_user_id"),
            rs.getString("diagnosis_doctor_name"),
            rs.getString("primary_doctor_user_id"),
            rs.getString("primary_doctor_name"),
            rs.getString("reviewer_user_id"),
            rs.getString("reviewer_name"),
            toLocalDateTime(rs.getTimestamp("primary_diagnosed_at")),
            toLocalDateTime(rs.getTimestamp("reviewed_at")),
            toLocalDateTime(rs.getTimestamp("assigned_at")),
            toLocalDateTime(rs.getTimestamp("accepted_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
