package com.company.bl.domain.repository;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateDehydrationBatchCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateDehydrationBatchItemCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateEmbeddingBoxCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateEmbeddingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateSamplingBlockCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateSamplingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CreateTechnicalTaskCommand;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatch;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatchItem;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.Embedding;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedTechnicalTasks;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PendingTechnicalTaskQuery;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.SamplingBlock;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.TechnicalTask;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateCaseMediaAssetCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateReworkOrderCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlicingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideQcEvaluationCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.CreateSlideStainingCommand;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.ReworkOrder;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slide;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.SlideQcEvaluation;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.SlideStaining;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slicing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TechnicalWorkflowRepository {

    Optional<PathologyCase> findPathologyCaseById(String caseId);

    Optional<PathologyCase> findPathologyCaseByPathologyNo(String pathologyNo);

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

    void assignTechnicalTask(String taskId,
                             String priority,
                             String stationCode,
                             String stationName,
                             String assignedToUserId,
                             String assignedToName,
                             LocalDateTime expectedCompletedAt,
                             String productionRemarks);

    void claimTechnicalTask(String taskId,
                            String assignedToUserId,
                            String assignedToName,
                            String stationCode,
                            String stationName,
                            String remarks);

    void releaseTechnicalTask(String taskId, String remarks);

    void updateTechnicalTaskPriority(String taskId, String priority, String productionRemarks);

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

    List<SlideQcEvaluation> findSlideQcEvaluationsByCaseId(String caseId);

    void insertCaseMediaAsset(CreateCaseMediaAssetCommand command);

    List<TrackingEvent> findTrackingEventsByCaseId(String caseId);

    List<TrackingEvent> findRecentTrackingEventsByCaseId(String caseId, int limit);

    void insertWorkflowEvent(TrackingEvent event);
}
