package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JdbcTechnicalWorkflowSpecimenRegistrationQueries {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTechnicalWorkflowSpecimenRegistrationQueries(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    TechnicalWorkflowRecords.PagedTechnicalSpecimenRegistrations findPendingTechnicalSpecimenRegistrations(
        TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery query
    ) {
        String where = """
             where tsr.registration_status = 'PENDING'
            """ + buildKeywordFilter(query) + buildReceivedAtFilter(query);
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_specimen_registrations tsr
            join pathology_cases pc on pc.id = tsr.case_id
            join applications a on a.id = tsr.application_id
            left join application_registration_workbench w on w.application_id = tsr.application_id
            """ + where, buildFilterParams(query), Long.class);
        List<TechnicalWorkflowRecords.TechnicalSpecimenRegistration> items = jdbcTemplate.query("""
            select
                tsr.case_id,
                tsr.application_id,
                pc.pathology_no,
                a.application_no,
                a.patient_name,
                a.patient_id,
                w.inpatient_no,
                a.application_type,
                a.submitting_department_name,
                w.check_item,
                tsr.registration_status,
                tsr.registered_by_user_id,
                tsr.registered_by_name,
                tsr.registered_at,
                tsr.remarks,
                pc.received_at,
                tsr.created_at,
                tsr.updated_at
            from technical_specimen_registrations tsr
            join pathology_cases pc on pc.id = tsr.case_id
            join applications a on a.id = tsr.application_id
            left join application_registration_workbench w on w.application_id = tsr.application_id
            """ + where + """
            order by case when pc.received_at is null then 1 else 0 end,
                     pc.received_at desc,
                     tsr.created_at desc,
                     tsr.case_id desc
            offset :offset rows fetch next :limit rows only
            """, buildPageParams(query), (rs, rowNum) -> new TechnicalWorkflowRecords.TechnicalSpecimenRegistration(
            rs.getString("case_id"),
            rs.getString("application_id"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("patient_id"),
            JdbcResultSetUtils.getNullableString(rs, "inpatient_no"),
            JdbcResultSetUtils.getNullableString(rs, "application_type"),
            JdbcResultSetUtils.getNullableString(rs, "submitting_department_name"),
            JdbcResultSetUtils.getNullableString(rs, "check_item"),
            rs.getString("registration_status"),
            JdbcResultSetUtils.getNullableString(rs, "registered_by_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "registered_by_name"),
            toLocalDateTime(rs.getTimestamp("registered_at")),
            JdbcResultSetUtils.getNullableString(rs, "remarks"),
            toLocalDateTime(rs.getTimestamp("received_at")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at"))));
        return new TechnicalWorkflowRecords.PagedTechnicalSpecimenRegistrations(items, total == null ? 0 : total);
    }

    Optional<TechnicalWorkflowRecords.TechnicalSpecimenRegistration> findTechnicalSpecimenRegistrationByCaseId(String caseId) {
        List<TechnicalWorkflowRecords.TechnicalSpecimenRegistration> rows = jdbcTemplate.query("""
            select
                tsr.case_id,
                tsr.application_id,
                pc.pathology_no,
                a.application_no,
                a.patient_name,
                a.patient_id,
                w.inpatient_no,
                a.application_type,
                a.submitting_department_name,
                w.check_item,
                tsr.registration_status,
                tsr.registered_by_user_id,
                tsr.registered_by_name,
                tsr.registered_at,
                tsr.remarks,
                pc.received_at,
                tsr.created_at,
                tsr.updated_at
            from technical_specimen_registrations tsr
            join pathology_cases pc on pc.id = tsr.case_id
            join applications a on a.id = tsr.application_id
            left join application_registration_workbench w on w.application_id = tsr.application_id
            where tsr.case_id = :caseId
            """, Map.of("caseId", caseId), (rs, rowNum) -> new TechnicalWorkflowRecords.TechnicalSpecimenRegistration(
            rs.getString("case_id"),
            rs.getString("application_id"),
            rs.getString("pathology_no"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("patient_id"),
            JdbcResultSetUtils.getNullableString(rs, "inpatient_no"),
            JdbcResultSetUtils.getNullableString(rs, "application_type"),
            JdbcResultSetUtils.getNullableString(rs, "submitting_department_name"),
            JdbcResultSetUtils.getNullableString(rs, "check_item"),
            rs.getString("registration_status"),
            JdbcResultSetUtils.getNullableString(rs, "registered_by_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "registered_by_name"),
            toLocalDateTime(rs.getTimestamp("registered_at")),
            JdbcResultSetUtils.getNullableString(rs, "remarks"),
            toLocalDateTime(rs.getTimestamp("received_at")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("updated_at"))));
        return rows.stream().findFirst();
    }

    private String buildKeywordFilter(TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery query) {
        if (query.keyword() == null || query.keyword().isBlank()) {
            return "";
        }
        return """
             and (
                    upper(coalesce(pc.pathology_no, '')) like :keyword
                 or upper(coalesce(a.patient_name, '')) like :keyword
                 or upper(coalesce(a.patient_id, '')) like :keyword
                 or upper(coalesce(a.application_no, '')) like :keyword
                 or upper(coalesce(w.inpatient_no, '')) like :keyword
             )
            """;
    }

    private MapSqlParameterSource buildFilterParams(TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            params.addValue("keyword", "%" + query.keyword().trim().toUpperCase() + "%");
        }
        if (query.receivedFrom() != null) {
            params.addValue("receivedFrom", query.receivedFrom());
        }
        if (query.receivedTo() != null) {
            params.addValue("receivedTo", query.receivedTo());
        }
        return params;
    }

    private String buildReceivedAtFilter(TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery query) {
        StringBuilder builder = new StringBuilder();
        if (query.receivedFrom() != null) {
            builder.append(" and pc.received_at >= :receivedFrom");
        }
        if (query.receivedTo() != null) {
            builder.append(" and pc.received_at < :receivedTo");
        }
        return builder.toString();
    }

    private MapSqlParameterSource buildPageParams(TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery query) {
        return buildFilterParams(query)
            .addValue("offset", Math.max(query.page() - 1, 0) * query.size())
            .addValue("limit", query.size());
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
