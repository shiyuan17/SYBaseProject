package com.company.bl.application.service;

import org.springframework.stereotype.Service;

@Service
public class TechnicalWorkflowAppService {

    private final TechnicalWorkflowQueryService technicalWorkflowQueryService;
    private final TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService;
    private final FrozenWorkflowService frozenWorkflowService;
    private final TechnicalTaskManagementService technicalTaskManagementService;
    private final TechnicalGrossingWorkflowService technicalGrossingWorkflowService;
    private final TechnicalProcessingWorkflowService technicalProcessingWorkflowService;
    private final TechnicalReworkWorkflowService technicalReworkWorkflowService;

    public TechnicalWorkflowAppService(TechnicalWorkflowQueryService technicalWorkflowQueryService,
                                       TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService,
                                       FrozenWorkflowService frozenWorkflowService,
                                       TechnicalTaskManagementService technicalTaskManagementService,
                                       TechnicalGrossingWorkflowService technicalGrossingWorkflowService,
                                       TechnicalProcessingWorkflowService technicalProcessingWorkflowService,
                                       TechnicalReworkWorkflowService technicalReworkWorkflowService) {
        this.technicalWorkflowQueryService = technicalWorkflowQueryService;
        this.technicalSpecimenRegistrationService = technicalSpecimenRegistrationService;
        this.frozenWorkflowService = frozenWorkflowService;
        this.technicalTaskManagementService = technicalTaskManagementService;
        this.technicalGrossingWorkflowService = technicalGrossingWorkflowService;
        this.technicalProcessingWorkflowService = technicalProcessingWorkflowService;
        this.technicalReworkWorkflowService = technicalReworkWorkflowService;
    }

    public FrozenWorkflowModels.FrozenTechnicalWorkbenchView getFrozenTechnicalWorkbench() {
        return frozenWorkflowService.getWorkbench();
    }

    public FrozenWorkflowModels.FrozenReminderSummary getFrozenReminderSummary() {
        return frozenWorkflowService.getReminderSummary();
    }

    public FrozenWorkflowModels.FrozenSessionListPage listFrozenSessions(
        FrozenWorkflowModels.FrozenSessionListQuery query
    ) {
        return frozenWorkflowService.listSessions(query);
    }

    public FrozenWorkflowModels.FrozenSessionDetail getFrozenSessionDetail(String sessionId) {
        return frozenWorkflowService.getSessionDetail(sessionId);
    }

    public FrozenWorkflowModels.FrozenTaskActionResult completeFrozenReceive(
        FrozenWorkflowModels.FrozenActionCommand command
    ) {
        return frozenWorkflowService.completeReceive(command);
    }

    public FrozenWorkflowModels.FrozenTaskActionResult completeFrozenGrossing(
        FrozenWorkflowModels.FrozenActionCommand command
    ) {
        return frozenWorkflowService.completeGrossing(command);
    }

    public FrozenWorkflowModels.FrozenTaskActionResult completeFrozenSlicing(
        FrozenWorkflowModels.FrozenActionCommand command
    ) {
        return frozenWorkflowService.completeSlicing(command);
    }

    public FrozenWorkflowModels.FrozenTaskActionResult saveFrozenPreliminaryReport(
        FrozenWorkflowModels.FrozenPhoneBackCommand command
    ) {
        return frozenWorkflowService.savePreliminaryReport(command);
    }

    public FrozenWorkflowModels.FrozenTaskActionResult completeFrozenPhoneBack(
        FrozenWorkflowModels.FrozenPhoneBackCommand command
    ) {
        return frozenWorkflowService.completePhoneBack(command);
    }

    public FrozenWorkflowModels.FrozenTaskActionResult confirmFrozenReport(
        FrozenWorkflowModels.FrozenActionCommand command
    ) {
        return frozenWorkflowService.confirmReport(command);
    }

