package com.company.bl.infrastructure.persistence;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;

final class JdbcTechnicalWorkflowSpecimenRegistrationMutations {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTechnicalWorkflowSpecimenRegistrationMutations(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void ensureTechnicalSpecimenRegistrationPending(String applicationId, String caseId) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_specimen_registrations
            where case_id = :caseId
            """, Map.of("caseId", caseId), Long.class);
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                insert into technical_specimen_registrations
                    (case_id, application_id, registration_status, created_at, updated_at)
                values
                    (:caseId, :applicationId, 'PENDING', :createdAt, :updatedAt)
                """, new MapSqlParameterSource()
                .addValue("caseId", caseId)
                .addValue("applicationId", applicationId)
                .addValue("createdAt", LocalDateTime.now())
                .addValue("updatedAt", LocalDateTime.now()));
            return;
        }
        jdbcTemplate.update("""
            update technical_specimen_registrations
            set application_id = :applicationId,
                updated_at = :updatedAt
            where case_id = :caseId
              and registration_status = 'PENDING'
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("applicationId", applicationId)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void completeTechnicalSpecimenRegistration(String caseId,
                                               String registeredByUserId,
                                               String registeredByName,
                                               String remarks,
                                               LocalDateTime registeredAt) {
        jdbcTemplate.update("""
            update technical_specimen_registrations
            set registration_status = 'COMPLETED',
                registered_by_user_id = :registeredByUserId,
                registered_by_name = :registeredByName,
                registered_at = :registeredAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where case_id = :caseId
              and registration_status <> 'COMPLETED'
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("registeredByUserId", registeredByUserId)
            .addValue("registeredByName", registeredByName)
            .addValue("registeredAt", registeredAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }
}
