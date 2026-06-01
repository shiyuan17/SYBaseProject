package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateCaseMediaAssetCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateReworkOrderCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlicingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideQcEvaluationCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideStainingCommand;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;

final class JdbcTechnicalWorkflowProcessingMutations {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTechnicalWorkflowProcessingMutations(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insertSlicing(CreateSlicingCommand command) {
        LocalDateTime createdAt = command.slicedAt() == null ? LocalDateTime.now() : command.slicedAt();
        jdbcTemplate.update("""
            insert into slicings
                (id, case_id, specimen_id, embedding_id, embedding_box_id, slicing_batch_no, slicing_status,
                 slide_count, slice_count_per_slide, slice_thickness, sliced_by_user_id, sliced_by_name, sliced_at,
                 quality_issue, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :embeddingId, :embeddingBoxId, :slicingBatchNo, :slicingStatus,
                 :slideCount, :sliceCountPerSlide, :sliceThickness, :slicedByUserId, :slicedByName, :slicedAt,
                 :qualityIssue, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("embeddingId", command.embeddingId())
            .addValue("embeddingBoxId", command.embeddingBoxId())
            .addValue("slicingBatchNo", command.slicingBatchNo())
            .addValue("slicingStatus", command.slicingStatus())
            .addValue("slideCount", command.slideCount())
            .addValue("sliceCountPerSlide", command.sliceCountPerSlide())
            .addValue("sliceThickness", command.sliceThickness())
            .addValue("slicedByUserId", command.slicedByUserId())
            .addValue("slicedByName", command.slicedByName())
            .addValue("slicedAt", command.slicedAt())
            .addValue("qualityIssue", command.qualityIssue())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", createdAt)
            .addValue("updatedAt", createdAt));
    }

    void insertSlide(CreateSlideCommand command) {
        jdbcTemplate.update("""
            insert into slides
                (id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id, slide_no, slide_label,
                 combined_slide_flag, quality_status, slide_status, slice_count, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :slicingId, :embeddingBoxId, :samplingBlockId, :slideNo, :slideLabel,
                 :combinedSlideFlag, :qualityStatus, :slideStatus, :sliceCount, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("slicingId", command.slicingId())
            .addValue("embeddingBoxId", command.embeddingBoxId())
            .addValue("samplingBlockId", command.samplingBlockId())
            .addValue("slideNo", command.slideNo())
            .addValue("slideLabel", command.slideLabel())
            .addValue("combinedSlideFlag", command.combinedSlideFlag() ? 1 : 0)
            .addValue("qualityStatus", command.qualityStatus())
            .addValue("slideStatus", command.slideStatus())
            .addValue("sliceCount", command.sliceCount())
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void insertSlideStaining(CreateSlideStainingCommand command) {
        LocalDateTime createdAt = command.stainedAt() == null ? LocalDateTime.now() : command.stainedAt();
        jdbcTemplate.update("""
            insert into slide_stainings
                (id, case_id, specimen_id, slide_id, staining_type, staining_status, stained_by_user_id,
                 stained_by_name, stained_at, quality_issue, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :slideId, :stainingType, :stainingStatus, :stainedByUserId,
                 :stainedByName, :stainedAt, :qualityIssue, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("slideId", command.slideId())
            .addValue("stainingType", command.stainingType())
            .addValue("stainingStatus", command.stainingStatus())
            .addValue("stainedByUserId", command.stainedByUserId())
            .addValue("stainedByName", command.stainedByName())
            .addValue("stainedAt", command.stainedAt())
            .addValue("qualityIssue", command.qualityIssue())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", createdAt)
            .addValue("updatedAt", createdAt));
    }

    void updateSlideStatus(String slideId, String slideStatus, String qualityStatus) {
        jdbcTemplate.update("""
            update slides
            set slide_status = :slideStatus,
                quality_status = :qualityStatus,
                updated_at = :updatedAt
            where id = :slideId
            """, new MapSqlParameterSource()
            .addValue("slideId", slideId)
            .addValue("slideStatus", slideStatus)
            .addValue("qualityStatus", qualityStatus)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void insertReworkOrder(CreateReworkOrderCommand command) {
        jdbcTemplate.update("""
            insert into rework_orders
                (id, case_id, specimen_id, sampling_block_id, embedding_box_id, slide_id, rework_type, status,
                 reason, requested_by_user_id, requested_by_name, requested_at, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :samplingBlockId, :embeddingBoxId, :slideId, :reworkType, :status,
                 :reason, :requestedByUserId, :requestedByName, :requestedAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("samplingBlockId", command.samplingBlockId())
            .addValue("embeddingBoxId", command.embeddingBoxId())
            .addValue("slideId", command.slideId())
            .addValue("reworkType", command.reworkType())
            .addValue("status", command.status())
            .addValue("reason", command.reason())
            .addValue("requestedByUserId", command.requestedByUserId())
            .addValue("requestedByName", command.requestedByName())
            .addValue("requestedAt", command.requestedAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.requestedAt())
            .addValue("updatedAt", command.requestedAt()));
    }

    void updateReworkOrderStatus(String reworkOrderId,
                                 String status,
                                 String executedByUserId,
                                 String executedByName,
                                 LocalDateTime executedAt,
                                 String remarks) {
        jdbcTemplate.update("""
            update rework_orders
            set status = :status,
                executed_by_user_id = :executedByUserId,
                executed_by_name = :executedByName,
                executed_at = :executedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", reworkOrderId)
            .addValue("status", status)
            .addValue("executedByUserId", executedByUserId)
            .addValue("executedByName", executedByName)
            .addValue("executedAt", executedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void insertSlideQcEvaluation(CreateSlideQcEvaluationCommand command) {
        jdbcTemplate.update("""
            insert into slide_qc_evaluations
                (id, case_id, specimen_id, slide_id, qc_type, evaluation_result, issue_description,
                 improvement_suggestion, evaluator_user_id, evaluator_name, evaluated_at, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :slideId, :qcType, :evaluationResult, :issueDescription,
                 :improvementSuggestion, :evaluatorUserId, :evaluatorName, :evaluatedAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("slideId", command.slideId())
            .addValue("qcType", command.qcType())
            .addValue("evaluationResult", command.evaluationResult())
            .addValue("issueDescription", command.issueDescription())
            .addValue("improvementSuggestion", command.improvementSuggestion())
            .addValue("evaluatorUserId", command.evaluatorUserId())
            .addValue("evaluatorName", command.evaluatorName())
            .addValue("evaluatedAt", command.evaluatedAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.evaluatedAt())
            .addValue("updatedAt", command.evaluatedAt()));
    }

    void insertCaseMediaAsset(CreateCaseMediaAssetCommand command) {
        jdbcTemplate.update("""
            insert into case_media_assets
                (id, case_id, specimen_id, object_type, object_id, media_type, file_url, file_name,
                 captured_at, captured_by_user_id, captured_by_name, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :objectType, :objectId, :mediaType, :fileUrl, :fileName,
                 :capturedAt, :capturedByUserId, :capturedByName, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("objectType", command.objectType())
            .addValue("objectId", command.objectId())
            .addValue("mediaType", command.mediaType())
            .addValue("fileUrl", command.fileUrl())
            .addValue("fileName", command.fileName())
            .addValue("capturedAt", command.capturedAt())
            .addValue("capturedByUserId", command.capturedByUserId())
            .addValue("capturedByName", command.capturedByName())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.capturedAt())
            .addValue("updatedAt", command.capturedAt()));
    }

    void deleteCaseMediaAsset(String assetId) {
        jdbcTemplate.update("""
            delete from case_media_assets
            where id = :assetId
            """, new MapSqlParameterSource()
            .addValue("assetId", assetId));
    }

    void insertWorkflowEvent(TrackingEvent event) {
        jdbcTemplate.update("""
            insert into workflow_events
                (id, application_id, specimen_id, case_id, transport_order_id, node_code, event_type,
                 event_status, event_time, operator_user_id, operator_name, source_terminal, event_content, created_at)
            values
                (:id, :applicationId, :specimenId, :caseId, :transportOrderId, :nodeCode, :eventType,
                 :eventStatus, :eventTime, :operatorUserId, :operatorName, :sourceTerminal, :eventContent, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", event.id())
            .addValue("applicationId", event.applicationId())
            .addValue("specimenId", event.specimenId())
            .addValue("caseId", event.caseId())
            .addValue("transportOrderId", event.transportOrderId())
            .addValue("nodeCode", event.nodeCode())
            .addValue("eventType", event.eventType())
            .addValue("eventStatus", event.eventStatus())
            .addValue("eventTime", event.eventTime())
            .addValue("operatorUserId", event.operatorUserId())
            .addValue("operatorName", event.operatorName())
            .addValue("sourceTerminal", event.sourceTerminal())
            .addValue("eventContent", event.eventContent())
            .addValue("createdAt", LocalDateTime.now()));
    }
}
