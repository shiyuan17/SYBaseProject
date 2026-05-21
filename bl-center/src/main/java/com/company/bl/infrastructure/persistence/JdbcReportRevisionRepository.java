package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ReportRevisionRepository;
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
public class JdbcReportRevisionRepository implements ReportRevisionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcReportRevisionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertRevisionRequest(CreateReportRevisionRequestCommand command) {
        jdbcTemplate.update("""
            insert into report_revision_requests
                (id, case_id, report_id, current_version_no, request_status, request_reason, requested_by_user_id,
                 requested_by_name, requested_at, remarks, created_at, updated_at)
            values
                (:id, :caseId, :reportId, :currentVersionNo, :requestStatus, :requestReason, :requestedByUserId,
                 :requestedByName, :requestedAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("reportId", command.reportId())
            .addValue("currentVersionNo", command.currentVersionNo())
            .addValue("requestStatus", command.requestStatus())
            .addValue("requestReason", command.requestReason())
            .addValue("requestedByUserId", command.requestedByUserId())
            .addValue("requestedByName", command.requestedByName())
            .addValue("requestedAt", command.requestedAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.requestedAt())
            .addValue("updatedAt", command.requestedAt()));
    }

    @Override
    public Optional<ReportRevisionRequest> findRevisionRequestById(String requestId) {
        List<ReportRevisionRequest> rows = jdbcTemplate.query("""
            select *
            from report_revision_requests
            where id = :requestId
            """, Map.of("requestId", requestId), this::mapRevisionRequest);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<ReportRevisionRequest> findPendingRevisionRequestByReportId(String reportId) {
        List<ReportRevisionRequest> rows = jdbcTemplate.query("""
            select *
            from report_revision_requests
            where report_id = :reportId
              and request_status = 'PENDING'
            order by created_at desc
            fetch first 1 row only
            """, Map.of("reportId", reportId), this::mapRevisionRequest);
        return rows.stream().findFirst();
    }

    @Override
    public List<ReportRevisionRequest> findRevisionRequestsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from report_revision_requests
            where case_id = :caseId
            order by created_at desc, id desc
            """, Map.of("caseId", caseId), this::mapRevisionRequest);
    }

    @Override
    public void approveRevisionRequest(String requestId,
                                       String reviewedByUserId,
                                       String reviewedByName,
                                       int approvedVersionNo,
                                       String remarks,
                                       LocalDateTime reviewedAt) {
        jdbcTemplate.update("""
            update report_revision_requests
            set request_status = 'APPROVED',
                reviewed_by_user_id = :reviewedByUserId,
                reviewed_by_name = :reviewedByName,
                reviewed_at = :reviewedAt,
                approved_version_no = :approvedVersionNo,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :requestId
            """, new MapSqlParameterSource()
            .addValue("requestId", requestId)
            .addValue("reviewedByUserId", reviewedByUserId)
            .addValue("reviewedByName", reviewedByName)
            .addValue("reviewedAt", reviewedAt)
            .addValue("approvedVersionNo", approvedVersionNo)
            .addValue("remarks", remarks)
            .addValue("updatedAt", reviewedAt));
    }

    @Override
    public void rejectRevisionRequest(String requestId,
                                      String reviewedByUserId,
                                      String reviewedByName,
                                      String rejectReason,
                                      LocalDateTime reviewedAt) {
        jdbcTemplate.update("""
            update report_revision_requests
            set request_status = 'REJECTED',
                reviewed_by_user_id = :reviewedByUserId,
                reviewed_by_name = :reviewedByName,
                reviewed_at = :reviewedAt,
                reject_reason = :rejectReason,
                updated_at = :updatedAt
            where id = :requestId
            """, new MapSqlParameterSource()
            .addValue("requestId", requestId)
            .addValue("reviewedByUserId", reviewedByUserId)
            .addValue("reviewedByName", reviewedByName)
            .addValue("reviewedAt", reviewedAt)
            .addValue("rejectReason", rejectReason)
            .addValue("updatedAt", reviewedAt));
    }

    private ReportRevisionRequest mapRevisionRequest(ResultSet rs, int rowNum) throws SQLException {
        return new ReportRevisionRequest(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("report_id"),
            rs.getInt("current_version_no"),
            rs.getString("request_status"),
            rs.getString("request_reason"),
            rs.getString("requested_by_user_id"),
            rs.getString("requested_by_name"),
            toLocalDateTime(rs.getTimestamp("requested_at")),
            rs.getString("reviewed_by_user_id"),
            rs.getString("reviewed_by_name"),
            toLocalDateTime(rs.getTimestamp("reviewed_at")),
            rs.getString("reject_reason"),
            (Integer) rs.getObject("approved_version_no"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
