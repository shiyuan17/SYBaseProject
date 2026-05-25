package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.DiagnosticReportRepository;
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
public class JdbcDiagnosticReportRepository implements DiagnosticReportRepository {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "ASSIGNED", "ACCEPTED", "IN_PROGRESS");
    private static final String ROLE_M4_DIAGNOSIS = "M4_DIAGNOSIS";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcDiagnosticReportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<DiagnosticTask> findDiagnosticTaskById(String taskId) {
        List<DiagnosticTask> rows = jdbcTemplate.query(diagnosticTaskSelectSql() + """
            where dt.id = :taskId
            """, Map.of("taskId", taskId), this::mapDiagnosticTask);
        return rows.stream().findFirst();
    }

    @Override
    public List<DiagnosticTask> findDiagnosticTasksByCaseId(String caseId) {
        return jdbcTemplate.query(diagnosticTaskSelectSql() + """
            where dt.case_id = :caseId
            order by dt.created_at asc, dt.id asc
            """, Map.of("caseId", caseId), this::mapDiagnosticTask);
    }

    @Override
    public List<DiagnosticTask> findActiveDiagnosticTasksByCaseIdAndType(String caseId, String taskType) {
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

    @Override
    public PagedDiagnosticTasks findDiagnosticTasks(PendingDiagnosticTaskQuery query) {
        String where = " where 1 = 1 " + buildDiagnosticTaskFilters(query);
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from diagnostic_tasks dt
            join pathology_cases pc on pc.id = dt.case_id
            """ + where, diagnosticTaskFilterParams(query), Long.class);
        List<DiagnosticTask> items = jdbcTemplate.query(diagnosticTaskSelectSql() + where + """
            
            order by dt.created_at asc, dt.id asc
            offset :offset rows fetch next :limit rows only
            """, diagnosticTaskPageParams(query), this::mapDiagnosticTask);
        return new PagedDiagnosticTasks(items, total == null ? 0 : total);
    }

    @Override
    public void insertDiagnosticTask(CreateDiagnosticTaskCommand command) {
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

    @Override
    public void assignDiagnosticTask(AssignDiagnosticTaskCommand command) {
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

    @Override
    public void acceptDiagnosticTask(String taskId, String remarks, LocalDateTime acceptedAt) {
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

    @Override
    public void startDiagnosticTask(String taskId, String remarks, LocalDateTime startedAt) {
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

    @Override
    public void markDiagnosticTaskSubmitted(String taskId, String remarks, LocalDateTime primaryDiagnosedAt) {
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

    @Override
    public void markDiagnosticTaskReviewed(String taskId,
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

    @Override
    public void revertDiagnosticTaskToInProgress(String taskId, String remarks) {
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

    @Override
    public void completeDiagnosticTask(String taskId, String remarks, LocalDateTime completedAt) {
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

    @Override
    public Optional<PathologyReport> findCurrentReportByCaseIdAndScope(String caseId, String reportScope) {
        List<PathologyReport> rows = jdbcTemplate.query("""
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

    @Override
    public Optional<PathologyReport> findPathologyReportById(String reportId) {
        List<PathologyReport> rows = jdbcTemplate.query("""
            select *
            from pathology_reports
            where id = :reportId
            """, Map.of("reportId", reportId), this::mapPathologyReport);
        return rows.stream().findFirst();
    }

    @Override
    public void insertPathologyReport(CreatePathologyReportCommand command) {
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

    @Override
    public void updatePathologyReportDraft(UpdatePathologyReportDraftCommand command) {
        jdbcTemplate.update("""
            update pathology_reports
            set gross_exam = :grossExam,
                microscopic_exam = :microscopicExam,
                clinical_diagnosis = :clinicalDiagnosis,
                final_diagnosis = :finalDiagnosis,
                rich_text_content = :richTextContent,
                remarks = :remarks,
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

    @Override
    public void submitPathologyReport(String reportId, String remarks, LocalDateTime submittedAt) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'SUBMITTED',
                submitted_at = :submittedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("submittedAt", submittedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", submittedAt));
    }

    @Override
    public void reviewPathologyReport(String reportId,
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
                remarks = :remarks,
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

    @Override
    public void rejectPathologyReport(String reportId, String rejectReason) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'DRAFT',
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("remarks", rejectReason)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    @Override
    public void resetPathologyReportForRevision(String reportId,
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
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("versionNo", versionNo)
            .addValue("remarks", remarks)
            .addValue("updatedAt", updatedAt));
    }

    @Override
    public void signPathologyReport(String reportId,
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
                remarks = :remarks,
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

    @Override
    public void publishPathologyReport(String reportId, String remarks, LocalDateTime publishedAt) {
        jdbcTemplate.update("""
            update pathology_reports
            set report_status = 'PUBLISHED',
                published_at = :publishedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :reportId
            """, new MapSqlParameterSource()
            .addValue("reportId", reportId)
            .addValue("publishedAt", publishedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", publishedAt));
    }

    @Override
    public void insertReportVersion(CreateReportVersionCommand command) {
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

    @Override
    public List<ReportVersion> findReportVersionsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from report_versions
            where case_id = :caseId
            order by version_no asc, created_at asc
            """, Map.of("caseId", caseId), this::mapReportVersion);
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

    private String buildDiagnosticTaskFilters(PendingDiagnosticTaskQuery query) {
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

    private MapSqlParameterSource diagnosticTaskFilterParams(PendingDiagnosticTaskQuery query) {
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

    private MapSqlParameterSource diagnosticTaskPageParams(PendingDiagnosticTaskQuery query) {
        return diagnosticTaskFilterParams(query)
            .addValue("limit", query.size())
            .addValue("offset", Math.max(query.page() - 1, 0) * query.size());
    }

    private DiagnosticTask mapDiagnosticTask(ResultSet rs, int rowNum) throws SQLException {
        return new DiagnosticTask(
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

    private PathologyReport mapPathologyReport(ResultSet rs, int rowNum) throws SQLException {
        return new PathologyReport(
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

    private ReportVersion mapReportVersion(ResultSet rs, int rowNum) throws SQLException {
        return new ReportVersion(
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
            toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
