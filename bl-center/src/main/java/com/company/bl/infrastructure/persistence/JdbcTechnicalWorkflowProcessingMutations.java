package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateCaseMediaAssetCommand;
import com.company.bl.support.application.WorkflowRequestContext;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateReworkOrderCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlicingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideQcEvaluationCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideStainingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

final class JdbcTechnicalWorkflowProcessingMutations {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTechnicalWorkflowProcessingMutations(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insertSlicing(CreateSlicingCommand command) {
        LocalDateTime createdAt = command.slicedAt() == null ? LocalDateTime.now() : command.slicedAt();
        jdbcTemplate.update("""
            insert into slicings
                (id, task_id, case_id, specimen_id, embedding_id, embedding_box_id, slicing_batch_no, slicing_status,
                 slide_count, slice_count_per_slide, slice_thickness, sliced_by_user_id, sliced_by_name, sliced_at,
                 quality_issue, remarks, created_at, updated_at)
            values
                (:id, :taskId, :caseId, :specimenId, :embeddingId, :embeddingBoxId, :slicingBatchNo, :slicingStatus,
                 :slideCount, :sliceCountPerSlide, :sliceThickness, :slicedByUserId, :slicedByName, :slicedAt,
                 :qualityIssue, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("taskId", command.taskId())
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

    void completeSlicingRecord(String slicingId,
                               String slicingStatus,
                               Integer sliceCountPerSlide,
                               String sliceThickness,
                               String slicedByUserId,
                               String slicedByName,
                               LocalDateTime slicedAt,
                               String qualityIssue,
                               String remarks) {
        jdbcTemplate.update("""
            update slicings
            set slicing_status = :slicingStatus,
                slice_count_per_slide = :sliceCountPerSlide,
                slice_thickness = :sliceThickness,
                sliced_by_user_id = :slicedByUserId,
                sliced_by_name = :slicedByName,
                sliced_at = :slicedAt,
                quality_issue = :qualityIssue,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :slicingId
            """, new MapSqlParameterSource()
            .addValue("slicingId", slicingId)
            .addValue("slicingStatus", slicingStatus)
            .addValue("sliceCountPerSlide", sliceCountPerSlide)
            .addValue("sliceThickness", sliceThickness)
            .addValue("slicedByUserId", slicedByUserId)
            .addValue("slicedByName", slicedByName)
            .addValue("slicedAt", slicedAt)
            .addValue("qualityIssue", qualityIssue)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
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

    void insertSlicingSlidePrintMergeGroup(String groupId,
                                           String caseId,
                                           String pathologyNo,
                                           String patientId,
                                           String embeddingBoxNo,
                                           String operatorUserId,
                                           String operatorName,
                                           String remarks,
                                           LocalDateTime createdAt) {
        jdbcTemplate.update("""
            insert into slicing_slide_print_merge_groups
                (id, case_id, pathology_no, patient_id, embedding_box_no, group_status,
                 created_by_user_id, created_by_name, remarks, created_at, updated_at)
            values
                (:id, :caseId, :pathologyNo, :patientId, :embeddingBoxNo, 'PENDING',
                 :createdByUserId, :createdByName, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", groupId)
            .addValue("caseId", caseId)
            .addValue("pathologyNo", pathologyNo)
            .addValue("patientId", patientId)
            .addValue("embeddingBoxNo", embeddingBoxNo)
            .addValue("createdByUserId", operatorUserId)
            .addValue("createdByName", operatorName)
            .addValue("remarks", remarks)
            .addValue("createdAt", createdAt)
            .addValue("updatedAt", createdAt));
    }

    void insertSlicingSlidePrintMergeGroupItem(String itemId,
                                               String groupId,
                                               String taskId,
                                               String embeddingBoxId,
                                               String embeddingBoxNo,
                                               int sequenceNo) {
        jdbcTemplate.update("""
            insert into slicing_slide_print_merge_group_items
                (id, group_id, task_id, embedding_box_id, embedding_box_no, sequence_no, created_at)
            values
                (:id, :groupId, :taskId, :embeddingBoxId, :embeddingBoxNo, :sequenceNo, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", itemId)
            .addValue("groupId", groupId)
            .addValue("taskId", taskId)
            .addValue("embeddingBoxId", embeddingBoxId)
            .addValue("embeddingBoxNo", embeddingBoxNo)
            .addValue("sequenceNo", sequenceNo)
            .addValue("createdAt", LocalDateTime.now()));
    }

    void cancelSlicingSlidePrintMergeGroups(List<String> printGroupIds, LocalDateTime updatedAt) {
        if (printGroupIds == null || printGroupIds.isEmpty()) {
            return;
        }
        jdbcTemplate.update("""
            update slicing_slide_print_merge_groups
            set group_status = 'CANCELLED',
                updated_at = :updatedAt
            where id in (:printGroupIds)
              and group_status = 'PENDING'
            """, new MapSqlParameterSource()
            .addValue("printGroupIds", printGroupIds)
            .addValue("updatedAt", updatedAt));
    }

    void markSlicingSlidePrintMergeGroupPrinted(String printGroupId,
                                                String slicingId,
                                                String operatorUserId,
                                                String operatorName,
                                                String remarks,
                                                LocalDateTime printedAt) {
        jdbcTemplate.update("""
            update slicing_slide_print_merge_groups
            set group_status = 'PRINTED',
                printed_slicing_id = :slicingId,
                printed_by_user_id = :printedByUserId,
                printed_by_name = :printedByName,
                printed_at = :printedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :printGroupId
              and group_status = 'PENDING'
            """, new MapSqlParameterSource()
            .addValue("printGroupId", printGroupId)
            .addValue("slicingId", slicingId)
            .addValue("printedByUserId", operatorUserId)
            .addValue("printedByName", operatorName)
            .addValue("printedAt", printedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", printedAt));
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
        String operatorIp = event.operatorIp() != null ? event.operatorIp() : WorkflowRequestContext.resolveClientIp();
        jdbcTemplate.update("""
            insert into workflow_events
                (id, application_id, specimen_id, case_id, transport_order_id, node_code, event_type,
                 event_status, event_time, operator_user_id, operator_name, source_terminal, event_content,
                 operator_ip, created_at)
            values
                (:id, :applicationId, :specimenId, :caseId, :transportOrderId, :nodeCode, :eventType,
                 :eventStatus, :eventTime, :operatorUserId, :operatorName, :sourceTerminal, :eventContent,
                 :operatorIp, :createdAt)
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
            .addValue("operatorIp", operatorIp)
            .addValue("createdAt", LocalDateTime.now()));
    }

    void insertWorkstationDailyClear(TechnicalWorkflowRecords.CreateWorkstationDailyClearCommand command) {
        String operatorIp = command.operatorIp() != null ? command.operatorIp() : WorkflowRequestContext.resolveClientIp();
        jdbcTemplate.update("""
            insert into workstation_daily_clears
                (id, workstation_type, work_date, operator_user_id, operator_name,
                 cleared_at, clear_status, operator_ip, created_at)
            values
                (:id, :workstationType, :workDate, :operatorUserId, :operatorName,
                 :clearedAt, :clearStatus, :operatorIp, :createdAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("workstationType", command.workstationType())
            .addValue("workDate", command.workDate())
            .addValue("operatorUserId", command.operatorUserId())
            .addValue("operatorName", command.operatorName())
            .addValue("clearedAt", command.clearedAt())
            .addValue("clearStatus", command.clearStatus())
            .addValue("operatorIp", operatorIp)
            .addValue("createdAt", command.clearedAt()));
    }
}
