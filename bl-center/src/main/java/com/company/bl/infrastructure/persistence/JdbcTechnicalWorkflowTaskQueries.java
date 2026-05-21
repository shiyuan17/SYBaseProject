package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatch;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatchItem;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedTechnicalTasks;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PendingTechnicalTaskQuery;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.SamplingBlock;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.TechnicalTask;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JdbcTechnicalWorkflowTaskQueries {

    private static final List<String> ACTIVE_TASK_STATUSES = List.of("PENDING", "IN_PROGRESS");

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

    List<Specimen> findSpecimensByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from specimens
            where case_id = :caseId
            order by specimen_no asc, created_at asc
            """, Map.of("caseId", caseId), rowMappers::mapSpecimen);
    }

    Optional<Specimen> findSpecimenById(String specimenId) {
        List<Specimen> rows = jdbcTemplate.query("""
            select *
            from specimens
            where id = :specimenId
            """, Map.of("specimenId", specimenId), rowMappers::mapSpecimen);
        return rows.stream().findFirst();
    }

    Optional<TechnicalTask> findTechnicalTaskById(String taskId) {
        List<TechnicalTask> rows = jdbcTemplate.query(taskSelectSql() + """
            where t.id = :taskId
            """, Map.of("taskId", taskId), rowMappers::mapTechnicalTask);
        return rows.stream().findFirst();
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

    PagedTechnicalTasks findTechnicalTasks(PendingTechnicalTaskQuery query) {
        String where = " where 1 = 1 " + buildTaskFilters(query);
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            """ + where, taskFilterParams(query), Long.class);
        List<TechnicalTask> items = jdbcTemplate.query(taskSelectSql() + where + """

            order by t.created_at asc, t.id asc
            offset :offset rows fetch next :limit rows only
            """, taskPageParams(query), rowMappers::mapTechnicalTask);
        return new PagedTechnicalTasks(items, total == null ? 0 : total);
    }

    List<SamplingBlock> findSamplingBlocksByIds(List<String> samplingBlockIds) {
        if (samplingBlockIds == null || samplingBlockIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
            select *
            from sampling_blocks
            where id in (:ids)
            order by sequence_no asc, id asc
            """, new MapSqlParameterSource().addValue("ids", samplingBlockIds), rowMappers::mapSamplingBlock);
    }

    Optional<SamplingBlock> findSamplingBlockById(String samplingBlockId) {
        List<SamplingBlock> rows = jdbcTemplate.query("""
            select *
            from sampling_blocks
            where id = :id
            """, Map.of("id", samplingBlockId), rowMappers::mapSamplingBlock);
        return rows.stream().findFirst();
    }

    List<SamplingBlock> findSamplingBlocksByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from sampling_blocks
            where case_id = :caseId
            order by sequence_no asc, id asc
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
                t.case_id,
                pc.pathology_no,
                t.specimen_id,
                t.task_type,
                t.task_status,
                t.object_type,
                t.object_id,
                t.parent_task_id,
                t.payload,
                t.remarks,
                t.created_at,
                t.started_at,
                t.completed_at
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            """;
    }

    private String buildTaskFilters(PendingTechnicalTaskQuery query) {
        StringBuilder builder = new StringBuilder();
        if (hasText(query.taskType())) {
            builder.append(" and t.task_type = :taskType");
        }
        if (hasText(query.taskStatus())) {
            builder.append(" and t.task_status = :taskStatus");
        }
        if (hasText(query.applicationNo())) {
            builder.append(" and a.application_no = :applicationNo");
        }
        if (hasText(query.pathologyNo())) {
            builder.append(" and pc.pathology_no = :pathologyNo");
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
        }
        if (hasText(query.applicationNo())) {
            params.addValue("applicationNo", query.applicationNo());
        }
        if (hasText(query.pathologyNo())) {
            params.addValue("pathologyNo", query.pathologyNo());
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
            params.addValue("activeStatuses", ACTIVE_TASK_STATUSES);
            params.addValue("grossingTimedOutBefore", query.grossingTimedOutBefore());
            params.addValue("dehydrationTimedOutBefore", query.dehydrationTimedOutBefore());
            params.addValue("stainingTimedOutBefore", query.stainingTimedOutBefore());
        }
        return params;
    }

    private MapSqlParameterSource taskPageParams(PendingTechnicalTaskQuery query) {
        return taskFilterParams(query)
            .addValue("limit", query.size())
            .addValue("offset", Math.max(query.page() - 1, 0) * query.size());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
