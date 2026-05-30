package com.company.bl.integration.infrastructure;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

final class JdbcM6HistoricalReportStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcM6HistoricalReportStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insertHistoricalImportJob(M6HistoricalReportRows.CreateHistoricalImportJobRow row) {
        jdbcTemplate.update("""
            insert into historical_import_jobs
                (id, source_system, patient_id, pathology_no, application_no, import_status, requested_by_user_id,
                 requested_by_name, total_count, success_count, failure_count, requested_at, completed_at,
                 last_error_message, remarks, created_at, updated_at)
            values
                (:id, :sourceSystem, :patientId, :pathologyNo, :applicationNo, :importStatus, :requestedByUserId,
                 :requestedByName, :totalCount, :successCount, :failureCount, :requestedAt, :completedAt,
                 :lastErrorMessage, :remarks, :createdAt, :updatedAt)
            """, toHistoricalJobParams(row));
    }

    void updateHistoricalImportJob(M6HistoricalReportRows.HistoricalImportJobRow row) {
        jdbcTemplate.update("""
            update historical_import_jobs
            set import_status = :importStatus,
                total_count = :totalCount,
                success_count = :successCount,
                failure_count = :failureCount,
                completed_at = :completedAt,
                last_error_message = :lastErrorMessage,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, toHistoricalJobParams(new M6HistoricalReportRows.CreateHistoricalImportJobRow(
            row.id(), row.sourceSystem(), row.patientId(), row.pathologyNo(), row.applicationNo(), row.importStatus(),
            row.requestedByUserId(), row.requestedByName(), row.totalCount(), row.successCount(), row.failureCount(),
            row.requestedAt(), row.completedAt(), row.lastErrorMessage(), row.remarks(), row.createdAt(), row.updatedAt())));
    }

    M6HistoricalReportRows.HistoricalImportJobRow findHistoricalImportJobById(String id) {
        List<M6HistoricalReportRows.HistoricalImportJobRow> rows = jdbcTemplate.query("""
            select id, source_system, patient_id, pathology_no, application_no, import_status, requested_by_user_id,
                   requested_by_name, total_count, success_count, failure_count, requested_at, completed_at,
                   last_error_message, remarks, created_at, updated_at
            from historical_import_jobs
            where id = :id
            """, Map.of("id", id), this::mapHistoricalImportJob);
        return rows.isEmpty() ? null : rows.get(0);
    }

    List<M6HistoricalReportRows.HistoricalImportJobRow> findHistoricalImportJobs(String sourceSystem,
                                                                                 String importStatus,
                                                                                 String patientId,
                                                                                 String pathologyNo,
                                                                                 String applicationNo) {
        return jdbcTemplate.query("""
            select id, source_system, patient_id, pathology_no, application_no, import_status, requested_by_user_id,
                   requested_by_name, total_count, success_count, failure_count, requested_at, completed_at,
                   last_error_message, remarks, created_at, updated_at
            from historical_import_jobs
            where (:sourceSystem is null or source_system = :sourceSystem)
              and (:importStatus is null or import_status = :importStatus)
              and (:patientId is null or patient_id = :patientId)
              and (:pathologyNo is null or pathology_no = :pathologyNo)
              and (:applicationNo is null or application_no = :applicationNo)
            order by requested_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("sourceSystem", blankToNull(sourceSystem))
            .addValue("importStatus", blankToNull(importStatus))
            .addValue("patientId", blankToNull(patientId))
            .addValue("pathologyNo", blankToNull(pathologyNo))
            .addValue("applicationNo", blankToNull(applicationNo)), this::mapHistoricalImportJob);
    }

    M6HistoricalReportRows.HistoricalReportRow findHistoricalReportBySourceAndExternalNo(String sourceSystem, String externalReportNo) {
        try {
            return jdbcTemplate.queryForObject("""
                select id, import_job_id, source_system, external_report_no, patient_id, patient_name, pathology_no,
                       application_no, report_date, final_diagnosis, report_summary, raw_payload, source_department_name,
                       source_doctor_name, attachment_url, created_at, updated_at
                from historical_reports
                where source_system = :sourceSystem and external_report_no = :externalReportNo
                """, new MapSqlParameterSource()
                .addValue("sourceSystem", sourceSystem)
                .addValue("externalReportNo", externalReportNo), this::mapHistoricalReport);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    void insertHistoricalReport(M6HistoricalReportRows.CreateHistoricalReportRow row) {
        jdbcTemplate.update("""
            insert into historical_reports
                (id, import_job_id, source_system, external_report_no, patient_id, patient_name, pathology_no,
                 application_no, report_date, final_diagnosis, report_summary, raw_payload, source_department_name,
                 source_doctor_name, attachment_url, created_at, updated_at)
            values
                (:id, :importJobId, :sourceSystem, :externalReportNo, :patientId, :patientName, :pathologyNo,
                 :applicationNo, :reportDate, :finalDiagnosis, :reportSummary, :rawPayload, :sourceDepartmentName,
                 :sourceDoctorName, :attachmentUrl, :createdAt, :updatedAt)
            """, toHistoricalReportParams(row));
    }

    void updateHistoricalReport(M6HistoricalReportRows.HistoricalReportRow row) {
        jdbcTemplate.update("""
            update historical_reports
            set import_job_id = :importJobId,
                patient_id = :patientId,
                patient_name = :patientName,
                pathology_no = :pathologyNo,
                application_no = :applicationNo,
                report_date = :reportDate,
                final_diagnosis = :finalDiagnosis,
                report_summary = :reportSummary,
                raw_payload = :rawPayload,
                source_department_name = :sourceDepartmentName,
                source_doctor_name = :sourceDoctorName,
                attachment_url = :attachmentUrl,
                updated_at = :updatedAt
            where id = :id
            """, toHistoricalReportParams(new M6HistoricalReportRows.CreateHistoricalReportRow(
            row.id(), row.importJobId(), row.sourceSystem(), row.externalReportNo(), row.patientId(), row.patientName(),
            row.pathologyNo(), row.applicationNo(), row.reportDate(), row.finalDiagnosis(), row.reportSummary(), row.rawPayload(),
            row.sourceDepartmentName(), row.sourceDoctorName(), row.attachmentUrl(), row.createdAt(), row.updatedAt())));
    }

    void deleteHistoricalReportVersions(String historicalReportId) {
        jdbcTemplate.update("delete from historical_report_versions where historical_report_id = :historicalReportId",
            Map.of("historicalReportId", historicalReportId));
    }

    void insertHistoricalReportVersion(M6HistoricalReportRows.CreateHistoricalReportVersionRow row) {
        jdbcTemplate.update("""
            insert into historical_report_versions
                (id, historical_report_id, version_no, final_diagnosis, report_summary, raw_payload, created_at, updated_at)
            values
                (:id, :historicalReportId, :versionNo, :finalDiagnosis, :reportSummary, :rawPayload, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("historicalReportId", row.historicalReportId())
            .addValue("versionNo", row.versionNo())
            .addValue("finalDiagnosis", row.finalDiagnosis())
            .addValue("reportSummary", row.reportSummary())
            .addValue("rawPayload", row.rawPayload())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt()));
    }

    List<M6HistoricalReportRows.HistoricalReportRow> findHistoricalReports(String sourceSystem,
                                                                           String patientId,
                                                                           String pathologyNo,
                                                                           String applicationNo,
                                                                           String externalReportNo,
                                                                           LocalDateTime from,
                                                                           LocalDateTime to) {
        return jdbcTemplate.query("""
            select id, import_job_id, source_system, external_report_no, patient_id, patient_name, pathology_no,
                   application_no, report_date, final_diagnosis, report_summary, raw_payload, source_department_name,
                   source_doctor_name, attachment_url, created_at, updated_at
            from historical_reports
            where (:sourceSystem is null or source_system = :sourceSystem)
              and (:patientId is null or patient_id = :patientId)
              and (:pathologyNo is null or pathology_no = :pathologyNo)
              and (:applicationNo is null or application_no = :applicationNo)
              and (:externalReportNo is null or external_report_no = :externalReportNo)
              and (:fromTime is null or report_date >= :fromTime)
              and (:toTime is null or report_date <= :toTime)
            order by report_date desc nulls last, id desc
            """, new MapSqlParameterSource()
            .addValue("sourceSystem", blankToNull(sourceSystem))
            .addValue("patientId", blankToNull(patientId))
            .addValue("pathologyNo", blankToNull(pathologyNo))
            .addValue("applicationNo", blankToNull(applicationNo))
            .addValue("externalReportNo", blankToNull(externalReportNo))
            .addValue("fromTime", from)
            .addValue("toTime", to), this::mapHistoricalReport);
    }

    List<M6HistoricalReportRows.HistoricalReportVersionRow> findHistoricalReportVersions(String historicalReportId) {
        return jdbcTemplate.query("""
            select id, historical_report_id, version_no, final_diagnosis, report_summary, raw_payload, created_at, updated_at
            from historical_report_versions
            where historical_report_id = :historicalReportId
            order by version_no
            """, Map.of("historicalReportId", historicalReportId), this::mapHistoricalReportVersion);
    }

    long countHistoricalImportJobsByStatus(String importStatus) {
        Long value = jdbcTemplate.queryForObject("""
            select count(*)
            from historical_import_jobs
            where import_status = :importStatus
            """, Map.of("importStatus", importStatus), Long.class);
        return value == null ? 0L : value;
    }

    private MapSqlParameterSource toHistoricalJobParams(M6HistoricalReportRows.CreateHistoricalImportJobRow row) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("sourceSystem", row.sourceSystem())
            .addValue("patientId", row.patientId())
            .addValue("pathologyNo", row.pathologyNo())
            .addValue("applicationNo", row.applicationNo())
            .addValue("importStatus", row.importStatus())
            .addValue("requestedByUserId", row.requestedByUserId())
            .addValue("requestedByName", row.requestedByName())
            .addValue("totalCount", row.totalCount())
            .addValue("successCount", row.successCount())
            .addValue("failureCount", row.failureCount())
            .addValue("requestedAt", row.requestedAt())
            .addValue("completedAt", row.completedAt())
            .addValue("lastErrorMessage", row.lastErrorMessage())
            .addValue("remarks", row.remarks())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private MapSqlParameterSource toHistoricalReportParams(M6HistoricalReportRows.CreateHistoricalReportRow row) {
        return new MapSqlParameterSource()
            .addValue("id", row.id())
            .addValue("importJobId", row.importJobId())
            .addValue("sourceSystem", row.sourceSystem())
            .addValue("externalReportNo", row.externalReportNo())
            .addValue("patientId", row.patientId())
            .addValue("patientName", row.patientName())
            .addValue("pathologyNo", row.pathologyNo())
            .addValue("applicationNo", row.applicationNo())
            .addValue("reportDate", row.reportDate())
            .addValue("finalDiagnosis", row.finalDiagnosis())
            .addValue("reportSummary", row.reportSummary())
            .addValue("rawPayload", row.rawPayload())
            .addValue("sourceDepartmentName", row.sourceDepartmentName())
            .addValue("sourceDoctorName", row.sourceDoctorName())
            .addValue("attachmentUrl", row.attachmentUrl())
            .addValue("createdAt", row.createdAt())
            .addValue("updatedAt", row.updatedAt());
    }

    private M6HistoricalReportRows.HistoricalImportJobRow mapHistoricalImportJob(ResultSet rs, int rowNum) throws SQLException {
        return new M6HistoricalReportRows.HistoricalImportJobRow(
            rs.getString("id"),
            rs.getString("source_system"),
            rs.getString("patient_id"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("import_status"),
            rs.getString("requested_by_user_id"),
            rs.getString("requested_by_name"),
            rs.getInt("total_count"),
            rs.getInt("success_count"),
            rs.getInt("failure_count"),
            toLocalDateTime(rs.getTimestamp("requested_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            rs.getString("last_error_message"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private M6HistoricalReportRows.HistoricalReportRow mapHistoricalReport(ResultSet rs, int rowNum) throws SQLException {
        return new M6HistoricalReportRows.HistoricalReportRow(
            rs.getString("id"),
            rs.getString("import_job_id"),
            rs.getString("source_system"),
            rs.getString("external_report_no"),
            rs.getString("patient_id"),
            rs.getString("patient_name"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            toLocalDateTime(rs.getTimestamp("report_date")),
            rs.getString("final_diagnosis"),
            rs.getString("report_summary"),
            rs.getString("raw_payload"),
            rs.getString("source_department_name"),
            rs.getString("source_doctor_name"),
            rs.getString("attachment_url"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private M6HistoricalReportRows.HistoricalReportVersionRow mapHistoricalReportVersion(ResultSet rs, int rowNum) throws SQLException {
        return new M6HistoricalReportRows.HistoricalReportVersionRow(
            rs.getString("id"),
            rs.getString("historical_report_id"),
            rs.getInt("version_no"),
            rs.getString("final_diagnosis"),
            rs.getString("report_summary"),
            rs.getString("raw_payload"),
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
