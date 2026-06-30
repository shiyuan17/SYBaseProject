package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.ReworkOrder;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slide;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.SlideQcEvaluation;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.SlideStaining;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slicing;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.Embedding;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingWorkstationRecord;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JdbcTechnicalWorkflowProcessingQueries {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcTechnicalWorkflowRowMappers rowMappers;

    JdbcTechnicalWorkflowProcessingQueries(NamedParameterJdbcTemplate jdbcTemplate, JdbcTechnicalWorkflowRowMappers rowMappers) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMappers = rowMappers;
    }

    Optional<Embedding> findLatestEmbeddingBySamplingBlockId(String samplingBlockId) {
        List<Embedding> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, sampling_id, sampling_block_id, embedding_status,
                   evaluation_level, sampling_evaluation, started_at, ended_at,
                   embedded_by_user_id, embedded_by_name, remarks
            from embeddings
            where sampling_block_id = :samplingBlockId
            order by created_at desc, id desc
            fetch first 1 row only
            """, Map.of("samplingBlockId", samplingBlockId), rowMappers::mapEmbedding);
        return rows.stream().findFirst();
    }

    Optional<EmbeddingBox> findEmbeddingBoxById(String embeddingBoxId) {
        List<EmbeddingBox> rows = jdbcTemplate.query("""
            select *
            from embedding_boxes
            where id = :id
            """, Map.of("id", embeddingBoxId), rowMappers::mapEmbeddingBox);
        return rows.stream().findFirst();
    }

    Optional<EmbeddingBox> findEmbeddingBoxByCaseIdAndNo(String caseId, String embeddingBoxNo) {
        List<EmbeddingBox> rows = jdbcTemplate.query("""
            select *
            from embedding_boxes
            where case_id = :caseId
              and embedding_box_no = :embeddingBoxNo
            """, Map.of(
            "caseId", caseId,
            "embeddingBoxNo", embeddingBoxNo), rowMappers::mapEmbeddingBox);
        return rows.stream().findFirst();
    }

    List<EmbeddingBox> findEmbeddingBoxesByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from embedding_boxes
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), rowMappers::mapEmbeddingBox);
    }

    List<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordsByCaseId(String caseId) {
        return jdbcTemplate.query(embeddingWorkstationSelectSql() + """
            where e.case_id = :caseId
            order by e.ended_at desc, e.created_at desc, e.id desc
            """, Map.of("caseId", caseId), rowMappers::mapEmbeddingWorkstationRecord);
    }

    Optional<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordByEmbeddingId(String embeddingId) {
        List<EmbeddingWorkstationRecord> rows = jdbcTemplate.query(embeddingWorkstationSelectSql() + """
            where e.id = :embeddingId
            """, Map.of("embeddingId", embeddingId), rowMappers::mapEmbeddingWorkstationRecord);
        return rows.stream().findFirst();
    }

    List<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordsByEndedAtRange(
        java.time.LocalDateTime endedFrom,
        java.time.LocalDateTime endedTo
    ) {
        StringBuilder sql = new StringBuilder(embeddingWorkstationSelectSql() + """
            where 1 = 1
            """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (endedFrom != null) {
            sql.append(" and e.ended_at >= :endedFrom\n");
            params.addValue("endedFrom", endedFrom);
        }
        if (endedTo != null) {
            sql.append(" and e.ended_at < :endedTo\n");
            params.addValue("endedTo", endedTo);
        }
        sql.append(" order by e.ended_at desc, e.created_at desc, e.id desc\n");
        return jdbcTemplate.query(sql.toString(), params, rowMappers::mapEmbeddingWorkstationRecord);
    }

    Optional<Slicing> findSlicingById(String slicingId) {
        List<Slicing> rows = jdbcTemplate.query("""
            select id, task_id, case_id, specimen_id, embedding_id, embedding_box_id, slicing_batch_no, slicing_status, slide_count
            from slicings
            where id = :id
            """, Map.of("id", slicingId), rowMappers::mapSlicing);
        return rows.stream().findFirst();
    }

    Optional<Slicing> findSlicingByTaskId(String taskId) {
        List<Slicing> rows = jdbcTemplate.query("""
            select id, task_id, case_id, specimen_id, embedding_id, embedding_box_id, slicing_batch_no, slicing_status, slide_count
            from slicings
            where task_id = :taskId
            order by created_at desc, id desc
            fetch first 1 row only
            """, Map.of("taskId", taskId), rowMappers::mapSlicing);
        return rows.stream().findFirst();
    }

    Optional<Slicing> findSlicingByTaskIdAndEmbeddingBoxId(String taskId, String embeddingBoxId) {
        List<Slicing> rows = jdbcTemplate.query("""
            select id, task_id, case_id, specimen_id, embedding_id, embedding_box_id, slicing_batch_no, slicing_status, slide_count
            from slicings
            where task_id = :taskId
              and embedding_box_id = :embeddingBoxId
            order by created_at desc, id desc
            fetch first 1 row only
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("embeddingBoxId", embeddingBoxId), rowMappers::mapSlicing);
        return rows.stream().findFirst();
    }

    List<Slide> findSlidesByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, combined_slide_flag, quality_status, slide_status, slice_count, created_at
            from slides
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), rowMappers::mapSlide);
    }

    List<Slide> findSlidesBySlicingId(String slicingId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, combined_slide_flag, quality_status, slide_status, slice_count, created_at
            from slides
            where slicing_id = :slicingId
            order by created_at asc, id asc
            """, Map.of("slicingId", slicingId), rowMappers::mapSlide);
    }

    Optional<Slide> findSlideById(String slideId) {
        List<Slide> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, combined_slide_flag, quality_status, slide_status, slice_count, created_at
            from slides
            where id = :slideId
            """, Map.of("slideId", slideId), rowMappers::mapSlide);
        return rows.stream().findFirst();
    }

    List<SlideStaining> findSlideStainingsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, slide_id, staining_type, staining_status, stained_at, quality_issue, remarks
            from slide_stainings
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), rowMappers::mapSlideStaining);
    }

    Optional<ReworkOrder> findReworkOrderById(String reworkOrderId) {
        List<ReworkOrder> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, sampling_block_id, embedding_box_id, slide_id, rework_type, status, reason,
                   requested_at, executed_at, created_at
            from rework_orders
            where id = :id
            """, Map.of("id", reworkOrderId), rowMappers::mapReworkOrder);
        return rows.stream().findFirst();
    }

    List<ReworkOrder> findReworkOrdersByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, sampling_block_id, embedding_box_id, slide_id, rework_type, status, reason,
                   requested_at, executed_at, created_at
            from rework_orders
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), rowMappers::mapReworkOrder);
    }

    List<SlideQcEvaluation> findSlideQcEvaluationsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select sqe.id, sqe.case_id, sqe.specimen_id, sqe.slide_id, s.slide_no, sqe.qc_type, sqe.evaluation_result,
                   sqe.issue_description, sqe.improvement_suggestion, sqe.evaluator_user_id, sqe.evaluator_name,
                   sqe.evaluated_at, sqe.remarks
            from slide_qc_evaluations sqe
            join slides s on s.id = sqe.slide_id
            where sqe.case_id = :caseId
            order by sqe.evaluated_at asc, sqe.created_at asc, sqe.id asc
            """, Map.of("caseId", caseId), rowMappers::mapSlideQcEvaluation);
    }

    TechnicalWorkflowRecords.SlicingWorkbenchStats summarizeSlicingWorkbench(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        MapSqlParameterSource params = buildWorkbenchParams(query);
        Long pendingTodayCount = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              """ + buildWorkbenchTodayWindowFilter("coalesce(t.expected_completed_at, t.created_at)")
            + buildWorkbenchKeywordFilter(query.keyword())
            + buildWorkbenchApplicationTypeFilter(query.applicationType())
            + buildWorkbenchOverdueFilter(query.overdueOnly()), params, Long.class);
        Long pendingTomorrowCount = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              """ + buildWorkbenchTomorrowWindowFilter("coalesce(t.expected_completed_at, t.created_at)")
            + buildWorkbenchKeywordFilter(query.keyword())
            + buildWorkbenchApplicationTypeFilter(query.applicationType()), params, Long.class);
        Long completedDeptTodayCount = jdbcTemplate.queryForObject("""
            select count(distinct t.id)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join slicings slc on slc.task_id = t.id
            where t.task_type = 'SLICING'
              and t.task_status = 'COMPLETED'
              """ + buildWorkbenchTodayWindowFilter("t.completed_at")
            + buildWorkbenchKeywordFilter(query.keyword())
            + buildWorkbenchApplicationTypeFilter(query.applicationType()), params, Long.class);
        Long completedMineTodayCount = jdbcTemplate.queryForObject("""
            select count(distinct t.id)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join slicings slc on slc.task_id = t.id
            where t.task_type = 'SLICING'
              and t.task_status = 'COMPLETED'
              """ + buildWorkbenchTodayWindowFilter("t.completed_at") + """
              and coalesce(slc.sliced_by_user_id, t.assigned_to_user_id, '') = :currentUserId
              """ + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchApplicationTypeFilter(query.applicationType()), params, Long.class);
        Long overdueCount = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              and t.created_at <= :slicingTimedOutBefore
              """ + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchApplicationTypeFilter(query.applicationType()), params, Long.class);
        Long ungroupedPendingPrintCount = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join slicings slc on slc.task_id = t.id
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              and slc.id is null
              and not exists (
                  select 1
                  from slicing_slide_print_merge_group_items mgi
                  join slicing_slide_print_merge_groups mg on mg.id = mgi.group_id
                  where mgi.task_id = t.id
                    and mg.group_status = 'PENDING'
              )
              """ + buildWorkbenchWorkDateFilter() + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchApplicationTypeFilter(query.applicationType()) + buildWorkbenchOverdueFilter(query.overdueOnly()), params, Long.class);
        Long pendingPrintGroupCount = jdbcTemplate.queryForObject("""
            select count(distinct mg.id)
            from slicing_slide_print_merge_groups mg
            join slicing_slide_print_merge_group_items mgi on mgi.group_id = mg.id
            join technical_pending_tasks t on t.id = mgi.task_id
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join slicings slc on slc.task_id = t.id
            where mg.group_status = 'PENDING'
              and slc.id is null
              """ + buildWorkbenchWorkDateFilter() + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchApplicationTypeFilter(query.applicationType()) + buildWorkbenchOverdueFilter(query.overdueOnly()), params, Long.class);
        return new TechnicalWorkflowRecords.SlicingWorkbenchStats(
            pendingTodayCount == null ? 0 : pendingTodayCount,
            pendingTomorrowCount == null ? 0 : pendingTomorrowCount,
            completedMineTodayCount == null ? 0 : completedMineTodayCount,
            completedDeptTodayCount == null ? 0 : completedDeptTodayCount,
            overdueCount == null ? 0 : overdueCount,
            (ungroupedPendingPrintCount == null ? 0 : ungroupedPendingPrintCount)
                + (pendingPrintGroupCount == null ? 0 : pendingPrintGroupCount));
    }

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findPendingSlicingWorkbenchRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        return findPendingSlicingPrintRows(query);
    }

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findPendingSlicingPrintRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        String where = """
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              and slc.id is null
              and not exists (
                  select 1
                  from slicing_slide_print_merge_group_items mgi
                  join slicing_slide_print_merge_groups mg on mg.id = mgi.group_id
                  where mgi.task_id = t.id
                    and mg.group_status = 'PENDING'
              )
            """ + buildWorkbenchWorkDateFilter() + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchApplicationTypeFilter(query.applicationType()) + buildWorkbenchOverdueFilter(query.overdueOnly());
        TechnicalWorkflowRecords.PagedSlicingWorkbenchRows baseRows = findSlicingWorkbenchRows(query, where, 1, 10_000, """
            order by task_status_sort asc,
                     timeout_sort asc,
                     expected_completed_sort asc,
                     task_created_at asc,
                     task_id asc
            """);
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> rows = new ArrayList<>(baseRows.items());
        rows.addAll(findPendingSlicingPrintMergeGroupRows(query));
        rows.sort(Comparator
            .comparing((TechnicalWorkflowRecords.SlicingWorkbenchRow row) -> row.pathologyNo() == null ? "" : row.pathologyNo())
            .thenComparing(row -> row.patientId() == null ? "" : row.patientId())
            .thenComparing(row -> row.embeddingBoxNo() == null ? "" : row.embeddingBoxNo())
            .thenComparing(TechnicalWorkflowRecords.SlicingWorkbenchRow::taskId));
        int fromIndex = Math.max(query.pendingPage() - 1, 0) * query.pendingSize();
        int toIndex = Math.min(fromIndex + query.pendingSize(), rows.size());
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> pageItems =
            fromIndex >= rows.size() ? List.of() : rows.subList(fromIndex, toIndex);
        return new TechnicalWorkflowRecords.PagedSlicingWorkbenchRows(pageItems, rows.size());
    }

    List<TechnicalWorkflowRecords.SlicingWorkbenchRow> findPendingSlicingPrintRowsByTaskIds(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query = new TechnicalWorkflowRecords.SlicingWorkbenchQuery(
            null,
            null,
            false,
            false,
            1,
            Math.max(taskIds.size(), 1),
            1,
            20,
            null,
            null,
            null,
            LocalDateTime.now().toLocalDate().atStartOfDay(),
            LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay(),
            LocalDateTime.now().toLocalDate().plusDays(2).atStartOfDay(),
            LocalDateTime.now().minusDays(1));
        String where = """
            where t.id in (:taskIds)
              and t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              and slc.id is null
              and not exists (
                  select 1
                  from slicing_slide_print_merge_group_items mgi
                  join slicing_slide_print_merge_groups mg on mg.id = mgi.group_id
                  where mgi.task_id = t.id
                    and mg.group_status = 'PENDING'
              )
            """;
        return findSlicingWorkbenchRows(
            query,
            where,
            1,
            Math.max(taskIds.size(), 1),
            " order by embedding_box_no asc, task_id asc\n",
            new MapSqlParameterSource().addValue("taskIds", taskIds)).items();
    }

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findPendingSlicingProcessRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        String where = """
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              and slc.id is not null
              and not exists (
                  select 1
                  from slicing_slide_print_merge_group_items mgi
                  join slicing_slide_print_merge_groups mg on mg.id = mgi.group_id
                  where mgi.task_id = t.id
                    and mg.group_status = 'PRINTED'
                    and mg.printed_slicing_id is not null
              )
            """ + buildWorkbenchWorkDateFilter() + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchApplicationTypeFilter(query.applicationType()) + buildWorkbenchOverdueFilter(query.overdueOnly());
        TechnicalWorkflowRecords.PagedSlicingWorkbenchRows baseRows = findSlicingWorkbenchRows(query, where, 1, 10_000, """
            order by task_status_sort asc,
                     slicing_created_sort asc,
                     slide_no asc,
                     task_id asc
            """);
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> rows = new ArrayList<>(baseRows.items());
        rows.addAll(findPrintedSlicingProcessMergeGroupRows(query));
        rows.sort(Comparator
            .comparing((TechnicalWorkflowRecords.SlicingWorkbenchRow row) -> row.pathologyNo() == null ? "" : row.pathologyNo())
            .thenComparing(row -> row.patientId() == null ? "" : row.patientId())
            .thenComparing(row -> row.embeddingBoxNo() == null ? "" : row.embeddingBoxNo())
            .thenComparing(TechnicalWorkflowRecords.SlicingWorkbenchRow::taskId));
        int fromIndex = Math.max(query.pendingPage() - 1, 0) * query.pendingSize();
        int toIndex = Math.min(fromIndex + query.pendingSize(), rows.size());
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> pageItems =
            fromIndex >= rows.size() ? List.of() : rows.subList(fromIndex, toIndex);
        return new TechnicalWorkflowRecords.PagedSlicingWorkbenchRows(pageItems, rows.size());
    }

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findCompletedSlicingWorkbenchRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        String where = """
            where t.task_type = 'SLICING'
              and t.task_status = 'COMPLETED'
            """ + buildWorkbenchCompletedDateRangeFilter()
            + buildWorkbenchKeywordFilter(query.keyword())
            + buildWorkbenchApplicationTypeFilter(query.applicationType());
        return findSlicingWorkbenchRows(query, where, query.completedPage(), query.completedSize(), """
            order by completed_sort desc,
                     slide_no asc,
                     task_id asc
            """);
    }

    private TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findSlicingWorkbenchRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query,
        String where,
        int page,
        int size,
        String orderBy
    ) {
        return findSlicingWorkbenchRows(query, where, page, size, orderBy, new MapSqlParameterSource());
    }

    private TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findSlicingWorkbenchRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query,
        String where,
        int page,
        int size,
        String orderBy,
        MapSqlParameterSource extraParams
    ) {
        MapSqlParameterSource params = buildWorkbenchParams(query)
            .addValue("limit", size)
            .addValue("offset", Math.max(page - 1, 0) * size);
        if (extraParams != null) {
            extraParams.getValues().forEach(params::addValue);
        }
        String fromSql = """
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join embedding_boxes eb on (t.object_type = 'EMBEDDING_BOX' and t.object_id = eb.id)
                or (t.object_type = 'SAMPLING_BLOCK' and t.object_id = eb.sampling_block_id)
            left join sampling_blocks sb on sb.id = coalesce(
                eb.sampling_block_id,
                case when t.object_type = 'SAMPLING_BLOCK' then t.object_id else null end)
            left join embeddings emb on emb.id = eb.embedding_id
            left join slicings slc on slc.task_id = t.id
            left join slides s on s.slicing_id = slc.id
            """;
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from (
                select t.id
            """ + fromSql + where + """
                group by t.id
            ) grouped_rows
            """, params, Long.class);
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> items = jdbcTemplate.query("""
            select
                t.id as task_id,
                t.case_id,
                a.application_type,
                pc.pathology_no,
                a.patient_name,
                a.patient_id,
                w.id_no as patient_id_display,
                t.specimen_id,
                sp.specimen_name_standardized as specimen_name,
                coalesce(eb.id, case when t.object_type = 'EMBEDDING_BOX' then t.object_id else null end) as embedding_box_id,
                coalesce(eb.embedding_box_no, sb.embedding_box_no) as embedding_box_no,
                min(s.id) as slide_id,
                min(s.slide_no) as slide_no,
                slc.sliced_by_name as slicing_operator_name,
                slc.remarks as slicing_remark,
                slc.sliced_at as completed_at,
                emb.sampling_evaluation as grossing_evaluation,
                emb.evaluation_level as embedding_evaluation,
                emb.embedded_by_name as embedding_operator_name,
                emb.remarks as embedding_clear_remark,
                coalesce(emb.remarks, sb.embedding_remarks) as embedding_remarks,
                t.production_remarks as shift_remark,
                eb.slice_notice,
                a.submitting_department_name,
                t.task_status,
                case
                    when slc.id is null then 'PENDING'
                    when count(s.id) > 0 then 'PRINTED'
                    else 'PENDING'
                end as slide_print_status,
                count(s.id) as printed_slide_count,
                max(coalesce(s.combined_slide_flag, 0)) as combined_slide,
                case when t.created_at <= :slicingTimedOutBefore then 1 else 0 end as timed_out,
                case when eb.id is null then 0 else 1 end as selectable,
                t.created_at as task_created_at,
                case when t.task_status = 'IN_PROGRESS' then 0 else 1 end as task_status_sort,
                case when t.created_at <= :slicingTimedOutBefore then 0 else 1 end as timeout_sort,
                coalesce(t.expected_completed_at, t.created_at) as expected_completed_sort,
                slc.created_at as slicing_created_sort,
                coalesce(slc.sliced_at, t.completed_at) as completed_sort
            """ + fromSql + where + """
            group by t.id, t.case_id, a.application_type, pc.pathology_no, a.patient_name, a.patient_id, w.id_no,
                     t.specimen_id, sp.specimen_name_standardized, t.object_type, t.object_id, eb.id, eb.embedding_box_no,
                     sb.embedding_box_no, slc.sliced_by_name,
                     slc.remarks, slc.sliced_at, emb.sampling_evaluation, emb.evaluation_level,
                     emb.embedded_by_name, emb.remarks, sb.embedding_remarks, t.production_remarks, eb.slice_notice,
                     a.submitting_department_name,
                     t.task_status, slc.id, t.created_at, t.expected_completed_at, slc.created_at, t.completed_at
            """ + orderBy + """
            offset :offset rows fetch next :limit rows only
            """, params, (rs, rowNum) -> new TechnicalWorkflowRecords.SlicingWorkbenchRow(
            rs.getString("task_id"),
            rs.getString("case_id"),
            JdbcResultSetUtils.getNullableString(rs, "application_type"),
            rs.getString("pathology_no"),
            rs.getString("patient_name"),
            rs.getString("patient_id"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id_display"),
            rs.getString("specimen_id"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_name"),
            rs.getString("embedding_box_id"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_box_no"),
            JdbcResultSetUtils.getNullableString(rs, "slide_id"),
            JdbcResultSetUtils.getNullableString(rs, "slide_no"),
            JdbcResultSetUtils.getNullableString(rs, "slicing_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "slicing_remark"),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            JdbcResultSetUtils.getNullableString(rs, "grossing_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_clear_remark"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_remarks"),
            JdbcResultSetUtils.getNullableString(rs, "shift_remark"),
            JdbcResultSetUtils.getNullableString(rs, "slice_notice"),
            JdbcResultSetUtils.getNullableString(rs, "submitting_department_name"),
            rs.getString("task_status"),
            JdbcResultSetUtils.getNullableString(rs, "slide_print_status"),
            rs.getInt("printed_slide_count"),
            rs.getInt("combined_slide") != 0,
            rs.getInt("timed_out") != 0,
            rs.getInt("selectable") != 0,
            null,
            false,
            List.of(rs.getString("task_id")),
            List.of(rs.getString("embedding_box_id"))));
        return new TechnicalWorkflowRecords.PagedSlicingWorkbenchRows(items, total == null ? 0 : total);
    }

    List<TechnicalWorkflowRecords.SlicingSlidePrintMergeGroupItem> findPendingSlicingPrintMergeGroupItems(String printGroupId) {
        return jdbcTemplate.query("""
            select
                mg.id as group_id,
                t.id as task_id,
                t.case_id,
                pc.pathology_no,
                a.patient_id,
                w.id_no as patient_id_display,
                eb.id as embedding_box_id,
                eb.embedding_box_no,
                mgi.sequence_no
            from slicing_slide_print_merge_groups mg
            join slicing_slide_print_merge_group_items mgi on mgi.group_id = mg.id
            join technical_pending_tasks t on t.id = mgi.task_id
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            join embedding_boxes eb on eb.id = mgi.embedding_box_id
            left join slicings slc on slc.task_id = t.id
            where mg.id = :printGroupId
              and mg.group_status = 'PENDING'
              and slc.id is null
            order by mgi.sequence_no asc
            """, Map.of("printGroupId", printGroupId), (rs, rowNum) -> new TechnicalWorkflowRecords.SlicingSlidePrintMergeGroupItem(
            rs.getString("group_id"),
            rs.getString("task_id"),
            rs.getString("case_id"),
            JdbcResultSetUtils.getNullableString(rs, "pathology_no"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id_display"),
            rs.getString("embedding_box_id"),
            rs.getString("embedding_box_no"),
            rs.getInt("sequence_no")));
    }

    private List<TechnicalWorkflowRecords.SlicingWorkbenchRow> findPendingSlicingPrintMergeGroupRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        MapSqlParameterSource params = buildWorkbenchParams(query);
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> itemRows = jdbcTemplate.query("""
            select
                mg.id as print_group_id,
                mg.embedding_box_no as merged_embedding_box_no,
                t.id as task_id,
                t.case_id,
                a.application_type,
                pc.pathology_no,
                a.patient_name,
                a.patient_id,
                w.id_no as patient_id_display,
                t.specimen_id,
                sp.specimen_name_standardized as specimen_name,
                eb.id as embedding_box_id,
                eb.embedding_box_no,
                null as slide_id,
                null as slide_no,
                null as slicing_operator_name,
                null as slicing_remark,
                null as completed_at,
                emb.sampling_evaluation as grossing_evaluation,
                emb.evaluation_level as embedding_evaluation,
                emb.embedded_by_name as embedding_operator_name,
                emb.remarks as embedding_clear_remark,
                coalesce(emb.remarks, sb.embedding_remarks) as embedding_remarks,
                t.production_remarks as shift_remark,
                eb.slice_notice,
                a.submitting_department_name,
                t.task_status,
                'PENDING' as slide_print_status,
                case when t.created_at <= :slicingTimedOutBefore then 1 else 0 end as timed_out,
                mgi.sequence_no
            from slicing_slide_print_merge_groups mg
            join slicing_slide_print_merge_group_items mgi on mgi.group_id = mg.id
            join technical_pending_tasks t on t.id = mgi.task_id
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            join embedding_boxes eb on eb.id = mgi.embedding_box_id
            left join sampling_blocks sb on sb.id = eb.sampling_block_id
            left join embeddings emb on emb.id = eb.embedding_id
            left join slicings slc on slc.task_id = t.id
            where mg.group_status = 'PENDING'
              and slc.id is null
              """ + buildWorkbenchWorkDateFilter() + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchApplicationTypeFilter(query.applicationType()) + buildWorkbenchOverdueFilter(query.overdueOnly()) + """
            order by mg.created_at asc, mgi.sequence_no asc
            """, params, (rs, rowNum) -> new TechnicalWorkflowRecords.SlicingWorkbenchRow(
            rs.getString("task_id"),
            rs.getString("case_id"),
            JdbcResultSetUtils.getNullableString(rs, "application_type"),
            JdbcResultSetUtils.getNullableString(rs, "pathology_no"),
            JdbcResultSetUtils.getNullableString(rs, "patient_name"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id_display"),
            rs.getString("specimen_id"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_name"),
            rs.getString("embedding_box_id"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_box_no"),
            null,
            null,
            null,
            null,
            null,
            JdbcResultSetUtils.getNullableString(rs, "grossing_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_clear_remark"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_remarks"),
            JdbcResultSetUtils.getNullableString(rs, "shift_remark"),
            JdbcResultSetUtils.getNullableString(rs, "slice_notice"),
            JdbcResultSetUtils.getNullableString(rs, "submitting_department_name"),
            rs.getString("task_status"),
            "PENDING",
            0,
            true,
            rs.getInt("timed_out") != 0,
            true,
            rs.getString("print_group_id"),
            true,
            List.of(rs.getString("task_id")),
            List.of(rs.getString("embedding_box_id"))));
        Map<String, List<TechnicalWorkflowRecords.SlicingWorkbenchRow>> groupedRows = new LinkedHashMap<>();
        for (TechnicalWorkflowRecords.SlicingWorkbenchRow row : itemRows) {
            groupedRows.computeIfAbsent(row.printGroupId(), ignored -> new ArrayList<>()).add(row);
        }
        return groupedRows.values().stream().map(this::mergePendingSlicingPrintGroupRow).toList();
    }

    private TechnicalWorkflowRecords.SlicingWorkbenchRow mergePendingSlicingPrintGroupRow(
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> rows
    ) {
        TechnicalWorkflowRecords.SlicingWorkbenchRow first = rows.get(0);
        List<String> taskIds = rows.stream().map(TechnicalWorkflowRecords.SlicingWorkbenchRow::taskId).toList();
        List<String> embeddingBoxIds = rows.stream().map(TechnicalWorkflowRecords.SlicingWorkbenchRow::embeddingBoxId).toList();
        List<String> embeddingBoxNos = rows.stream()
            .map(TechnicalWorkflowRecords.SlicingWorkbenchRow::embeddingBoxNo)
            .filter(value -> value != null && !value.isBlank())
            .toList();
        int printedSlideCount = rows.stream()
            .mapToInt(TechnicalWorkflowRecords.SlicingWorkbenchRow::printedSlideCount)
            .sum();
        boolean combinedSlide = rows.stream().anyMatch(TechnicalWorkflowRecords.SlicingWorkbenchRow::combinedSlide);
        return new TechnicalWorkflowRecords.SlicingWorkbenchRow(
            first.printGroupId(),
            first.caseId(),
            first.applicationType(),
            first.pathologyNo(),
            first.patientName(),
            first.patientId(),
            first.patientIdDisplay(),
            first.specimenId(),
            first.specimenName(),
            String.join("+", embeddingBoxIds),
            embeddingBoxNos.isEmpty() ? null : String.join("+", embeddingBoxNos),
            null,
            null,
            first.slicingOperatorName(),
            first.slicingRemark(),
            first.completedAt(),
            first.grossingEvaluation(),
            first.embeddingEvaluation(),
            first.embeddingOperatorName(),
            first.embeddingClearRemark(),
            first.embeddingRemarks(),
            first.shiftRemark(),
            first.sliceNotice(),
            first.submittingDepartmentName(),
            first.taskStatus(),
            first.slidePrintStatus(),
            printedSlideCount,
            combinedSlide,
            rows.stream().anyMatch(TechnicalWorkflowRecords.SlicingWorkbenchRow::timedOut),
            true,
            first.printGroupId(),
            true,
            taskIds,
            embeddingBoxIds);
    }

    private List<TechnicalWorkflowRecords.SlicingWorkbenchRow> findPrintedSlicingProcessMergeGroupRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        MapSqlParameterSource params = buildWorkbenchParams(query);
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> itemRows = jdbcTemplate.query("""
            select
                mg.id as print_group_id,
                mg.embedding_box_no as merged_embedding_box_no,
                t.id as task_id,
                t.case_id,
                a.application_type,
                pc.pathology_no,
                a.patient_name,
                a.patient_id,
                w.id_no as patient_id_display,
                t.specimen_id,
                sp.specimen_name_standardized as specimen_name,
                eb.id as embedding_box_id,
                eb.embedding_box_no,
                min(s.id) as slide_id,
                min(s.slide_no) as slide_no,
                slc.sliced_by_name as slicing_operator_name,
                slc.remarks as slicing_remark,
                null as completed_at,
                emb.sampling_evaluation as grossing_evaluation,
                emb.evaluation_level as embedding_evaluation,
                emb.embedded_by_name as embedding_operator_name,
                emb.remarks as embedding_clear_remark,
                coalesce(emb.remarks, sb.embedding_remarks) as embedding_remarks,
                t.production_remarks as shift_remark,
                mg.embedding_box_no as slice_notice,
                a.submitting_department_name,
                t.task_status,
                'PRINTED' as slide_print_status,
                count(s.id) as printed_slide_count,
                max(coalesce(s.combined_slide_flag, 0)) as combined_slide,
                case when t.created_at <= :slicingTimedOutBefore then 1 else 0 end as timed_out,
                mg.created_at as print_group_created_at,
                mgi.sequence_no
            from slicing_slide_print_merge_groups mg
            join slicing_slide_print_merge_group_items mgi on mgi.group_id = mg.id
            join technical_pending_tasks t on t.id = mgi.task_id
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join application_registration_workbench w on w.application_id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            join embedding_boxes eb on eb.id = mgi.embedding_box_id
            left join sampling_blocks sb on sb.id = eb.sampling_block_id
            left join embeddings emb on emb.id = eb.embedding_id
            join slicings slc on slc.id = mg.printed_slicing_id
            left join slides s on s.slicing_id = slc.id
            where mg.group_status = 'PRINTED'
              and mg.printed_slicing_id is not null
              and t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              """ + buildWorkbenchWorkDateFilter() + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchApplicationTypeFilter(query.applicationType()) + buildWorkbenchOverdueFilter(query.overdueOnly()) + """
            group by mg.id, mg.embedding_box_no, t.id, t.case_id, a.application_type, pc.pathology_no, a.patient_name,
                     a.patient_id, w.id_no, t.specimen_id, sp.specimen_name_standardized, eb.id, eb.embedding_box_no,
                     slc.sliced_by_name, slc.remarks, emb.sampling_evaluation, emb.evaluation_level,
                     emb.embedded_by_name, emb.remarks, sb.embedding_remarks, t.production_remarks,
                     a.submitting_department_name, t.task_status, t.created_at, mg.created_at, mgi.sequence_no
            order by print_group_created_at asc, mgi.sequence_no asc
            """, params, (rs, rowNum) -> new TechnicalWorkflowRecords.SlicingWorkbenchRow(
            rs.getString("task_id"),
            rs.getString("case_id"),
            JdbcResultSetUtils.getNullableString(rs, "application_type"),
            JdbcResultSetUtils.getNullableString(rs, "pathology_no"),
            JdbcResultSetUtils.getNullableString(rs, "patient_name"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id"),
            JdbcResultSetUtils.getNullableString(rs, "patient_id_display"),
            rs.getString("specimen_id"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_name"),
            rs.getString("embedding_box_id"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_box_no"),
            JdbcResultSetUtils.getNullableString(rs, "slide_id"),
            JdbcResultSetUtils.getNullableString(rs, "slide_no"),
            JdbcResultSetUtils.getNullableString(rs, "slicing_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "slicing_remark"),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            JdbcResultSetUtils.getNullableString(rs, "grossing_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_clear_remark"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_remarks"),
            JdbcResultSetUtils.getNullableString(rs, "shift_remark"),
            JdbcResultSetUtils.getNullableString(rs, "slice_notice"),
            JdbcResultSetUtils.getNullableString(rs, "submitting_department_name"),
            rs.getString("task_status"),
            "PRINTED",
            rs.getInt("printed_slide_count"),
            rs.getInt("combined_slide") != 0,
            rs.getInt("timed_out") != 0,
            true,
            rs.getString("print_group_id"),
            true,
            List.of(rs.getString("task_id")),
            List.of(rs.getString("embedding_box_id"))));
        Map<String, List<TechnicalWorkflowRecords.SlicingWorkbenchRow>> groupedRows = new LinkedHashMap<>();
        for (TechnicalWorkflowRecords.SlicingWorkbenchRow row : itemRows) {
            groupedRows.computeIfAbsent(row.printGroupId(), ignored -> new ArrayList<>()).add(row);
        }
        return groupedRows.values().stream().map(this::mergePendingSlicingPrintGroupRow).toList();
    }

    List<TechnicalWorkflowRecords.CaseMediaAsset> findCaseMediaAssets(
        String caseId,
        String objectType,
        String objectId,
        String mediaType
    ) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, object_type, object_id, media_type, file_url, file_name,
                   captured_at, captured_by_user_id, captured_by_name, remarks
            from case_media_assets
            where case_id = :caseId
              and object_type = :objectType
              and object_id = :objectId
              and media_type = :mediaType
            order by captured_at asc, created_at asc, id asc
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("objectType", objectType)
            .addValue("objectId", objectId)
            .addValue("mediaType", mediaType), rowMappers::mapCaseMediaAsset);
    }

    List<TechnicalWorkflowRecords.CaseMediaAsset> findCaseMediaAssets(
        String caseId,
        String objectType,
        String mediaType
    ) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, object_type, object_id, media_type, file_url, file_name,
                   captured_at, captured_by_user_id, captured_by_name, remarks
            from case_media_assets
            where case_id = :caseId
              and object_type = :objectType
              and media_type = :mediaType
            order by captured_at asc, created_at asc, id asc
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("objectType", objectType)
            .addValue("mediaType", mediaType), rowMappers::mapCaseMediaAsset);
    }

    Optional<TechnicalWorkflowRecords.CaseMediaAsset> findCaseMediaAssetById(String assetId) {
        List<TechnicalWorkflowRecords.CaseMediaAsset> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, object_type, object_id, media_type, file_url, file_name,
                   captured_at, captured_by_user_id, captured_by_name, remarks
            from case_media_assets
            where id = :assetId
            """, Map.of("assetId", assetId), rowMappers::mapCaseMediaAsset);
        return rows.stream().findFirst();
    }

    private String embeddingWorkstationSelectSql() {
        return """
            select
                t.id as task_id,
                e.case_id,
                pc.pathology_no,
                e.specimen_id,
                s.specimen_name_standardized as specimen_name,
                e.sampling_block_id,
                sb.block_code as sampling_block_code,
                sb.block_description as sampling_block_description,
                sm.gross_description,
                e.id as embedding_id,
                box.id as embedding_box_id,
                box.embedding_box_no,
                box.slice_notice,
                e.evaluation_level,
                e.sampling_evaluation,
                e.remarks as embedding_remarks,
                sm.sampled_by_name,
                sm.sampled_at,
                e.embedded_by_name,
                e.started_at,
                e.ended_at,
                coalesce(t.task_status, 'COMPLETED') as task_status
            from embeddings e
            join pathology_cases pc on pc.id = e.case_id
            join specimens s on s.id = e.specimen_id
            left join sampling_blocks sb on sb.id = e.sampling_block_id
            left join samplings sm on sm.id = e.sampling_id
            left join embedding_boxes box on box.embedding_id = e.id
            left join technical_pending_tasks t
              on t.task_type = 'EMBEDDING'
             and t.object_type = 'SAMPLING_BLOCK'
             and t.object_id = e.sampling_block_id
            """;
    }

    private MapSqlParameterSource buildWorkbenchParams(TechnicalWorkflowRecords.SlicingWorkbenchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("dateFrom", query.dateFrom())
            .addValue("dateToExclusive", query.dateToExclusive())
            .addValue("todayStart", query.todayStart())
            .addValue("tomorrowStart", query.tomorrowStart())
            .addValue("dayAfterTomorrowStart", query.dayAfterTomorrowStart())
            .addValue("currentUserId", query.currentUserId() == null ? "" : query.currentUserId())
            .addValue("slicingTimedOutBefore", query.slicingTimedOutBefore());
        if (query.keyword() != null && !query.keyword().isBlank()) {
            params.addValue("keywordLike", "%" + query.keyword().trim().toUpperCase() + "%");
        }
        if (query.applicationType() != null && !query.applicationType().isBlank()) {
            params.addValue("applicationType", query.applicationType().trim());
        }
        return params;
    }

    private String buildWorkbenchKeywordFilter(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        return """
              and (
                    upper(coalesce(pc.pathology_no, '')) like :keywordLike
                 or upper(coalesce(a.patient_id, '')) like :keywordLike
                 or upper(coalesce(a.patient_name, '')) like :keywordLike
                 or upper(coalesce(a.application_no, '')) like :keywordLike
                 or upper(coalesce(sp.specimen_name_standardized, '')) like :keywordLike
              )
            """;
    }

    private String buildWorkbenchApplicationTypeFilter(String applicationType) {
        if (applicationType == null || applicationType.isBlank()) {
            return "";
        }
        return " and a.application_type = :applicationType\n";
    }

    private String buildWorkbenchWorkDateFilter() {
        return buildWorkbenchTaskDateRangeFilter("coalesce(t.expected_completed_at, t.created_at)");
    }

    private String buildWorkbenchCompletedDateRangeFilter() {
        return buildWorkbenchTaskDateRangeFilter("t.completed_at");
    }

    private String buildWorkbenchTaskDateRangeFilter(String expression) {
        return """
              and (:dateFrom is null or %s >= :dateFrom)
              and (:dateToExclusive is null or %s < :dateToExclusive)
            """.formatted(expression, expression);
    }

    private String buildWorkbenchTodayWindowFilter(String expression) {
        return """
              and %s >= :todayStart
              and %s < :tomorrowStart
            """.formatted(expression, expression);
    }

    private String buildWorkbenchTomorrowWindowFilter(String expression) {
        return """
              and %s >= :tomorrowStart
              and %s < :dayAfterTomorrowStart
            """.formatted(expression, expression);
    }

    private String buildWorkbenchOverdueFilter(boolean enabled) {
        if (!enabled) {
            return "";
        }
        return """
              and t.created_at <= :slicingTimedOutBefore
            """;
    }

    List<TrackingEvent> findTrackingEventsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", caseId), rowMappers::mapTrackingEvent);
    }

    List<TrackingEvent> findRecentTrackingEventsByCaseId(String caseId, int limit) {
        return jdbcTemplate.query("""
            select *
            from workflow_events
            where case_id = :caseId
            order by event_time desc, created_at desc
            offset 0 rows fetch next :limit rows only
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("limit", limit), rowMappers::mapTrackingEvent);
    }

    Optional<TechnicalWorkflowRecords.WorkstationDailyClearRecord> findWorkstationDailyClear(
        String workstationType,
        LocalDate workDate
    ) {
        List<TechnicalWorkflowRecords.WorkstationDailyClearRecord> rows = jdbcTemplate.query("""
            select id, workstation_type, work_date, operator_user_id, operator_name,
                   cleared_at, clear_status, operator_ip
            from workstation_daily_clears
            where workstation_type = :workstationType
              and work_date = :workDate
            """, new MapSqlParameterSource()
            .addValue("workstationType", workstationType)
            .addValue("workDate", workDate), rowMappers::mapWorkstationDailyClearRecord);
        return rows.stream().findFirst();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
