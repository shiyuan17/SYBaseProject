package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CaseLifecycleTrackingResponse", description = "病例全生命周期追踪视图")
public record CaseLifecycleTrackingResponse(
    @Schema(description = "病例摘要")
    CaseSummary caseSummary,
    @Schema(description = "申请单归档摘要")
    ApplicationForm applicationForm,
    @Schema(description = "全局生命周期时间线")
    List<StageGroup> overallTimeline,
    @Schema(description = "标本对象树")
    List<SpecimenItem> specimens,
    @Schema(description = "报告生命周期")
    ReportLifecycle reportLifecycle
) {
    public record CaseSummary(
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

    public record ApplicationForm(
        String archiveStatus,
        String archiveLocation,
        String imageUrl,
        String applicantDoctorName,
        String applicationDate,
        String remarks
    ) {
    }

    public record StageGroup(
        String stageCode,
        String stageTitle,
        List<LifecycleNode> nodes
    ) {
    }

    public record LifecycleNode(
        String stageCode,
        String nodeCode,
        String title,
        String status,
        String occurredAt,
        String operatorName,
        String operatorIp,
        String operatorDevice,
        List<KeyFact> keyFacts,
        String eventContent
    ) {
    }

    public record KeyFact(
        String label,
        String value
    ) {
    }

    public record SpecimenItem(
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
        List<LifecycleNode> specimenEvents,
        List<BlockItem> blocks
    ) {
    }

    public record BlockItem(
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
        List<LifecycleNode> blockEvents,
        List<SlideItem> slides
    ) {
    }

    public record SlideItem(
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
        List<LifecycleNode> slideEvents
    ) {
    }

    public record ReportLifecycle(
        DiagnosticWorkbenchResponse.CurrentReportSummary currentReport,
        List<PendingDiagnosticTaskResponse> diagnosticTasks,
        List<ReportTrackingResponse.ReportVersionSummary> versions,
        List<DiagnosticWorkbenchResponse.RevisionRequestSummary> revisions,
        List<DiagnosticWorkbenchResponse.ConsultationSummary> consultations,
        List<DiagnosticWorkbenchResponse.MedicalOrderSummary> medicalOrders
    ) {
    }
}
