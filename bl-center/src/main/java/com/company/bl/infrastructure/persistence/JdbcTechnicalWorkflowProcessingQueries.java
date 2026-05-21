package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.ReworkOrder;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slide;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.SlideQcEvaluation;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.SlideStaining;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slicing;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.Embedding;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
            select id, case_id, specimen_id, sampling_id, sampling_block_id, embedding_status
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
}
