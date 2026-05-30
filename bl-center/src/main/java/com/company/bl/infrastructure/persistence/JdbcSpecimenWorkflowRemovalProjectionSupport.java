package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class JdbcSpecimenWorkflowRemovalProjectionSupport extends AbstractJdbcSpecimenWorkflowReadSupport {

    JdbcSpecimenWorkflowRemovalProjectionSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    SpecimenWorkflowRepository.PagedSpecimenRemovalItems findSpecimenRemovalItems(
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query
    ) {
        String abnormalExpression = specimenManagementAbnormalExpression("s");
        String whereClause = specimenRemovalWhereClause(query, abnormalExpression, false);
        long total = countSpecimenRemoval(whereClause, query);
        List<SpecimenWorkflowRepository.SpecimenRemovalListRow> items = querySpecimenRemoval(
            specimenRemovalSelectSql(whereClause, abnormalExpression),
            query);
        SpecimenWorkflowRepository.SpecimenRemovalSummary summary = summarizeSpecimenRemoval(whereClause, query);
        return new SpecimenWorkflowRepository.PagedSpecimenRemovalItems(items, total, summary);
    }

    List<SpecimenWorkflowRepository.SpecimenRemovalListRow> listSpecimenRemovalExportRows(
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query
    ) {
        String abnormalExpression = specimenManagementAbnormalExpression("s");
        String whereClause = specimenRemovalWhereClause(query, abnormalExpression, true);
        return jdbcTemplate.query(
            specimenRemovalSelectSql(whereClause, abnormalExpression)
                + " order by coalesce(" + specimenRemovalAtExpression("s") + ", s.registered_at, evt.latest_event_time) desc, s.id desc",
            specimenRemovalParams(query),
            this::mapSpecimenRemovalListRow);
    }

    private long countSpecimenRemoval(String whereClause, SpecimenWorkflowRepository.SpecimenRemovalListQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            specimenRemovalParams(query),
            Long.class);
        return total == null ? 0L : total;
    }

    private List<SpecimenWorkflowRepository.SpecimenRemovalListRow> querySpecimenRemoval(
        String sql,
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query
    ) {
        MapSqlParameterSource parameters = specimenRemovalParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(
            sql + " order by coalesce(" + specimenRemovalAtExpression("s") + ", s.registered_at, evt.latest_event_time) desc, s.id desc"
                + " offset :offset rows fetch next :size rows only",
            parameters,
            this::mapSpecimenRemovalListRow);
    }

    private SpecimenWorkflowRepository.SpecimenRemovalSummary summarizeSpecimenRemoval(
        String whereClause,
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query
    ) {
        String specimenRemovalAtExpression = specimenRemovalAtExpression("s");
        return jdbcTemplate.queryForObject(
            """
            select
                count(1) as total_count,
            """
                + "    sum(case when " + specimenRemovalAtExpression + " is not null then 1 else 0 end) as confirmed_count,\n"
                + "    sum(case when " + specimenRemovalAtExpression + " is null then 1 else 0 end) as pending_count,\n"
                + "    sum(case when (" + specimenManagementAbnormalExpression("s") + ")\n"
                + """
                    then 1 else 0
                end) as abnormal_count
            """
                + whereClause,
            specimenRemovalParams(query),
            this::mapSpecimenRemovalSummary);
    }

    private String specimenRemovalWhereClause(
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query,
        String abnormalExpression,
        boolean includeRemoved
    ) {
        StringBuilder builder = new StringBuilder("""
            from specimens s
            join applications a on a.id = s.application_id
            left join specimen_fixation_records sfr on sfr.specimen_id = s.id
            left join application_registration_workbench w on w.application_id = a.id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
            where 1 = 1
            """);
        builder.append(buildSpecimenRemovalFilters(query, abnormalExpression));
        if (!includeRemoved) {
            builder.append(" and ").append(specimenRemovalAtExpression("s")).append(" is null");
        }
        return builder.toString();
    }

    private String specimenRemovalSelectSql(String whereClause, String abnormalExpression) {
        return """
            select
                s.id as specimen_id,
                s.specimen_no,
                s.barcode,
                a.id as application_id,
                a.application_no,
                a.patient_name,
                a.patient_gender,
                w.inpatient_no,
                coalesce(w.room_id, w.surgery_name) as surgery_name,
                a.submitting_department_id,
                a.submitting_department_name,
                s.specimen_name_standardized as specimen_name,
                s.specimen_type,
                s.specimen_count,
            """ + containerNameSelect("s")
            + containerCountSelect("s")
            + """
                s.specimen_status,
                s.fixation_status,
                """ + buildVerificationStatusExpression("sfr", "s") + """
                 as verification_status,
            """ + specimenRemovalAtSelect("s")
            + specimenRemovalOperatorNameSelect("s")
            + """
                s.registered_at,
                s.label_print_batch_no,
                s.registered_by_name,
                evt.latest_event_time,
            """
            + "    case when (" + abnormalExpression + ")\n"
            + """
                    then 1 else 0
                end as abnormal_flag
            """
            + whereClause;
    }

    private String buildSpecimenRemovalFilters(
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query,
        String abnormalExpression
    ) {
        StringBuilder builder = new StringBuilder();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            builder.append("""
                 and (
                    s.specimen_no like :keyword
                    or s.barcode like :keyword
                    or a.application_no like :keyword
                    or a.patient_name like :keyword
                    or coalesce(w.inpatient_no, '') like :keyword
                    or coalesce(w.surgery_name, '') like :keyword
                )
                """);
        }
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            builder.append(" and a.application_no like :applicationNo");
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            builder.append(" and a.submitting_department_id = :departmentId");
        }
        if (query.specimenStatus() != null && !query.specimenStatus().isBlank()) {
            builder.append(" and s.specimen_status = :specimenStatus");
        }
        if (query.abnormalFlag() != null) {
            builder.append(
                query.abnormalFlag()
                    ? " and (" + abnormalExpression + ")"
                    : " and not (" + abnormalExpression + ")");
        }
        if (query.dateFrom() != null) {
            builder.append(" and s.registered_at >= :dateFrom");
        }
        if (query.dateTo() != null) {
            builder.append(" and s.registered_at < :dateTo");
        }
        return builder.toString();
    }

    private String specimenManagementAbnormalExpression(String specimenAlias) {
        return specimenAlias + ".specimen_status in ('REJECTED', 'RETURNED')"
            + " or " + specimenAlias + ".fixation_status = 'ABNORMAL'"
            + " or " + specimenAlias + ".unqualified_reason is not null";
    }

    private MapSqlParameterSource specimenRemovalParams(SpecimenWorkflowRepository.SpecimenRemovalListQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            parameters.addValue("keyword", "%" + query.keyword() + "%");
        }
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            parameters.addValue("applicationNo", "%" + query.applicationNo() + "%");
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            parameters.addValue("departmentId", query.departmentId());
        }
        if (query.specimenStatus() != null && !query.specimenStatus().isBlank()) {
            parameters.addValue("specimenStatus", query.specimenStatus());
        }
        if (query.abnormalFlag() != null) {
            parameters.addValue("abnormalFlag", query.abnormalFlag());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        return parameters;
    }

    private SpecimenWorkflowRepository.SpecimenRemovalSummary mapSpecimenRemovalSummary(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.SpecimenRemovalSummary(
            rs.getLong("total_count"),
            rs.getLong("confirmed_count"),
            rs.getLong("pending_count"),
            rs.getLong("abnormal_count"));
    }

    private SpecimenWorkflowRepository.SpecimenRemovalListRow mapSpecimenRemovalListRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.SpecimenRemovalListRow(
            rs.getString("specimen_id"),
            rs.getString("specimen_no"),
            rs.getString("barcode"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            JdbcResultSetUtils.getNullableString(rs, "patient_gender"),
            JdbcResultSetUtils.getNullableString(rs, "inpatient_no"),
            JdbcResultSetUtils.getNullableString(rs, "surgery_name"),
            rs.getString("submitting_department_id"),
            rs.getString("submitting_department_name"),
            rs.getString("specimen_name"),
            rs.getString("specimen_type"),
            rs.getObject("specimen_count", Integer.class),
            JdbcResultSetUtils.getNullableString(rs, "container_name"),
            JdbcResultSetUtils.getNullableInteger(rs, "container_count"),
            rs.getString("specimen_status"),
            rs.getString("fixation_status"),
            rs.getString("verification_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_removal_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_removal_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "specimen_removal_operator_name"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "label_print_batch_no"),
            JdbcResultSetUtils.getNullableString(rs, "registered_by_name"),
            rs.getTimestamp("latest_event_time") == null ? null : rs.getTimestamp("latest_event_time").toLocalDateTime(),
            rs.getInt("abnormal_flag") == 1);
    }
}
