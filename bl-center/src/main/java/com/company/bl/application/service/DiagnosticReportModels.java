package com.company.bl.application.service;

import java.util.List;

public final class DiagnosticReportModels {

    private DiagnosticReportModels() {
    }

    public record PendingDiagnosticTaskQuery(
        int page,
        int size,
        String taskType,
        String taskStatus,
        String pathologyNo
    ) {
    }

    public record PendingDiagnosticTaskPage(List<TaskView> items, int page, int size, long total) {
    }

    public record TaskView(
        String id,
        String applicationId,
        String applicationNo,
        String patientName,
        String caseId,
        String pathologyNo,
        String taskType,
        String taskStatus,
        String diagnosisDoctorUserId,
        String diagnosisDoctorName,
        String primaryDoctorUserId,
        String primaryDoctorName,
        String reviewerUserId,
        String reviewerName,
        String assignedAt,
        String acceptedAt,
        String completedAt,
        String remarks
    ) {
    }

    public record DiagnosticTaskResult(String taskId, String caseId, String caseStatus, String taskStatus) {
    }

    public record AssignDiagnosticTaskCommand(
        String taskId,
        String diagnosisDoctorUserId,
        String diagnosisDoctorName,
        String primaryDoctorUserId,
        String primaryDoctorName,
        String reviewerUserId,
        String reviewerName,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record TaskActionCommand(
        String taskId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record CreatePathologyReportCommand(
        String caseId,
        String taskId,
        String clinicalDiagnosis,
        String grossExam,
        String microscopicExam,
        String finalDiagnosis,
        String richTextContent,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record UpdateReportDraftCommand(
        String reportId,
        String clinicalDiagnosis,
        String grossExam,
        String microscopicExam,
        String finalDiagnosis,
        String richTextContent,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ReportActionCommand(
        String reportId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record RejectReportCommand(
        String reportId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String rejectReason
    ) {
    }

    public record PathologyReportResult(
        String reportId,
        String caseId,
        String reportNo,
        String reportStatus,
        Integer versionNo,
        String versionStatus
    ) {
    }

    public record DiagnosticWorkbenchView(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        String submittingDepartmentName,
        String submittingDoctorName,
        String clinicalDiagnosis,
        List<WorkbenchSpecimenSummary> specimens,
        List<WorkbenchBlockSummary> blocks,
        List<WorkbenchSlideSummary> slides,
        List<TaskView> diagnosticTasks,
        PathologyReportView currentReport,
        List<TrackingEventView> recentEvents
    ) {
    }

    public record WorkbenchSpecimenSummary(
        String specimenId,
        String specimenNo,
        String barcode,
        String specimenName,
        String specimenStatus
    ) {
    }

    public record WorkbenchBlockSummary(
        String blockId,
        String specimenId,
        String blockCode,
        String embeddingBoxNo,
        String description
    ) {
    }

    public record WorkbenchSlideSummary(
        String slideId,
        String specimenId,
        String embeddingBoxId,
        String slideNo,
        String slideStatus,
        String qualityStatus
    ) {
    }

    public record PathologyReportView(
        String reportId,
        String reportNo,
        String reportStatus,
        String clinicalDiagnosis,
        String grossExam,
        String microscopicExam,
        String finalDiagnosis,
        String richTextContent,
        String submittedAt,
        String reviewedAt,
        String signedAt,
        String publishedAt,
        String reviewerName,
        String signedByName,
        int versionNo
    ) {
    }

    public record TrackingEventView(
        String nodeCode,
        String eventType,
        String eventStatus,
        String eventTime,
        String operatorName,
        String eventContent
    ) {
    }

    public record ReportTrackingView(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        List<TaskView> diagnosticTasks,
        PathologyReportView currentReport,
        List<ReportVersionView> versions,
        List<TrackingEventView> events
    ) {
    }

    public record ReportVersionView(
        String versionId,
        int versionNo,
        String versionStatus,
        String finalDiagnosisSnapshot,
        String signedAt,
        String createdAt
    ) {
    }
}
