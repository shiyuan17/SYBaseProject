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

final class JdbcPathologyReportStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcPathologyReportStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<DiagnosticReportRepository.PathologyReport> findCurrentReportByCaseIdAndScope(String caseId, String reportScope) {
        List<DiagnosticReportRepository.PathologyReport> rows = jdbcTemplate.query("""
            select *
            from pathology_reports
            where case_id = :caseId
              and report_scope = :reportScope
            order by report_seq desc, created_at desc
            fetch first 1 row only
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("reportScope", reportScope), this::mapPathologyReport);
        return rows.stream().findFirst();
    }

    Optional<DiagnosticReportRepository.PathologyReport> findPathologyReportById(String reportId) {
        List<DiagnosticReportRepository.PathologyReport> rows = jdbcTemplate.query("""
            select *
            from pathology_reports
            where id = :reportId
            """, Map.of("reportId", reportId), this::mapPathologyReport);
        return rows.stream().findFirst();
    }

    List<DiagnosticReportRepository.PathologyReport> findPathologyReportsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from pathology_reports
            where case_id = :caseId
            order by coalesce(published_at, signed_at, reviewed_at, submitted_at, created_at) desc,
                     version_no desc,
                     created_at desc
            """, Map.of("caseId", caseId), this::mapPathologyReport);
    }

    void insertPathologyReport(DiagnosticReportRepository.CreatePathologyReportCommand command) {
        jdbcTemplate.update("""
            insert into pathology_reports
                (id, case_id, task_id, report_no, pathology_no, report_scope, report_seq, report_status, version_no,
                 specimen_type, patient_name, submitting_department_id, submitting_department_name, report_date,
                 gross_exam, microscopic_exam, clinical_diagnosis, final_diagnosis, rich_text_content, remarks,
                 created_at, updated_at)
            values
                (:id, :caseId, :taskId, :reportNo, :pathologyNo, :reportScope, :reportSeq, :reportStatus, :versionNo,
                 :specimenType, :patientName, :submittingDepartmentId, :submittingDepartmentName, :reportDate,
                 :grossExam, :microscopicExam, :clinicalDiagnosis, :finalDiagnosis, :richTextContent, :remarks,
                 :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("taskId", command.taskId())
            .addValue("reportNo", command.reportNo())
            .addValue("pathologyNo", command.pathologyNo())
            .addValue("reportScope", command.reportScope())
            .addValue("reportSeq", command.reportSeq())
            .addValue("reportStatus", command.reportStatus())
            .addValue("versionNo", command.versionNo())
            .addValue("specimenType", command.specimenType())
            .addValue("patientName", command.patientName())
            .addValue("submittingDepartmentId", command.submittingDepartmentId())
            .addValue("submittingDepartmentName", command.submittingDepartmentName())
            .addValue("reportDate", command.reportDate())
            .addValue("grossExam", command.grossExam())
            .addValue("microscopicExam", command.microscopicExam())
            .addValue("clinicalDiagnosis", command.clinicalDiagnosis())
            .addValue("finalDiagnosis", command.finalDiagnosis())
            .addValue("richTextContent", command.richTextContent())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.createdAt()));
    }

    void updatePathologyReportDraft(DiagnosticReportRepository.UpdatePathologyReportDraftCommand command) {
        jdbcTemplate.update("""
            update pathology_reports
            set gross_exam = :grossExam,
                microscopic_exam = :microscopicExam,
                clinical_diagnosis = :clinicalDiagnosis,
                final_diagnosis = :finalDiagnosis,
                rich_text_content = :richTextContent,
                remarks = coalesce(:remarks, remarks),
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", command.reportId())
            .addValue("grossExam", command.grossExam())
            .addValue("microscopicExam", command.microscopicExam())
            .addValue("clinicalDiagnosis", command.clinicalDiagnosis())
            .addValue("finalDiagnosis", command.finalDiagnosis())
            .addValue("richTextContent", command.richTextContent())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }

    void submitPathologyReport(String reportId, String remarks, LocalDateTime submittedAt) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'SUBMITTED',
                submitted_at = :submittedAt,
                remarks = coalesce(:remarks, remarks),
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("submittedAt", submittedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", submittedAt));
    }

    void reviewPathologyReport(String reportId,
                               String reviewerUserId,
                               String reviewerName,
                               String remarks,
                               LocalDateTime reviewedAt) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'REVIEWED',
                reviewer_user_id = :reviewerUserId,
                reviewer_name = :reviewerName,
                reviewed_at = :reviewedAt,
                remarks = coalesce(:remarks, remarks),
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("reviewerUserId", reviewerUserId)
            .addValue("reviewerName", reviewerName)
            .addValue("reviewedAt", reviewedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", reviewedAt));
    }

    void rejectPathologyReport(String reportId, String rejectReason) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'DRAFT',
                remarks = coalesce(:remarks, remarks),
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("remarks", rejectReason)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void resetPathologyReportForRevision(String reportId,
                                         int versionNo,
                                         String remarks,
                                         LocalDateTime updatedAt) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'DRAFT',
                version_no = :versionNo,
                submitted_at = null,
                reviewer_user_id = null,
                reviewer_name = null,
                reviewed_at = null,
                signed_by_user_id = null,
                signed_by_name = null,
                signed_at = null,
                published_at = null,
                remarks = coalesce(:remarks, remarks),
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("versionNo", versionNo)
            .addValue("remarks", remarks)
            .addValue("updatedAt", updatedAt));
    }

    void signPathologyReport(String reportId,
                             String signedByUserId,
                             String signedByName,
                             String remarks,
                             LocalDateTime signedAt) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'SIGNED',
                signed_by_user_id = :signedByUserId,
                signed_by_name = :signedByName,
                signed_at = :signedAt,
                remarks = coalesce(:remarks, remarks),
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("signedByUserId", signedByUserId)
            .addValue("signedByName", signedByName)
            .addValue("signedAt", signedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", signedAt));
    }

    void publishPathologyReport(String reportId, String remarks, LocalDateTime publishedAt) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'PUBLISHED',
                published_at = :publishedAt,
                remarks = coalesce(:remarks, remarks),
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("publishedAt", publishedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", publishedAt));
    }

    void insertReportVersion(DiagnosticReportRepository.CreateReportVersionCommand command) {
        jdbcTemplate.update("""
            insert into report_versions
                (id, report_id, case_id, report_scope, report_seq, version_no, version_status, final_diagnosis_snapshot,
                 content_snapshot, signed_by_user_id, signed_by_name, signed_at, created_at)
            values
                (:id, :reportId, :caseId, :reportScope, :reportSeq, :versionNo, :versionStatus, :finalDiagnosisSnapshot,
                 :contentSnapshot, :signedByUserId, :signedByName, :signedAt, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("reportId", command.reportId())
            .addValue("caseId", command.caseId())
            .addValue("reportScope", command.reportScope())
            .addValue("reportSeq", command.reportSeq())
            .addValue("versionNo", command.versionNo())
            .addValue("versionStatus", command.versionStatus())
            .addValue("finalDiagnosisSnapshot", command.finalDiagnosisSnapshot())
            .addValue("contentSnapshot", command.contentSnapshot())
            .addValue("signedByUserId", command.signedByUserId())
            .addValue("signedByName", command.signedByName())
            .addValue("signedAt", command.signedAt())
            .addValue("createdAt", command.createdAt()));
    }

    Optional<DiagnosticReportRepository.ReportVersion> findReportVersionById(String versionId) {
        List<DiagnosticReportRepository.ReportVersion> rows = jdbcTemplate.query("""
            select *
            from report_versions
            where id = :versionId
            """, Map.of("versionId", versionId), this::mapReportVersion);
        return rows.stream().findFirst();
    }

    List<DiagnosticReportRepository.ReportVersion> findReportVersionsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from report_versions
            where case_id = :caseId
            order by version_no asc, created_at asc
            """, Map.of("caseId", caseId), this::mapReportVersion);
    }

    List<DiagnosticReportRepository.ReportVersion> findFormalReportVersionsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from report_versions
            where case_id = :caseId
              and version_status in ('SIGNED', 'PUBLISHED')
            order by coalesce(signed_at, created_at) desc, created_at desc, version_no desc
            """, Map.of("caseId", caseId), this::mapReportVersion);
    }

    List<DiagnosticReportRepository.ReportVersion> findScheduledReportVersionsDue(LocalDateTime scheduledBeforeOrAt) {
        return jdbcTemplate.query("""
            select *
            from report_versions
            where delivery_schedule_status = 'SCHEDULED'
              and planned_issue_at is not null
              and planned_issue_at <= :scheduledBeforeOrAt
            order by planned_issue_at asc, created_at asc
            """, Map.of("scheduledBeforeOrAt", scheduledBeforeOrAt), this::mapReportVersion);
    }

    void markReportVersionsPrinted(List<String> versionIds, LocalDateTime printedAt) {
        jdbcTemplate.update("""
            update report_versions
            set print_status = 'PRINTED',
                printed_at = :printedAt
            where id in (:versionIds)
            """, new MapSqlParameterSource()
            .addValue("versionIds", versionIds)
            .addValue("printedAt", printedAt));
    }

    void markReportVersionsIssued(List<String> versionIds, LocalDateTime issuedAt) {
        jdbcTemplate.update("""
            update report_versions
            set delivery_status = 'ISSUED',
                delivery_schedule_status = 'EXECUTED',
                issued_at = :issuedAt
            where id in (:versionIds)
            """, new MapSqlParameterSource()
            .addValue("versionIds", versionIds)
            .addValue("issuedAt", issuedAt));
    }

    void scheduleReportVersionsIssue(List<String> versionIds, LocalDateTime plannedIssueAt) {
        jdbcTemplate.update("""
            update report_versions
            set delivery_status = 'PENDING',
                delivery_schedule_status = 'SCHEDULED',
                planned_issue_at = :plannedIssueAt
            where id in (:versionIds)
            """, new MapSqlParameterSource()
            .addValue("versionIds", versionIds)
            .addValue("plannedIssueAt", plannedIssueAt));
    }

    void markReportVersionsRecalled(List<String> versionIds, LocalDateTime recalledAt) {
        jdbcTemplate.update("""
            update report_versions
            set delivery_status = 'RECALLED',
                recalled_at = :recalledAt
            where id in (:versionIds)
            """, new MapSqlParameterSource()
            .addValue("versionIds", versionIds)
            .addValue("recalledAt", recalledAt));
    }

    private DiagnosticReportRepository.PathologyReport mapPathologyReport(ResultSet rs, int rowNum) throws SQLException {
        return new DiagnosticReportRepository.PathologyReport(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("task_id"),
            rs.getString("report_no"),
            rs.getString("pathology_no"),
            rs.getString("report_scope"),
            rs.getInt("report_seq"),
            rs.getString("report_status"),
            rs.getInt("version_no"),
            rs.getString("specimen_type"),
            rs.getString("patient_name"),
            rs.getString("submitting_department_id"),
            rs.getString("submitting_department_name"),
            toLocalDateTime(rs.getTimestamp("report_date")),
            rs.getString("gross_exam"),
            rs.getString("microscopic_exam"),
            rs.getString("clinical_diagnosis"),
            rs.getString("final_diagnosis"),
            toLocalDateTime(rs.getTimestamp("submitted_at")),
            rs.getString("reviewer_user_id"),
            rs.getString("reviewer_name"),
            toLocalDateTime(rs.getTimestamp("reviewed_at")),
            rs.getString("signed_by_user_id"),
            rs.getString("signed_by_name"),
            toLocalDateTime(rs.getTimestamp("signed_at")),
            toLocalDateTime(rs.getTimestamp("published_at")),
            rs.getString("rich_text_content"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private DiagnosticReportRepository.ReportVersion mapReportVersion(ResultSet rs, int rowNum) throws SQLException {
        return new DiagnosticReportRepository.ReportVersion(
            rs.getString("id"),
            rs.getString("report_id"),
            rs.getString("case_id"),
            rs.getString("report_scope"),
            rs.getInt("report_seq"),
            rs.getInt("version_no"),
            rs.getString("version_status"),
            rs.getString("final_diagnosis_snapshot"),
            rs.getString("content_snapshot"),
            rs.getString("signed_by_user_id"),
            rs.getString("signed_by_name"),
            toLocalDateTime(rs.getTimestamp("signed_at")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            rs.getString("print_status"),
            toLocalDateTime(rs.getTimestamp("printed_at")),
            rs.getString("delivery_status"),
            toLocalDateTime(rs.getTimestamp("planned_issue_at")),
            rs.getString("delivery_schedule_status"),
            toLocalDateTime(rs.getTimestamp("issued_at")),
            toLocalDateTime(rs.getTimestamp("recalled_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
