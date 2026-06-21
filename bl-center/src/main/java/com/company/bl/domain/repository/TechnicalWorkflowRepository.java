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
import com.company.bl.domain.repository.TechnicalWorkflowRecords.CaseMediaAsset;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatch;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.DehydrationBatchItem;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.Embedding;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingWorkstationRecord;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.WorkstationDailyClearRecord;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedTechnicalSpecimenRegistrations;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedTechnicalTasks;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PendingTechnicalTaskQuery;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.SamplingBlock;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.TechnicalSpecimenRegistration;
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

import java.time.LocalDate;
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

    List<TechnicalTask> findActiveTechnicalTasksByTypeAndCreatedRange(String taskType,
                                                                      LocalDateTime createdFrom,
                                                                      LocalDateTime createdTo);

    PagedTechnicalTasks findTechnicalTasks(PendingTechnicalTaskQuery query);

    TechnicalWorkflowRecords.PagedTechnicalTrackingCases findTechnicalTrackingCases(
        TechnicalWorkflowRecords.TechnicalTrackingCaseListQuery query
    );

    TechnicalWorkflowRecords.SlicingWorkbenchStats summarizeSlicingWorkbench(TechnicalWorkflowRecords.SlicingWorkbenchQuery query);

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findPendingSlicingWorkbenchRows(TechnicalWorkflowRecords.SlicingWorkbenchQuery query);

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findPendingSlicingPrintRows(TechnicalWorkflowRecords.SlicingWorkbenchQuery query);

    List<TechnicalWorkflowRecords.SlicingWorkbenchRow> findPendingSlicingPrintRowsByTaskIds(List<String> taskIds);

    List<TechnicalWorkflowRecords.SlicingSlidePrintMergeGroupItem> findPendingSlicingPrintMergeGroupItems(String printGroupId);

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findPendingSlicingProcessRows(TechnicalWorkflowRecords.SlicingWorkbenchQuery query);

    TechnicalWorkflowRecords.PagedSlicingWorkbenchRows findCompletedSlicingWorkbenchRows(TechnicalWorkflowRecords.SlicingWorkbenchQuery query);

    PagedTechnicalSpecimenRegistrations findTechnicalSpecimenRegistrations(PendingTechnicalSpecimenRegistrationQuery query);

    PagedTechnicalSpecimenRegistrations findPendingTechnicalSpecimenRegistrations(PendingTechnicalSpecimenRegistrationQuery query);

    Optional<TechnicalSpecimenRegistration> findTechnicalSpecimenRegistrationByCaseId(String caseId);

    void updatePathologyCaseStatus(String caseId, String caseStatus);

    void startTechnicalTask(String taskId,
                            String operatorUserId,
                            String operatorName,
                            String taskStatus,
                            String remarks,
                            LocalDateTime startedAt);

    void completeTechnicalTask(String taskId,
                               String taskStatus,
                               String remarks,
                               LocalDateTime completedAt);

    void resetTechnicalTaskToPending(String taskId,
                                     String remarks,
                                     LocalDateTime updatedAt);

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

    void updateTechnicalTaskRemarks(String taskId, String remarks, String productionRemarks);

    void insertTechnicalTask(CreateTechnicalTaskCommand command);

    void ensureTechnicalSpecimenRegistrationPending(String applicationId, String caseId);

    void completeTechnicalSpecimenRegistration(String caseId,
                                               String registeredByUserId,
                                               String registeredByName,
                                               String remarks,
                                               LocalDateTime registeredAt);

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

    Optional<EmbeddingBox> findEmbeddingBoxByCaseIdAndNo(String caseId, String embeddingBoxNo);

    List<EmbeddingBox> findEmbeddingBoxesByCaseId(String caseId);

    List<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordsByCaseId(String caseId);

    List<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordsByEndedAtRange(LocalDateTime endedFrom,
                                                                                   LocalDateTime endedTo);

    Optional<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordByEmbeddingId(String embeddingId);

    void updateEmbeddingQualityReview(String embeddingId, String evaluationLevel, String samplingEvaluation);

    void updateEmbeddingBoxSliceNoticeByEmbeddingId(String embeddingId, String sliceNotice);

    void insertSlicing(CreateSlicingCommand command);

    Optional<Slicing> findSlicingById(String slicingId);

    Optional<Slicing> findSlicingByTaskId(String taskId);

    Optional<Slicing> findSlicingByTaskIdAndEmbeddingBoxId(String taskId, String embeddingBoxId);

    void insertSlicingSlidePrintMergeGroup(String groupId,
                                           String caseId,
                                           String pathologyNo,
                                           String patientId,
                                           String embeddingBoxNo,
                                           String operatorUserId,
                                           String operatorName,
                                           String remarks,
                                           LocalDateTime createdAt);

    void insertSlicingSlidePrintMergeGroupItem(String itemId,
                                               String groupId,
                                               String taskId,
                                               String embeddingBoxId,
                                               String embeddingBoxNo,
                                               int sequenceNo);

    void cancelSlicingSlidePrintMergeGroups(List<String> printGroupIds, LocalDateTime updatedAt);

    void markSlicingSlidePrintMergeGroupPrinted(String printGroupId,
                                                String slicingId,
                                                String operatorUserId,
                                                String operatorName,
                                                String remarks,
                                                LocalDateTime printedAt);

    void completeSlicingRecord(String slicingId,
                               String slicingStatus,
                               Integer sliceCountPerSlide,
                               String sliceThickness,
                               String slicedByUserId,
                               String slicedByName,
                               LocalDateTime slicedAt,
                               String qualityIssue,
                               String remarks);

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

    List<CaseMediaAsset> findCaseMediaAssets(String caseId, String objectType, String objectId, String mediaType);

    List<CaseMediaAsset> findCaseMediaAssets(String caseId, String objectType, String mediaType);

    Optional<CaseMediaAsset> findCaseMediaAssetById(String assetId);

    void deleteCaseMediaAsset(String assetId);

    List<TrackingEvent> findTrackingEventsByCaseId(String caseId);

    List<TrackingEvent> findRecentTrackingEventsByCaseId(String caseId, int limit);

    void insertWorkflowEvent(TrackingEvent event);

    Optional<WorkstationDailyClearRecord> findWorkstationDailyClear(
        String workstationType,
        LocalDate workDate
    );

    WorkstationDailyClearRecord insertWorkstationDailyClear(
        TechnicalWorkflowRecords.CreateWorkstationDailyClearCommand command
    );
}
