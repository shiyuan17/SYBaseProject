package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

abstract class AbstractJdbcSpecimenWorkflowProjectionSupport extends AbstractJdbcSpecimenWorkflowReadSupport {

    protected AbstractJdbcSpecimenWorkflowProjectionSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    public SpecimenWorkflowRepository.PagedPendingSpecimens findPendingFixations(SpecimenWorkflowRepository.PendingSpecimenQuery query) {
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

    public SpecimenWorkflowRepository.PagedPendingSpecimens findPendingReceipts(SpecimenWorkflowRepository.PendingSpecimenQuery query) {
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

    public SpecimenWorkflowRepository.PagedPendingTransportOrders findPendingTransportOrders(
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

    public List<SpecimenWorkflowRepository.DuplicateApplicationRow> findDuplicateApplications(
        SpecimenWorkflowRepository.DuplicateApplicationQuery query
    ) {
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
            where (
                (:patientId is not null and a.patient_id = :patientId)
                or (:patientName is not null and a.patient_name = :patientName)
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

    public SpecimenWorkflowRepository.PagedSpecimenManagementItems findSpecimenManagementItems(
        SpecimenWorkflowRepository.SpecimenManagementListQuery query
    ) {
        String abnormalExpression = specimenManagementAbnormalExpression("s");
        String whereClause = """
            from specimens s
            join applications a on a.id = s.application_id
            left join specimen_fixation_records sfr on sfr.specimen_id = s.id
            left join (
                select specimen_id, max(event_time) as latest_event_time
                from workflow_events
                group by specimen_id
            ) evt on evt.specimen_id = s.id
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
                a.patient_name,
                a.submitting_department_id,
                a.submitting_department_name,
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
                + checkInStatusSelect("s", "check_in_status")
                + checkedInAtSelect("s")
                + checkedInByNameSelect("s")
                + """
                s.label_print_status,
                s.label_print_batch_no,
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

    public SpecimenWorkflowRepository.PagedSpecimenRemovalItems findSpecimenRemovalItems(
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

    public List<SpecimenWorkflowRepository.SpecimenRemovalListRow> listSpecimenRemovalExportRows(
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
                .append(buildPendingVerificationExpression(specimenAlias, fixationRecordAlias))
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

    private String buildPendingVerificationExpression(String specimenAlias, String fixationRecordAlias) {
        return buildVerificationStatusExpression(fixationRecordAlias, specimenAlias);
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
            rs.getString("patient_name"),
            rs.getString("submitting_department_id"),
            rs.getString("submitting_department_name"),
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
            JdbcResultSetUtils.getNullableString(rs, "check_in_status"),
            JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at") == null
                ? null
                : JdbcResultSetUtils.getNullableTimestamp(rs, "checked_in_at").toLocalDateTime(),
            JdbcResultSetUtils.getNullableString(rs, "checked_in_by_name"),
            rs.getString("label_print_status"),
            rs.getString("label_print_batch_no"),
            rs.getTimestamp("registered_at") == null ? null : rs.getTimestamp("registered_at").toLocalDateTime(),
            rs.getTimestamp("latest_event_time") == null ? null : rs.getTimestamp("latest_event_time").toLocalDateTime(),
            rs.getInt("abnormal_flag") == 1);
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

    private SpecimenWorkflowRepository.SpecimenManagementSummary mapSpecimenManagementSummary(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.SpecimenManagementSummary(
            rs.getLong("total_count"),
            rs.getLong("label_printed_count"),
            rs.getLong("pending_label_count"),
            rs.getLong("abnormal_count"));
    }
}
