package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class JdbcSpecimenWorkflowPendingProjectionSupport extends AbstractJdbcSpecimenWorkflowReadSupport {

    JdbcSpecimenWorkflowPendingProjectionSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    SpecimenWorkflowRepository.PagedPendingSpecimens findPendingFixations(SpecimenWorkflowRepository.PendingSpecimenQuery query) {
        String whereClause = """
            from specimens s
            join applications a on a.id = s.application_id
            left join specimen_fixation_records sfr on sfr.specimen_id = s.id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
            where (
                    (s.specimen_status = 'REGISTERED' and coalesce(s.fixation_status, 'PENDING') = 'PENDING')
                    or (s.specimen_status = 'FIXING' and s.fixation_status = 'FIXING')
                    or (s.specimen_status = 'FIXED' and s.fixation_status = 'COMPLETED')
                  )
              and not exists (
                    select 1
                    from transport_order_items toi
                    where toi.specimen_id = s.id
                )
            """ + buildPendingFilters(query, "a", "s", "sfr");
        long total = countPending(whereClause, query);
        List<SpecimenWorkflowRepository.PendingSpecimenRow> items = queryPending("""
            select
                a.id as application_id,
                a.application_no,
                a.patient_name,
                a.submitting_department_id,
                a.submitting_department_name,
                cast(null as varchar(64)) as transport_order_id,
                s.id as specimen_id,
                s.specimen_no,
                s.barcode,
            """ + containerNameSelect("s")
            + containerCountSelect("s")
            + """
                s.specimen_status,
                s.fixation_status,
                sfr.fixation_start_at as fixation_started_at,
                sfr.fixation_completed_at as fixation_completed_at,
                sfr.fixation_liquid_type as fixation_liquid_type,
                sfr.verified_by_user_id as fixation_operator_user_id,
                sfr.verified_by_name as fixation_operator_name,
                """ + buildVerificationStatusExpression("sfr", "s") + """
                 as verification_status,
                sfr.verification_started_at as verification_started_at,
                coalesce(sfr.verification_completed_at, sfr.verified_at) as verification_completed_at,
            """ + specimenConfirmedAtSelect("s")
            + checkInStatusSelect("s", "check_in_status")
            + checkedInAtSelect("s")
            + checkedInByNameSelect("s")
            + """
                s.registered_at,
                evt.latest_event_time,
                case
                    when s.label_print_status = 'FAILED'
                        or s.specimen_status in ('REJECTED', 'RETURNED')
                        or s.fixation_status = 'ABNORMAL'
                        or s.unqualified_reason is not null
                    then 1 else 0
                end as abnormal_flag
            """ + whereClause + " order by s.registered_at asc, s.id asc", query);
        return new SpecimenWorkflowRepository.PagedPendingSpecimens(items, total);
    }

    SpecimenWorkflowRepository.PagedPendingSpecimens findPendingReceipts(SpecimenWorkflowRepository.PendingSpecimenQuery query) {
        String whereClause = """
            from specimens s
            join applications a on a.id = s.application_id
            left join specimen_fixation_records sfr on sfr.specimen_id = s.id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
            where (
                    exists (
                        select 1
                        from transport_order_items toi
                        join transport_orders t on t.id = toi.transport_order_id
                        where toi.specimen_id = s.id
                          and t.order_status in ('PRINTED', 'HANDED_OVER', 'PARTIALLY_RECEIVED')
                    )
                    or s.specimen_status = 'IN_TRANSIT'
                  )
              and s.specimen_status not in ('RECEIVED', 'REJECTED', 'RETURNED')
              and not exists (
                    select 1
                    from specimen_receipts sr
                    where sr.specimen_id = s.id
                      and sr.receipt_status = 'RECEIVED'
                )
            """ + buildPendingFilters(query, "a", "s", "sfr");
        long total = countPending(whereClause, query);
        List<SpecimenWorkflowRepository.PendingSpecimenRow> items = queryPending("""
            select
                a.id as application_id,
                a.application_no,
                a.patient_name,
                a.submitting_department_id,
                a.submitting_department_name,
                (
                    select toi.transport_order_id
                    from transport_order_items toi
                    join transport_orders t on t.id = toi.transport_order_id
                    where toi.specimen_id = s.id
                      and t.order_status in ('PRINTED', 'HANDED_OVER', 'PARTIALLY_RECEIVED')
                    order by toi.verified_at desc, toi.id desc
                    fetch next 1 rows only
                ) as transport_order_id,
                s.id as specimen_id,
                s.specimen_no,
                s.barcode,
            """ + containerNameSelect("s")
            + containerCountSelect("s")
            + """
                s.specimen_status,
                s.fixation_status,
                sfr.fixation_start_at as fixation_started_at,
                sfr.fixation_completed_at as fixation_completed_at,
                sfr.fixation_liquid_type as fixation_liquid_type,
                sfr.verified_by_user_id as fixation_operator_user_id,
                sfr.verified_by_name as fixation_operator_name,
                """ + buildVerificationStatusExpression("sfr", "s") + """
                 as verification_status,
                sfr.verification_started_at as verification_started_at,
                coalesce(sfr.verification_completed_at, sfr.verified_at) as verification_completed_at,
            """ + specimenConfirmedAtSelect("s")
            + checkInStatusSelect("s", "check_in_status")
            + checkedInAtSelect("s")
            + checkedInByNameSelect("s")
            + """
                s.registered_at,
                evt.latest_event_time,
                case
                    when s.label_print_status = 'FAILED'
                        or s.specimen_status in ('REJECTED', 'RETURNED')
                        or s.fixation_status = 'ABNORMAL'
                        or s.unqualified_reason is not null
                    then 1 else 0
                end as abnormal_flag
            """ + whereClause + " order by coalesce(evt.latest_event_time, s.registered_at) asc, s.id asc", query);
        return new SpecimenWorkflowRepository.PagedPendingSpecimens(items, total);
    }

    SpecimenWorkflowRepository.PagedPendingTransportOrders findPendingTransportOrders(
        SpecimenWorkflowRepository.PendingTransportOrderQuery query
    ) {
        String whereClause = """
            from transport_orders t
            join applications a on a.id = t.application_id
            where t.order_status <> 'COMPLETED'
            """ + buildTransportPendingFilters(query);
        long total = countPendingTransportOrders(whereClause, query);
        List<SpecimenWorkflowRepository.PendingTransportOrderRow> items = queryPendingTransportOrders("""
            select
                t.id,
                t.transport_order_no,
                t.application_id,
                a.application_no,
                a.patient_name,
                t.handover_department_name,
                t.receiver_department_name,
                t.order_status,
                t.to_be_transported_at,
                t.handed_over_at
            """ + whereClause + " order by t.to_be_transported_at asc, t.id asc", query);
        return new SpecimenWorkflowRepository.PagedPendingTransportOrders(items, total);
    }

    private long countPending(String whereClause, SpecimenWorkflowRepository.PendingSpecimenQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            pendingParams(query),
            Long.class);
        return total == null ? 0L : total;
    }

    private long countPendingTransportOrders(String whereClause, SpecimenWorkflowRepository.PendingTransportOrderQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            pendingTransportOrderParams(query),
            Long.class);
        return total == null ? 0L : total;
    }

    private List<SpecimenWorkflowRepository.PendingSpecimenRow> queryPending(
        String sql,
        SpecimenWorkflowRepository.PendingSpecimenQuery query
    ) {
        MapSqlParameterSource parameters = pendingParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(sql + " offset :offset rows fetch next :size rows only", parameters, this::mapPendingSpecimenRow);
    }

    private List<SpecimenWorkflowRepository.PendingTransportOrderRow> queryPendingTransportOrders(
        String sql,
        SpecimenWorkflowRepository.PendingTransportOrderQuery query
    ) {
        MapSqlParameterSource parameters = pendingTransportOrderParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(sql + " offset :offset rows fetch next :size rows only", parameters, this::mapPendingTransportOrderRow);
    }

    private String buildPendingFilters(
        SpecimenWorkflowRepository.PendingSpecimenQuery query,
        String applicationAlias,
        String specimenAlias,
        String fixationRecordAlias
    ) {
        StringBuilder builder = new StringBuilder();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            builder.append(" and ").append(applicationAlias).append(".id = :applicationId");
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            builder.append(" and ").append(specimenAlias).append(".specimen_no = :specimenNo");
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            builder.append(" and ").append(applicationAlias).append(".submitting_department_id = :departmentId");
        }
        if (query.fixationStatus() != null && !query.fixationStatus().isBlank()) {
            builder.append(" and ").append(specimenAlias).append(".fixation_status = :fixationStatus");
        }
        if (query.verificationStatus() != null && !query.verificationStatus().isBlank()) {
            builder.append(" and ")
                .append(buildVerificationStatusExpression(fixationRecordAlias, specimenAlias))
                .append(" = :verificationStatus");
        }
        if (query.dateFrom() != null) {
            builder.append(" and ").append(specimenAlias).append(".registered_at >= :dateFrom");
        }
        if (query.dateTo() != null) {
            builder.append(" and ").append(specimenAlias).append(".registered_at < :dateTo");
        }
        return builder.toString();
    }

    private String buildTransportPendingFilters(SpecimenWorkflowRepository.PendingTransportOrderQuery query) {
        StringBuilder builder = new StringBuilder();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            builder.append(" and a.id = :applicationId");
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            builder.append("""
                 and exists (
                    select 1
                    from transport_order_items toi
                    join specimens s on s.id = toi.specimen_id
                    where toi.transport_order_id = t.id
                      and s.specimen_no = :specimenNo
                )
                """);
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            builder.append(" and a.submitting_department_id = :departmentId");
        }
        if (query.dateFrom() != null) {
            builder.append(" and t.to_be_transported_at >= :dateFrom");
        }
        if (query.dateTo() != null) {
            builder.append(" and t.to_be_transported_at < :dateTo");
        }
        if (query.status() != null && !query.status().isBlank()) {
            builder.append(" and t.order_status = :status");
        }
        return builder.toString();
    }

    private MapSqlParameterSource pendingParams(SpecimenWorkflowRepository.PendingSpecimenQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            parameters.addValue("applicationId", query.applicationId());
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            parameters.addValue("specimenNo", query.specimenNo());
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            parameters.addValue("departmentId", query.departmentId());
        }
        if (query.fixationStatus() != null && !query.fixationStatus().isBlank()) {
            parameters.addValue("fixationStatus", query.fixationStatus());
        }
        if (query.verificationStatus() != null && !query.verificationStatus().isBlank()) {
            parameters.addValue("verificationStatus", query.verificationStatus());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        return parameters;
    }

    private MapSqlParameterSource pendingTransportOrderParams(SpecimenWorkflowRepository.PendingTransportOrderQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationId() != null && !query.applicationId().isBlank()) {
            parameters.addValue("applicationId", query.applicationId());
        }
        if (query.specimenNo() != null && !query.specimenNo().isBlank()) {
            parameters.addValue("specimenNo", query.specimenNo());
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            parameters.addValue("departmentId", query.departmentId());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        if (query.status() != null && !query.status().isBlank()) {
            parameters.addValue("status", query.status());
        }
        return parameters;
    }

    private SpecimenWorkflowRepository.PendingSpecimenRow mapPendingSpecimenRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.PendingSpecimenRow(
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("submitting_department_id"),
            rs.getString("submitting_department_name"),
            rs.getString("transport_order_id"),
            rs.getString("specimen_id"),
            rs.getString("specimen_no"),
            rs.getString("barcode"),
            JdbcResultSetUtils.getNullableString(rs, "container_name"),
            JdbcResultSetUtils.getNullableInteger(rs, "container_count"),
            rs.getString("specimen_status"),
            rs.getString("fixation_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "fixation_started_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "fixation_started_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableTimestamp(rs, "fixation_completed_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "fixation_completed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "fixation_liquid_type"),
            JdbcResultSetUtils.getNullableString(rs, "fixation_operator_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "fixation_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "verification_status"),
            rs.getTimestamp("verification_started_at") == null
                ? null
                : rs.getTimestamp("verification_started_at").toLocalDateTime(),
            rs.getTimestamp("verification_completed_at") == null
                ? null
                : rs.getTimestamp("verification_completed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "check_in_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "checked_in_by_name"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            rs.getTimestamp("latest_event_time") == null ? null : rs.getTimestamp("latest_event_time").toLocalDateTime(),
            rs.getInt("abnormal_flag") == 1);
    }

    private SpecimenWorkflowRepository.PendingTransportOrderRow mapPendingTransportOrderRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.PendingTransportOrderRow(
            rs.getString("id"),
            rs.getString("transport_order_no"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("handover_department_name"),
            rs.getString("receiver_department_name"),
            rs.getString("order_status"),
            rs.getTimestamp("to_be_transported_at") == null ? null : rs.getTimestamp("to_be_transported_at").toLocalDateTime(),
            rs.getTimestamp("handed_over_at") == null ? null : rs.getTimestamp("handed_over_at").toLocalDateTime());
    }
}