    public FrozenWorkflowModels.FrozenTaskActionResult completeFrozenParaffinCompare(
        FrozenWorkflowModels.FrozenParaffinCompareCommand command
    ) {
        return frozenWorkflowService.completeParaffinCompare(command);
    }

    public FrozenWorkflowModels.FrozenTaskActionResult completeFrozenRemainingTissue(
        FrozenWorkflowModels.FrozenRemainingTissueCommand command
    ) {
        return frozenWorkflowService.completeRemainingTissue(command);
    }

    public TechnicalWorkflowModels.PendingTechnicalTaskPage listPendingTasks(TechnicalWorkflowModels.PendingTechnicalTaskQuery query) {
        return technicalWorkflowQueryService.listPendingTasks(query);
    }

    public TechnicalWorkflowModels.EmbeddingWorkstationSummary getEmbeddingWorkstationSummary(
        java.time.LocalDate dateFrom,
        java.time.LocalDate dateTo,
        java.time.LocalDate workDate
    ) {
        return technicalWorkflowQueryService.getEmbeddingWorkstationSummary(dateFrom, dateTo, workDate);
    }

    public TechnicalWorkflowModels.SlicingWorkbenchView getSlicingWorkbench(TechnicalWorkflowModels.SlicingWorkbenchQuery query) {
        return technicalWorkflowQueryService.getSlicingWorkbench(query);
    }

    public TechnicalWorkflowModels.TechnicalTrackingCaseListPage listTechnicalTrackingCases(
        TechnicalWorkflowModels.TechnicalTrackingCaseListQuery query
    ) {
        return technicalWorkflowQueryService.listTechnicalTrackingCases(query);
    }

    public TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage listPendingTechnicalSpecimenRegistrations(
        TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery query) {
        return technicalSpecimenRegistrationService.listPendingRegistrations(query);
    }

