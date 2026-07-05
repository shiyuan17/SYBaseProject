package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcSpecimenWorkflowQueryRepository
    extends AbstractJdbcSpecimenWorkflowProjectionSupport
    implements SpecimenWorkflowQueryRepository {

    private static final String APPLICATION_TYPE_FROZEN = "FROZEN";
    private static final String CASE_STATUS_REQUESTED = "REQUESTED";
    private static final String CURRENT_NODE_FROZEN_APPOINTMENT = "APPOINTMENT";

    public JdbcSpecimenWorkflowQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public SpecimenWorkflowRepository.PagedApplications findApplications(SpecimenWorkflowRepository.ApplicationListQuery query) {
        String whereClause = """
            from applications a
            where 1 = 1
            """ + buildApplicationFilters(query);
        long total = countApplications(whereClause, query);
        List<SpecimenWorkflowRepository.ApplicationListRow> items = queryApplications("""
            select
                a.id,
                a.application_no,
                (
                    select pc.pathology_no
                    from pathology_cases pc
                    where pc.application_id = a.id
                    order by coalesce(pc.updated_at, pc.created_at) desc, pc.id desc
                    fetch next 1 rows only
                ) as pathology_no,
                a.patient_name,
                a.patient_gender,
                a.patient_age,
                a.status,
                a.submitting_department_name,
                a.submitting_doctor_name,
                a.application_type,
                a.application_form_status,
                case
                    when a.status = 'VOIDED' then a.status
                    when a.application_type = 'FROZEN'
                         and exists (
                             select 1
                             from pathology_cases pc
                             where pc.application_id = a.id
                               and pc.case_status = 'REQUESTED'
                         )
                    then 'APPOINTMENT'
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
                end as current_node,
                case
                    when exists (
                        select 1
                        from specimens s
                        where s.application_id = a.id
                          and (
                              s.specimen_status in ('REJECTED', 'RETURNED')
                              or s.fixation_status = 'ABNORMAL'
                          )
                    )
                    then 1 else 0
                end as abnormal_flag,
                (
                    select count(1)
                    from specimens s
                    where s.application_id = a.id
                ) as registered_specimen_count,
                (
                    select listagg(trim(s.specimen_no), ',') within group (
                        order by coalesce(s.registered_at, s.created_at), s.id
                    )
                    from specimens s
                    where s.application_id = a.id
                ) as specimen_nos,
                  
                (
                    select case
                        when sum(case when sb.label_print_status = 'FAILED' then 1 else 0 end) > 0 then 'FAILED'
                        when sum(case when sb.label_print_status = 'PENDING' then 1 else 0 end) > 0 then 'PENDING'
                        when sum(case when sb.label_print_status = 'SUCCESS' then 1 else 0 end) > 0 then 'SUCCESS'
                        else null
                    end
                    from specimens sb
                    where sb.application_id = a.id
                      and sb.label_print_batch_no = (
                          select latest.label_print_batch_no
                          from specimens latest
                          where latest.application_id = a.id
                            and latest.label_print_batch_no is not null
                          order by latest.registered_at desc, latest.created_at desc, latest.id desc
                          fetch next 1 rows only
                        )
                ) as latest_label_print_status,
                case
                    when a.status = 'VOIDED' then 0
                    when exists (
                        select 1
                        from specimens s
                        where s.application_id = a.id
                          and (
                              s.fixation_status <> 'PENDING'
                              or s.specimen_status <> 'REGISTERED'
                          )
                    )
                    or exists (
                        select 1
                        from pathology_cases pc
                        where pc.application_id = a.id
                    )
                    then 0
                    else 1
                end as editable,
                case
                    when a.status = 'VOIDED' then 0
                    when exists (
                        select 1
                        from specimens s
                        where s.application_id = a.id
                          and (
                              s.fixation_status <> 'PENDING'
                              or s.specimen_status <> 'REGISTERED'
                          )
                    )
                    or exists (
                        select 1
                        from pathology_cases pc
                        where pc.application_id = a.id
                    )
                    then 0
                    else 1
                end as deletable,
                case when a.status = 'VOIDED' then 1 else 0 end as voided,
                case
                    when a.status = 'VOIDED' then '申请单已作废，不能再编辑或作废'
                    when exists (
                        select 1
                        from specimens s
                        where s.application_id = a.id
                          and (
                              s.fixation_status <> 'PENDING'
                              or s.specimen_status <> 'REGISTERED'
                          )
                    )
                    or exists (
                        select 1
                        from pathology_cases pc
                        where pc.application_id = a.id
                    )
                    then '申请单已进入下游流程，不能再编辑或作废'
                    else null
                end as operation_disabled_reason,
                a.application_date,
                a.submission_date,
                a.created_at,
                a.updated_at
            """ + whereClause + " order by coalesce(a.updated_at, a.created_at) desc, a.id desc", query);
        return new SpecimenWorkflowRepository.PagedApplications(items, total);
    }

    @Override
    public ApplicationTracking getApplicationTracking(String applicationId, Application application) {
        List<Specimen> specimens = findSpecimensByApplicationId(applicationId);
        List<TrackingEvent> events = findTrackingEventsByApplicationId(applicationId);
        java.util.Optional<PathologyCase> pathologyCase = findPathologyCaseByApplicationId(applicationId);
        boolean abnormal = specimens.stream().anyMatch(specimen ->
            specimen.specimenStatus() == SpecimenStatus.REJECTED
                || specimen.specimenStatus() == SpecimenStatus.RETURNED
                || specimen.fixationStatus() == FixationStatus.ABNORMAL);
        String currentNode = application.getStatus() == ApplicationStatus.VOIDED
            ? application.getStatus().name()
            : isFrozenRequested(pathologyCase.orElse(null), application)
                ? CURRENT_NODE_FROZEN_APPOINTMENT
            : events.isEmpty()
                ? application.getStatus().name()
                : events.get(events.size() - 1).nodeCode();
        return new ApplicationTracking(application, currentNode, abnormal, specimens, events);
    }

    private boolean isFrozenRequested(PathologyCase pathologyCase, Application application) {
        return pathologyCase != null
            && APPLICATION_TYPE_FROZEN.equalsIgnoreCase(application.getApplicationType())
            && CASE_STATUS_REQUESTED.equalsIgnoreCase(pathologyCase.caseStatus());
    }

    @Override
    public List<SpecimenWorkflowRepository.SpecimenVerificationRecordRow> listSpecimenVerificationRecords(String barcode) {
        return jdbcTemplate.query("""
            select
                records.application_id,
                records.specimen_id,
                records.barcode,
                records.verification_type,
                records.result,
                records.operator_name,
                records.terminal_code,
                records.remarks,
                records.verified_at
            from (
                select
                    we.application_id,
                    we.specimen_id,
                    s.barcode,
                    case
                        when we.node_code = 'VERIFICATION' and we.event_type = 'STARTED' then 'SPECIMEN_VERIFICATION_START'
                        when we.node_code = 'VERIFICATION' and we.event_type = 'COMPLETED' then 'SPECIMEN_VERIFICATION_COMPLETE'
                        when we.node_code = 'CONFIRMATION' and we.event_type = 'COMPLETED' then 'SPECIMEN_CONFIRM'
                        when we.node_code = 'CHECK_IN' and we.event_type = 'CHECKED_IN' then 'SPECIMEN_CHECK_IN'
                        else we.event_type
                    end as verification_type,
                    we.event_status as result,
                    we.operator_name,
                    we.source_terminal as terminal_code,
                    we.event_content as remarks,
                    we.event_time as verified_at
                from workflow_events we
                join specimens s on s.id = we.specimen_id
                where s.barcode = :barcode
                  and we.node_code in ('VERIFICATION', 'CONFIRMATION', 'CHECK_IN')

                union all

                select
                    toi.application_id,
                    toi.specimen_id,
                    s.barcode,
                    'TRANSPORT_HANDOVER_VERIFICATION' as verification_type,
                    coalesce(toi.verification_result, toi.item_status) as result,
                    toi.verified_by_name as operator_name,
                    cast(null as varchar(64)) as terminal_code,
                    toi.remarks,
                    toi.verified_at
                from transport_order_items toi
                join specimens s on s.id = toi.specimen_id
                where s.barcode = :barcode
                  and toi.verified_at is not null
            ) records
            order by records.verified_at desc, records.verification_type desc
            """, Map.of("barcode", barcode), (rs, rowNum) -> new SpecimenWorkflowRepository.SpecimenVerificationRecordRow(
            rs.getString("application_id"),
            rs.getString("specimen_id"),
            rs.getString("barcode"),
            rs.getString("verification_type"),
            rs.getString("result"),
            rs.getString("operator_name"),
            rs.getString("terminal_code"),
            rs.getString("remarks"),
            rs.getTimestamp("verified_at") == null ? null : rs.getTimestamp("verified_at").toLocalDateTime()
        ));
    }

    private long countApplications(String whereClause, SpecimenWorkflowRepository.ApplicationListQuery query) {
        Long total = jdbcTemplate.queryForObject(
            "select count(1) " + whereClause,
            applicationParams(query),
            Long.class);
        return total == null ? 0L : total;
    }

    private List<SpecimenWorkflowRepository.ApplicationListRow> queryApplications(String sql, SpecimenWorkflowRepository.ApplicationListQuery query) {
        MapSqlParameterSource parameters = applicationParams(query)
            .addValue("offset", Math.max(0, (query.page() - 1) * query.size()))
            .addValue("size", query.size());
        return jdbcTemplate.query(sql + " offset :offset rows fetch next :size rows only", parameters, this::mapApplicationListRow);
    }

    private String buildApplicationFilters(SpecimenWorkflowRepository.ApplicationListQuery query) {
        StringBuilder builder = new StringBuilder();
        if (!"VOIDED".equalsIgnoreCase(query.applicationFormStatus())) {
            builder.append(" and a.status <> 'VOIDED'");
        }
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            builder.append(" and a.application_no like :applicationNo");
        }
        if (query.pathologyNo() != null && !query.pathologyNo().isBlank()) {
            builder.append("""
                 and exists (
                     select 1
                     from pathology_cases pc
                     where pc.application_id = a.id
                       and upper(coalesce(pc.pathology_no, '')) like :pathologyNo
                 )
                """);
        }
        if (query.patientName() != null && !query.patientName().isBlank()) {
            builder.append(" and a.patient_name like :patientName");
        }
        if (query.submittingDepartmentId() != null && !query.submittingDepartmentId().isBlank()) {
            builder.append(" and a.submitting_department_id = :submittingDepartmentId");
        }
        if (query.applicationType() != null && !query.applicationType().isBlank()) {
            builder.append(" and a.application_type = :applicationType");
        }
        if (query.applicationFormStatus() != null && !query.applicationFormStatus().isBlank()) {
            if ("VOIDED".equalsIgnoreCase(query.applicationFormStatus())) {
                builder.append(" and a.status = 'VOIDED'");
            } else {
                builder.append(" and a.application_form_status = :applicationFormStatus");
            }
        }
        if (query.dateFrom() != null) {
            builder.append(" and a.application_date >= :dateFrom");
        }
        if (query.dateTo() != null) {
            builder.append(" and a.application_date < :dateTo");
        }
        return builder.toString();
    }

    private MapSqlParameterSource applicationParams(SpecimenWorkflowRepository.ApplicationListQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) {
            parameters.addValue("applicationNo", "%" + query.applicationNo() + "%");
        }
        if (query.pathologyNo() != null && !query.pathologyNo().isBlank()) {
            parameters.addValue("pathologyNo", "%" + query.pathologyNo().toUpperCase() + "%");
        }
        if (query.patientName() != null && !query.patientName().isBlank()) {
            parameters.addValue("patientName", "%" + query.patientName() + "%");
        }
        if (query.submittingDepartmentId() != null && !query.submittingDepartmentId().isBlank()) {
            parameters.addValue("submittingDepartmentId", query.submittingDepartmentId());
        }
        if (query.applicationType() != null && !query.applicationType().isBlank()) {
            parameters.addValue("applicationType", query.applicationType());
        }
        if (query.applicationFormStatus() != null && !query.applicationFormStatus().isBlank()) {
            parameters.addValue("applicationFormStatus", query.applicationFormStatus());
        }
        if (query.dateFrom() != null) {
            parameters.addValue("dateFrom", query.dateFrom());
        }
        if (query.dateTo() != null) {
            parameters.addValue("dateTo", query.dateTo());
        }
        return parameters;
    }

    private SpecimenWorkflowRepository.ApplicationListRow mapApplicationListRow(ResultSet rs, int rowNum) throws SQLException {
        return new SpecimenWorkflowRepository.ApplicationListRow(
            rs.getString("id"),
            rs.getString("application_no"),
            JdbcResultSetUtils.getNullableString(rs, "pathology_no"),
            rs.getString("patient_name"),
            rs.getString("patient_gender"),
            rs.getString("patient_age"),
            rs.getString("status"),
            rs.getString("submitting_department_name"),
            rs.getString("submitting_doctor_name"),
            rs.getString("application_type"),
            rs.getString("application_form_status"),
            rs.getString("current_node"),
            rs.getInt("abnormal_flag") == 1,
            rs.getInt("registered_specimen_count"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_nos"),
            
            rs.getString("latest_label_print_status"),
            rs.getInt("editable") == 1,
            rs.getInt("deletable") == 1,
            rs.getInt("voided") == 1,
            JdbcResultSetUtils.getNullableString(rs, "operation_disabled_reason"),
            rs.getDate("application_date") == null ? null : rs.getDate("application_date").toLocalDate(),
            rs.getDate("submission_date") == null ? null : rs.getDate("submission_date").toLocalDate(),
            rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
    }
}


