package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
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
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
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
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedSlicingWorkbenchRows;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedTechnicalTasks;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.PendingTechnicalTaskQuery;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.SamplingBlock;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.SlicingWorkbenchQuery;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.SlicingWorkbenchStats;
import com.company.bl.domain.repository.TechnicalWorkflowRecords.TechnicalTask;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTechnicalWorkflowRepository implements TechnicalWorkflowRepository {

    private final JdbcTechnicalWorkflowTaskQueries taskQueries;
    private final JdbcTechnicalWorkflowSpecimenRegistrationQueries specimenRegistrationQueries;
    private final JdbcTechnicalWorkflowProcessingQueries processingQueries;
    private final JdbcTechnicalWorkflowTaskMutations taskMutations;
    private final JdbcTechnicalWorkflowSpecimenRegistrationMutations specimenRegistrationMutations;
    private final JdbcTechnicalWorkflowProcessingMutations processingMutations;

    public JdbcTechnicalWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        JdbcTechnicalWorkflowRowMappers rowMappers = new JdbcTechnicalWorkflowRowMappers();
        this.taskQueries = new JdbcTechnicalWorkflowTaskQueries(jdbcTemplate, rowMappers);
        this.specimenRegistrationQueries = new JdbcTechnicalWorkflowSpecimenRegistrationQueries(jdbcTemplate);
        this.processingQueries = new JdbcTechnicalWorkflowProcessingQueries(jdbcTemplate, rowMappers);
        this.taskMutations = new JdbcTechnicalWorkflowTaskMutations(jdbcTemplate);
        this.specimenRegistrationMutations = new JdbcTechnicalWorkflowSpecimenRegistrationMutations(jdbcTemplate);
        this.processingMutations = new JdbcTechnicalWorkflowProcessingMutations(jdbcTemplate);
    }

    @Override
    public Optional<PathologyCase> findPathologyCaseById(String caseId) {
        return taskQueries.findPathologyCaseById(caseId);
    }

    @Override
    public Optional<PathologyCase> findPathologyCaseByPathologyNo(String pathologyNo) {
        return taskQueries.findPathologyCaseByPathologyNo(pathologyNo);
    }

    @Override
    public List<Specimen> findSpecimensByCaseId(String caseId) {
        return taskQueries.findSpecimensByCaseId(caseId);
    }

    @Override
    public Optional<Specimen> findSpecimenById(String specimenId) {
        return taskQueries.findSpecimenById(specimenId);
    }

    @Override
    public Optional<TechnicalTask> findTechnicalTaskById(String taskId) {
        return taskQueries.findTechnicalTaskById(taskId);
    }

    @Override
    public List<TechnicalTask> findActiveTechnicalTasksByCaseId(String caseId) {
        return taskQueries.findActiveTechnicalTasksByCaseId(caseId);
    }

    @Override
    public List<TechnicalTask> findActiveTechnicalTasksByObject(String taskType, String objectType, String objectId) {
        return taskQueries.findActiveTechnicalTasksByObject(taskType, objectType, objectId);
    }

    @Override
    public List<TechnicalTask> findActiveTechnicalTasksByTypeAndCreatedRange(
        String taskType,
        LocalDateTime createdFrom,
        LocalDateTime createdTo
    ) {
        return taskQueries.findActiveTechnicalTasksByTypeAndCreatedRange(taskType, createdFrom, createdTo);
    }

    @Override
    public PagedTechnicalTasks findTechnicalTasks(PendingTechnicalTaskQuery query) {
        return taskQueries.findTechnicalTasks(query);
    }

    @Override
    public SlicingWorkbenchStats summarizeSlicingWorkbench(SlicingWorkbenchQuery query) {
        return processingQueries.summarizeSlicingWorkbench(query);
    }

    @Override
    public PagedSlicingWorkbenchRows findPendingSlicingWorkbenchRows(SlicingWorkbenchQuery query) {
        return processingQueries.findPendingSlicingWorkbenchRows(query);
    }

    @Override
    public PagedSlicingWorkbenchRows findPendingSlicingPrintRows(SlicingWorkbenchQuery query) {
        return processingQueries.findPendingSlicingPrintRows(query);
    }

    @Override
    public List<com.company.bl.domain.repository.TechnicalWorkflowRecords.SlicingWorkbenchRow> findPendingSlicingPrintRowsByTaskIds(List<String> taskIds) {
        return processingQueries.findPendingSlicingPrintRowsByTaskIds(taskIds);
    }

    @Override
    public List<com.company.bl.domain.repository.TechnicalWorkflowRecords.SlicingSlidePrintMergeGroupItem> findPendingSlicingPrintMergeGroupItems(String printGroupId) {
        return processingQueries.findPendingSlicingPrintMergeGroupItems(printGroupId);
    }

    @Override
    public PagedSlicingWorkbenchRows findPendingSlicingProcessRows(SlicingWorkbenchQuery query) {
        return processingQueries.findPendingSlicingProcessRows(query);
    }

    @Override
    public PagedSlicingWorkbenchRows findCompletedSlicingWorkbenchRows(SlicingWorkbenchQuery query) {
        return processingQueries.findCompletedSlicingWorkbenchRows(query);
    }

    @Override
    public com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedTechnicalSpecimenRegistrations findTechnicalSpecimenRegistrations(
        com.company.bl.domain.repository.TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery query) {
        return specimenRegistrationQueries.findTechnicalSpecimenRegistrations(query);
    }

    @Override
    public com.company.bl.domain.repository.TechnicalWorkflowRecords.PagedTechnicalSpecimenRegistrations findPendingTechnicalSpecimenRegistrations(
        com.company.bl.domain.repository.TechnicalWorkflowRecords.PendingTechnicalSpecimenRegistrationQuery query) {
        return specimenRegistrationQueries.findPendingTechnicalSpecimenRegistrations(query);
    }

    @Override
    public java.util.Optional<com.company.bl.domain.repository.TechnicalWorkflowRecords.TechnicalSpecimenRegistration> findTechnicalSpecimenRegistrationByCaseId(
        String caseId) {
        return specimenRegistrationQueries.findTechnicalSpecimenRegistrationByCaseId(caseId);
    }

    @Override
    public void updatePathologyCaseStatus(String caseId, String caseStatus) {
        taskMutations.updatePathologyCaseStatus(caseId, caseStatus);
    }

    @Override
    public void startTechnicalTask(String taskId, String operatorUserId, String operatorName, String taskStatus, String remarks, LocalDateTime startedAt) {
        taskMutations.startTechnicalTask(taskId, taskStatus, remarks, startedAt);
    }

    @Override
    public void completeTechnicalTask(String taskId, String taskStatus, String remarks, LocalDateTime completedAt) {
        taskMutations.completeTechnicalTask(taskId, taskStatus, remarks, completedAt);
    }

    @Override
    public void resetTechnicalTaskToPending(String taskId, String remarks, LocalDateTime updatedAt) {
        taskMutations.resetTechnicalTaskToPending(taskId, remarks, updatedAt);
    }

    @Override
    public void assignTechnicalTask(String taskId,
                                    String priority,
                                    String stationCode,
                                    String stationName,
                                    String assignedToUserId,
                                    String assignedToName,
                                    LocalDateTime expectedCompletedAt,
                                    String productionRemarks) {
        taskMutations.assignTechnicalTask(
            taskId, priority, stationCode, stationName, assignedToUserId, assignedToName,
            expectedCompletedAt, productionRemarks);
    }

    @Override
    public void claimTechnicalTask(String taskId,
                                   String assignedToUserId,
                                   String assignedToName,
                                   String stationCode,
                                   String stationName,
                                   String remarks) {
        taskMutations.claimTechnicalTask(taskId, assignedToUserId, assignedToName, stationCode, stationName, remarks);
    }

    @Override
    public void releaseTechnicalTask(String taskId, String remarks) {
        taskMutations.releaseTechnicalTask(taskId, remarks);
    }

    @Override
    public void updateTechnicalTaskPriority(String taskId, String priority, String productionRemarks) {
        taskMutations.updateTechnicalTaskPriority(taskId, priority, productionRemarks);
    }

    @Override
    public void updateTechnicalTaskRemarks(String taskId, String remarks, String productionRemarks) {
        taskMutations.updateTechnicalTaskRemarks(taskId, remarks, productionRemarks);
    }

    @Override
    public void insertTechnicalTask(CreateTechnicalTaskCommand command) {
        taskMutations.insertTechnicalTask(command);
    }

    @Override
    public void ensureTechnicalSpecimenRegistrationPending(String applicationId, String caseId) {
        specimenRegistrationMutations.ensureTechnicalSpecimenRegistrationPending(applicationId, caseId);
    }

    @Override
    public void completeTechnicalSpecimenRegistration(String caseId,
                                                      String registeredByUserId,
                                                      String registeredByName,
                                                      String remarks,
                                                      LocalDateTime registeredAt) {
        specimenRegistrationMutations.completeTechnicalSpecimenRegistration(
            caseId, registeredByUserId, registeredByName, remarks, registeredAt);
    }

    @Override
    public void insertSampling(CreateSamplingCommand command) {
        taskMutations.insertSampling(command);
    }

    @Override
    public void insertSamplingBlock(CreateSamplingBlockCommand command) {
        taskMutations.insertSamplingBlock(command);
    }

    @Override
    public List<SamplingBlock> findSamplingBlocksByIds(List<String> samplingBlockIds) {
        return taskQueries.findSamplingBlocksByIds(samplingBlockIds);
    }

    @Override
    public Optional<SamplingBlock> findSamplingBlockById(String samplingBlockId) {
        return taskQueries.findSamplingBlockById(samplingBlockId);
    }

    @Override
    public List<SamplingBlock> findSamplingBlocksByCaseId(String caseId) {
        return taskQueries.findSamplingBlocksByCaseId(caseId);
    }

    @Override
    public void insertDehydrationBatch(CreateDehydrationBatchCommand command) {
        taskMutations.insertDehydrationBatch(command);
    }

    @Override
    public void insertDehydrationBatchItem(CreateDehydrationBatchItemCommand command) {
        taskMutations.insertDehydrationBatchItem(command);
    }

    @Override
    public Optional<DehydrationBatch> findDehydrationBatchById(String batchId) {
        return taskQueries.findDehydrationBatchById(batchId);
    }

    @Override
    public List<DehydrationBatchItem> findDehydrationBatchItems(String batchId) {
        return taskQueries.findDehydrationBatchItems(batchId);
    }

    @Override
    public void updateDehydrationBatchStatus(String batchId,
                                             String batchStatus,
                                             String operatorUserId,
                                             String operatorName,
                                             LocalDateTime startedAt,
                                             LocalDateTime completedAt,
                                             String remarks) {
        taskMutations.updateDehydrationBatchStatus(batchId, batchStatus, operatorUserId, operatorName, startedAt, completedAt, remarks);
    }

    @Override
    public void updateDehydrationBatchItemStatus(String batchId, String samplingBlockId, String itemStatus, String remarks) {
        taskMutations.updateDehydrationBatchItemStatus(batchId, samplingBlockId, itemStatus, remarks);
    }

    @Override
    public void insertEmbedding(CreateEmbeddingCommand command) {
        taskMutations.insertEmbedding(command);
    }

    @Override
    public Optional<Embedding> findLatestEmbeddingBySamplingBlockId(String samplingBlockId) {
        return processingQueries.findLatestEmbeddingBySamplingBlockId(samplingBlockId);
    }

    @Override
    public void insertEmbeddingBox(CreateEmbeddingBoxCommand command) {
        taskMutations.insertEmbeddingBox(command);
    }

    @Override
    public Optional<EmbeddingBox> findEmbeddingBoxById(String embeddingBoxId) {
        return processingQueries.findEmbeddingBoxById(embeddingBoxId);
    }

    @Override
    public Optional<EmbeddingBox> findEmbeddingBoxByCaseIdAndNo(String caseId, String embeddingBoxNo) {
        return processingQueries.findEmbeddingBoxByCaseIdAndNo(caseId, embeddingBoxNo);
    }

    @Override
    public List<EmbeddingBox> findEmbeddingBoxesByCaseId(String caseId) {
        return processingQueries.findEmbeddingBoxesByCaseId(caseId);
    }

    @Override
    public List<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordsByCaseId(String caseId) {
        return processingQueries.findEmbeddingWorkstationRecordsByCaseId(caseId);
    }

    @Override
    public Optional<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordByEmbeddingId(String embeddingId) {
        return processingQueries.findEmbeddingWorkstationRecordByEmbeddingId(embeddingId);
    }

    @Override
    public List<EmbeddingWorkstationRecord> findEmbeddingWorkstationRecordsByEndedAtRange(
        LocalDateTime endedFrom,
        LocalDateTime endedTo
    ) {
        return processingQueries.findEmbeddingWorkstationRecordsByEndedAtRange(endedFrom, endedTo);
    }

    @Override
    public void updateEmbeddingQualityReview(String embeddingId, String evaluationLevel, String samplingEvaluation) {
        taskMutations.updateEmbeddingQualityReview(embeddingId, evaluationLevel, samplingEvaluation);
    }

    @Override
    public void updateEmbeddingBoxSliceNoticeByEmbeddingId(String embeddingId, String sliceNotice) {
        taskMutations.updateEmbeddingBoxSliceNoticeByEmbeddingId(embeddingId, sliceNotice);
    }

    @Override
    public void insertSlicing(CreateSlicingCommand command) {
        processingMutations.insertSlicing(command);
    }

    @Override
    public Optional<Slicing> findSlicingById(String slicingId) {
        return processingQueries.findSlicingById(slicingId);
    }

    @Override
    public Optional<Slicing> findSlicingByTaskId(String taskId) {
        return processingQueries.findSlicingByTaskId(taskId);
    }

    @Override
    public Optional<Slicing> findSlicingByTaskIdAndEmbeddingBoxId(String taskId, String embeddingBoxId) {
        return processingQueries.findSlicingByTaskIdAndEmbeddingBoxId(taskId, embeddingBoxId);
    }

    @Override
    public void insertSlicingSlidePrintMergeGroup(String groupId,
                                                  String caseId,
                                                  String pathologyNo,
                                                  String patientId,
                                                  String embeddingBoxNo,
                                                  String operatorUserId,
                                                  String operatorName,
                                                  String remarks,
                                                  LocalDateTime createdAt) {
        processingMutations.insertSlicingSlidePrintMergeGroup(
            groupId,
            caseId,
            pathologyNo,
            patientId,
            embeddingBoxNo,
            operatorUserId,
            operatorName,
            remarks,
            createdAt);
    }

    @Override
    public void insertSlicingSlidePrintMergeGroupItem(String itemId,
                                                      String groupId,
                                                      String taskId,
                                                      String embeddingBoxId,
                                                      String embeddingBoxNo,
                                                      int sequenceNo) {
        processingMutations.insertSlicingSlidePrintMergeGroupItem(
            itemId,
            groupId,
            taskId,
            embeddingBoxId,
            embeddingBoxNo,
            sequenceNo);
    }

    @Override
    public void cancelSlicingSlidePrintMergeGroups(List<String> printGroupIds, LocalDateTime updatedAt) {
        processingMutations.cancelSlicingSlidePrintMergeGroups(printGroupIds, updatedAt);
    }

    @Override
    public void markSlicingSlidePrintMergeGroupPrinted(String printGroupId,
                                                       String slicingId,
                                                       String operatorUserId,
                                                       String operatorName,
                                                       String remarks,
                                                       LocalDateTime printedAt) {
        processingMutations.markSlicingSlidePrintMergeGroupPrinted(
            printGroupId,
            slicingId,
            operatorUserId,
            operatorName,
            remarks,
            printedAt);
    }

    @Override
    public void completeSlicingRecord(String slicingId,
                                      String slicingStatus,
                                      Integer sliceCountPerSlide,
                                      String sliceThickness,
                                      String slicedByUserId,
                                      String slicedByName,
                                      LocalDateTime slicedAt,
                                      String qualityIssue,
                                      String remarks) {
        processingMutations.completeSlicingRecord(
            slicingId,
            slicingStatus,
            sliceCountPerSlide,
            sliceThickness,
            slicedByUserId,
            slicedByName,
            slicedAt,
            qualityIssue,
            remarks);
    }

    @Override
    public void insertSlide(CreateSlideCommand command) {
        processingMutations.insertSlide(command);
    }

    @Override
    public List<Slide> findSlidesByCaseId(String caseId) {
        return processingQueries.findSlidesByCaseId(caseId);
    }

    @Override
    public List<Slide> findSlidesBySlicingId(String slicingId) {
        return processingQueries.findSlidesBySlicingId(slicingId);
    }

    @Override
    public Optional<Slide> findSlideById(String slideId) {
        return processingQueries.findSlideById(slideId);
    }

    @Override
    public void insertSlideStaining(CreateSlideStainingCommand command) {
        processingMutations.insertSlideStaining(command);
    }

    @Override
    public List<SlideStaining> findSlideStainingsByCaseId(String caseId) {
        return processingQueries.findSlideStainingsByCaseId(caseId);
    }

    @Override
    public void updateSlideStatus(String slideId, String slideStatus, String qualityStatus) {
        processingMutations.updateSlideStatus(slideId, slideStatus, qualityStatus);
    }

    @Override
    public void insertReworkOrder(CreateReworkOrderCommand command) {
        processingMutations.insertReworkOrder(command);
    }

    @Override
    public Optional<ReworkOrder> findReworkOrderById(String reworkOrderId) {
        return processingQueries.findReworkOrderById(reworkOrderId);
    }

    @Override
    public List<ReworkOrder> findReworkOrdersByCaseId(String caseId) {
        return processingQueries.findReworkOrdersByCaseId(caseId);
    }

    @Override
    public void updateReworkOrderStatus(String reworkOrderId,
                                        String status,
                                        String executedByUserId,
                                        String executedByName,
                                        LocalDateTime executedAt,
                                        String remarks) {
        processingMutations.updateReworkOrderStatus(reworkOrderId, status, executedByUserId, executedByName, executedAt, remarks);
    }

    @Override
    public void insertSlideQcEvaluation(CreateSlideQcEvaluationCommand command) {
        processingMutations.insertSlideQcEvaluation(command);
    }

    @Override
    public List<SlideQcEvaluation> findSlideQcEvaluationsByCaseId(String caseId) {
        return processingQueries.findSlideQcEvaluationsByCaseId(caseId);
    }

    @Override
    public void insertCaseMediaAsset(CreateCaseMediaAssetCommand command) {
        processingMutations.insertCaseMediaAsset(command);
    }

    @Override
    public List<CaseMediaAsset> findCaseMediaAssets(String caseId, String objectType, String objectId, String mediaType) {
        return processingQueries.findCaseMediaAssets(caseId, objectType, objectId, mediaType);
    }

    @Override
    public List<CaseMediaAsset> findCaseMediaAssets(String caseId, String objectType, String mediaType) {
        return processingQueries.findCaseMediaAssets(caseId, objectType, mediaType);
    }

    @Override
    public Optional<CaseMediaAsset> findCaseMediaAssetById(String assetId) {
        return processingQueries.findCaseMediaAssetById(assetId);
    }

    @Override
    public void deleteCaseMediaAsset(String assetId) {
        processingMutations.deleteCaseMediaAsset(assetId);
    }

    @Override
    public List<TrackingEvent> findTrackingEventsByCaseId(String caseId) {
        return processingQueries.findTrackingEventsByCaseId(caseId);
    }

    @Override
    public List<TrackingEvent> findRecentTrackingEventsByCaseId(String caseId, int limit) {
        return processingQueries.findRecentTrackingEventsByCaseId(caseId, limit);
    }

    @Override
    public void insertWorkflowEvent(TrackingEvent event) {
        processingMutations.insertWorkflowEvent(event);
    }

    @Override
    public Optional<TechnicalWorkflowRecords.WorkstationDailyClearRecord> findWorkstationDailyClear(
        String workstationType,
        LocalDate workDate
    ) {
        return processingQueries.findWorkstationDailyClear(workstationType, workDate);
    }

    @Override
    public TechnicalWorkflowRecords.WorkstationDailyClearRecord insertWorkstationDailyClear(
        TechnicalWorkflowRecords.CreateWorkstationDailyClearCommand command
    ) {
        processingMutations.insertWorkstationDailyClear(command);
        return processingQueries.findWorkstationDailyClear(command.workstationType(), command.workDate())
            .orElseThrow();
    }
}
