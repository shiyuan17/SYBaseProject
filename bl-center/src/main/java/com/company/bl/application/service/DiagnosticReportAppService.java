package com.company.bl.application.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiagnosticReportAppService {

    private final DiagnosticReportQueryService diagnosticReportQueryService;
    private final DiagnosticTaskWorkflowService diagnosticTaskWorkflowService;
    private final DiagnosticReportLifecycleService diagnosticReportLifecycleService;
    private final ReportRevisionWorkflowService reportRevisionWorkflowService;
    private final MedicalOrderWorkflowService medicalOrderWorkflowService;
    private final InternalConsultationWorkflowService internalConsultationWorkflowService;

    public DiagnosticReportAppService(DiagnosticReportQueryService diagnosticReportQueryService,
                                      DiagnosticTaskWorkflowService diagnosticTaskWorkflowService,
                                      DiagnosticReportLifecycleService diagnosticReportLifecycleService,
                                      ReportRevisionWorkflowService reportRevisionWorkflowService,
                                      MedicalOrderWorkflowService medicalOrderWorkflowService,
                                      InternalConsultationWorkflowService internalConsultationWorkflowService) {
        this.diagnosticReportQueryService = diagnosticReportQueryService;
        this.diagnosticTaskWorkflowService = diagnosticTaskWorkflowService;
        this.diagnosticReportLifecycleService = diagnosticReportLifecycleService;
        this.reportRevisionWorkflowService = reportRevisionWorkflowService;
        this.medicalOrderWorkflowService = medicalOrderWorkflowService;
        this.internalConsultationWorkflowService = internalConsultationWorkflowService;
    }

    public DiagnosticReportModels.PendingDiagnosticTaskPage listPendingTasks(DiagnosticReportModels.PendingDiagnosticTaskQuery query) {
        return diagnosticReportQueryService.listPendingTasks(query);
    }

    public void createPrimaryDiagnosticTaskIfAbsent(String caseId, String remarks) {
        diagnosticTaskWorkflowService.createPrimaryDiagnosticTaskIfAbsent(caseId, remarks);
    }

    public void createFrozenDiagnosticTaskIfAbsent(String caseId, String remarks) {
        diagnosticTaskWorkflowService.createFrozenDiagnosticTaskIfAbsent(caseId, remarks);
    }

    public DiagnosticReportModels.DiagnosticTaskResult assignTask(DiagnosticReportModels.AssignDiagnosticTaskCommand command) {
        return diagnosticTaskWorkflowService.assignTask(command);
    }

    public DiagnosticReportModels.DiagnosticTaskResult acceptTask(DiagnosticReportModels.TaskActionCommand command) {
        return diagnosticTaskWorkflowService.acceptTask(command);
    }

    public DiagnosticReportModels.DiagnosticTaskResult startTask(DiagnosticReportModels.TaskActionCommand command) {
        return diagnosticTaskWorkflowService.startTask(command);
    }

    public DiagnosticReportViews.DiagnosticWorkbenchView getDiagnosticWorkbench(String caseId) {
        return diagnosticReportQueryService.getDiagnosticWorkbench(caseId);
    }

    public DiagnosticReportModels.PathologyReportResult createReport(DiagnosticReportModels.CreatePathologyReportCommand command) {
        return diagnosticReportLifecycleService.createReport(command);
    }

    public DiagnosticReportModels.PathologyReportResult saveDraft(DiagnosticReportModels.UpdateReportDraftCommand command) {
        return diagnosticReportLifecycleService.saveDraft(command);
    }

    public DiagnosticReportModels.PathologyReportResult submitReport(DiagnosticReportModels.ReportActionCommand command) {
        return diagnosticReportLifecycleService.submitReport(command);
    }

    public DiagnosticReportModels.PathologyReportResult reviewReport(DiagnosticReportModels.ReportActionCommand command) {
        return diagnosticReportLifecycleService.reviewReport(command);
    }

    public DiagnosticReportModels.PathologyReportResult rejectReport(DiagnosticReportModels.RejectReportCommand command) {
        return diagnosticReportLifecycleService.rejectReport(command);
    }

    public DiagnosticReportModels.PathologyReportResult signReport(DiagnosticReportModels.ReportActionCommand command) {
        return diagnosticReportLifecycleService.signReport(command);
    }

    public DiagnosticReportModels.PathologyReportResult publishReport(DiagnosticReportModels.ReportActionCommand command) {
        return diagnosticReportLifecycleService.publishReport(command);
    }

    public DiagnosticReportViews.ReportTrackingView getReportTracking(String caseId) {
        return diagnosticReportQueryService.getReportTracking(caseId);
    }

    public DiagnosticReportViews.CaseLifecycleTrackingView getCaseLifecycleTracking(String caseId) {
        return diagnosticReportQueryService.getCaseLifecycleTracking(caseId);
    }

    public List<DiagnosticReportModels.FormalReportVersionView> listFormalReportVersions(String caseId) {
        return diagnosticReportQueryService.listFormalReportVersions(caseId);
    }

    public List<DiagnosticReportModels.CaseReportVersionView> listCaseReportVersions(String caseId) {
        return diagnosticReportQueryService.listCaseReportVersions(caseId);
    }

    public DiagnosticReportModels.FormalReportVersionBatchActionResult printFormalReportVersions(
        DiagnosticReportModels.FormalReportVersionBatchActionCommand command
    ) {
        return diagnosticReportLifecycleService.printFormalReportVersions(command);
    }

    public DiagnosticReportModels.FormalReportVersionBatchActionResult issueFormalReportVersions(
        DiagnosticReportModels.FormalReportVersionBatchActionCommand command
    ) {
        return diagnosticReportLifecycleService.issueFormalReportVersions(command);
    }

    public DiagnosticReportModels.FormalReportVersionBatchActionResult recallFormalReportVersions(
        DiagnosticReportModels.FormalReportVersionBatchActionCommand command
    ) {
        return diagnosticReportLifecycleService.recallFormalReportVersions(command);
    }

    public DiagnosticReportModels.ReportRevisionResult createRevisionRequest(DiagnosticReportModels.CreateReportRevisionRequestCommand command) {
        return reportRevisionWorkflowService.createRevisionRequest(command);
    }

    public DiagnosticReportModels.ReportRevisionResult approveRevisionRequest(DiagnosticReportModels.ReviewReportRevisionCommand command) {
        return reportRevisionWorkflowService.approveRevisionRequest(command);
    }

    public DiagnosticReportModels.ReportRevisionResult rejectRevisionRequest(DiagnosticReportModels.ReviewReportRevisionCommand command) {
        return reportRevisionWorkflowService.rejectRevisionRequest(command);
    }

    public DiagnosticReportModels.PendingMedicalOrderPage listPendingMedicalOrders(DiagnosticReportModels.PendingMedicalOrderQuery query) {
        return medicalOrderWorkflowService.listPendingMedicalOrders(query);
    }

    public DiagnosticReportModels.MedicalOrderResult createMedicalOrder(DiagnosticReportModels.CreateMedicalOrderCommand command) {
        return medicalOrderWorkflowService.createMedicalOrder(command);
    }

    public DiagnosticReportModels.MedicalOrderBlockResult createMedicalOrderBlock(
        DiagnosticReportModels.CreateMedicalOrderBlockCommand command
    ) {
        return medicalOrderWorkflowService.createMedicalOrderBlock(command);
    }

    public DiagnosticReportModels.MedicalOrderTargetSnapshotResult changeMedicalOrderBlock(
        DiagnosticReportModels.ChangeMedicalOrderBlockCommand command
    ) {
        return medicalOrderWorkflowService.changeMedicalOrderBlock(command);
    }

    public DiagnosticReportModels.MedicalOrderResult acceptMedicalOrder(DiagnosticReportModels.MedicalOrderActionCommand command) {
        return medicalOrderWorkflowService.acceptMedicalOrder(command);
    }

    public DiagnosticReportModels.MedicalOrderSlidePrintResult printMedicalOrderSlide(DiagnosticReportModels.MedicalOrderActionCommand command) {
        return medicalOrderWorkflowService.printMedicalOrderSlide(command);
    }

    public DiagnosticReportModels.MedicalOrderResult completeMedicalOrder(DiagnosticReportModels.MedicalOrderActionCommand command) {
        return medicalOrderWorkflowService.completeMedicalOrder(command);
    }

    public DiagnosticReportModels.MedicalOrderResult terminateMedicalOrder(DiagnosticReportModels.TerminateMedicalOrderCommand command) {
        return medicalOrderWorkflowService.terminateMedicalOrder(command);
    }

    public DiagnosticReportModels.MedicalOrderQcEvaluationResult createMedicalOrderQcEvaluation(
        DiagnosticReportModels.MedicalOrderQcEvaluationCommand command
    ) {
        return medicalOrderWorkflowService.createMedicalOrderQcEvaluation(command);
    }

    public DiagnosticReportModels.MedicalOrderQcEvaluationResult getLatestMedicalOrderQcEvaluation(String orderId) {
        return medicalOrderWorkflowService.getLatestMedicalOrderQcEvaluation(orderId);
    }

    public DiagnosticReportModels.MedicalOrderResult cancelMedicalOrder(DiagnosticReportModels.MedicalOrderActionCommand command) {
        return medicalOrderWorkflowService.cancelMedicalOrder(command);
    }

    public DiagnosticReportModels.MedicalOrderBillingResult executeMedicalOrderBilling(DiagnosticReportModels.MedicalOrderBillingCommand command) {
        return medicalOrderWorkflowService.executeMedicalOrderBilling(command);
    }

    public DiagnosticReportModels.MedicalOrderBillingResult confirmMedicalOrderBilling(DiagnosticReportModels.MedicalOrderBillingCommand command) {
        return medicalOrderWorkflowService.confirmMedicalOrderBilling(command);
    }

    public DiagnosticReportModels.ConsultationResult createConsultation(DiagnosticReportModels.CreateConsultationCommand command) {
        return internalConsultationWorkflowService.createConsultation(command);
    }

    public DiagnosticReportModels.ConsultationResult commentConsultationParticipant(DiagnosticReportModels.CommentConsultationParticipantCommand command) {
        return internalConsultationWorkflowService.commentConsultationParticipant(command);
    }

    public DiagnosticReportModels.ConsultationResult completeConsultation(DiagnosticReportModels.CompleteConsultationCommand command) {
        return internalConsultationWorkflowService.completeConsultation(command);
    }
}
