package com.company.bl.application.service;

import java.util.List;

public final class DiagnosticReportViews {

    private DiagnosticReportViews() {
    }

    public record MedicalOrderView(
        String orderId,
        String caseId,
        String pathologyNo,
        String applicationNo,
        String patientName,
        String orderNumber,
        String orderType,
        String orderContent,
        String executionScope,
        String billingStatus,
        String status,
        String doctorName,
        String executorName,
        String orderDate,
        String acceptedAt,
        String completedAt,
        String cancelledAt,
        String remarks
    ) {
    }

    public record DiagnosticWorkbenchView(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        String patientId,
        String patientGender,
        String patientAge,
        String applicationType,
        String inpatientNo,
        String outpatientNo,
        String bedNo,
        String phone,
        String submittingDepartmentName,
        String submittingDoctorName,
        String clinicalDiagnosis,
        String applicationRemarks,
        String applicationFormArchiveStatus,
        String applicationFormArchiveLocation,
        String applicationFormImageUrl,
        List<WorkbenchSpecimenSummary> specimens,
        List<WorkbenchBlockSummary> blocks,
        List<WorkbenchSlideSummary> slides,
        List<DiagnosticReportModels.TaskView> diagnosticTasks,
        PathologyReportView currentReport,
        List<TrackingEventView> recentEvents,
        List<RevisionRequestView> revisions,
        List<MedicalOrderView> medicalOrders,
        List<ConsultationView> consultations,
        List<HistoricalPathologyView> historicalPathologies,
        List<PacsExaminationView> pacsExaminations,
        List<ReportTraceView> reportTraces,
        List<RemarkSectionView> remarkSections,
        List<ChargeItemView> chargeItems,
        boolean hasPendingRevision
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
        String description,
        String archiveStatus,
        String archiveLocation,
        String loanStatus
    ) {
    }

    public record WorkbenchSlideSummary(
        String slideId,
        String specimenId,
        String embeddingBoxId,
        String slideNo,
        String slideStatus,
        String qualityStatus,
        String archiveStatus,
        String archiveLocation,
        String loanStatus
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

    public record RevisionRequestView(
        String requestId,
        String reportId,
        int currentVersionNo,
        String requestStatus,
        String requestReason,
        String requestedByName,
        String requestedAt,
        String reviewedByName,
        String reviewedAt,
        String rejectReason,
        Integer approvedVersionNo
    ) {
    }

    public record ConsultationView(
        String consultationId,
        String consultationType,
        String status,
        String requestedByName,
        String requestedAt,
        String hostName,
        String completedAt,
        String opinion,
        int participantCount
    ) {
    }

    public record HistoricalPathologyView(
        String age,
        String inpatientNo,
        String examinationNo,
        String submissionType,
        String reportTime,
        String diagnosis
    ) {
    }

    public record PacsExaminationView(
        String submissionType,
        String imagingDiagnosis,
        String reportTime,
        String examinationNo,
        String imagingDescription,
        String reportStatus
    ) {
    }

    public record ReportTraceView(
        int sequenceNo,
        String reportDoctorName,
        String reportTime,
        String reportStatus,
        String diagnosisInfo
    ) {
    }

    public record RemarkSectionView(
        String sectionKey,
        String title,
        String relatedNo,
        String content
    ) {
    }

    public record ChargeItemView(
        String itemName,
        String chargedAt,
        String chargedByName
    ) {
    }

    public record ReportTrackingView(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        String applicationFormArchiveStatus,
        String applicationFormArchiveLocation,
        String applicationFormImageUrl,
        List<DiagnosticReportModels.TaskView> diagnosticTasks,
        PathologyReportView currentReport,
        List<ReportVersionView> versions,
        List<TrackingEventView> events,
        List<RevisionRequestView> revisions,
        List<MedicalOrderView> medicalOrders,
        List<ConsultationView> consultations,
        Integer latestEffectiveVersionNo,
        Integer currentDraftVersionNo,
        boolean hasPendingRevision
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
