package com.company.bl.application.service;

import org.springframework.stereotype.Service;

@Service
public class TechnicalWorkflowAppService {

    private final TechnicalWorkflowQueryService technicalWorkflowQueryService;
    private final TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService;
    private final TechnicalTaskManagementService technicalTaskManagementService;
    private final TechnicalGrossingWorkflowService technicalGrossingWorkflowService;
    private final TechnicalProcessingWorkflowService technicalProcessingWorkflowService;
    private final TechnicalReworkWorkflowService technicalReworkWorkflowService;

    public TechnicalWorkflowAppService(TechnicalWorkflowQueryService technicalWorkflowQueryService,
                                       TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService,
                                       TechnicalTaskManagementService technicalTaskManagementService,
                                       TechnicalGrossingWorkflowService technicalGrossingWorkflowService,
                                       TechnicalProcessingWorkflowService technicalProcessingWorkflowService,
                                       TechnicalReworkWorkflowService technicalReworkWorkflowService) {
        this.technicalWorkflowQueryService = technicalWorkflowQueryService;
        this.technicalSpecimenRegistrationService = technicalSpecimenRegistrationService;
        this.technicalTaskManagementService = technicalTaskManagementService;
        this.technicalGrossingWorkflowService = technicalGrossingWorkflowService;
        this.technicalProcessingWorkflowService = technicalProcessingWorkflowService;
        this.technicalReworkWorkflowService = technicalReworkWorkflowService;
    }

    public TechnicalWorkflowModels.PendingTechnicalTaskPage listPendingTasks(TechnicalWorkflowModels.PendingTechnicalTaskQuery query) {
        return technicalWorkflowQueryService.listPendingTasks(query);
    }

    public TechnicalWorkflowModels.EmbeddingWorkstationSummary getEmbeddingWorkstationSummary(java.time.LocalDate workDate) {
        return technicalWorkflowQueryService.getEmbeddingWorkstationSummary(workDate);
    }

    public TechnicalWorkflowModels.SlicingWorkbenchView getSlicingWorkbench(TechnicalWorkflowModels.SlicingWorkbenchQuery query) {
        return technicalWorkflowQueryService.getSlicingWorkbench(query);
    }

