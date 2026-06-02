package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class JdbcSpecimenWorkflowManagementProjectionSupport extends AbstractJdbcSpecimenWorkflowReadSupport {

    private volatile Boolean patientsTableAvailable;

    JdbcSpecimenWorkflowManagementProjectionSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    List<SpecimenWorkflowRepository.DuplicateApplicationRow> findDuplicateApplications(
        SpecimenWorkflowRepository.DuplicateApplicationQuery query
    ) {
        String patientJoinClause = hasPatientsTable()
            ? """
            left join patients p
                on p.id = a.patient_id
                or p.patient_no = a.patient_id
                or p.inpatient_no = a.patient_id
                or p.outpatient_no = a.patient_id
            """
            : "";
        String patientMatchCondition = hasPatientsTable()
            ? """
                (:patientId is not null and (a.patient_id = :patientId or p.id = :patientId))
                or (:patientName is not null and a.patient_name = :patientName)
            """
            : """
                (:patientId is not null and a.patient_id = :patientId)
                or (:patientName is not null and a.patient_name = :patientName)
            """;
        String sql = """
            select
                a.id,
                a.application_no,
                a.patient_name,
                a.specimen_site,
                a.status,
                a.application_date,
                case
                    when :externalOrderNo is not null and a.external_order_no = :externalOrderNo then 1
                    else 0
                end as external_order_matched,
                case
                    when :applicationDate is not null
                         and :applicationType is not null
                         and :specimenSite is not null
                         and a.application_date = :applicationDate
                         and a.application_type = :applicationType
                         and a.specimen_site = :specimenSite then 1
                    else 0
                end as same_day_site_matched,
                case
                    when a.status = 'VOIDED' then a.status
                    else coalesce(
                        (
                            select we.node_code
                            from workflow_events we
                            where we.application_id = a.id
                            order by we.event_time desc, we.created_at desc, we.id desc
                            fetch next 1 rows only
                        ),
                        a.status
                    )
                end as current_node
            from applications a
            """ + patientJoinClause + """
            where (
            """ + patientMatchCondition + """
            )
            and a.status <> 'VOIDED'
            and (
                (:externalOrderNo is not null and a.external_order_no = :externalOrderNo)
                or (
                    :applicationDate is not null
                    and :applicationType is not null
                    and :specimenSite is not null
                    and a.application_date = :applicationDate
                    and a.application_type = :applicationType
                    and a.specimen_site = :specimenSite
                )
            )
            order by coalesce(a.updated_at, a.created_at) desc, a.id desc
            """;
        return jdbcTemplate.query(sql, duplicateApplicationParams(query), this::mapDuplicateApplicationRow);
    }

    private boolean hasPatientsTable() {
        Boolean cached = patientsTableAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            tableExists(connection.getMetaData(), "PATIENTS"));
        patientsTableAvailable = Boolean.TRUE.equals(resolved);
        return patientsTableAvailable;
    }

    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, tableName, null)) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet tables = metadata.getTables(null, null, tableName.toLowerCase(), null)) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    SpecimenWorkflowRepository.PagedSpecimenManagementItems findSpecimenManagementItems(
        SpecimenWorkflowRepository.SpecimenManagementListQuery query
    ) {
        String abnormalExpression = specimenManagementAbnormalExpression("s");
        String whereClause = """
            from specimens s
            join applications a on a.id = s.application_id
            left join specimen_fixation_records sfr on sfr.specimen_id = s.id
            left join application_registration_workbench w on w.application_id = a.id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
            left join (
                select
                    ranked.specimen_id,
                    ranked.operator_user_id,
                    ranked.operator_name
                from (
                    select
                        we.specimen_id,
                        we.operator_user_id,
                        we.operator_name,
                        row_number() over (
                            partition by we.specimen_id
                            order by we.event_time desc, we.created_at desc, we.id desc
                        ) as rn
                    from workflow_events we
                    where we.node_code = 'CONFIRMATION'
                      and we.event_type = 'COMPLETED'
                      and we.event_status = 'SUCCESS'
                ) ranked
                where ranked.rn = 1
            ) confirm_evt on confirm_evt.specimen_id = s.id
            where 1 = 1
            """ + buildSpecimenManagementFilters(query, abnormalExpression);
        long total = countSpecimenManagement(whereClause, query);
        List<SpecimenWorkflowRepository.SpecimenManagementListRow> items = querySpecimenManagement(
            """
            select
                s.id as specimen_id,
                s.specimen_no,
                s.barcode,
                a.id as application_id,
                a.application_no,
                a.patient_id,
                a.patient_name,
                a.patient_gender,
                a.submitting_department_id,
                a.submitting_department_name,
                w.building_id,
                w.room_id,
                coalesce(w.room_id, w.surgery_name) as surgery_name,
                s.specimen_name_standardized as specimen_name,
                s.specimen_type,
                s.specimen_site,
                s.specimen_count,
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
            """ + specimenConfirmedAtSelect("s")
                + """
                confirm_evt.operator_user_id as specimen_confirmed_by_user_id,
                confirm_evt.operator_name as specimen_confirmed_by_name,
            """
                + specimenRemovalAtSelect("s")
                + checkInStatusSelect("s", "check_in_status")
                + checkedInAtSelect("s")
                + checkedInByNameSelect("s")
                + """
                s.label_print_status,
                s.label_print_batch_no,
                s.registered_by_name as registration_operator_name,
                s.registered_at,
                evt.latest_event_time,
            """
                + "    case when (" + abnormalExpression + ")\n"
                + """
                    then 1 else 0
                end as abnormal_flag
            """
                + whereClause
                + " order by coalesce(s.registered_at, evt.latest_event_time) desc, s.id desc",
            query);
        SpecimenWorkflowRepository.SpecimenManagementSummary summary = summarizeSpecimenManagement(whereClause, query);
        return new SpecimenWorkflowRepository.PagedSpecimenManagementItems(items, total, summary);
    }

    private long countSpecimenManagement(String whereClause, SpecimenWorkflowRepository.SpecimenManagementListQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            specimenManagementParams(query),
            Long.class);
        return total == null ? 0L : total;
    }

    private List<SpecimenWorkflowRepository.SpecimenManagementListRow> querySpecimenManagement(
        String sql,
        SpecimenWorkflowRepository.SpecimenManagementListQuery query
    ) {
        MapSqlParameterSource parameters = specimenManagementParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(
            sql + " offset :offset rows fetch next :size rows only",
            parameters,
            this::mapSpecimenManagementListRow);
    }

    private SpecimenWorkflowRepository.SpecimenManagementSummary summarizeSpecimenManagement(
        String whereClause,
        SpecimenWorkflowRepository.SpecimenManagementListQuery query
    ) {
        return jdbcTemplate.queryForObject(
            """
            select
                count(1) as total_count,
                sum(case when s.label_print_status = 'SUCCESS' then 1 else 0 end) as label_printed_count,
                sum(case when s.label_print_status in ('PENDING', 'FAILED') then 1 else 0 end) as pending_label_count,
                sum(case when """ + barcodeUnboundExpression("s") + """
                    then 1 else 0
                end) as unbound_count,
            """
                + "    sum(case when (" + specimenManagementAbnormalExpression("s") + ")\n"
                + """
                    then 1 else 0
                end) as abnormal_count
            """
                + whereClause,
            specimenManagementParams(query),
            this::mapSpecimenManagementSummary);
    }


    private String buildSpecimenManagementFilters(
        SpecimenWorkflowRepository.SpecimenManagementListQuery query,
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
                )
                """);
        }
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            builder.append(" and a.application_no like :applicationNo");
        }
        if (query.departmentId() != null && !query.departmentId().isBlank()) {
            builder.append(" and a.submitting_department_id = :departmentId");
        }
        if (query.buildingId() != null && !query.buildingId().isBlank()) {
            builder.append(" and w.building_id = :buildingId");
        }
        if (query.roomId() != null && !query.roomId().isBlank()) {
            builder.append(" and w.room_id = :roomId");
        }
        if ("UNBOUND".equalsIgnoreCase(query.barcodeBindingStatus())) {
            builder.append(" and ").append(barcodeUnboundExpression("s"));
        } else if ("BOUND".equalsIgnoreCase(query.barcodeBindingStatus())) {
            builder.append(" and ").append(barcodeBoundExpression("s"));
        }
        if (query.specimenStatus() != null && !query.specimenStatus().isBlank()) {
            builder.append(" and s.specimen_status = :specimenStatus");
        }
        if (query.labelPrintStatus() != null && !query.labelPrintStatus().isBlank()) {
            builder.append(" and s.label_print_status = :labelPrintStatus");
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

    private String barcodeUnboundExpression(String specimenAlias) {
        return "(" + specimenAlias + ".barcode is null or trim(" + specimenAlias + ".barcode) = '')";
    }

    private String barcodeBoundExpression(String specimenAlias) {
        return "(" + specimenAlias + ".barcode is not null and trim(" + specimenAlias + ".barcode) <> '')";
    }

    private MapSqlParameterSource duplicateApplicationParams(SpecimenWorkflowRepository.DuplicateApplicationQuery query) {
        return new MapSqlParameterSource()
            .addValue("patientId", query.patientId())
            .addValue("patientName", query.patientName())
            .addValue("externalOrderNo", query.externalOrderNo())
            .addValue("applicationDate", query.applicationDate())
            .addValue("applicationType", query.applicationType())
            .addValue("specimenSite", query.specimenSite());
    }

    private MapSqlParameterSource specimenManagementParams(SpecimenWorkflowRepository.SpecimenManagementListQuery query) {
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
        if (query.buildingId() != null && !query.buildingId().isBlank()) {
            parameters.addValue("buildingId", query.buildingId());
        }
        if (query.roomId() != null && !query.roomId().isBlank()) {
            parameters.addValue("roomId", query.roomId());
        }
        if (query.specimenStatus() != null && !query.specimenStatus().isBlank()) {
            parameters.addValue("specimenStatus", query.specimenStatus());
        }
        if (query.labelPrintStatus() != null && !query.labelPrintStatus().isBlank()) {
            parameters.addValue("labelPrintStatus", query.labelPrintStatus());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        return parameters;
    }

    private SpecimenWorkflowRepository.DuplicateApplicationRow mapDuplicateApplicationRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.DuplicateApplicationRow(
            rs.getString("id"),
            rs.getString("application_no"),
            rs.getString("patient_name"),
            rs.getString("specimen_site"),
            rs.getString("status"),
            rs.getString("current_node"),
            rs.getDate("application_date") == null ? null : rs.getDate("application_date").toLocalDate(),
            rs.getInt("external_order_matched") == 1,
            rs.getInt("same_day_site_matched") == 1);
    }

    private SpecimenWorkflowRepository.SpecimenManagementListRow mapSpecimenManagementListRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.SpecimenManagementListRow(
            rs.getString("specimen_id"),
            rs.getString("specimen_no"),
            rs.getString("barcode"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id"),
            rs.getString("patient_name"),
            JdbcResultSetUtils.getNullableString(rs, "patient_gender"),
            rs.getString("submitting_department_id"),
            rs.getString("submitting_department_name"),
            JdbcResultSetUtils.getNullableString(rs, "building_id"),
            JdbcResultSetUtils.getNullableString(rs, "room_id"),
            JdbcResultSetUtils.getNullableString(rs, "surgery_name"),
            rs.getString("specimen_name"),
            rs.getString("specimen_type"),
            rs.getString("specimen_site"),
            rs.getObject("specimen_count", Integer.class),
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
            rs.getString("verification_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_confirmed_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "specimen_confirmed_by_user_id"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_confirmed_by_name"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_removal_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "specimen_removal_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "check_in_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "checked_in_by_name"),
            rs.getString("label_print_status"),
            rs.getString("label_print_batch_no"),
            JdbcResultSetUtils.getNullableString(rs, "registration_operator_name"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            rs.getTimestamp("latest_event_time") == null ? null : rs.getTimestamp("latest_event_time").toLocalDateTime(),
            rs.getInt("abnormal_flag") == 1);
    }

    private SpecimenWorkflowRepository.SpecimenManagementSummary mapSpecimenManagementSummary(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.SpecimenManagementSummary(
            rs.getLong("total_count"),
            rs.getLong("label_printed_count"),
            rs.getLong("pending_label_count"),
            rs.getLong("abnormal_count"),
            rs.getLong("unbound_count"));
    }
}
