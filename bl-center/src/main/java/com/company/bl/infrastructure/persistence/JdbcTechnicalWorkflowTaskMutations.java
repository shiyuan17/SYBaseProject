package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateDehydrationBatchCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateDehydrationBatchItemCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateEmbeddingBoxCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateEmbeddingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateSamplingBlockCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateSamplingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateTechnicalTaskCommand;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;

final class JdbcTechnicalWorkflowTaskMutations {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcTechnicalWorkflowTaskMutations(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void updatePathologyCaseStatus(String caseId, String caseStatus) {
        jdbcTemplate.update("""
            update pathology_cases
            set case_status = :caseStatus,
                updated_at = :updatedAt
            where id = :caseId
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("caseStatus", caseStatus)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void startTechnicalTask(String taskId, String remarks, LocalDateTime startedAt) {
        jdbcTemplate.update("""
            update technical_pending_tasks
            set task_status = 'IN_PROGRESS',
                started_at = coalesce(started_at, :startedAt),
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("startedAt", startedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void completeTechnicalTask(String taskId, String taskStatus, String remarks, LocalDateTime completedAt) {
        jdbcTemplate.update("""
            update technical_pending_tasks
            set task_status = :taskStatus,
                completed_at = :completedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("taskStatus", taskStatus)
            .addValue("completedAt", completedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void assignTechnicalTask(String taskId,
                             String priority,
                             String stationCode,
                             String stationName,
                             String assignedToUserId,
                             String assignedToName,
                             LocalDateTime expectedCompletedAt,
                             String productionRemarks) {
        jdbcTemplate.update("""
            update technical_pending_tasks
            set priority = coalesce(:priority, priority),
                station_code = :stationCode,
                station_name = :stationName,
                assigned_to_user_id = :assignedToUserId,
                assigned_to_name = :assignedToName,
                expected_completed_at = :expectedCompletedAt,
                production_remarks = :productionRemarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("priority", priority)
            .addValue("stationCode", stationCode)
            .addValue("stationName", stationName)
            .addValue("assignedToUserId", assignedToUserId)
            .addValue("assignedToName", assignedToName)
            .addValue("expectedCompletedAt", expectedCompletedAt)
            .addValue("productionRemarks", productionRemarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void claimTechnicalTask(String taskId,
                            String assignedToUserId,
                            String assignedToName,
                            String stationCode,
                            String stationName,
                            String remarks) {
        jdbcTemplate.update("""
            update technical_pending_tasks
            set assigned_to_user_id = :assignedToUserId,
                assigned_to_name = :assignedToName,
                station_code = coalesce(:stationCode, station_code),
                station_name = coalesce(:stationName, station_name),
                production_remarks = coalesce(:remarks, production_remarks),
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("assignedToUserId", assignedToUserId)
            .addValue("assignedToName", assignedToName)
            .addValue("stationCode", stationCode)
            .addValue("stationName", stationName)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void releaseTechnicalTask(String taskId, String remarks) {
        jdbcTemplate.update("""
            update technical_pending_tasks
            set assigned_to_user_id = null,
                assigned_to_name = null,
                production_remarks = coalesce(:remarks, production_remarks),
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void updateTechnicalTaskPriority(String taskId, String priority, String productionRemarks) {
        jdbcTemplate.update("""
            update technical_pending_tasks
            set priority = :priority,
                production_remarks = coalesce(:productionRemarks, production_remarks),
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("priority", priority)
            .addValue("productionRemarks", productionRemarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void updateTechnicalTaskRemarks(String taskId, String remarks, String productionRemarks) {
        jdbcTemplate.update("""
            update technical_pending_tasks
            set remarks = :remarks,
                production_remarks = :productionRemarks,
                updated_at = :updatedAt
            where id = :taskId
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("remarks", remarks)
            .addValue("productionRemarks", productionRemarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void insertTechnicalTask(CreateTechnicalTaskCommand command) {
        jdbcTemplate.update("""
            insert into technical_pending_tasks
                (id, application_id, case_id, specimen_id, task_type, task_status, object_type, object_id,
                 parent_task_id, priority, current_node, station_code, station_name, assigned_to_user_id,
                 assigned_to_name, expected_completed_at, production_remarks, received_at, payload,
                 created_at, updated_at, remarks)
            values
                (:id, :applicationId, :caseId, :specimenId, :taskType, :taskStatus, :objectType, :objectId,
                 :parentTaskId, :priority, :currentNode, :stationCode, :stationName, :assignedToUserId,
                 :assignedToName, :expectedCompletedAt, :productionRemarks, :receivedAt, :payload,
                 :createdAt, :updatedAt, :remarks)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("applicationId", command.applicationId())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("taskType", command.taskType())
            .addValue("taskStatus", command.taskStatus())
            .addValue("objectType", command.objectType())
            .addValue("objectId", command.objectId())
            .addValue("parentTaskId", command.parentTaskId())
            .addValue("priority", command.priority())
            .addValue("currentNode", command.currentNode())
            .addValue("stationCode", command.stationCode())
            .addValue("stationName", command.stationName())
            .addValue("assignedToUserId", command.assignedToUserId())
            .addValue("assignedToName", command.assignedToName())
            .addValue("expectedCompletedAt", command.expectedCompletedAt())
            .addValue("productionRemarks", command.productionRemarks())
            .addValue("receivedAt", command.receivedAt())
            .addValue("payload", command.payload())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.createdAt())
            .addValue("remarks", command.remarks()));
    }

    void insertSampling(CreateSamplingCommand command) {
        jdbcTemplate.update("""
            insert into samplings
                (id, case_id, specimen_id, sampling_status, block_count, gross_image_count, sampling_template_id,
                 size_text, cut_surface_feature, margin_marking, gross_description, sampled_by_user_id,
                 sampled_by_name, sampled_at, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :samplingStatus, :blockCount, :grossImageCount, :samplingTemplateId,
                 :sizeText, :cutSurfaceFeature, :marginMarking, :grossDescription, :sampledByUserId,
                 :sampledByName, :sampledAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("samplingStatus", command.samplingStatus())
            .addValue("blockCount", command.blockCount())
            .addValue("grossImageCount", command.grossImageCount())
            .addValue("samplingTemplateId", command.samplingTemplateId())
            .addValue("sizeText", command.sizeText())
            .addValue("cutSurfaceFeature", command.cutSurfaceFeature())
            .addValue("marginMarking", command.marginMarking())
            .addValue("grossDescription", command.grossDescription())
            .addValue("sampledByUserId", command.sampledByUserId())
            .addValue("sampledByName", command.sampledByName())
            .addValue("sampledAt", command.sampledAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.sampledAt())
            .addValue("updatedAt", command.sampledAt()));
    }

    void insertSamplingBlock(CreateSamplingBlockCommand command) {
        jdbcTemplate.update("""
            insert into sampling_blocks
                (id, case_id, specimen_id, sampling_id, sequence_no, block_code, block_site,
                 block_description, embedding_box_no, special_requirement, embedding_box_name,
                 embedding_box_status, embedding_remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :samplingId, :sequenceNo, :blockCode, :blockSite,
                 :blockDescription, :embeddingBoxNo, :specialRequirement, :embeddingBoxName,
                 :embeddingBoxStatus, :embeddingRemarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("samplingId", command.samplingId())
            .addValue("sequenceNo", command.sequenceNo())
            .addValue("blockCode", command.blockCode())
            .addValue("blockSite", command.blockSite())
            .addValue("blockDescription", command.blockDescription())
            .addValue("embeddingBoxNo", command.embeddingBoxNo())
            .addValue("specialRequirement", command.specialRequirement())
            .addValue("embeddingBoxName", command.embeddingBoxName())
            .addValue("embeddingBoxStatus", command.embeddingBoxStatus())
            .addValue("embeddingRemarks", command.embeddingRemarks())
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void insertDehydrationBatch(CreateDehydrationBatchCommand command) {
        jdbcTemplate.update("""
            insert into dehydration_batches
                (id, case_id, batch_no, batch_status, basket_no, device_no, operator_user_id,
                 operator_name, remarks, created_at, updated_at)
            values
                (:id, :caseId, :batchNo, :batchStatus, :basketNo, :deviceNo, :operatorUserId,
                 :operatorName, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("batchNo", command.batchNo())
            .addValue("batchStatus", command.batchStatus())
            .addValue("basketNo", command.basketNo())
            .addValue("deviceNo", command.deviceNo())
            .addValue("operatorUserId", command.operatorUserId())
            .addValue("operatorName", command.operatorName())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.createdAt()));
    }

    void insertDehydrationBatchItem(CreateDehydrationBatchItemCommand command) {
        jdbcTemplate.update("""
            insert into dehydration_batch_items
                (id, batch_id, case_id, specimen_id, sampling_block_id, item_status, loaded_at, remarks, created_at, updated_at)
            values
                (:id, :batchId, :caseId, :specimenId, :samplingBlockId, :itemStatus, :loadedAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("batchId", command.batchId())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("samplingBlockId", command.samplingBlockId())
            .addValue("itemStatus", command.itemStatus())
            .addValue("loadedAt", command.loadedAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void updateDehydrationBatchStatus(String batchId,
                                      String batchStatus,
                                      String operatorUserId,
                                      String operatorName,
                                      LocalDateTime startedAt,
                                      LocalDateTime completedAt,
                                      String remarks) {
        jdbcTemplate.update("""
            update dehydration_batches
            set batch_status = :batchStatus,
                operator_user_id = :operatorUserId,
                operator_name = :operatorName,
                started_at = coalesce(:startedAt, started_at),
                completed_at = coalesce(:completedAt, completed_at),
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :batchId
            """, new MapSqlParameterSource()
            .addValue("batchId", batchId)
            .addValue("batchStatus", batchStatus)
            .addValue("operatorUserId", operatorUserId)
            .addValue("operatorName", operatorName)
            .addValue("startedAt", startedAt)
            .addValue("completedAt", completedAt)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void updateDehydrationBatchItemStatus(String batchId, String samplingBlockId, String itemStatus, String remarks) {
        jdbcTemplate.update("""
            update dehydration_batch_items
            set item_status = :itemStatus,
                remarks = :remarks,
                updated_at = :updatedAt
            where batch_id = :batchId
              and sampling_block_id = :samplingBlockId
            """, new MapSqlParameterSource()
            .addValue("batchId", batchId)
            .addValue("samplingBlockId", samplingBlockId)
            .addValue("itemStatus", itemStatus)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void insertEmbedding(CreateEmbeddingCommand command) {
        LocalDateTime createdAt = command.endedAt() == null ? LocalDateTime.now() : command.endedAt();
        jdbcTemplate.update("""
            insert into embeddings
                (id, case_id, specimen_id, sampling_id, sampling_block_id, embedding_status, evaluation_level,
                 sampling_evaluation, started_at, ended_at, embedded_by_user_id, embedded_by_name, remarks,
                 created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :samplingId, :samplingBlockId, :embeddingStatus, :evaluationLevel,
                 :samplingEvaluation, :startedAt, :endedAt, :embeddedByUserId, :embeddedByName, :remarks,
                 :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("samplingId", command.samplingId())
            .addValue("samplingBlockId", command.samplingBlockId())
            .addValue("embeddingStatus", command.embeddingStatus())
            .addValue("evaluationLevel", command.evaluationLevel())
            .addValue("samplingEvaluation", command.samplingEvaluation())
            .addValue("startedAt", command.startedAt())
            .addValue("endedAt", command.endedAt())
            .addValue("embeddedByUserId", command.embeddedByUserId())
            .addValue("embeddedByName", command.embeddedByName())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", createdAt)
            .addValue("updatedAt", createdAt));
    }

    void insertEmbeddingBox(CreateEmbeddingBoxCommand command) {
        jdbcTemplate.update("""
            insert into embedding_boxes
                (id, case_id, specimen_id, sampling_block_id, embedding_id, embedding_box_no, block_count,
                 re_embedding_flag, slice_notice, storage_status, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :samplingBlockId, :embeddingId, :embeddingBoxNo, :blockCount,
                 :reEmbeddingFlag, :sliceNotice, :storageStatus, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("samplingBlockId", command.samplingBlockId())
            .addValue("embeddingId", command.embeddingId())
            .addValue("embeddingBoxNo", command.embeddingBoxNo())
            .addValue("blockCount", command.blockCount())
            .addValue("reEmbeddingFlag", command.reEmbeddingFlag() ? 1 : 0)
            .addValue("sliceNotice", command.sliceNotice())
            .addValue("storageStatus", command.storageStatus())
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void updateEmbeddingQualityReview(String embeddingId, String evaluationLevel, String samplingEvaluation) {
        jdbcTemplate.update("""
            update embeddings
            set evaluation_level = :evaluationLevel,
                sampling_evaluation = :samplingEvaluation,
                updated_at = :updatedAt
            where id = :embeddingId
            """, new MapSqlParameterSource()
            .addValue("embeddingId", embeddingId)
            .addValue("evaluationLevel", evaluationLevel)
            .addValue("samplingEvaluation", samplingEvaluation)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    void updateEmbeddingBoxSliceNoticeByEmbeddingId(String embeddingId, String sliceNotice) {
        jdbcTemplate.update("""
            update embedding_boxes
            set slice_notice = :sliceNotice,
                updated_at = :updatedAt
            where embedding_id = :embeddingId
            """, new MapSqlParameterSource()
            .addValue("embeddingId", embeddingId)
            .addValue("sliceNotice", sliceNotice)
            .addValue("updatedAt", LocalDateTime.now()));
    }
}
