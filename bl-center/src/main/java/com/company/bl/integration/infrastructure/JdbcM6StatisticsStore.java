package com.company.bl.integration.infrastructure;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

final class JdbcM6StatisticsStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcM6StatisticsStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<M6StatisticsRows.StatIndicatorDefinitionRow> findStatIndicatorDefinitions(String category) {
        return jdbcTemplate.query("""
            select id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type,
                   description, sort_order, enabled, created_at, updated_at
            from stat_indicator_definitions
            where enabled = 1 and (:category is null or indicator_category = :category)
            order by sort_order, indicator_code
            """, new MapSqlParameterSource().addValue("category", blankToNull(category)), this::mapStatIndicatorDefinition);
    }

    M6StatisticsRows.StatIndicatorDefinitionRow findStatIndicatorDefinitionByCode(String indicatorCode) {
        List<M6StatisticsRows.StatIndicatorDefinitionRow> rows = jdbcTemplate.query("""
            select id, indicator_code, indicator_name, indicator_category, metric_scope, aggregation_type,
                   description, sort_order, enabled, created_at, updated_at
            from stat_indicator_definitions
            where indicator_code = :indicatorCode
            """, Map.of("indicatorCode", indicatorCode), this::mapStatIndicatorDefinition);
        return rows.isEmpty() ? null : rows.get(0);
    }

    List<M6StatisticsRows.StatReportTemplateRow> findStatReportTemplates(String templateType) {
        return jdbcTemplate.query("""
            select id, template_code, template_name, template_type, indicator_code, default_columns, parameter_schema,
                   sort_order, enabled, created_at, updated_at
            from stat_report_templates
            where enabled = 1 and (:templateType is null or template_type = :templateType)
            order by sort_order, template_code
            """, new MapSqlParameterSource().addValue("templateType", blankToNull(templateType)), this::mapStatReportTemplate);
    }

    M6StatisticsRows.StatReportTemplateRow findStatReportTemplateByCode(String templateCode) {
        List<M6StatisticsRows.StatReportTemplateRow> rows = jdbcTemplate.query("""
            select id, template_code, template_name, template_type, indicator_code, default_columns, parameter_schema,
                   sort_order, enabled, created_at, updated_at
            from stat_report_templates
            where template_code = :templateCode
            """, Map.of("templateCode", templateCode), this::mapStatReportTemplate);
        return rows.isEmpty() ? null : rows.get(0);
    }

    void insertStatExportJob(M6StatisticsRows.CreateStatExportJobRow row) {
        jdbcTemplate.update("""
            insert into stat_export_jobs
                (id, export_no, template_id, indicator_code, export_status, filter_payload, file_name, content_type,
                 requested_by_user_id, requested_by_name, error_message, created_at, completed_at)
            values
                (:id, :exportNo, :templateId, :indicatorCode, :exportStatus, :filterPayload, :fileName, :contentType,
                 :requestedByUserId, :requestedByName, :errorMessage, :createdAt, :completedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("exportNo", row.exportNo())
            .addValue("templateId", row.templateId())
            .addValue("indicatorCode", row.indicatorCode())
            .addValue("exportStatus", row.exportStatus())
            .addValue("filterPayload", row.filterPayload())
            .addValue("fileName", row.fileName())
            .addValue("contentType", row.contentType())
            .addValue("requestedByUserId", row.requestedByUserId())
            .addValue("requestedByName", row.requestedByName())
            .addValue("errorMessage", row.errorMessage())
            .addValue("createdAt", row.createdAt())
            .addValue("completedAt", row.completedAt()));
    }

    void completeStatExportJob(String id, String exportStatus, String errorMessage, LocalDateTime completedAt) {
        jdbcTemplate.update("""
            update stat_export_jobs
            set export_status = :exportStatus,
                error_message = :errorMessage,
                completed_at = :completedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("exportStatus", exportStatus)
            .addValue("errorMessage", errorMessage)
            .addValue("completedAt", completedAt));
    }

    private M6StatisticsRows.StatIndicatorDefinitionRow mapStatIndicatorDefinition(ResultSet rs, int rowNum) throws SQLException {
        return new M6StatisticsRows.StatIndicatorDefinitionRow(
            rs.getString("id"),
            rs.getString("indicator_code"),
            rs.getString("indicator_name"),
            rs.getString("indicator_category"),
            rs.getString("metric_scope"),
            rs.getString("aggregation_type"),
            rs.getString("description"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1,
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private M6StatisticsRows.StatReportTemplateRow mapStatReportTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new M6StatisticsRows.StatReportTemplateRow(
            rs.getString("id"),
            rs.getString("template_code"),
            rs.getString("template_name"),
            rs.getString("template_type"),
            rs.getString("indicator_code"),
            rs.getString("default_columns"),
            rs.getString("parameter_schema"),
            rs.getInt("sort_order"),
            rs.getInt("enabled") == 1,
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
