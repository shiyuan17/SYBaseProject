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

    SpecimenWorkflowRepository.PagedSpecimenOutbounds findSpecimenOutbounds(
        SpecimenWorkflowRepository.SpecimenOutboundListQuery query
    ) {
        String whereClause = specimenOutboundWhereClause(query);
        long total = countSpecimenOutbounds(whereClause, query);
        List<SpecimenWorkflowRepository.SpecimenOutboundRow> items = querySpecimenOutbounds(
            specimenOutboundSelectSql(whereClause),
            query);
        return new SpecimenWorkflowRepository.PagedSpecimenOutbounds(items, total);
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

    private long countSpecimenOutbounds(
        String whereClause,
        SpecimenWorkflowRepository.SpecimenOutboundListQuery query
    ) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            specimenOutboundParams(query),
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

    private List<SpecimenWorkflowRepository.SpecimenOutboundRow> querySpecimenOutbounds(
        String sql,
        SpecimenWorkflowRepository.SpecimenOutboundListQuery query
    ) {
        MapSqlParameterSource parameters = specimenOutboundParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(
            sql + specimenOutboundOrderBy()
                + " offset :offset rows fetch next :size rows only",
            parameters,
            this::mapSpecimenOutboundRow);
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

    private String specimenOutboundWhereClause(
        SpecimenWorkflowRepository.SpecimenOutboundListQuery query
    ) {
        String resolvedCheckInStatus = hasSpecimenConfirmationColumns()
            ? "coalesce(s.check_in_status, 'NOT_CHECKED_IN')"
            : "cast('NOT_CHECKED_IN' as varchar(32))";
        String outboundUserColumns = hasTransportOrderOutboundColumns()
            ? "                        t.outbound_user_name as outbound_user_name,\n"
            : "                        cast(null as varchar(100)) as outbound_user_name,\n";
        StringBuilder builder = new StringBuilder("""
            from specimens s
            join applications a on a.id = s.application_id
            left join application_registration_workbench w on w.application_id = a.id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
            left join (
                select
                    ranked.specimen_id,
                    ranked.transport_order_id,
                    ranked.handed_over_at,
                    ranked.outbound_user_name
                from (
                    select
                        toi.specimen_id,
                        t.id as transport_order_id,
                        t.handed_over_at,
            """ + outboundUserColumns + """
                        row_number() over (
                            partition by toi.specimen_id
                            order by coalesce(t.handed_over_at, t.to_be_transported_at) desc, t.id desc
                        ) as rn
                    from transport_order_items toi
                    join transport_orders t on t.id = toi.transport_order_id
                    where t.order_status <> 'CANCELLED'
                ) ranked
                where ranked.rn = 1
            ) latest_order on latest_order.specimen_id = s.id
            where 1 = 1
            """);
        boolean hasExplicitCondition =
            query.applicationId() != null && !query.applicationId().isBlank()
                || query.specimenNo() != null && !query.specimenNo().isBlank();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            builder.append(" and a.id = :applicationId");
        }
        if (!hasExplicitCondition) {
            builder.append("""
                  and (
                    latest_order.transport_order_id is not null
                    or (
                        s.specimen_status = 'CHECKED_IN'
                        and
                """);
            builder.append(resolvedCheckInStatus);
            builder.append("""
                         = 'CHECKED_IN'
                    )
                  )
                """);
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            builder.append(" and s.specimen_no = :specimenNo");
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

    private String specimenOutboundSelectSql(String whereClause) {
        String resolvedCheckInStatus = hasSpecimenConfirmationColumns()
            ? "coalesce(s.check_in_status, 'NOT_CHECKED_IN')"
            : "cast('NOT_CHECKED_IN' as varchar(32))";
        return """
            select
                s.id as specimen_id,
                latest_order.transport_order_id as transport_order_id,
                a.id as application_id,
                a.application_no,
                s.barcode,
                s.specimen_no,
                a.patient_name,
                a.patient_gender,
                a.patient_id,
                w.inpatient_no,
                coalesce(w.room_id, w.surgery_name) as surgery_name,
                s.specimen_name_standardized as specimen_name,
                s.specimen_status,
                s.fixation_status,
                """ + resolvedCheckInStatus + """
                 as check_in_status,
                s.specimen_confirmed_at,
                a.submitting_department_id,
                a.submitting_department_name,
                s.registered_at,
                s.registered_by_name,
                latest_order.handed_over_at as outbound_at,
                latest_order.outbound_user_name as outbound_user_name
            """ + whereClause;
    }

    private String specimenOutboundOrderBy() {
        return """
             order by
                case when latest_order.handed_over_at is null then 0 else 1 end asc,
                case
                    when latest_order.handed_over_at is null then coalesce(evt.latest_event_time, s.registered_at)
                    else null
                end desc,
                latest_order.handed_over_at desc,
                s.id desc
            """;
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

    private MapSqlParameterSource specimenOutboundParams(
        SpecimenWorkflowRepository.SpecimenOutboundListQuery query
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            parameters.addValue("applicationId", query.applicationId());
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            parameters.addValue("specimenNo", query.specimenNo());
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

    private SpecimenWorkflowRepository.SpecimenOutboundRow mapSpecimenOutboundRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.SpecimenOutboundRow(
            rs.getString("specimen_id"),
            rs.getString("transport_order_id"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            JdbcResultSetUtils.getNullableString(rs, "barcode"),
            rs.getString("specimen_no"),
            rs.getString("patient_name"),
            JdbcResultSetUtils.getNullableString(rs, "patient_gender"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id"),
            JdbcResultSetUtils.getNullableString(rs, "inpatient_no"),
            JdbcResultSetUtils.getNullableString(rs, "surgery_name"),
            rs.getString("specimen_name"),
            rs.getString("specimen_status"),
            rs.getString("fixation_status"),
            JdbcResultSetUtils.getNullableString(rs, "check_in_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "submitting_department_id"),
            JdbcResultSetUtils.getNullableString(rs, "submitting_department_name"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "registered_by_name"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "outbound_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "outbound_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "outbound_user_name"));
    }
}