    public TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage listPendingTechnicalSpecimenRegistrations(
        TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery query) {
        return technicalSpecimenRegistrationService.listPendingRegistrations(query);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail getTechnicalSpecimenRegistrationDetail(String caseId) {
        return technicalSpecimenRegistrationService.getRegistrationDetail(caseId);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace getTechnicalSpecimenRegistrationWorkspace(String caseId) {
        return technicalSpecimenRegistrationService.getRegistrationWorkspace(caseId);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace saveTechnicalSpecimenRegistrationMaterials(
        TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationMaterialsCommand command) {
        return technicalSpecimenRegistrationService.saveRegistrationMaterials(command);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationMediaAsset uploadTechnicalSpecimenRegistrationMediaAsset(
        TechnicalWorkflowModels.UploadTechnicalSpecimenRegistrationMediaAssetCommand command) {
        return technicalSpecimenRegistrationService.uploadMediaAsset(command);
    }

    public void deleteTechnicalSpecimenRegistrationMediaAsset(
        TechnicalWorkflowModels.DeleteTechnicalSpecimenRegistrationMediaAssetCommand command) {
        technicalSpecimenRegistrationService.deleteMediaAsset(command);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationCompleteResult completeTechnicalSpecimenRegistration(
        TechnicalWorkflowModels.CompleteTechnicalSpecimenRegistrationCommand command) {
        return technicalSpecimenRegistrationService.completeRegistration(command);
    }

    public TechnicalWorkflowModels.TaskView assignTechnicalTask(TechnicalWorkflowModels.TechnicalTaskAssignCommand command) {
        return technicalTaskManagementService.assignTechnicalTask(command);
    }

    public TechnicalWorkflowModels.TaskView claimTechnicalTask(TechnicalWorkflowModels.TechnicalTaskClaimCommand command) {
        return technicalTaskManagementService.claimTechnicalTask(command);
    }

    public TechnicalWorkflowModels.TaskView releaseTechnicalTask(TechnicalWorkflowModels.TechnicalTaskReleaseCommand command) {
        return technicalTaskManagementService.releaseTechnicalTask(command);
    }

    public TechnicalWorkflowModels.TaskView updateTechnicalTaskPriority(TechnicalWorkflowModels.TechnicalTaskPriorityCommand command) {
        return technicalTaskManagementService.updateTechnicalTaskPriority(command);
    }

    public TechnicalWorkflowModels.TaskStartResult startGrossing(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalGrossingWorkflowService.startGrossing(command);
    }

    public TechnicalWorkflowModels.GrossingWorkbenchContext getGrossingWorkbenchContext(String taskId) {
        return technicalGrossingWorkflowService.getGrossingWorkbenchContext(taskId);
    }

    public TechnicalWorkflowModels.GrossingResult completeGrossing(TechnicalWorkflowModels.GrossingCompleteCommand command) {
        return technicalGrossingWorkflowService.completeGrossing(command);
    }

    public TechnicalWorkflowModels.DehydrationBatchResult createDehydrationBatch(TechnicalWorkflowModels.CreateDehydrationBatchCommand command) {
        return technicalGrossingWorkflowService.createDehydrationBatch(command);
    }

    public TechnicalWorkflowModels.DehydrationBatchResult startDehydrationBatch(TechnicalWorkflowModels.BatchOperatorCommand command) {
        return technicalGrossingWorkflowService.startDehydrationBatch(command);
    }

    public TechnicalWorkflowModels.DehydrationBatchResult completeDehydrationBatch(
        TechnicalWorkflowModels.CompleteDehydrationBatchCommand command) {
        return technicalGrossingWorkflowService.completeDehydrationBatch(command);
    }

    public TechnicalWorkflowModels.TaskStartResult startEmbedding(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalProcessingWorkflowService.startEmbedding(command);
    }

    public TechnicalWorkflowModels.EmbeddingResult completeEmbedding(TechnicalWorkflowModels.EmbeddingCompleteCommand command) {
        return technicalProcessingWorkflowService.completeEmbedding(command);
    }

    public TechnicalWorkflowModels.TaskStartResult startSlicing(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalProcessingWorkflowService.startSlicing(command);
    }

    public TechnicalWorkflowModels.SlicingResult completeSlicing(TechnicalWorkflowModels.SlicingCompleteCommand command) {
        return technicalProcessingWorkflowService.completeSlicing(command);
    }

    public TechnicalWorkflowModels.SlideQcEvaluationResult createSlideQcEvaluation(
        TechnicalWorkflowModels.CreateSlideQcEvaluationCommand command) {
        return technicalProcessingWorkflowService.createSlideQcEvaluation(command);
    }

    public TechnicalWorkflowModels.TaskStartResult startSlideStaining(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalProcessingWorkflowService.startSlideStaining(command);
    }

    public TechnicalWorkflowModels.SlideStainingResult completeSlideStaining(
        TechnicalWorkflowModels.SlideStainingCompleteCommand command) {
        return technicalProcessingWorkflowService.completeSlideStaining(command);
    }

    public TechnicalWorkflowModels.ReworkOrderResult createReworkOrder(TechnicalWorkflowModels.CreateReworkOrderCommand command) {
        return technicalReworkWorkflowService.createReworkOrder(command);
    }

    public TechnicalWorkflowModels.ReworkOrderResult executeReworkOrder(TechnicalWorkflowModels.ExecuteReworkOrderCommand command) {
        return technicalReworkWorkflowService.executeReworkOrder(command);
    }

    public TechnicalWorkflowModels.TechnicalTrackingView getTechnicalTracking(String caseId) {
        return technicalWorkflowQueryService.getTechnicalTracking(caseId);
    }
}
