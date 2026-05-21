package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ReportTrackingResponse", description = "Case report tracking aggregate view")
public record ReportTrackingResponse(
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
    @Schema(description = "Application form archive status")
    String applicationFormArchiveStatus,
    @Schema(description = "Application form archive location")
    String applicationFormArchiveLocation,
    @Schema(description = "Application form archive image URL")
    String applicationFormImageUrl,
    @Schema(description = "Diagnostic tasks")
    List<PendingDiagnosticTaskResponse> diagnosticTasks,
    @Schema(description = "Current report")
    DiagnosticWorkbenchResponse.CurrentReportSummary currentReport,
    @Schema(description = "Report versions")
    List<ReportVersionSummary> versions,
    @Schema(description = "Workflow events")
    List<DiagnosticWorkbenchResponse.EventSummary> events,
    @Schema(description = "Revision request chain")
    List<DiagnosticWorkbenchResponse.RevisionRequestSummary> revisions,
    @Schema(description = "Medical order chain")
    List<DiagnosticWorkbenchResponse.MedicalOrderSummary> medicalOrders,
    @Schema(description = "Consultation chain")
    List<DiagnosticWorkbenchResponse.ConsultationSummary> consultations,
    @Schema(description = "Latest effective version number")
    Integer latestEffectiveVersionNo,
    @Schema(description = "Current draft version number")
    Integer currentDraftVersionNo,
    @Schema(description = "Whether there is a pending revision request")
    boolean hasPendingRevision
) {
    @Schema(name = "ReportTrackingVersionSummary", description = "Report version summary in tracking view")
    public record ReportVersionSummary(
        @Schema(description = "Version ID")
        String versionId,
        @Schema(description = "Version number")
        int versionNo,
        @Schema(description = "Version status")
        String versionStatus,
        @Schema(description = "Final diagnosis snapshot")
        String finalDiagnosisSnapshot,
        @Schema(description = "Signed at")
        String signedAt,
        @Schema(description = "Created at")
        String createdAt
    ) {
    }
}
