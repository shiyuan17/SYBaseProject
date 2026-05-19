package com.company.bl.domain.repository;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TechnicalWorkflowRepository {

    Optional<PathologyCase> findPathologyCaseById(String caseId);

    List<Specimen> findSpecimensByCaseId(String caseId);

    Optional<Specimen> findSpecimenById(String specimenId);

    Optional<TechnicalTask> findTechnicalTaskById(String taskId);

    List<TechnicalTask> findActiveTechnicalTasksByCaseId(String caseId);

    List<TechnicalTask> findActiveTechnicalTasksByObject(String taskType, String objectType, String objectId);

    PagedTechnicalTasks findTechnicalTasks(PendingTechnicalTaskQuery query);

    void updatePathologyCaseStatus(String caseId, String caseStatus);

    void startTechnicalTask(String taskId,
                            String operatorUserId,
                            String operatorName,
                            String remarks,
                            LocalDateTime startedAt);

    void completeTechnicalTask(String taskId,
                               String taskStatus,
                               String remarks,
                               LocalDateTime completedAt);

    void insertTechnicalTask(CreateTechnicalTaskCommand command);

    void insertSampling(CreateSamplingCommand command);

    void insertSamplingBlock(CreateSamplingBlockCommand command);

    List<SamplingBlock> findSamplingBlocksByIds(List<String> samplingBlockIds);

    Optional<SamplingBlock> findSamplingBlockById(String samplingBlockId);

    List<SamplingBlock> findSamplingBlocksByCaseId(String caseId);

    void insertDehydrationBatch(CreateDehydrationBatchCommand command);

    void insertDehydrationBatchItem(CreateDehydrationBatchItemCommand command);

    Optional<DehydrationBatch> findDehydrationBatchById(String batchId);

    List<DehydrationBatchItem> findDehydrationBatchItems(String batchId);

    void updateDehydrationBatchStatus(String batchId,
                                      String batchStatus,
                                      String operatorUserId,
                                      String operatorName,
                                      LocalDateTime startedAt,
                                      LocalDateTime completedAt,
                                      String remarks);

    void updateDehydrationBatchItemStatus(String batchId,
                                          String samplingBlockId,
                                          String itemStatus,
                                          String remarks);

    void insertEmbedding(CreateEmbeddingCommand command);

    Optional<Embedding> findLatestEmbeddingBySamplingBlockId(String samplingBlockId);

    void insertEmbeddingBox(CreateEmbeddingBoxCommand command);

    Optional<EmbeddingBox> findEmbeddingBoxById(String embeddingBoxId);

    Optional<EmbeddingBox> findEmbeddingBoxByNo(String embeddingBoxNo);

    List<EmbeddingBox> findEmbeddingBoxesByCaseId(String caseId);

    void insertSlicing(CreateSlicingCommand command);

    Optional<Slicing> findSlicingById(String slicingId);

    void insertSlide(CreateSlideCommand command);

    List<Slide> findSlidesByCaseId(String caseId);

    List<Slide> findSlidesBySlicingId(String slicingId);

    Optional<Slide> findSlideById(String slideId);

    void insertSlideStaining(CreateSlideStainingCommand command);

    List<SlideStaining> findSlideStainingsByCaseId(String caseId);

    void updateSlideStatus(String slideId, String slideStatus, String qualityStatus);

    void insertReworkOrder(CreateReworkOrderCommand command);

    Optional<ReworkOrder> findReworkOrderById(String reworkOrderId);

    List<ReworkOrder> findReworkOrdersByCaseId(String caseId);

    void updateReworkOrderStatus(String reworkOrderId,
                                 String status,
                                 String executedByUserId,
                                 String executedByName,
                                 LocalDateTime executedAt,
                                 String remarks);

    void insertSlideQcEvaluation(CreateSlideQcEvaluationCommand command);

    void insertCaseMediaAsset(CreateCaseMediaAssetCommand command);

    List<TrackingEvent> findTrackingEventsByCaseId(String caseId);

    void insertWorkflowEvent(TrackingEvent event);

    record PendingTechnicalTaskQuery(
        int page,
        int size,
        String taskType,
        String taskStatus,
        String applicationNo,
        String pathologyNo,
        String objectType
    ) {
    }

    record PagedTechnicalTasks(List<TechnicalTask> items, long total) {
    }

    record TechnicalTask(
        String id,
        String applicationId,
        String applicationNo,
        String caseId,
        String pathologyNo,
        String specimenId,
        String taskType,
        String taskStatus,
        String objectType,
        String objectId,
        String parentTaskId,
        String payload,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
    ) {
    }

    record CreateTechnicalTaskCommand(
        String id,
        String applicationId,
        String caseId,
        String specimenId,
        String taskType,
        String taskStatus,
        String objectType,
        String objectId,
        String parentTaskId,
        String payload,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    record CreateSamplingCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingStatus,
        int blockCount,
        int grossImageCount,
        String samplingTemplateId,
        String grossDescription,
        String sampledByUserId,
        String sampledByName,
        LocalDateTime sampledAt,
        String remarks
    ) {
    }

    record CreateSamplingBlockCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingId,
        int sequenceNo,
        String blockCode,
        String blockSite,
        String blockDescription,
        String embeddingBoxNo,
        String specialRequirement
    ) {
    }

    record SamplingBlock(
        String id,
        String caseId,
        String specimenId,
        String samplingId,
        int sequenceNo,
        String blockCode,
        String blockSite,
        String blockDescription,
        String embeddingBoxNo,
        String specialRequirement
    ) {
    }

    record CreateDehydrationBatchCommand(
        String id,
        String caseId,
        String batchNo,
        String batchStatus,
        String basketNo,
        String deviceNo,
        String operatorUserId,
        String operatorName,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    record CreateDehydrationBatchItemCommand(
        String id,
        String batchId,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String itemStatus,
        LocalDateTime loadedAt,
        String remarks
    ) {
    }

    record DehydrationBatch(
        String id,
        String caseId,
        String batchNo,
        String batchStatus,
        String basketNo,
        String deviceNo,
        String operatorUserId,
        String operatorName,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String remarks
    ) {
    }

    record DehydrationBatchItem(
        String id,
        String batchId,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String itemStatus,
        LocalDateTime loadedAt,
        String remarks
    ) {
    }

    record CreateEmbeddingCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingId,
        String samplingBlockId,
        String embeddingStatus,
        String evaluationLevel,
        String samplingEvaluation,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String embeddedByUserId,
        String embeddedByName,
        String remarks
    ) {
    }

    record Embedding(
        String id,
        String caseId,
        String specimenId,
        String samplingId,
        String samplingBlockId,
        String embeddingStatus
    ) {
    }

    record CreateEmbeddingBoxCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingId,
        String embeddingBoxNo,
        int blockCount,
        boolean reEmbeddingFlag,
        String sliceNotice,
        String storageStatus
    ) {
    }

    record EmbeddingBox(
        String id,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingId,
        String embeddingBoxNo,
        int blockCount,
        boolean reEmbeddingFlag,
        String sliceNotice,
        String storageStatus
    ) {
    }

    record CreateSlicingCommand(
        String id,
        String caseId,
        String specimenId,
        String embeddingId,
        String embeddingBoxId,
        String slicingBatchNo,
        String slicingStatus,
        int slideCount,
        Integer sliceCountPerSlide,
        String sliceThickness,
        String slicedByUserId,
        String slicedByName,
        LocalDateTime slicedAt,
        String qualityIssue,
        String remarks
    ) {
    }

    record Slicing(
        String id,
        String caseId,
        String specimenId,
        String embeddingId,
        String embeddingBoxId,
        String slicingBatchNo,
        String slicingStatus,
        int slideCount
    ) {
    }

    record CreateSlideCommand(
        String id,
        String caseId,
        String specimenId,
        String slicingId,
        String embeddingBoxId,
        String samplingBlockId,
        String slideNo,
        String slideLabel,
        boolean combinedSlideFlag,
        String qualityStatus,
        String slideStatus,
        Integer sliceCount
    ) {
    }

    record Slide(
        String id,
        String caseId,
        String specimenId,
        String slicingId,
        String embeddingBoxId,
        String samplingBlockId,
        String slideNo,
        String qualityStatus,
        String slideStatus,
        Integer sliceCount
    ) {
    }

    record CreateSlideStainingCommand(
        String id,
        String caseId,
        String specimenId,
        String slideId,
        String stainingType,
        String stainingStatus,
        String stainedByUserId,
        String stainedByName,
        LocalDateTime stainedAt,
        String qualityIssue,
        String remarks
    ) {
    }

    record SlideStaining(
        String id,
        String caseId,
        String specimenId,
        String slideId,
        String stainingType,
        String stainingStatus,
        LocalDateTime stainedAt,
        String qualityIssue,
        String remarks
    ) {
    }

    record CreateReworkOrderCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingBoxId,
        String slideId,
        String reworkType,
        String status,
        String reason,
        String requestedByUserId,
        String requestedByName,
        LocalDateTime requestedAt,
        String remarks
    ) {
    }

    record ReworkOrder(
        String id,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingBoxId,
        String slideId,
        String reworkType,
        String status,
        String reason
    ) {
    }

    record CreateSlideQcEvaluationCommand(
        String id,
        String caseId,
        String specimenId,
        String slideId,
        String qcType,
        String evaluationResult,
        String issueDescription,
        String improvementSuggestion,
        String evaluatorUserId,
        String evaluatorName,
        LocalDateTime evaluatedAt,
        String remarks
    ) {
    }

    record CreateCaseMediaAssetCommand(
        String id,
        String caseId,
        String specimenId,
        String objectType,
        String objectId,
        String mediaType,
        String fileUrl,
        String fileName,
        LocalDateTime capturedAt,
        String capturedByUserId,
        String capturedByName,
        String remarks
    ) {
    }
}
