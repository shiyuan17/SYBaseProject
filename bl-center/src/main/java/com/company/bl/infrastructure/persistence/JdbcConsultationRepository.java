package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ConsultationRepository;
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
public class JdbcConsultationRepository implements ConsultationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcConsultationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertConsultationCase(CreateConsultationCaseCommand command) {
        jdbcTemplate.update("""
            insert into consultation_cases
                (id, case_id, consultation_type, status, requested_by_user_id, requested_by_name, requested_at,
                 host_user_id, host_name, remarks, created_at, updated_at)
            values
                (:id, :caseId, :consultationType, :status, :requestedByUserId, :requestedByName, :requestedAt,
                 :hostUserId, :hostName, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("consultationType", command.consultationType())
            .addValue("status", command.status())
            .addValue("requestedByUserId", command.requestedByUserId())
            .addValue("requestedByName", command.requestedByName())
            .addValue("requestedAt", command.requestedAt())
            .addValue("hostUserId", command.hostUserId())
            .addValue("hostName", command.hostName())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.requestedAt())
            .addValue("updatedAt", command.requestedAt()));
    }

    @Override
    public void insertConsultationParticipant(CreateConsultationParticipantCommand command) {
        jdbcTemplate.update("""
            insert into consultation_participants
                (id, consultation_id, case_id, participant_user_id, participant_name, participant_role, remarks,
                 created_at, updated_at)
            values
                (:id, :consultationId, :caseId, :participantUserId, :participantName, :participantRole, :remarks,
                 :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("consultationId", command.consultationId())
            .addValue("caseId", command.caseId())
            .addValue("participantUserId", command.participantUserId())
            .addValue("participantName", command.participantName())
            .addValue("participantRole", command.participantRole())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.createdAt()));
    }

    @Override
    public Optional<ConsultationCase> findConsultationCaseById(String consultationId) {
        List<ConsultationCase> rows = jdbcTemplate.query("""
            select *
            from consultation_cases
            where id = :consultationId
            """, Map.of("consultationId", consultationId), this::mapConsultationCase);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<ConsultationParticipant> findConsultationParticipantById(String participantId) {
        List<ConsultationParticipant> rows = jdbcTemplate.query("""
            select *
            from consultation_participants
            where id = :participantId
            """, Map.of("participantId", participantId), this::mapConsultationParticipant);
        return rows.stream().findFirst();
    }

    @Override
    public List<ConsultationCase> findConsultationsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from consultation_cases
            where case_id = :caseId
            order by created_at desc, id desc
            """, Map.of("caseId", caseId), this::mapConsultationCase);
    }

    @Override
    public List<ConsultationParticipant> findConsultationParticipants(String consultationId) {
        return jdbcTemplate.query("""
            select *
            from consultation_participants
            where consultation_id = :consultationId
            order by created_at asc, id asc
            """, Map.of("consultationId", consultationId), this::mapConsultationParticipant);
    }

    @Override
    public void startConsultation(String consultationId, String remarks, LocalDateTime updatedAt) {
        jdbcTemplate.update("""
            update consultation_cases
            set status = 'IN_PROGRESS',
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :consultationId
            """, new MapSqlParameterSource()
            .addValue("consultationId", consultationId)
            .addValue("remarks", remarks)
            .addValue("updatedAt", updatedAt));
    }

    @Override
    public void commentConsultationParticipant(String participantId,
                                               String opinion,
                                               String draftedByUserId,
                                               String draftedByName,
                                               String remarks,
                                               LocalDateTime commentedAt) {
        jdbcTemplate.update("""
            update consultation_participants
            set opinion = :opinion,
                drafted_by_user_id = :draftedByUserId,
                drafted_by_name = :draftedByName,
                read_flag = 1,
                read_at = coalesce(read_at, :commentedAt),
                commented_at = :commentedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :participantId
            """, new MapSqlParameterSource()
            .addValue("participantId", participantId)
            .addValue("opinion", opinion)
            .addValue("draftedByUserId", draftedByUserId)
            .addValue("draftedByName", draftedByName)
            .addValue("commentedAt", commentedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", commentedAt));
    }

    @Override
    public void completeConsultation(String consultationId,
                                     String opinion,
                                     String remarks,
                                     LocalDateTime completedAt) {
        jdbcTemplate.update("""
            update consultation_cases
            set status = 'COMPLETED',
                opinion = :opinion,
                completed_at = :completedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :consultationId
            """, new MapSqlParameterSource()
            .addValue("consultationId", consultationId)
            .addValue("opinion", opinion)
            .addValue("completedAt", completedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", completedAt));
    }

    private ConsultationCase mapConsultationCase(ResultSet rs, int rowNum) throws SQLException {
        return new ConsultationCase(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("consultation_type"),
            rs.getString("status"),
            rs.getString("requested_by_user_id"),
            rs.getString("requested_by_name"),
            toLocalDateTime(rs.getTimestamp("requested_at")),
            rs.getString("host_user_id"),
            rs.getString("host_name"),
            rs.getString("opinion"),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private ConsultationParticipant mapConsultationParticipant(ResultSet rs, int rowNum) throws SQLException {
        return new ConsultationParticipant(
            rs.getString("id"),
            rs.getString("consultation_id"),
            rs.getString("case_id"),
            rs.getString("participant_user_id"),
            rs.getString("participant_name"),
            rs.getString("participant_role"),
            rs.getString("opinion"),
            rs.getString("drafted_by_user_id"),
            rs.getString("drafted_by_name"),
            rs.getInt("read_flag") == 1,
            toLocalDateTime(rs.getTimestamp("read_at")),
            toLocalDateTime(rs.getTimestamp("commented_at")),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