    public TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage listTechnicalSpecimenRegistrations(
        TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery query) {
        return technicalSpecimenRegistrationService.listRegistrations(query);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail getTechnicalSpecimenRegistrationDetail(String caseId) {
        return technicalSpecimenRegistrationService.getRegistrationDetail(caseId);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace getTechnicalSpecimenRegistrationWorkspace(String caseId) {
        return technicalSpecimenRegistrationService.getRegistrationWorkspace(caseId);
    }

    public ApplicationRegistrationWorkbenchAppService.WorkbenchRecord getTechnicalSpecimenRegistrationApplicationWorkbench(
        String caseId
    ) {
        return technicalSpecimenRegistrationService.getApplicationWorkbench(caseId);
    }

    public ApplicationRegistrationWorkbenchAppService.WorkbenchRecord saveTechnicalSpecimenRegistrationApplicationWorkbenchPatientInfo(
        String caseId,
        ApplicationRegistrationWorkbenchAppService.SavePatientInfoCommand command
    ) {
        return technicalSpecimenRegistrationService.saveApplicationWorkbenchPatientInfo(caseId, command);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace saveTechnicalSpecimenRegistrationMaterials(
        TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationMaterialsCommand command) {
        return technicalSpecimenRegistrationService.saveRegistrationMaterials(command);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace verifyTechnicalSpecimenRegistrationMaterial(
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialVerificationCommand command) {
        return technicalSpecimenRegistrationService.verifyRegistrationMaterial(command);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace cancelTechnicalSpecimenRegistrationMaterialVerification(
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationMaterialVerificationCommand command) {
        return technicalSpecimenRegistrationService.cancelRegistrationMaterialVerification(command);
    }

    public TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace saveTechnicalSpecimenRegistrationDetailSections(
        TechnicalWorkflowModels.SaveTechnicalSpecimenRegistrationDetailSectionsCommand command) {
        return technicalSpecimenRegistrationService.saveRegistrationDetailSections(command);
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

    public TechnicalWorkflowModels.TaskView updateTechnicalTaskRemarks(TechnicalWorkflowModels.TechnicalTaskRemarksCommand command) {
        return technicalTaskManagementService.updateTechnicalTaskRemarks(command);
    }

    public TechnicalWorkflowModels.TaskStartResult startGrossing(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalGrossingWorkflowService.startGrossing(command);
    }

    public TechnicalWorkflowModels.GrossingWorkbenchContext getGrossingWorkbenchContext(String taskId) {
        return technicalGrossingWorkflowService.getGrossingWorkbenchContext(taskId);
    }

    public TechnicalWorkflowModels.GrossingDraft saveGrossingDraft(TechnicalWorkflowModels.GrossingDraftCommand command) {
        return technicalGrossingWorkflowService.saveGrossingDraft(command);
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

    public TechnicalWorkflowModels.TaskStartResult startDehydration(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalGrossingWorkflowService.startDehydration(command);
    }

    public TechnicalWorkflowModels.TaskStartResult completeDehydration(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalGrossingWorkflowService.completeDehydration(command);
    }

    public TechnicalWorkflowModels.TaskStartResult startEmbedding(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalProcessingWorkflowService.startEmbedding(command);
    }

    public TechnicalWorkflowModels.EmbeddingResult completeEmbedding(TechnicalWorkflowModels.EmbeddingCompleteCommand command) {
        return technicalProcessingWorkflowService.completeEmbedding(command);
    }

    public TechnicalWorkflowModels.TaskStartResult cancelEmbedding(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalProcessingWorkflowService.cancelEmbedding(command);
    }

    public TechnicalWorkflowModels.WorkstationDailyClearView confirmEmbeddingWorkstationClear(
        TechnicalWorkflowModels.WorkstationClearCommand command
    ) {
        return technicalProcessingWorkflowService.confirmEmbeddingWorkstationClear(command);
    }

    public TechnicalWorkflowModels.EmbeddingQualityReviewResult updateEmbeddingQualityReview(
        TechnicalWorkflowModels.EmbeddingQualityReviewCommand command
    ) {
        return technicalProcessingWorkflowService.updateEmbeddingQualityReview(command);
    }

    public TechnicalWorkflowModels.TaskStartResult startSlicing(TechnicalWorkflowModels.TaskStartCommand command) {
        return technicalProcessingWorkflowService.startSlicing(command);
    }

    public TechnicalWorkflowModels.SlicingSlidePrintResult printSlicingSlides(
        TechnicalWorkflowModels.SlicingSlidePrintCommand command) {
        return technicalProcessingWorkflowService.printSlicingSlides(command);
    }

    public TechnicalWorkflowModels.SlicingSlidePrintMergeGroupResult createSlicingSlidePrintMergeGroups(
        TechnicalWorkflowModels.SlicingSlidePrintMergeGroupCommand command) {
        return technicalProcessingWorkflowService.createSlicingSlidePrintMergeGroups(command);
    }

    public TechnicalWorkflowModels.SlicingSlidePrintMergeGroupResult cancelSlicingSlidePrintMergeGroups(
        TechnicalWorkflowModels.SlicingSlidePrintMergeGroupCancelCommand command) {
        return technicalProcessingWorkflowService.cancelSlicingSlidePrintMergeGroups(command);
    }

    public TechnicalWorkflowModels.SlicingSlidePrintResult printSlicingSlideMergeGroup(
        TechnicalWorkflowModels.SlicingSlidePrintMergeGroupPrintCommand command) {
        return technicalProcessingWorkflowService.printSlicingSlideMergeGroup(command);
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

    public TechnicalWorkflowModels.TechnicalTrackingView getTechnicalTracking(
        String caseId,
        java.time.LocalDate dateFrom,
        java.time.LocalDate dateTo,
        java.time.LocalDate workDate
    ) {
        return technicalWorkflowQueryService.getTechnicalTracking(caseId, dateFrom, dateTo, workDate);
    }
}
