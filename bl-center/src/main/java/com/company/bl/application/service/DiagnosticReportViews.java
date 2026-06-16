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
        String orderItemId,
        String orderItemCode,
        String orderItemName,
        String orderCategoryId,
        String orderCategoryCode,
        String orderCategoryName,
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
        String signedVersionDeliveryStatus,
        String signedVersionIssuedAt,
        String signedVersionPlannedIssueAt,
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
        int participantCount,
        List<ConsultationParticipantView> participants
    ) {
    }

    public record ConsultationParticipantView(
        String participantId,
        String participantUserId,
        String participantName,
        String participantRole,
        String opinion,
        String draftedByName,
        String commentedAt
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
        String createdAt,
        String deliveryStatus,
        String issuedAt,
        String plannedIssueAt
    ) {
    }

    public record CaseLifecycleTrackingView(
        CaseSummaryView caseSummary,
        ApplicationFormView applicationForm,
        List<LifecycleStageGroupView> overallTimeline,
        List<LifecycleSpecimenView> specimens,
        ReportLifecycleView reportLifecycle
    ) {
    }

    public record CaseSummaryView(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        String patientGender,
        String patientAge,
        String applicationType,
        String submittingDepartmentName,
        String submittingDoctorName,
        String applicationDate,
        String currentStage,
        boolean hasPendingRevision
    ) {
    }

    public record ApplicationFormView(
        String archiveStatus,
        String archiveLocation,
        String imageUrl,
        String applicantDoctorName,
        String applicationDate,
        String remarks
    ) {
    }

    public record LifecycleStageGroupView(
        String stageCode,
        String stageTitle,
        List<LifecycleNodeView> nodes
    ) {
    }

    public record LifecycleNodeView(
        String stageCode,
        String nodeCode,
        String title,
        String status,
        String occurredAt,
        String operatorName,
        List<KeyFactView> keyFacts,
        String eventContent
    ) {
    }

    public record KeyFactView(
        String label,
        String value
    ) {
    }

    public record LifecycleSpecimenView(
        String specimenId,
        String specimenNo,
        String barcode,
        String specimenName,
        String specimenStatus,
        String archiveStatus,
        String archiveLocation,
        String loanStatus,
        String createdAt,
        String removalAt,
        String fixedAt,
        String confirmedAt,
        String checkedInAt,
        String receiptStatus,
        String receivedAt,
        String contentDescribedByName,
        List<LifecycleNodeView> specimenEvents,
        List<LifecycleBlockView> blocks
    ) {
    }

    public record LifecycleBlockView(
        String blockId,
        String specimenId,
        String blockCode,
        String embeddingBoxNo,
        String description,
        String specimenName,
        String grossDescription,
        String archiveStatus,
        String archiveLocation,
        String loanStatus,
        String sampledByName,
        String sampledAt,
        String embeddedByName,
        String embeddingStartedAt,
        String embeddingEndedAt,
        String sliceNotice,
        String evaluationLevel,
        String samplingEvaluation,
        String embeddingRemarks,
        List<LifecycleNodeView> blockEvents,
        List<LifecycleSlideView> slides
    ) {
    }

    public record LifecycleSlideView(
        String slideId,
        String specimenId,
        String embeddingBoxId,
        String slideNo,
        String slideStatus,
        String qualityStatus,
        String archiveStatus,
        String archiveLocation,
        String loanStatus,
        String printedAt,
        String slicedAt,
        String slicedByName,
        String stainedAt,
        String stainedByName,
        String qcResult,
        String qcEvaluatedAt,
        String qcEvaluatorName,
        String reworkStatus,
        String reworkReason,
        List<LifecycleNodeView> slideEvents
    ) {
    }

    public record ReportLifecycleView(
        PathologyReportView currentReport,
        List<DiagnosticReportModels.TaskView> diagnosticTasks,
        List<ReportVersionView> versions,
        List<RevisionRequestView> revisions,
        List<ConsultationView> consultations,
        List<MedicalOrderView> medicalOrders
    ) {
    }
}
