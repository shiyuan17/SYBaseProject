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
import java.time.LocalDateTime;
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

    Optional<EmbeddingBox> findEmbeddingBoxByNo(String embeddingBoxNo) {
        List<EmbeddingBox> rows = jdbcTemplate.query("""
            select *
            from embedding_boxes
            where embedding_box_no = :embeddingBoxNo
            """, Map.of("embeddingBoxNo", embeddingBoxNo), rowMappers::mapEmbeddingBox);
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

    List<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordsByEndedAtRange(
        java.time.LocalDateTime endedFrom,
        java.time.LocalDateTime endedTo
    ) {
        return jdbcTemplate.query(embeddingWorkstationSelectSql() + """
            where e.ended_at >= :endedFrom
              and e.ended_at < :endedTo
            order by e.ended_at desc, e.created_at desc, e.id desc
            """, new MapSqlParameterSource()
            .addValue("endedFrom", endedFrom)
            .addValue("endedTo", endedTo), rowMappers::mapEmbeddingWorkstationRecord);
    }

    Optional<Slicing> findSlicingById(String slicingId) {
        List<Slicing> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, embedding_id, embedding_box_id, slicing_batch_no, slicing_status, slide_count
            from slicings
            where id = :id
            """, Map.of("id", slicingId), rowMappers::mapSlicing);
        return rows.stream().findFirst();
    }

    List<Slide> findSlidesByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, quality_status, slide_status, slice_count
            from slides
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), rowMappers::mapSlide);
    }

    List<Slide> findSlidesBySlicingId(String slicingId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, quality_status, slide_status, slice_count
            from slides
            where slicing_id = :slicingId
            order by created_at asc, id asc
            """, Map.of("slicingId", slicingId), rowMappers::mapSlide);
    }

    Optional<Slide> findSlideById(String slideId) {
        List<Slide> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, quality_status, slide_status, slice_count
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
            select id, case_id, specimen_id, sampling_block_id, embedding_box_id, slide_id, rework_type, status, reason
            from rework_orders
            where id = :id
            """, Map.of("id", reworkOrderId), rowMappers::mapReworkOrder);
        return rows.stream().findFirst();
    }

    List<ReworkOrder> findReworkOrdersByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, sampling_block_id, embedding_box_id, slide_id, rework_type, status, reason
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
            left join specimens sp on sp.id = t.specimen_id
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              and coalesce(t.expected_completed_at, t.created_at) >= :todayStart
              and coalesce(t.expected_completed_at, t.created_at) < :tomorrowStart
              """ + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchOverdueFilter(query.overdueOnly()), params, Long.class);
        Long pendingTomorrowCount = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              and coalesce(t.expected_completed_at, t.created_at) >= :tomorrowStart
              and coalesce(t.expected_completed_at, t.created_at) < :dayAfterTomorrowStart
              """ + buildWorkbenchKeywordFilter(query.keyword()), params, Long.class);
        Long completedDeptTodayCount = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join slicings slc
              on slc.case_id = t.case_id
             and slc.embedding_box_id = t.object_id
             and slc.sliced_at = t.completed_at
            left join slides s on s.slicing_id = slc.id
            where t.task_type = 'SLICING'
              and t.task_status = 'COMPLETED'
              and t.completed_at >= :todayStart
              and t.completed_at < :tomorrowStart
              """ + buildWorkbenchKeywordFilter(query.keyword()), params, Long.class);
        Long completedMineTodayCount = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join slicings slc
              on slc.case_id = t.case_id
             and slc.embedding_box_id = t.object_id
             and slc.sliced_at = t.completed_at
            left join slides s on s.slicing_id = slc.id
            where t.task_type = 'SLICING'
              and t.task_status = 'COMPLETED'
              and t.completed_at >= :todayStart
              and t.completed_at < :tomorrowStart
              and coalesce(slc.sliced_by_user_id, t.assigned_to_user_id, '') = :currentUserId
              """ + buildWorkbenchKeywordFilter(query.keyword()), params, Long.class);
        Long overdueCount = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
              and t.created_at <= :slicingTimedOutBefore
              """ + buildWorkbenchKeywordFilter(query.keyword()), params, Long.class);
        return new TechnicalWorkflowRecords.SlicingWorkbenchStats(
            pendingTodayCount == null ? 0 : pendingTodayCount,
            pendingTomorrowCount == null ? 0 : pendingTomorrowCount,
            completedMineTodayCount == null ? 0 : completedMineTodayCount,
            completedDeptTodayCount == null ? 0 : completedDeptTodayCount,
            overdueCount == null ? 0 : overdueCount,
            0L);
    }

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findPendingSlicingWorkbenchRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        String where = """
            where t.task_type = 'SLICING'
              and t.task_status in ('PENDING', 'IN_PROGRESS')
            """ + buildWorkbenchKeywordFilter(query.keyword()) + buildWorkbenchTodayFilter(query.pendingTodayOnly()) + buildWorkbenchOverdueFilter(query.overdueOnly());
        MapSqlParameterSource params = buildWorkbenchParams(query)
            .addValue("limit", query.pendingSize())
            .addValue("offset", Math.max(query.pendingPage() - 1, 0) * query.pendingSize());
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join embedding_boxes eb on t.object_type = 'EMBEDDING_BOX' and t.object_id = eb.id
            left join embeddings emb on emb.id = eb.embedding_id
            """ + where, params, Long.class);
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> items = jdbcTemplate.query("""
            select
                t.id as task_id,
                t.case_id,
                pc.pathology_no,
                a.patient_name,
                a.patient_id,
                t.specimen_id,
                sp.specimen_name_standardized as specimen_name,
                t.object_id as embedding_box_id,
                cast(null as varchar(64)) as slide_id,
                cast(null as varchar(64)) as slide_no,
                cast(null as varchar(100)) as slicing_operator_name,
                cast(null as varchar(500)) as slicing_remark,
                cast(null as timestamp) as completed_at,
                emb.sampling_evaluation as grossing_evaluation,
                emb.evaluation_level as embedding_evaluation,
                emb.embedded_by_name as embedding_operator_name,
                emb.remarks as embedding_clear_remark,
                t.production_remarks as shift_remark,
                eb.slice_notice,
                t.task_status,
                case when t.created_at <= :slicingTimedOutBefore then 1 else 0 end as timed_out,
                1 as selectable
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join embedding_boxes eb on t.object_type = 'EMBEDDING_BOX' and t.object_id = eb.id
            left join embeddings emb on emb.id = eb.embedding_id
            """ + where + """
            order by case when t.task_status = 'IN_PROGRESS' then 0 else 1 end,
                     case when t.created_at <= :slicingTimedOutBefore then 0 else 1 end,
                     coalesce(t.expected_completed_at, t.created_at) asc,
                     t.created_at asc,
                     t.id asc
            offset :offset rows fetch next :limit rows only
            """, params, (rs, rowNum) -> new TechnicalWorkflowRecords.SlicingWorkbenchRow(
            rs.getString("task_id"),
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("patient_name"),
            rs.getString("patient_id"),
            rs.getString("specimen_id"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_name"),
            rs.getString("embedding_box_id"),
            JdbcResultSetUtils.getNullableString(rs, "slide_id"),
            JdbcResultSetUtils.getNullableString(rs, "slide_no"),
            JdbcResultSetUtils.getNullableString(rs, "slicing_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "slicing_remark"),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            JdbcResultSetUtils.getNullableString(rs, "grossing_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_clear_remark"),
            JdbcResultSetUtils.getNullableString(rs, "shift_remark"),
            JdbcResultSetUtils.getNullableString(rs, "slice_notice"),
            rs.getString("task_status"),
            rs.getInt("timed_out") != 0,
            rs.getInt("selectable") != 0));
        return new TechnicalWorkflowRecords.PagedSlicingWorkbenchRows(items, total == null ? 0 : total);
    }

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findCompletedSlicingWorkbenchRows(
        TechnicalWorkflowRecords.SlicingWorkbenchQuery query
    ) {
        String where = """
            where t.task_type = 'SLICING'
              and t.task_status = 'COMPLETED'
              and t.completed_at >= :todayStart
              and t.completed_at < :tomorrowStart
            """ + buildWorkbenchKeywordFilter(query.keyword());
        MapSqlParameterSource params = buildWorkbenchParams(query)
            .addValue("limit", query.completedSize())
            .addValue("offset", Math.max(query.completedPage() - 1, 0) * query.completedSize());
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join embedding_boxes eb on t.object_type = 'EMBEDDING_BOX' and t.object_id = eb.id
            left join slicings slc
              on slc.case_id = t.case_id
             and slc.embedding_box_id = t.object_id
             and slc.sliced_at = t.completed_at
            left join slides s on s.slicing_id = slc.id
            """ + where, params, Long.class);
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> items = jdbcTemplate.query("""
            select
                t.id as task_id,
                t.case_id,
                pc.pathology_no,
                a.patient_name,
                a.patient_id,
                t.specimen_id,
                sp.specimen_name_standardized as specimen_name,
                t.object_id as embedding_box_id,
                s.id as slide_id,
                s.slide_no,
                slc.sliced_by_name as slicing_operator_name,
                slc.remarks as slicing_remark,
                slc.sliced_at as completed_at,
                emb.sampling_evaluation as grossing_evaluation,
                emb.evaluation_level as embedding_evaluation,
                emb.embedded_by_name as embedding_operator_name,
                emb.remarks as embedding_clear_remark,
                t.production_remarks as shift_remark,
                eb.slice_notice,
                t.task_status,
                0 as timed_out,
                case when s.id is null then 0 else 1 end as selectable
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            left join specimens sp on sp.id = t.specimen_id
            left join embedding_boxes eb on t.object_type = 'EMBEDDING_BOX' and t.object_id = eb.id
            left join embeddings emb on emb.id = eb.embedding_id
            left join slicings slc
              on slc.case_id = t.case_id
             and slc.embedding_box_id = t.object_id
             and slc.sliced_at = t.completed_at
            left join slides s on s.slicing_id = slc.id
            """ + where + """
            order by coalesce(slc.sliced_at, t.completed_at) desc,
                     s.slide_no asc,
                     t.id asc
            offset :offset rows fetch next :limit rows only
            """, params, (rs, rowNum) -> new TechnicalWorkflowRecords.SlicingWorkbenchRow(
            rs.getString("task_id"),
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("patient_name"),
            rs.getString("patient_id"),
            rs.getString("specimen_id"),
            JdbcResultSetUtils.getNullableString(rs, "specimen_name"),
            rs.getString("embedding_box_id"),
            JdbcResultSetUtils.getNullableString(rs, "slide_id"),
            JdbcResultSetUtils.getNullableString(rs, "slide_no"),
            JdbcResultSetUtils.getNullableString(rs, "slicing_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "slicing_remark"),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            JdbcResultSetUtils.getNullableString(rs, "grossing_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_evaluation"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_operator_name"),
            JdbcResultSetUtils.getNullableString(rs, "embedding_clear_remark"),
            JdbcResultSetUtils.getNullableString(rs, "shift_remark"),
            JdbcResultSetUtils.getNullableString(rs, "slice_notice"),
            rs.getString("task_status"),
            rs.getInt("timed_out") != 0,
            rs.getInt("selectable") != 0));
        return new TechnicalWorkflowRecords.PagedSlicingWorkbenchRows(items, total == null ? 0 : total);
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
            .addValue("todayStart", query.todayStart())
            .addValue("tomorrowStart", query.tomorrowStart())
            .addValue("dayAfterTomorrowStart", query.dayAfterTomorrowStart())
            .addValue("currentUserId", query.currentUserId() == null ? "" : query.currentUserId())
            .addValue("slicingTimedOutBefore", query.slicingTimedOutBefore());
        if (query.keyword() != null && !query.keyword().isBlank()) {
            params.addValue("keywordLike", "%" + query.keyword().trim().toUpperCase() + "%");
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

    private String buildWorkbenchTodayFilter(boolean enabled) {
        if (!enabled) {
            return "";
        }
        return """
              and coalesce(t.expected_completed_at, t.created_at) >= :todayStart
              and coalesce(t.expected_completed_at, t.created_at) < :tomorrowStart
            """;
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
