package com.company.bl.application.service;

import org.springframework.stereotype.Service;

@Service
public class DiagnosticReportAppService {

    private final DiagnosticReportQueryService diagnosticReportQueryService;
    private final DiagnosticTaskWorkflowService diagnosticTaskWorkflowService;
    private final DiagnosticReportLifecycleService diagnosticReportLifecycleService;

    public DiagnosticReportAppService(DiagnosticReportQueryService diagnosticReportQueryService,
                                      DiagnosticTaskWorkflowService diagnosticTaskWorkflowService,
                                      DiagnosticReportLifecycleService diagnosticReportLifecycleService) {
        this.diagnosticReportQueryService = diagnosticReportQueryService;
        this.diagnosticTaskWorkflowService = diagnosticTaskWorkflowService;
        this.diagnosticReportLifecycleService = diagnosticReportLifecycleService;
    }

    public DiagnosticReportModels.PendingDiagnosticTaskPage listPendingTasks(DiagnosticReportModels.PendingDiagnosticTaskQuery query) {
        return diagnosticReportQueryService.listPendingTasks(query);
    }

    public void createPrimaryDiagnosticTaskIfAbsent(String caseId, String remarks) {
        diagnosticTaskWorkflowService.createPrimaryDiagnosticTaskIfAbsent(caseId, remarks);
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

    public DiagnosticReportModels.DiagnosticWorkbenchView getDiagnosticWorkbench(String caseId) {
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

    public DiagnosticReportModels.ReportTrackingView getReportTracking(String caseId) {
        return diagnosticReportQueryService.getReportTracking(caseId);
    }
}
