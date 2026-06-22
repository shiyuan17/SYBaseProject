package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "DiagnosticWorkbenchResponse", description = "Diagnostic workbench aggregate view")
public record DiagnosticWorkbenchResponse(
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "Application number")
    String applicationNo,
    @Schema(description = "Pathology number")
    String pathologyNo,
    @Schema(description = "Case status")
    String caseStatus,
    @Schema(description = "Patient name")
    String patientName,
    @Schema(description = "Patient ID")
    String patientId,
    @Schema(description = "Patient ID display value from registration workbench id_no")
    String patientIdDisplay,
    @Schema(description = "Patient gender")
    String patientGender,
    @Schema(description = "Patient age")
    String patientAge,
    @Schema(description = "Application type")
    String applicationType,
    @Schema(description = "Inpatient number")
    String inpatientNo,
    @Schema(description = "Outpatient number")
    String outpatientNo,
    @Schema(description = "Bed number")
    String bedNo,
    @Schema(description = "Phone")
    String phone,
    @Schema(description = "Submitting department")
    String submittingDepartmentName,
    @Schema(description = "Submitting doctor")
    String submittingDoctorName,
    @Schema(description = "Clinical diagnosis")
    String clinicalDiagnosis,
    @Schema(description = "Application remarks")
    String applicationRemarks,
    @Schema(description = "Application form archive status")
    String applicationFormArchiveStatus,
    @Schema(description = "Application form archive location")
    String applicationFormArchiveLocation,
    @Schema(description = "Application form archive image URL")
    String applicationFormImageUrl,
    @Schema(description = "Specimen summaries")
    List<SpecimenSummary> specimens,
    @Schema(description = "Block summaries")
    List<BlockSummary> blocks,
    @Schema(description = "Slide summaries")
    List<SlideSummary> slides,
    @Schema(description = "Diagnostic tasks")
    List<PendingDiagnosticTaskResponse> diagnosticTasks,
    @Schema(description = "Current report")
    CurrentReportSummary currentReport,
    @Schema(description = "Recent workflow events")
    List<EventSummary> recentEvents,
    @Schema(description = "Revision request summaries")
    List<RevisionRequestSummary> revisions,
    @Schema(description = "Medical order summaries")
    List<MedicalOrderSummary> medicalOrders,
    @Schema(description = "Consultation summaries")
    List<ConsultationSummary> consultations,
    @Schema(description = "Historical pathology summaries")
    List<HistoricalPathologySummary> historicalPathologies,
    @Schema(description = "PACS examination summaries")
    List<PacsExaminationSummary> pacsExaminations,
    @Schema(description = "Report trace summaries")
    List<ReportTraceSummary> reportTraces,
    @Schema(description = "Remark sections")
    List<RemarkSectionSummary> remarkSections,
    @Schema(description = "Charge item summaries")
    List<ChargeItemSummary> chargeItems,
    @Schema(description = "Whether there is a pending revision request")
    boolean hasPendingRevision
) {
    @Schema(name = "DiagnosticWorkbenchSpecimenSummary", description = "Specimen summary in diagnostic workbench")
    public record SpecimenSummary(
        @Schema(description = "Specimen ID")
        String specimenId,
        @Schema(description = "Specimen number")
        String specimenNo,
        @Schema(description = "Specimen barcode")
        String barcode,
        @Schema(description = "Specimen name")
        String specimenName,
        @Schema(description = "Specimen status")
        String specimenStatus
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchBlockSummary", description = "Block summary in diagnostic workbench")
    public record BlockSummary(
        @Schema(description = "Block ID")
        String blockId,
        @Schema(description = "Specimen ID")
        String specimenId,
        @Schema(description = "Block code")
        String blockCode,
        @Schema(description = "Embedding box number")
        String embeddingBoxNo,
        @Schema(description = "Description")
        String description,
        @Schema(description = "Archive status")
        String archiveStatus,
        @Schema(description = "Archive location")
        String archiveLocation,
        @Schema(description = "Loan status")
        String loanStatus
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchSlideSummary", description = "Slide summary in diagnostic workbench")
    public record SlideSummary(
        @Schema(description = "Slide ID")
        String slideId,
        @Schema(description = "Specimen ID")
        String specimenId,
        @Schema(description = "Embedding box ID")
        String embeddingBoxId,
        @Schema(description = "Slide number")
        String slideNo,
        @Schema(description = "Slide status")
        String slideStatus,
        @Schema(description = "Quality status")
        String qualityStatus,
        @Schema(description = "Archive status")
        String archiveStatus,
        @Schema(description = "Archive location")
        String archiveLocation,
        @Schema(description = "Loan status")
        String loanStatus
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchCurrentReportSummary", description = "Current report summary in diagnostic workbench")
    public record CurrentReportSummary(
        @Schema(description = "Report ID")
        String reportId,
        @Schema(description = "Report number")
        String reportNo,
        @Schema(description = "Report status")
        String reportStatus,
        @Schema(description = "Clinical diagnosis")
        String clinicalDiagnosis,
        @Schema(description = "Gross exam")
        String grossExam,
        @Schema(description = "Microscopic exam")
        String microscopicExam,
        @Schema(description = "Final diagnosis")
        String finalDiagnosis,
        @Schema(description = "Rich text content")
        String richTextContent,
        @Schema(description = "Submitted at")
        String submittedAt,
        @Schema(description = "Reviewed at")
        String reviewedAt,
        @Schema(description = "Signed at")
        String signedAt,
        @Schema(description = "Published at")
        String publishedAt,
        @Schema(description = "Reviewer name")
        String reviewerName,
        @Schema(description = "Signed by name")
        String signedByName,
        @Schema(description = "Current version number")
        int versionNo
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchEventSummary", description = "Workflow event summary in diagnostic workbench")
    public record EventSummary(
        @Schema(description = "Node code")
        String nodeCode,
        @Schema(description = "Event type")
        String eventType,
        @Schema(description = "Event status")
        String eventStatus,
        @Schema(description = "Event time")
        String eventTime,
        @Schema(description = "Operator name")
        String operatorName,
        @Schema(description = "Event content")
        String eventContent
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchRevisionRequestSummary", description = "Revision request summary in diagnostic workbench")
    public record RevisionRequestSummary(
        @Schema(description = "Revision request ID")
        String requestId,
        @Schema(description = "Report ID")
        String reportId,
        @Schema(description = "Current version number")
        int currentVersionNo,
        @Schema(description = "Request status")
        String requestStatus,
        @Schema(description = "Request reason")
        String requestReason,
        @Schema(description = "Requested by")
        String requestedByName,
        @Schema(description = "Requested at")
        String requestedAt,
        @Schema(description = "Reviewed by")
        String reviewedByName,
        @Schema(description = "Reviewed at")
        String reviewedAt,
        @Schema(description = "Reject reason")
        String rejectReason,
        @Schema(description = "Approved version number")
        Integer approvedVersionNo
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchMedicalOrderSummary", description = "Medical order summary in diagnostic workbench")
    public record MedicalOrderSummary(
        @Schema(description = "Order ID")
        String orderId,
        @Schema(description = "Case ID")
        String caseId,
        @Schema(description = "Pathology number")
        String pathologyNo,
        @Schema(description = "Application number")
        String applicationNo,
        @Schema(description = "Patient name")
        String patientName,
        @Schema(description = "Patient ID")
        String patientId,
        @Schema(description = "Patient ID display")
        String patientIdDisplay,
        @Schema(description = "Order number")
        String orderNumber,
        @Schema(description = "Order type")
        String orderType,
        @Schema(description = "Order content")
        String orderContent,
        @Schema(description = "Medical order dictionary item ID")
        String orderItemId,
        @Schema(description = "Medical order dictionary item code")
        String orderItemCode,
        @Schema(description = "Medical order dictionary item name")
        String orderItemName,
        @Schema(description = "Medical order category ID")
        String orderCategoryId,
        @Schema(description = "Medical order category code")
        String orderCategoryCode,
        @Schema(description = "Medical order category name")
        String orderCategoryName,
        @Schema(description = "Execution scope")
        String executionScope,
        @Schema(description = "Billing status")
        String billingStatus,
        @Schema(description = "Order status")
        String status,
        @Schema(description = "Doctor name")
        String doctorName,
        @Schema(description = "Executor name")
        String executorName,
        @Schema(description = "Order date")
        String orderDate,
        @Schema(description = "Accepted at")
        String acceptedAt,
        @Schema(description = "Printed at")
        String printedAt,
        @Schema(description = "Printed by name")
        String printedByName,
        @Schema(description = "Released at")
        String releasedAt,
        @Schema(description = "Released by name")
        String releasedByName,
        @Schema(description = "Completed at")
        String completedAt,
        @Schema(description = "Cancelled at")
        String cancelledAt,
        @Schema(description = "Terminated at")
        String terminatedAt,
        @Schema(description = "Terminated by name")
        String terminatedByName,
        @Schema(description = "Termination reason code")
        String terminationReasonCode,
        @Schema(description = "Termination reason label")
        String terminationReasonLabel,
        @Schema(description = "Termination remarks")
        String terminationRemarks,
        @Schema(description = "Remarks")
        String remarks,
        @Schema(description = "Target type")
        String targetType,
        @Schema(description = "Target specimen ID")
        String targetSpecimenId,
        @Schema(description = "Target specimen number")
        String targetSpecimenNo,
        @Schema(description = "Target block ID")
        String targetBlockId,
        @Schema(description = "Target block number")
        String targetBlockNo,
        @Schema(description = "Target slide ID")
        String targetSlideId,
        @Schema(description = "Target slide number")
        String targetSlideNo,
        @Schema(description = "Specimen number")
        String specimenNo,
        @Schema(description = "Block number")
        String blockNo,
        @Schema(description = "Slide number")
        String slideNo
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchConsultationSummary", description = "Consultation summary in diagnostic workbench")
    public record ConsultationSummary(
        @Schema(description = "Consultation ID")
        String consultationId,
        @Schema(description = "Consultation type")
        String consultationType,
        @Schema(description = "Consultation status")
        String status,
        @Schema(description = "Requested by")
        String requestedByName,
        @Schema(description = "Requested at")
        String requestedAt,
        @Schema(description = "Host name")
        String hostName,
        @Schema(description = "Completed at")
        String completedAt,
        @Schema(description = "Opinion")
        String opinion,
        @Schema(description = "Participant count")
        int participantCount,
        @Schema(description = "Participant summaries")
        List<ConsultationParticipantSummary> participants
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchConsultationParticipantSummary", description = "Consultation participant summary in diagnostic workbench")
    public record ConsultationParticipantSummary(
        @Schema(description = "Participant ID")
        String participantId,
        @Schema(description = "Participant user ID")
        String participantUserId,
        @Schema(description = "Participant name")
        String participantName,
        @Schema(description = "Participant role")
        String participantRole,
        @Schema(description = "Participant opinion")
        String opinion,
        @Schema(description = "Drafted by name")
        String draftedByName,
        @Schema(description = "Commented at")
        String commentedAt
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchHistoricalPathologySummary", description = "Historical pathology summary in diagnostic workbench")
    public record HistoricalPathologySummary(
        @Schema(description = "Age")
        String age,
        @Schema(description = "Inpatient number")
        String inpatientNo,
        @Schema(description = "Examination number")
        String examinationNo,
        @Schema(description = "Submission type")
        String submissionType,
        @Schema(description = "Report time")
        String reportTime,
        @Schema(description = "Diagnosis")
        String diagnosis
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchPacsExaminationSummary", description = "PACS examination summary in diagnostic workbench")
    public record PacsExaminationSummary(
        @Schema(description = "Submission type")
        String submissionType,
        @Schema(description = "Imaging diagnosis")
        String imagingDiagnosis,
        @Schema(description = "Report time")
        String reportTime,
        @Schema(description = "Examination number")
        String examinationNo,
        @Schema(description = "Imaging description")
        String imagingDescription,
        @Schema(description = "Report status")
        String reportStatus
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchReportTraceSummary", description = "Report trace summary in diagnostic workbench")
    public record ReportTraceSummary(
        @Schema(description = "Sequence number")
        int sequenceNo,
        @Schema(description = "Report doctor")
        String reportDoctorName,
        @Schema(description = "Report time")
        String reportTime,
        @Schema(description = "Report status")
        String reportStatus,
        @Schema(description = "Diagnosis info")
        String diagnosisInfo
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchRemarkSectionSummary", description = "Remark section in diagnostic workbench")
    public record RemarkSectionSummary(
        @Schema(description = "Section key")
        String sectionKey,
        @Schema(description = "Title")
        String title,
        @Schema(description = "Related number")
        String relatedNo,
        @Schema(description = "Content")
        String content
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchChargeItemSummary", description = "Charge item summary in diagnostic workbench")
    public record ChargeItemSummary(
        @Schema(description = "Item name")
        String itemName,
        @Schema(description = "Charged at")
        String chargedAt,
        @Schema(description = "Charged by")
        String chargedByName
    ) {
    }
}
