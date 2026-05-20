package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ReportTrackingResponse", description = "病例报告闭环追踪视图")
public record ReportTrackingResponse(
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "病例状态")
    String caseStatus,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "诊断任务列表")
    List<PendingDiagnosticTaskResponse> diagnosticTasks,
    @Schema(description = "当前报告")
    DiagnosticWorkbenchResponse.CurrentReportSummary currentReport,
    @Schema(description = "版本列表")
    List<ReportVersionSummary> versions,
    @Schema(description = "事件列表")
    List<DiagnosticWorkbenchResponse.EventSummary> events
) {
    @Schema(name = "ReportTrackingVersionSummary", description = "报告闭环追踪中的版本摘要")
    public record ReportVersionSummary(
        @Schema(description = "版本 ID")
        String versionId,
        @Schema(description = "版本号")
        int versionNo,
        @Schema(description = "版本状态")
        String versionStatus,
        @Schema(description = "最终诊断快照")
        String finalDiagnosisSnapshot,
        @Schema(description = "签发时间")
        String signedAt,
        @Schema(description = "创建时间")
        String createdAt
    ) {
    }
}
