package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatch;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatchItem;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedTechnicalTasks;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PendingTechnicalTaskQuery;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.SamplingBlock;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.TechnicalTask;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JdbcTechnicalWorkflowTaskQueries {

    private static final List<String> ACTIVE_TASK_STATUSES = List.of(
        "PENDING",
        "IN_PROGRESS",
        "EMBEDDING_CONFIRM_PENDING");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcTechnicalWorkflowRowMappers rowMappers;

    JdbcTechnicalWorkflowTaskQueries(NamedParameterJdbcTemplate jdbcTemplate, JdbcTechnicalWorkflowRowMappers rowMappers) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMappers = rowMappers;
    }

    Optional<PathologyCase> findPathologyCaseById(String caseId) {
        List<PathologyCase> rows = jdbcTemplate.query("""
            select *
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", caseId), rowMappers::mapPathologyCase);
        return rows.stream().findFirst();
    }

    Optional<PathologyCase> findPathologyCaseByPathologyNo(String pathologyNo) {
        List<PathologyCase> rows = jdbcTemplate.query("""
            select *
            from pathology_cases
            where pathology_no = :pathologyNo
            """, Map.of("pathologyNo", pathologyNo), rowMappers::mapPathologyCase);
        return rows.stream().findFirst();
    }

    void lockPathologyCase(String caseId) {
        jdbcTemplate.queryForObject("""
            select id
            from pathology_cases
            where id = :caseId
            for update
            """, Map.of("caseId", caseId), String.class);
    }

    List<Specimen> findSpecimensByCaseId(String caseId) {
        return jdbcTemplate.query(specimenSelectSql() + """
            from specimens
            left join specimen_fixation_records sfr on sfr.specimen_id = specimens.id
            where specimens.case_id = :caseId
              and specimens.specimen_status not in ('REJECTED', 'RETURNED')
            order by specimens.specimen_no asc, specimens.created_at asc
            """, Map.of("caseId", caseId), rowMappers::mapSpecimen);
    }

    Optional<Specimen> findSpecimenById(String specimenId) {
        List<Specimen> rows = jdbcTemplate.query(specimenSelectSql() + """
            from specimens
            left join specimen_fixation_records sfr on sfr.specimen_id = specimens.id
            where specimens.id = :specimenId
            """, Map.of("specimenId", specimenId), rowMappers::mapSpecimen);
        return rows.stream().findFirst();
    }

    Optional<TechnicalTask> findTechnicalTaskById(String taskId) {
        List<TechnicalTask> rows = jdbcTemplate.query(taskSelectSql() + """
            where t.id = :taskId
            """, Map.of("taskId", taskId), rowMappers::mapTechnicalTask);
        return rows.stream().findFirst();
    }

    List<TechnicalTask> findTechnicalTasksByCaseId(String caseId) {
        return jdbcTemplate.query(taskSelectSql() + """
            where t.case_id = :caseId
            order by t.created_at asc, t.id asc
            """, Map.of("caseId", caseId), rowMappers::mapTechnicalTask);
    }

    List<TechnicalTask> findActiveTechnicalTasksByCaseId(String caseId) {
        return jdbcTemplate.query(taskSelectSql() + """
            where t.case_id = :caseId
              and t.task_status in (:statuses)
            order by t.created_at asc, t.id asc
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("statuses", ACTIVE_TASK_STATUSES), rowMappers::mapTechnicalTask);
    }

    List<TechnicalTask> findActiveTechnicalTasksByObject(String taskType, String objectType, String objectId) {
        return jdbcTemplate.query(taskSelectSql() + """
            where t.task_type = :taskType
              and t.object_type = :objectType
              and t.object_id = :objectId
              and t.task_status in (:statuses)
            order by t.created_at asc, t.id asc
            """, new MapSqlParameterSource()
            .addValue("taskType", taskType)
            .addValue("objectType", objectType)
            .addValue("objectId", objectId)
            .addValue("statuses", ACTIVE_TASK_STATUSES), rowMappers::mapTechnicalTask);
    }

    List<TechnicalTask> findActiveTechnicalTasksByTypeAndCreatedRange(
        String taskType,
        java.time.LocalDateTime createdFrom,
        java.time.LocalDateTime createdTo
    ) {
        StringBuilder sql = new StringBuilder(taskSelectSql() + """
            where t.task_type = :taskType
              and t.task_status in (:statuses)
            """);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("taskType", taskType)
            .addValue("statuses", ACTIVE_TASK_STATUSES);
        if (createdFrom != null) {
            sql.append(" and t.created_at >= :createdFrom\n");
            params.addValue("createdFrom", createdFrom);
        }
        if (createdTo != null) {
            sql.append(" and t.created_at < :createdTo\n");
            params.addValue("createdTo", createdTo);
        }
        sql.append(" order by t.created_at asc, t.id asc\n");
        return jdbcTemplate.query(sql.toString(), params, rowMappers::mapTechnicalTask);
    }

    PagedTechnicalTasks findTechnicalTasks(PendingTechnicalTaskQuery query) {
        String where = " where 1 = 1 " + buildTaskFilters(query);
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join patients p
                on p.id = a.patient_id
                or p.patient_no = a.patient_id
                or p.inpatient_no = a.patient_id
                or p.outpatient_no = a.patient_id
            left join application_registration_workbench w on w.application_id = t.application_id
            """ + where, taskFilterParams(query), Long.class);
        List<TechnicalTask> items = jdbcTemplate.query(taskSelectSql() + where + """

            order by t.created_at asc, t.id asc
            offset :offset rows fetch next :limit rows only
            """, taskPageParams(query), rowMappers::mapTechnicalTask);
        return new PagedTechnicalTasks(items, total == null ? 0 : total);
    }

    TechnicalWorkflowRecords.PagedTechnicalTrackingCases findTechnicalTrackingCases(
        TechnicalWorkflowRecords.TechnicalTrackingCaseListQuery query
    ) {
        String activityUnionSql = """
            select
                t.case_id,
                t.created_at as activity_at,
                'TASK' as activity_type
            from technical_pending_tasks t
            where (:dateFrom is null or t.created_at >= :dateFrom)
              and (:dateToExclusive is null or t.created_at < :dateToExclusive)
            union all
            select
                e.case_id,
                coalesce(e.ended_at, e.started_at, e.created_at) as activity_at,
                'EMBEDDING' as activity_type
            from embeddings e
            where (:dateFrom is null or coalesce(e.ended_at, e.started_at, e.created_at) >= :dateFrom)
              and (:dateToExclusive is null or coalesce(e.ended_at, e.started_at, e.created_at) < :dateToExclusive)
            union all
            select
                s.case_id,
                s.created_at as activity_at,
                'SLIDE' as activity_type
            from slides s
            where (:dateFrom is null or s.created_at >= :dateFrom)
              and (:dateToExclusive is null or s.created_at < :dateToExclusive)
            union all
            select
                sq.case_id,
                sq.evaluated_at as activity_at,
                'QC' as activity_type
            from slide_qc_evaluations sq
            where (:dateFrom is null or sq.evaluated_at >= :dateFrom)
              and (:dateToExclusive is null or sq.evaluated_at < :dateToExclusive)
            union all
            select
                ro.case_id,
                coalesce(ro.executed_at, ro.requested_at, ro.created_at) as activity_at,
                'REWORK' as activity_type
            from rework_orders ro
            where (:dateFrom is null or coalesce(ro.executed_at, ro.requested_at, ro.created_at) >= :dateFrom)
              and (:dateToExclusive is null or coalesce(ro.executed_at, ro.requested_at, ro.created_at) < :dateToExclusive)
            union all
            select
                we.case_id,
                we.event_time as activity_at,
                'EVENT' as activity_type
            from workflow_events we
            where (:dateFrom is null or we.event_time >= :dateFrom)
              and (:dateToExclusive is null or we.event_time < :dateToExclusive)
            """;
        String baseSql = """
            with tracking_activities as (
            """ + activityUnionSql + """
            ),
            matched_cases as (
                select
                    ta.case_id,
                    max(ta.activity_at) as latest_activity_at
                from tracking_activities ta
                where ta.activity_at is not null
                group by ta.case_id
            ),
            matched_activity_types as (
                select
                    ta.case_id,
                    listagg(distinct ta.activity_type, ',') within group (order by ta.activity_type) as matched_activity_types
                from tracking_activities ta
                where ta.activity_at is not null
                group by ta.case_id
            )
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("dateFrom", query.dateFrom())
            .addValue("dateToExclusive", query.dateToExclusive());

        Long total = jdbcTemplate.queryForObject(baseSql + """
            select count(1)
            from matched_cases mc
            """, params, Long.class);

        List<TechnicalWorkflowRecords.TechnicalTrackingCaseListItem> items =
            jdbcTemplate.query(baseSql + """
                select
                    pc.id as case_id,
                    pc.pathology_no,
                    a.patient_name,
                    w.id_no as patient_id_display,
                    a.application_no,
                    a.application_type,
                    a.submitting_department_name,
                    pc.case_status,
                    mc.latest_activity_at,
                    mat.matched_activity_types
                from matched_cases mc
                join pathology_cases pc on pc.id = mc.case_id
                join applications a on a.id = pc.application_id
                left join application_registration_workbench w on w.application_id = a.id
                join matched_activity_types mat on mat.case_id = mc.case_id
                order by mc.latest_activity_at desc, pc.id desc
                offset :offset rows fetch next :limit rows only
                """,
                params
                    .addValue("offset", Math.max(query.page() - 1, 0) * query.size())
                    .addValue("limit", query.size()),
                (rs, rowNum) -> new TechnicalWorkflowRecords.TechnicalTrackingCaseListItem(
                    rs.getString("case_id"),
                    JdbcResultSetUtils.getNullableString(rs, "pathology_no"),
                    JdbcResultSetUtils.getNullableString(rs, "patient_name"),
                    JdbcResultSetUtils.getNullableString(rs, "patient_id_display"),
                    JdbcResultSetUtils.getNullableString(rs, "application_no"),
                    JdbcResultSetUtils.getNullableString(rs, "application_type"),
                    JdbcResultSetUtils.getNullableString(rs, "submitting_department_name"),
                    JdbcResultSetUtils.getNullableString(rs, "case_status"),
                    toLocalDateTime(rs.getTimestamp("latest_activity_at")),
                    splitMatchedActivityTypes(
                        JdbcResultSetUtils.getNullableString(
                            rs,
                            "matched_activity_types"
                        )
                    )
                ));
        return new TechnicalWorkflowRecords.PagedTechnicalTrackingCases(
            items,
            total == null ? 0 : total
        );
    }

    List<SamplingBlock> findSamplingBlocksByIds(List<String> samplingBlockIds) {
        if (samplingBlockIds == null || samplingBlockIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
            select sb.id, sb.case_id, sb.specimen_id, sb.sampling_id, sb.sequence_no, sb.block_code,
                   sb.block_site, sb.block_description, sb.embedding_box_no, sb.embedding_box_name, sb.special_requirement,
                   sb.embedding_remarks,
                   s.specimen_name_standardized as specimen_name, sm.gross_description
            from sampling_blocks sb
            left join specimens s on s.id = sb.specimen_id
            left join samplings sm on sm.id = sb.sampling_id
            where sb.id in (:ids)
            order by sb.sequence_no asc, sb.id asc
            """, new MapSqlParameterSource().addValue("ids", samplingBlockIds), rowMappers::mapSamplingBlock);
    }

    Optional<SamplingBlock> findSamplingBlockById(String samplingBlockId) {
        List<SamplingBlock> rows = jdbcTemplate.query("""
            select sb.id, sb.case_id, sb.specimen_id, sb.sampling_id, sb.sequence_no, sb.block_code,
                   sb.block_site, sb.block_description, sb.embedding_box_no, sb.embedding_box_name, sb.special_requirement,
                   sb.embedding_remarks,
                   s.specimen_name_standardized as specimen_name, sm.gross_description
            from sampling_blocks sb
            left join specimens s on s.id = sb.specimen_id
            left join samplings sm on sm.id = sb.sampling_id
            where sb.id = :id
            """, Map.of("id", samplingBlockId), rowMappers::mapSamplingBlock);
        return rows.stream().findFirst();
    }

    List<SamplingBlock> findSamplingBlocksByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select sb.id, sb.case_id, sb.specimen_id, sb.sampling_id, sb.sequence_no, sb.block_code,
                   sb.block_site, sb.block_description, sb.embedding_box_no, sb.embedding_box_name, sb.special_requirement,
                   sb.embedding_remarks,
                   s.specimen_name_standardized as specimen_name, sm.gross_description
            from sampling_blocks sb
            left join specimens s on s.id = sb.specimen_id
            left join samplings sm on sm.id = sb.sampling_id
            where sb.case_id = :caseId
            order by sb.sequence_no asc, sb.id asc
            """, Map.of("caseId", caseId), rowMappers::mapSamplingBlock);
    }

    Optional<DehydrationBatch> findDehydrationBatchById(String batchId) {
        List<DehydrationBatch> rows = jdbcTemplate.query("""
            select *
            from dehydration_batches
            where id = :id
            """, Map.of("id", batchId), rowMappers::mapDehydrationBatch);
        return rows.stream().findFirst();
    }

    List<DehydrationBatchItem> findDehydrationBatchItems(String batchId) {
        return jdbcTemplate.query("""
            select *
            from dehydration_batch_items
            where batch_id = :batchId
            order by loaded_at asc, id asc
            """, Map.of("batchId", batchId), rowMappers::mapDehydrationBatchItem);
    }

    private String taskSelectSql() {
        return """
            select
                t.id,
                t.application_id,
                a.application_no,
                a.patient_name,
                coalesce(p.patient_no, p.inpatient_no, p.outpatient_no, a.patient_id) as patient_id,
                w.id_no as patient_id_display,
                t.case_id,
                pc.pathology_no,
                t.specimen_id,
                t.task_type,
                t.task_status,
                t.object_type,
                t.object_id,
                coalesce(slide_box.embedding_box_no, slide_block.embedding_box_no, slide_block.block_code, slide.slide_no, sb.embedding_box_no, sb.block_code, t.object_id) as object_display_no,
                sb.block_code as sampling_block_code,
                sb.block_description as sampling_block_description,
                sb.embedding_remarks as embedding_remarks,
                s.specimen_name_standardized as specimen_name,
                sm.gross_description as gross_description,
                sm.sampled_by_name,
                sm.sampled_at,
                t.parent_task_id,
                t.priority,
                t.current_node,
                t.station_code,
                t.station_name,
                t.assigned_to_user_id,
                t.assigned_to_name,
                t.expected_completed_at,
                coalesce(t.production_remarks, parent.production_remarks) as production_remarks,
                t.received_at,
                t.payload,
                t.remarks,
                t.created_at,
                t.started_at,
                t.completed_at
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join patients p
              on p.id = a.patient_id
              or p.patient_no = a.patient_id
              or p.inpatient_no = a.patient_id
              or p.outpatient_no = a.patient_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join technical_pending_tasks parent on parent.id = t.parent_task_id
            left join sampling_blocks sb
              on t.object_type = 'SAMPLING_BLOCK'
             and t.object_id = sb.id
            left join specimens s on s.id = coalesce(sb.specimen_id, t.specimen_id)
            left join slides slide
              on t.object_type = 'SLIDE'
             and t.object_id = slide.id
            left join sampling_blocks slide_block
              on slide.sampling_block_id = slide_block.id
            left join embedding_boxes slide_box
              on slide.embedding_box_id = slide_box.id
            left join samplings sm on sb.sampling_id = sm.id
            """;
    }

    private String specimenSelectSql() {
        return """
            select
                specimens.*,
                case
                    when coalesce(sfr.verification_completed_at, sfr.verified_at) is not null
                    then 'VERIFIED'
                    when sfr.verification_started_at is not null
                    then 'VERIFYING'
                    else 'UNVERIFIED'
                end as verification_status,
                sfr.verification_started_at,
                coalesce(sfr.verification_completed_at, sfr.verified_at) as verification_completed_at,
                sfr.verified_by_user_id,
                sfr.verified_by_name,
                cast(null as varchar(32)) as resolved_check_in_status,
                cast(null as timestamp) as checked_in_at,
                cast(null as varchar(100)) as checked_in_by_name,
                cast(null as varchar(32)) as latest_receipt_status,
                cast(null as varchar(64)) as latest_quality_check_result,
                cast(null as varchar(255)) as latest_quality_issue_codes
            """;
    }

    private String buildTaskFilters(PendingTechnicalTaskQuery query) {
        StringBuilder builder = new StringBuilder();
        if (hasText(query.taskType())) {
            builder.append(" and t.task_type = :taskType");
        }
        if (hasText(query.taskStatus())) {
            builder.append(" and t.task_status = :taskStatus");
        } else if (!query.includeAllStatuses()) {
            builder.append(" and t.task_status in (:activeStatuses)");
        }
        if (hasText(query.priority())) {
            builder.append(" and t.priority = :priority");
        }
        if (hasText(query.assignedToUserId())) {
            builder.append(" and t.assigned_to_user_id = :assignedToUserId");
        }
        if (hasText(query.currentNode())) {
            builder.append(" and t.current_node = :currentNode");
        }
        if (hasText(query.taskId())) {
            builder.append(" and t.id = :taskId");
        }
        if (hasText(query.applicationNo())) {
            builder.append(" and a.application_no = :applicationNo");
        }
        if (hasText(query.pathologyNo())) {
            builder.append(" and pc.pathology_no = :pathologyNo");
        }
        if (hasText(query.keyword())) {
            builder.append("""
                 and (
                    upper(coalesce(pc.pathology_no, '')) like :keywordLike
                    or upper(coalesce(p.patient_no, p.inpatient_no, p.outpatient_no, a.patient_id, '')) like :keywordLike
                    or upper(coalesce(a.patient_name, '')) like :keywordLike
                    or upper(coalesce(a.application_no, '')) like :keywordLike
                 )
                """);
        }
        if (hasText(query.objectType())) {
            builder.append(" and t.object_type = :objectType");
        }
        if (query.createdFrom() != null) {
            builder.append(" and t.created_at >= :createdFrom");
        }
        if (query.createdTo() != null) {
            builder.append(" and t.created_at <= :createdTo");
        }
        if (query.timedOutOnly()) {
            builder.append("""
                 and (
                    (t.task_type = 'GROSSING' and t.task_status in (:activeStatuses) and t.created_at <= :grossingTimedOutBefore)
                    or (t.task_type = 'DEHYDRATION' and t.task_status in (:activeStatuses) and t.created_at <= :dehydrationTimedOutBefore)
                    or (t.task_type = 'SLICING' and t.task_status in (:activeStatuses) and t.created_at <= :slicingTimedOutBefore)
                    or (t.task_type = 'STAINING' and t.task_status in (:activeStatuses) and t.created_at <= :stainingTimedOutBefore)
                 )
                """);
        }
        return builder.toString();
    }

    private MapSqlParameterSource taskFilterParams(PendingTechnicalTaskQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (hasText(query.taskType())) {
            params.addValue("taskType", query.taskType());
        }
        if (hasText(query.taskStatus())) {
            params.addValue("taskStatus", query.taskStatus());
        } else if (!query.includeAllStatuses()) {
            params.addValue("activeStatuses", ACTIVE_TASK_STATUSES);
        }
        if (query.timedOutOnly() && !params.hasValue("activeStatuses")) {
            params.addValue("activeStatuses", ACTIVE_TASK_STATUSES);
        }
        if (hasText(query.priority())) {
            params.addValue("priority", query.priority());
        }
        if (hasText(query.assignedToUserId())) {
            params.addValue("assignedToUserId", query.assignedToUserId());
        }
        if (hasText(query.currentNode())) {
            params.addValue("currentNode", query.currentNode());
        }
        if (hasText(query.taskId())) {
            params.addValue("taskId", query.taskId());
        }
        if (hasText(query.applicationNo())) {
            params.addValue("applicationNo", query.applicationNo());
        }
        if (hasText(query.pathologyNo())) {
            params.addValue("pathologyNo", query.pathologyNo());
        }
        if (hasText(query.keyword())) {
            params.addValue("keywordLike", "%" + query.keyword().trim().toUpperCase() + "%");
        }
        if (hasText(query.objectType())) {
            params.addValue("objectType", query.objectType());
        }
        if (query.createdFrom() != null) {
            params.addValue("createdFrom", query.createdFrom());
        }
        if (query.createdTo() != null) {
            params.addValue("createdTo", query.createdTo());
        }
        if (query.timedOutOnly()) {
            params.addValue("grossingTimedOutBefore", query.grossingTimedOutBefore());
            params.addValue("dehydrationTimedOutBefore", query.dehydrationTimedOutBefore());
            params.addValue("slicingTimedOutBefore", query.slicingTimedOutBefore());
            params.addValue("stainingTimedOutBefore", query.stainingTimedOutBefore());
        }
        return params;
    }

    private MapSqlParameterSource taskPageParams(PendingTechnicalTaskQuery query) {
        return taskFilterParams(query)
            .addValue("limit", query.size())
            .addValue("offset", Math.max(query.page() - 1, 0) * query.size());
    }

    private List<String> splitMatchedActivityTypes(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(rawValue.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private java.time.LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
