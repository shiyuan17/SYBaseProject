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
        String pathologyNo,
        String currentUserId,
        String currentRoleCode
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

    public record CreateReportRevisionRequestCommand(
        String reportId,
        String requestReason,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ReviewReportRevisionCommand(
        String requestId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        String rejectReason
    ) {
    }

    public record ReportRevisionResult(
        String requestId,
        String caseId,
        String reportId,
        String requestStatus,
        Integer approvedVersionNo
    ) {
    }

    public record PendingMedicalOrderQuery(
        int page,
        int size,
        String pathologyNo,
        String status
    ) {
    }

    public record PendingMedicalOrderPage(List<DiagnosticReportViews.MedicalOrderView> items, int page, int size, long total) {
    }

    public record CreateMedicalOrderCommand(
        String caseId,
        String orderType,
        String orderContent,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record MedicalOrderActionCommand(
        String orderId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record MedicalOrderResult(
        String orderId,
        String caseId,
        String orderNumber,
        String status
    ) {
    }

    public record ConsultationParticipantInput(
        String participantUserId,
        String participantName,
        String participantRole
    ) {
    }

    public record CreateConsultationCommand(
        String caseId,
        List<ConsultationParticipantInput> participants,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record CommentConsultationParticipantCommand(
        String consultationId,
        String participantId,
        String opinion,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record CompleteConsultationCommand(
        String consultationId,
        String opinion,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ConsultationResult(
        String consultationId,
        String caseId,
        String status
    ) {
    }

}
