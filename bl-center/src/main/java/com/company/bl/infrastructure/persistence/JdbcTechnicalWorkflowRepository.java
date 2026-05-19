package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcTechnicalWorkflowRepository implements TechnicalWorkflowRepository {

    private static final List<String> ACTIVE_TASK_STATUSES = List.of("PENDING", "IN_PROGRESS");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcTechnicalWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<PathologyCase> findPathologyCaseById(String caseId) {
        List<PathologyCase> rows = jdbcTemplate.query("""
            select *
            from pathology_cases
            where id = :caseId
            """, Map.of("caseId", caseId), this::mapPathologyCase);
        return rows.stream().findFirst();
    }

    @Override
    public List<Specimen> findSpecimensByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from specimens
            where case_id = :caseId
            order by specimen_no asc, created_at asc
            """, Map.of("caseId", caseId), this::mapSpecimen);
    }

    @Override
    public Optional<Specimen> findSpecimenById(String specimenId) {
        List<Specimen> rows = jdbcTemplate.query("""
            select *
            from specimens
            where id = :specimenId
            """, Map.of("specimenId", specimenId), this::mapSpecimen);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<TechnicalTask> findTechnicalTaskById(String taskId) {
        List<TechnicalTask> rows = jdbcTemplate.query(taskSelectSql() + """
            where t.id = :taskId
            """, Map.of("taskId", taskId), this::mapTechnicalTask);
        return rows.stream().findFirst();
    }

    @Override
    public List<TechnicalTask> findActiveTechnicalTasksByCaseId(String caseId) {
        return jdbcTemplate.query(taskSelectSql() + """
            where t.case_id = :caseId
              and t.task_status in (:statuses)
            order by t.created_at asc, t.id asc
            """, new MapSqlParameterSource()
            .addValue("caseId", caseId)
            .addValue("statuses", ACTIVE_TASK_STATUSES), this::mapTechnicalTask);
    }

    @Override
    public List<TechnicalTask> findActiveTechnicalTasksByObject(String taskType, String objectType, String objectId) {
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
            .addValue("statuses", ACTIVE_TASK_STATUSES), this::mapTechnicalTask);
    }

    @Override
    public PagedTechnicalTasks findTechnicalTasks(PendingTechnicalTaskQuery query) {
        String where = " where 1 = 1 " + buildTaskFilters(query);
        Long total = jdbcTemplate.queryForObject("""
            select count(1)
            from technical_pending_tasks t
            join pathology_cases pc on pc.id = t.case_id
            join applications a on a.id = t.application_id
            """ + where, taskFilterParams(query), Long.class);
        List<TechnicalTask> items = jdbcTemplate.query(taskSelectSql() + where + """
            
            order by t.created_at asc, t.id asc
            limit :limit offset :offset
            """, taskPageParams(query), this::mapTechnicalTask);
        return new PagedTechnicalTasks(items, total == null ? 0 : total);
    }

    @Override
    public void updatePathologyCaseStatus(String caseId, String caseStatus) {
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

    @Override
    public void startTechnicalTask(String taskId,
                                   String operatorUserId,
                                   String operatorName,
                                   String remarks,
                                   LocalDateTime startedAt) {
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

    @Override
    public void completeTechnicalTask(String taskId,
                                      String taskStatus,
                                      String remarks,
                                      LocalDateTime completedAt) {
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

    @Override
    public void insertTechnicalTask(CreateTechnicalTaskCommand command) {
        jdbcTemplate.update("""
            insert into technical_pending_tasks
                (id, application_id, case_id, specimen_id, task_type, task_status, object_type, object_id,
                 parent_task_id, payload, created_at, updated_at, remarks)
            values
                (:id, :applicationId, :caseId, :specimenId, :taskType, :taskStatus, :objectType, :objectId,
                 :parentTaskId, :payload, :createdAt, :updatedAt, :remarks)
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
            .addValue("payload", command.payload())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.createdAt())
            .addValue("remarks", command.remarks()));
    }

    @Override
    public void insertSampling(CreateSamplingCommand command) {
        jdbcTemplate.update("""
            insert into samplings
                (id, case_id, specimen_id, sampling_status, block_count, gross_image_count, sampling_template_id,
                 gross_description, sampled_by_user_id, sampled_by_name, sampled_at, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :samplingStatus, :blockCount, :grossImageCount, :samplingTemplateId,
                 :grossDescription, :sampledByUserId, :sampledByName, :sampledAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("samplingStatus", command.samplingStatus())
            .addValue("blockCount", command.blockCount())
            .addValue("grossImageCount", command.grossImageCount())
            .addValue("samplingTemplateId", command.samplingTemplateId())
            .addValue("grossDescription", command.grossDescription())
            .addValue("sampledByUserId", command.sampledByUserId())
            .addValue("sampledByName", command.sampledByName())
            .addValue("sampledAt", command.sampledAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.sampledAt())
            .addValue("updatedAt", command.sampledAt()));
    }

    @Override
    public void insertSamplingBlock(CreateSamplingBlockCommand command) {
        jdbcTemplate.update("""
            insert into sampling_blocks
                (id, case_id, specimen_id, sampling_id, sequence_no, block_code, block_site,
                 block_description, embedding_box_no, special_requirement, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :samplingId, :sequenceNo, :blockCode, :blockSite,
                 :blockDescription, :embeddingBoxNo, :specialRequirement, :createdAt, :updatedAt)
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
            .addValue("createdAt", LocalDateTime.now())
            .addValue("updatedAt", LocalDateTime.now()));
    }

    @Override
    public List<SamplingBlock> findSamplingBlocksByIds(List<String> samplingBlockIds) {
        if (samplingBlockIds == null || samplingBlockIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
            select *
            from sampling_blocks
            where id in (:ids)
            order by sequence_no asc, id asc
            """, new MapSqlParameterSource().addValue("ids", samplingBlockIds), this::mapSamplingBlock);
    }

    @Override
    public Optional<SamplingBlock> findSamplingBlockById(String samplingBlockId) {
        List<SamplingBlock> rows = jdbcTemplate.query("""
            select *
            from sampling_blocks
            where id = :id
            """, Map.of("id", samplingBlockId), this::mapSamplingBlock);
        return rows.stream().findFirst();
    }

    @Override
    public List<SamplingBlock> findSamplingBlocksByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from sampling_blocks
            where case_id = :caseId
            order by sequence_no asc, id asc
            """, Map.of("caseId", caseId), this::mapSamplingBlock);
    }

    @Override
    public void insertDehydrationBatch(CreateDehydrationBatchCommand command) {
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

    @Override
    public void insertDehydrationBatchItem(CreateDehydrationBatchItemCommand command) {
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

    @Override
    public Optional<DehydrationBatch> findDehydrationBatchById(String batchId) {
        List<DehydrationBatch> rows = jdbcTemplate.query("""
            select *
            from dehydration_batches
            where id = :id
            """, Map.of("id", batchId), this::mapDehydrationBatch);
        return rows.stream().findFirst();
    }

    @Override
    public List<DehydrationBatchItem> findDehydrationBatchItems(String batchId) {
        return jdbcTemplate.query("""
            select *
            from dehydration_batch_items
            where batch_id = :batchId
            order by loaded_at asc, id asc
            """, Map.of("batchId", batchId), this::mapDehydrationBatchItem);
    }

    @Override
    public void updateDehydrationBatchStatus(String batchId,
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

    @Override
    public void updateDehydrationBatchItemStatus(String batchId,
                                                 String samplingBlockId,
                                                 String itemStatus,
                                                 String remarks) {
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

    @Override
    public void insertEmbedding(CreateEmbeddingCommand command) {
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
            .addValue("createdAt", command.endedAt() == null ? LocalDateTime.now() : command.endedAt())
            .addValue("updatedAt", command.endedAt() == null ? LocalDateTime.now() : command.endedAt()));
    }

    @Override
    public Optional<Embedding> findLatestEmbeddingBySamplingBlockId(String samplingBlockId) {
        List<Embedding> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, sampling_id, sampling_block_id, embedding_status
            from embeddings
            where sampling_block_id = :samplingBlockId
            order by created_at desc, id desc
            limit 1
            """, Map.of("samplingBlockId", samplingBlockId), this::mapEmbedding);
        return rows.stream().findFirst();
    }

    @Override
    public void insertEmbeddingBox(CreateEmbeddingBoxCommand command) {
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

    @Override
    public Optional<EmbeddingBox> findEmbeddingBoxById(String embeddingBoxId) {
        List<EmbeddingBox> rows = jdbcTemplate.query("""
            select *
            from embedding_boxes
            where id = :id
            """, Map.of("id", embeddingBoxId), this::mapEmbeddingBox);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<EmbeddingBox> findEmbeddingBoxByNo(String embeddingBoxNo) {
        List<EmbeddingBox> rows = jdbcTemplate.query("""
            select *
            from embedding_boxes
            where embedding_box_no = :embeddingBoxNo
            """, Map.of("embeddingBoxNo", embeddingBoxNo), this::mapEmbeddingBox);
        return rows.stream().findFirst();
    }

    @Override
    public List<EmbeddingBox> findEmbeddingBoxesByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from embedding_boxes
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), this::mapEmbeddingBox);
    }

    @Override
    public void insertSlicing(CreateSlicingCommand command) {
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
            .addValue("createdAt", command.slicedAt() == null ? LocalDateTime.now() : command.slicedAt())
            .addValue("updatedAt", command.slicedAt() == null ? LocalDateTime.now() : command.slicedAt()));
    }

    @Override
    public Optional<Slicing> findSlicingById(String slicingId) {
        List<Slicing> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, embedding_id, embedding_box_id, slicing_batch_no, slicing_status, slide_count
            from slicings
            where id = :id
            """, Map.of("id", slicingId), this::mapSlicing);
        return rows.stream().findFirst();
    }

    @Override
    public void insertSlide(CreateSlideCommand command) {
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

    @Override
    public List<Slide> findSlidesByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, quality_status, slide_status, slice_count
            from slides
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), this::mapSlide);
    }

    @Override
    public List<Slide> findSlidesBySlicingId(String slicingId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, quality_status, slide_status, slice_count
            from slides
            where slicing_id = :slicingId
            order by created_at asc, id asc
            """, Map.of("slicingId", slicingId), this::mapSlide);
    }

    @Override
    public Optional<Slide> findSlideById(String slideId) {
        List<Slide> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, slicing_id, embedding_box_id, sampling_block_id,
                   slide_no, quality_status, slide_status, slice_count
            from slides
            where id = :slideId
            """, Map.of("slideId", slideId), this::mapSlide);
        return rows.stream().findFirst();
    }

    @Override
    public void insertSlideStaining(CreateSlideStainingCommand command) {
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
            .addValue("createdAt", command.stainedAt() == null ? LocalDateTime.now() : command.stainedAt())
            .addValue("updatedAt", command.stainedAt() == null ? LocalDateTime.now() : command.stainedAt()));
    }

    @Override
    public List<SlideStaining> findSlideStainingsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, slide_id, staining_type, staining_status, stained_at, quality_issue, remarks
            from slide_stainings
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), this::mapSlideStaining);
    }

    @Override
    public void updateSlideStatus(String slideId, String slideStatus, String qualityStatus) {
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

    @Override
    public void insertReworkOrder(CreateReworkOrderCommand command) {
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

    @Override
    public Optional<ReworkOrder> findReworkOrderById(String reworkOrderId) {
        List<ReworkOrder> rows = jdbcTemplate.query("""
            select id, case_id, specimen_id, sampling_block_id, embedding_box_id, slide_id, rework_type, status, reason
            from rework_orders
            where id = :id
            """, Map.of("id", reworkOrderId), this::mapReworkOrder);
        return rows.stream().findFirst();
    }

    @Override
    public List<ReworkOrder> findReworkOrdersByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select id, case_id, specimen_id, sampling_block_id, embedding_box_id, slide_id, rework_type, status, reason
            from rework_orders
            where case_id = :caseId
            order by created_at asc, id asc
            """, Map.of("caseId", caseId), this::mapReworkOrder);
    }

    @Override
    public void updateReworkOrderStatus(String reworkOrderId,
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

    @Override
    public void insertSlideQcEvaluation(CreateSlideQcEvaluationCommand command) {
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

    @Override
    public void insertCaseMediaAsset(CreateCaseMediaAssetCommand command) {
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

    @Override
    public List<TrackingEvent> findTrackingEventsByCaseId(String caseId) {
        return jdbcTemplate.query("""
            select *
            from workflow_events
            where case_id = :caseId
            order by event_time asc, created_at asc
            """, Map.of("caseId", caseId), this::mapTrackingEvent);
    }

    @Override
    public void insertWorkflowEvent(TrackingEvent event) {
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

    private PathologyCase mapPathologyCase(ResultSet rs, int rowNum) throws SQLException {
        return new PathologyCase(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("pathology_no"),
            rs.getString("case_status"),
            rs.getString("source_hospital_id"),
            rs.getString("source_hospital_name"),
            rs.getString("source_department_id"),
            rs.getString("source_department_name"),
            rs.getString("received_by_user_id"),
            rs.getString("received_by_name"),
            toLocalDateTime(rs.getTimestamp("received_at")));
    }

    private Specimen mapSpecimen(ResultSet rs, int rowNum) throws SQLException {
        return new Specimen(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("case_id"),
            rs.getString("specimen_no"),
            rs.getString("barcode"),
            rs.getString("specimen_type"),
            rs.getString("specimen_name_standardized"),
            rs.getString("specimen_site"),
            rs.getString("collection_mode"),
            rs.getObject("specimen_count") == null ? null : rs.getInt("specimen_count"),
            SpecimenStatus.from(rs.getString("specimen_status")),
            FixationStatus.from(rs.getString("fixation_status")),
            rs.getInt("qualified_flag") != 0,
            rs.getString("unqualified_reason"),
            rs.getString("clinical_symptom"),
            rs.getString("applicant_department_id"),
            rs.getString("applicant_department_name"),
            rs.getString("applicant_doctor_user_id"),
            rs.getString("applicant_doctor_name"),
            toLocalDate(rs.getDate("submission_date")),
            rs.getString("label_print_batch_no"),
            rs.getString("label_print_status"),
            rs.getString("registered_by_user_id"),
            rs.getString("registered_by_name"),
            toLocalDateTime(rs.getTimestamp("registered_at")),
            rs.getString("terminal_code"),
            rs.getString("remarks"));
    }

    private TechnicalTask mapTechnicalTask(ResultSet rs, int rowNum) throws SQLException {
        return new TechnicalTask(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("case_id"),
            rs.getString("pathology_no"),
            rs.getString("specimen_id"),
            rs.getString("task_type"),
            rs.getString("task_status"),
            rs.getString("object_type"),
            rs.getString("object_id"),
            rs.getString("parent_task_id"),
            rs.getString("payload"),
            rs.getString("remarks"),
            toLocalDateTime(rs.getTimestamp("created_at")),
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")));
    }

    private SamplingBlock mapSamplingBlock(ResultSet rs, int rowNum) throws SQLException {
        return new SamplingBlock(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_id"),
            rs.getInt("sequence_no"),
            rs.getString("block_code"),
            rs.getString("block_site"),
            rs.getString("block_description"),
            rs.getString("embedding_box_no"),
            rs.getString("special_requirement"));
    }

    private DehydrationBatch mapDehydrationBatch(ResultSet rs, int rowNum) throws SQLException {
        return new DehydrationBatch(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("batch_no"),
            rs.getString("batch_status"),
            rs.getString("basket_no"),
            rs.getString("device_no"),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            toLocalDateTime(rs.getTimestamp("started_at")),
            toLocalDateTime(rs.getTimestamp("completed_at")),
            rs.getString("remarks"));
    }

    private DehydrationBatchItem mapDehydrationBatchItem(ResultSet rs, int rowNum) throws SQLException {
        return new DehydrationBatchItem(
            rs.getString("id"),
            rs.getString("batch_id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_block_id"),
            rs.getString("item_status"),
            toLocalDateTime(rs.getTimestamp("loaded_at")),
            rs.getString("remarks"));
    }

    private Embedding mapEmbedding(ResultSet rs, int rowNum) throws SQLException {
        return new Embedding(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_id"),
            rs.getString("sampling_block_id"),
            rs.getString("embedding_status"));
    }

    private EmbeddingBox mapEmbeddingBox(ResultSet rs, int rowNum) throws SQLException {
        return new EmbeddingBox(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_block_id"),
            rs.getString("embedding_id"),
            rs.getString("embedding_box_no"),
            rs.getInt("block_count"),
            rs.getInt("re_embedding_flag") == 1,
            rs.getString("slice_notice"),
            rs.getString("storage_status"));
    }

    private Slicing mapSlicing(ResultSet rs, int rowNum) throws SQLException {
        return new Slicing(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("embedding_id"),
            rs.getString("embedding_box_id"),
            rs.getString("slicing_batch_no"),
            rs.getString("slicing_status"),
            rs.getInt("slide_count"));
    }

    private Slide mapSlide(ResultSet rs, int rowNum) throws SQLException {
        return new Slide(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("slicing_id"),
            rs.getString("embedding_box_id"),
            rs.getString("sampling_block_id"),
            rs.getString("slide_no"),
            rs.getString("quality_status"),
            rs.getString("slide_status"),
            rs.getObject("slice_count") == null ? null : rs.getInt("slice_count"));
    }

    private SlideStaining mapSlideStaining(ResultSet rs, int rowNum) throws SQLException {
        return new SlideStaining(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("slide_id"),
            rs.getString("staining_type"),
            rs.getString("staining_status"),
            toLocalDateTime(rs.getTimestamp("stained_at")),
            rs.getString("quality_issue"),
            rs.getString("remarks"));
    }

    private ReworkOrder mapReworkOrder(ResultSet rs, int rowNum) throws SQLException {
        return new ReworkOrder(
            rs.getString("id"),
            rs.getString("case_id"),
            rs.getString("specimen_id"),
            rs.getString("sampling_block_id"),
            rs.getString("embedding_box_id"),
            rs.getString("slide_id"),
            rs.getString("rework_type"),
            rs.getString("status"),
            rs.getString("reason"));
    }

    private TrackingEvent mapTrackingEvent(ResultSet rs, int rowNum) throws SQLException {
        return new TrackingEvent(
            rs.getString("id"),
            rs.getString("application_id"),
            rs.getString("specimen_id"),
            rs.getString("case_id"),
            rs.getString("transport_order_id"),
            rs.getString("node_code"),
            rs.getString("event_type"),
            rs.getString("event_status"),
            toLocalDateTime(rs.getTimestamp("event_time")),
            rs.getString("operator_user_id"),
            rs.getString("operator_name"),
            rs.getString("source_terminal"),
            rs.getString("event_content"));
    }

    private LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
