package com.company.bl.application.service;

import java.time.LocalDate;
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
        String patientId,
        String patientIdDisplay,
        String caseId,
        String pathologyNo,
        String applicationType,
        String checkItem,
        Integer blockCount,
        String submittingDepartmentName,
        String specimenName,
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

    public record FormalReportVersionView(
        String versionId,
        String reportId,
        String reportNo,
        Integer versionNo,
        String versionStatus,
        String signedByName,
        String signedAt,
        String publishedAt,
        String printStatus,
        String printedAt,
        String deliveryStatus,
        String plannedIssueAt,
        String issuedAt,
        String recalledAt
    ) {
    }

    public record CaseReportVersionView(
        String versionId,
        String reportId,
        String reportNo,
        Integer versionNo,
        String versionStatus,
        String signedByName,
        String submittedAt,
        String reviewedAt,
        String signedAt,
        String publishedAt,
        String printStatus,
        String printedAt,
        String deliveryStatus,
        String plannedIssueAt,
        String issuedAt,
        String recalledAt
    ) {
    }

    public record FormalReportVersionBatchActionCommand(
        List<String> versionIds,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String issueMode,
        String plannedIssueAt,
        String remarks
    ) {
    }

    public record FormalReportVersionBatchActionItemResult(
        String versionId,
        boolean success,
        String message
    ) {
    }

    public record FormalReportVersionBatchActionResult(
        int totalCount,
        int successCount,
        int failureCount,
        List<FormalReportVersionBatchActionItemResult> items
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
        String status,
        String orderCategoryCode,
        LocalDate dateFrom,
        LocalDate dateTo,
        LocalDate workDate
    ) {
    }

    public record PendingMedicalOrderPage(List<DiagnosticReportViews.MedicalOrderView> items, int page, int size, long total) {
    }

    public record CreateMedicalOrderCommand(
        String caseId,
        String orderType,
        String orderContent,
        String orderItemId,
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

    public record MedicalOrderBillingCommand(
        String caseId,
        List<String> orderIds,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record MedicalOrderBillingResult(
        int totalCount,
        int successCount,
        int failureCount,
        List<MedicalOrderBillingItemResult> items
    ) {
    }

    public record MedicalOrderBillingItemResult(
        String orderId,
        String billingStatus,
        String billingRecordId,
        String message
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
