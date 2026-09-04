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

    private static final String PATHOLOGY_REPORT_COLUMNS = """
        id, case_id, task_id, report_no, pathology_no, report_scope, report_seq, report_status, version_no,
        specimen_type, patient_name, submitting_department_id, submitting_department_name, report_date,
        gross_exam, microscopic_exam, clinical_diagnosis, final_diagnosis, submitted_at,
        reviewer_user_id, reviewer_name, reviewed_at, signed_by_user_id, signed_by_name, signed_at,
        published_at, rich_text_content, render_snapshot, remarks, created_at, updated_at
        """;
    private static final String REPORT_VERSION_COLUMNS = """
        id, report_id, case_id, report_scope, report_seq, version_no, version_status,
        final_diagnosis_snapshot, content_snapshot, render_snapshot, artifact_id,
        signed_by_user_id, signed_by_name, signed_at, created_at, print_status, printed_at,
        delivery_status, planned_issue_at, delivery_schedule_status, issued_at, recalled_at
        """;
    private static final String REPORT_VERSION_ARTIFACT_COLUMNS = """
        id, report_id, version_no, artifact_format, file_name, storage_key, content_type,
        byte_size, sha256, generated_at
        """;
    private static final String REPORT_RENDER_ASSET_COLUMNS = """
        id, case_id, file_name, storage_key, content_type, byte_size, sha256, created_at
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcPathologyReportStore(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<DiagnosticReportRepository.PathologyReport> findCurrentReportByCaseIdAndScope(String caseId, String reportScope) {
        List<DiagnosticReportRepository.PathologyReport> rows = jdbcTemplate.query(
            "select " + PATHOLOGY_REPORT_COLUMNS + """
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
        List<DiagnosticReportRepository.PathologyReport> rows = jdbcTemplate.query(
            "select " + PATHOLOGY_REPORT_COLUMNS + """
            from pathology_reports
            where id = :reportId
            """, Map.of("reportId", reportId), this::mapPathologyReport);
        return rows.stream().findFirst();
    }

    void lockPathologyReport(String reportId) {
        jdbcTemplate.queryForList("select id from pathology_reports where id = :reportId for update",
            Map.of("reportId", reportId), String.class);
    }

    List<DiagnosticReportRepository.PathologyReport> findPathologyReportsByCaseId(String caseId) {
        return jdbcTemplate.query(
            "select " + PATHOLOGY_REPORT_COLUMNS + """
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
                 gross_exam, microscopic_exam, clinical_diagnosis, final_diagnosis, rich_text_content, render_snapshot, remarks,
                 created_at, updated_at)
            values
                (:id, :caseId, :taskId, :reportNo, :pathologyNo, :reportScope, :reportSeq, :reportStatus, :versionNo,
                 :specimenType, :patientName, :submittingDepartmentId, :submittingDepartmentName, :reportDate,
                 :grossExam, :microscopicExam, :clinicalDiagnosis, :finalDiagnosis, :richTextContent, :renderSnapshot, :remarks,
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
            .addValue("renderSnapshot", command.renderSnapshot())
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
                render_snapshot = :renderSnapshot,
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
            .addValue("renderSnapshot", command.renderSnapshot())
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
                 content_snapshot, render_snapshot, artifact_id, signed_by_user_id, signed_by_name, signed_at, created_at)
            values
                (:id, :reportId, :caseId, :reportScope, :reportSeq, :versionNo, :versionStatus, :finalDiagnosisSnapshot,
                 :contentSnapshot, :renderSnapshot, :artifactId, :signedByUserId, :signedByName, :signedAt, :createdAt)
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
            .addValue("renderSnapshot", command.renderSnapshot())
            .addValue("artifactId", command.artifactId())
            .addValue("signedByUserId", command.signedByUserId())
            .addValue("signedByName", command.signedByName())
            .addValue("signedAt", command.signedAt())
            .addValue("createdAt", command.createdAt()));
    }

    void insertReportVersionArtifact(DiagnosticReportRepository.CreateReportVersionArtifactCommand command) {
        jdbcTemplate.update("""
            insert into report_version_artifacts
                (id, report_id, version_no, artifact_format, file_name, storage_key, content_type,
                 byte_size, sha256, generated_at)
            values
                (:id, :reportId, :versionNo, :artifactFormat, :fileName, :storageKey, :contentType,
                 :byteSize, :sha256, :generatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("reportId", command.reportId())
            .addValue("versionNo", command.versionNo())
            .addValue("artifactFormat", command.artifactFormat())
            .addValue("fileName", command.fileName())
            .addValue("storageKey", command.storageKey())
            .addValue("contentType", command.contentType())
            .addValue("byteSize", command.byteSize())
            .addValue("sha256", command.sha256())
            .addValue("generatedAt", command.generatedAt()));
    }

    void updateReportVersionArtifact(DiagnosticReportRepository.CreateReportVersionArtifactCommand command) {
        jdbcTemplate.update("""
            update report_version_artifacts
            set file_name = :fileName,
                storage_key = :storageKey,
                content_type = :contentType,
                byte_size = :byteSize,
                sha256 = :sha256,
                generated_at = :generatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("fileName", command.fileName())
            .addValue("storageKey", command.storageKey())
            .addValue("contentType", command.contentType())
            .addValue("byteSize", command.byteSize())
            .addValue("sha256", command.sha256())
            .addValue("generatedAt", command.generatedAt()));
    }

    Optional<DiagnosticReportRepository.ReportVersionArtifact> findReportVersionArtifact(
        String reportId,
        int versionNo,
        String artifactFormat
    ) {
        List<DiagnosticReportRepository.ReportVersionArtifact> rows = jdbcTemplate.query(
            "select " + REPORT_VERSION_ARTIFACT_COLUMNS + """
            from report_version_artifacts
            where report_id = :reportId
              and version_no = :versionNo
              and artifact_format = :artifactFormat
            order by generated_at desc, id desc
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("versionNo", versionNo)
            .addValue("artifactFormat", artifactFormat), this::mapReportVersionArtifact);
        return rows.stream().findFirst();
    }

    Optional<DiagnosticReportRepository.ReportVersionArtifact> findReportVersionArtifactById(String artifactId) {
        List<DiagnosticReportRepository.ReportVersionArtifact> rows = jdbcTemplate.query(
            "select " + REPORT_VERSION_ARTIFACT_COLUMNS + """
            from report_version_artifacts
            where id = :artifactId
            """, Map.of("artifactId", artifactId), this::mapReportVersionArtifact);
        return rows.stream().findFirst();
    }

    List<DiagnosticReportRepository.ReportVersionArtifact> findReportVersionArtifacts(
        String reportId,
        String artifactFormat
    ) {
        return jdbcTemplate.query(
            "select " + REPORT_VERSION_ARTIFACT_COLUMNS + """
            from report_version_artifacts
            where report_id = :reportId
              and artifact_format = :artifactFormat
            order by version_no desc, generated_at desc
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("artifactFormat", artifactFormat), this::mapReportVersionArtifact);
    }

    Optional<DiagnosticReportRepository.ReportVersion> findLatestFormalReportVersion(String reportId, int versionNo) {
        List<DiagnosticReportRepository.ReportVersion> rows = jdbcTemplate.query(
            "select " + REPORT_VERSION_COLUMNS + """
            from report_versions
            where report_id = :reportId
              and version_no = :versionNo
              and version_status in ('SIGNED', 'PUBLISHED')
            order by case when version_status = 'PUBLISHED' then 0 else 1 end, created_at desc
            fetch first 1 row only
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("versionNo", versionNo), this::mapReportVersion);
        return rows.stream().findFirst();
    }

    void updateReportVersionArtifactId(String reportId, int versionNo, String artifactId) {
        jdbcTemplate.update("""
            update report_versions
            set artifact_id = :artifactId
            where report_id = :reportId
              and version_no = :versionNo
              and version_status in ('SIGNED', 'PUBLISHED')
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("versionNo", versionNo)
            .addValue("artifactId", artifactId));
    }

    boolean existsReportVersionArtifactByStorageKey(String storageKey) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*)
            from report_version_artifacts
            where storage_key = :storageKey
            """, Map.of("storageKey", storageKey), Integer.class);
        return count != null && count > 0;
    }

    void insertReportRenderAsset(DiagnosticReportRepository.CreateReportRenderAssetCommand command) {
        jdbcTemplate.update("""
            insert into report_render_assets
                (id, case_id, file_name, storage_key, content_type, byte_size, sha256, created_at)
            values
                (:id, :caseId, :fileName, :storageKey, :contentType, :byteSize, :sha256, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("fileName", command.fileName())
            .addValue("storageKey", command.storageKey())
            .addValue("contentType", command.contentType())
            .addValue("byteSize", command.byteSize())
            .addValue("sha256", command.sha256())
            .addValue("createdAt", command.createdAt()));
    }

    Optional<DiagnosticReportRepository.ReportRenderAsset> findReportRenderAssetById(String assetId) {
        List<DiagnosticReportRepository.ReportRenderAsset> rows = jdbcTemplate.query(
            "select " + REPORT_RENDER_ASSET_COLUMNS + """
            from report_render_assets where id = :assetId
            """, Map.of("assetId", assetId), this::mapReportRenderAsset);
        return rows.stream().findFirst();
    }

    boolean existsReportRenderAssetByStorageKey(String storageKey) {
        Integer count = jdbcTemplate.queryForObject("""
            select count(*)
            from report_render_assets
            where storage_key = :storageKey
            """, Map.of("storageKey", storageKey), Integer.class);
        return count != null && count > 0;
    }

    void deleteReportRenderAsset(String assetId) {
        jdbcTemplate.update("delete from report_render_assets where id = :assetId", Map.of("assetId", assetId));
    }

    Optional<DiagnosticReportRepository.ReportVersion> findReportVersionById(String versionId) {
        List<DiagnosticReportRepository.ReportVersion> rows = jdbcTemplate.query(
            "select " + REPORT_VERSION_COLUMNS + """
            from report_versions
            where id = :versionId
            """, Map.of("versionId", versionId), this::mapReportVersion);
        return rows.stream().findFirst();
    }

    List<DiagnosticReportRepository.ReportVersion> findReportVersionsByCaseId(String caseId) {
        return jdbcTemplate.query(
            "select " + REPORT_VERSION_COLUMNS + """
            from report_versions
            where case_id = :caseId
            order by version_no asc, created_at asc
            """, Map.of("caseId", caseId), this::mapReportVersion);
    }

    List<DiagnosticReportRepository.ReportVersion> findFormalReportVersionsByCaseId(String caseId) {
        return jdbcTemplate.query(
            "select " + REPORT_VERSION_COLUMNS + """
            from report_versions
            where case_id = :caseId
              and version_status in ('SIGNED', 'PUBLISHED')
            order by coalesce(signed_at, created_at) desc, created_at desc, version_no desc
            """, Map.of("caseId", caseId), this::mapReportVersion);
    }

    List<DiagnosticReportRepository.ReportVersion> findScheduledReportVersionsDue(LocalDateTime scheduledBeforeOrAt) {
        return jdbcTemplate.query(
            "select " + REPORT_VERSION_COLUMNS + """
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
            rs.getString("render_snapshot"),
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
            rs.getString("render_snapshot"),
            rs.getString("artifact_id"),
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

    private DiagnosticReportRepository.ReportVersionArtifact mapReportVersionArtifact(ResultSet rs, int rowNum) throws SQLException {
        return new DiagnosticReportRepository.ReportVersionArtifact(
            rs.getString("id"),
            rs.getString("report_id"),
            rs.getInt("version_no"),
            rs.getString("artifact_format"),
            rs.getString("file_name"),
            rs.getString("storage_key"),
            rs.getString("content_type"),
            rs.getLong("byte_size"),
            rs.getString("sha256"),
            toLocalDateTime(rs.getTimestamp("generated_at")));
    }

    private DiagnosticReportRepository.ReportRenderAsset mapReportRenderAsset(ResultSet rs, int rowNum) throws SQLException {
        return new DiagnosticReportRepository.ReportRenderAsset(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("file_name"),
            rs.getString("storage_key"),
            rs.getString("content_type"),
            rs.getLong("byte_size"),
            rs.getString("sha256"),
            toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
