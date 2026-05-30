package com.company.bl.integration.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

final class JdbcM6IntegrationTaskStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcM6IntegrationTaskStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insertIntegrationTask(M6IntegrationTaskRows.CreateIntegrationTaskRow row) {
        jdbcTemplate.update("""
            insert into integration_tasks
                (id, task_type, business_type, business_id, stage_code, external_system, request_payload,
                 response_payload, task_status, retry_count, max_retry_count, next_retry_at, last_attempt_at,
                 last_error_code, last_error_message, compensation_status, reconciliation_status, resolved_at, created_at, updated_at)
            values
                (:id, :taskType, :businessType, :businessId, :stageCode, :externalSystem, :requestPayload,
                 :responsePayload, :taskStatus, :retryCount, :maxRetryCount, :nextRetryAt, :lastAttemptAt,
                 :lastErrorCode, :lastErrorMessage, :compensationStatus, :reconciliationStatus, :resolvedAt, :createdAt, :updatedAt)
            """, toIntegrationParams(row));
    }

    void updateIntegrationTask(M6IntegrationTaskRows.IntegrationTaskRow row) {
        jdbcTemplate.update("""
            update integration_tasks
            set request_payload = :requestPayload,
                response_payload = :responsePayload,
                task_status = :taskStatus,
                retry_count = :retryCount,
                max_retry_count = :maxRetryCount,
                next_retry_at = :nextRetryAt,
                last_attempt_at = :lastAttemptAt,
                last_error_code = :lastErrorCode,
                last_error_message = :lastErrorMessage,
                compensation_status = :compensationStatus,
                reconciliation_status = :reconciliationStatus,
                resolved_at = :resolvedAt,
                updated_at = :updatedAt
            where id = :id
            """, toIntegrationParams(new M6IntegrationTaskRows.CreateIntegrationTaskRow(
            row.id(), row.taskType(), row.businessType(), row.businessId(), row.stageCode(), row.externalSystem(), row.requestPayload(),
            row.responsePayload(), row.taskStatus(), row.retryCount(), row.maxRetryCount(), row.nextRetryAt(), row.lastAttemptAt(),
            row.lastErrorCode(), row.lastErrorMessage(), row.compensationStatus(), row.reconciliationStatus(), row.resolvedAt(),
            row.createdAt(), row.updatedAt())));
    }

    M6IntegrationTaskRows.IntegrationTaskRow findIntegrationTaskById(String id) {
        List<M6IntegrationTaskRows.IntegrationTaskRow> rows = jdbcTemplate.query("""
            select id, task_type, business_type, business_id, stage_code, external_system, request_payload, response_payload,
                   task_status, retry_count, max_retry_count, next_retry_at, last_attempt_at, last_error_code, last_error_message,
                   compensation_status, reconciliation_status, resolved_at, created_at, updated_at
            from integration_tasks
            where id = :id
            """, Map.of("id", id), this::mapIntegrationTask);
        return rows.isEmpty() ? null : rows.get(0);
    }

    M6IntegrationTaskRows.IntegrationTaskRow findLatestIntegrationTask(String businessType, String businessId, String stageCode) {
        List<M6IntegrationTaskRows.IntegrationTaskRow> rows = jdbcTemplate.query("""
            select id, task_type, business_type, business_id, stage_code, external_system, request_payload, response_payload,
                   task_status, retry_count, max_retry_count, next_retry_at, last_attempt_at, last_error_code, last_error_message,
                   compensation_status, reconciliation_status, resolved_at, created_at, updated_at
            from integration_tasks
            where business_type = :businessType
              and business_id = :businessId
              and (:stageCode is null or stage_code = :stageCode)
            order by created_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("businessType", businessType)
            .addValue("businessId", businessId)
            .addValue("stageCode", stageCode), this::mapIntegrationTask);
        return rows.isEmpty() ? null : rows.get(0);
    }

    List<M6IntegrationTaskRows.IntegrationTaskRow> findIntegrationTasks(String taskType,
                                                                        String businessType,
                                                                        String businessId,
                                                                        String taskStatus,
                                                                        String stageCode,
                                                                        String externalSystem,
                                                                        String compensationStatus,
                                                                        String reconciliationStatus) {
        return jdbcTemplate.query("""
            select id, task_type, business_type, business_id, stage_code, external_system, request_payload, response_payload,
                   task_status, retry_count, max_retry_count, next_retry_at, last_attempt_at, last_error_code, last_error_message,
                   compensation_status, reconciliation_status, resolved_at, created_at, updated_at
            from integration_tasks
            where (:taskType is null or task_type = :taskType)
              and (:businessType is null or business_type = :businessType)
              and (:businessId is null or business_id = :businessId)
              and (:taskStatus is null or task_status = :taskStatus)
              and (:stageCode is null or stage_code = :stageCode)
              and (:externalSystem is null or external_system = :externalSystem)
              and (:compensationStatus is null or compensation_status = :compensationStatus)
              and (:reconciliationStatus is null or reconciliation_status = :reconciliationStatus)
            order by created_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("taskType", blankToNull(taskType))
            .addValue("businessType", blankToNull(businessType))
            .addValue("businessId", blankToNull(businessId))
            .addValue("taskStatus", blankToNull(taskStatus))
            .addValue("stageCode", blankToNull(stageCode))
            .addValue("externalSystem", blankToNull(externalSystem))
            .addValue("compensationStatus", blankToNull(compensationStatus))
            .addValue("reconciliationStatus", blankToNull(reconciliationStatus)), this::mapIntegrationTask);
    }

    long countIntegrationTasksByStatus(String taskStatus) {
        Long value = jdbcTemplate.queryForObject("""
            select count(*)
            from integration_tasks
            where task_status = :taskStatus
            """, Map.of("taskStatus", taskStatus), Long.class);
        return value == null ? 0L : value;
    }

    long countIntegrationTasksByCompensationStatus(String compensationStatus) {
        Long value = jdbcTemplate.queryForObject("""
            select count(*)
            from integration_tasks
            where compensation_status = :compensationStatus
            """, Map.of("compensationStatus", compensationStatus), Long.class);
        return value == null ? 0L : value;
    }

    long countIntegrationTasksByBusinessAndReconciliationStatus(String businessType, String reconciliationStatus) {
        Long value = jdbcTemplate.queryForObject("""
            select count(*)
            from integration_tasks
            where business_type = :businessType
              and reconciliation_status = :reconciliationStatus
            """, new MapSqlParameterSource()
            .addValue("businessType", businessType)
            .addValue("reconciliationStatus", reconciliationStatus), Long.class);
        return value == null ? 0L : value;
    }

    private MapSqlParameterSource toIntegrationParams(M6IntegrationTaskRows.CreateIntegrationTaskRow row) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("taskType", row.taskType())
            .addValue("businessType", row.businessType())
            .addValue("businessId", row.businessId())
            .addValue("stageCode", row.stageCode())
            .addValue("externalSystem", row.externalSystem())
            .addValue("requestPayload", row.requestPayload())
            .addValue("responsePayload", row.responsePayload())
            .addValue("taskStatus", row.taskStatus())
            .addValue("retryCount", row.retryCount())
            .addValue("maxRetryCount", row.maxRetryCount())
            .addValue("nextRetryAt", row.nextRetryAt())
            .addValue("lastAttemptAt", row.lastAttemptAt())
            .addValue("lastErrorCode", row.lastErrorCode())
            .addValue("lastErrorMessage", row.lastErrorMessage())
            .addValue("compensationStatus", row.compensationStatus())
            .addValue("reconciliationStatus", row.reconciliationStatus())
            .addValue("resolvedAt", row.resolvedAt())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private M6IntegrationTaskRows.IntegrationTaskRow mapIntegrationTask(ResultSet rs, int rowNum) throws SQLException {
        return new M6IntegrationTaskRows.IntegrationTaskRow(
            rs.getString("id"),
            rs.getString("task_type"),
            rs.getString("business_type"),
            rs.getString("business_id"),
            rs.getString("stage_code"),
            rs.getString("external_system"),
            rs.getString("request_payload"),
            rs.getString("response_payload"),
            rs.getString("task_status"),
            rs.getInt("retry_count"),
            rs.getInt("max_retry_count"),
            toLocalDateTime(rs.getTimestamp("next_retry_at")),
            toLocalDateTime(rs.getTimestamp("last_attempt_at")),
            rs.getString("last_error_code"),
            rs.getString("last_error_message"),
            rs.getString("compensation_status"),
            rs.getString("reconciliation_status"),
            toLocalDateTime(rs.getTimestamp("resolved_at")),
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
