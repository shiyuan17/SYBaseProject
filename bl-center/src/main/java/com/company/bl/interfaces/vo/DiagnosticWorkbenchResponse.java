package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "DiagnosticWorkbenchResponse", description = "病例诊断工作台聚合视图")
public record DiagnosticWorkbenchResponse(
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
    @Schema(description = "送检科室")
    String submittingDepartmentName,
    @Schema(description = "送检医生")
    String submittingDoctorName,
    @Schema(description = "临床诊断")
    String clinicalDiagnosis,
    @Schema(description = "标本摘要")
    List<SpecimenSummary> specimens,
    @Schema(description = "蜡块摘要")
    List<BlockSummary> blocks,
    @Schema(description = "玻片摘要")
    List<SlideSummary> slides,
    @Schema(description = "诊断任务列表")
    List<PendingDiagnosticTaskResponse> diagnosticTasks,
    @Schema(description = "当前报告")
    CurrentReportSummary currentReport,
    @Schema(description = "最近事件")
    List<EventSummary> recentEvents
) {
    @Schema(name = "DiagnosticWorkbenchSpecimenSummary", description = "诊断工作台中的标本摘要")
    public record SpecimenSummary(
        @Schema(description = "标本 ID")
        String specimenId,
        @Schema(description = "标本号")
        String specimenNo,
        @Schema(description = "标本条码")
        String barcode,
        @Schema(description = "标本名称")
        String specimenName,
        @Schema(description = "标本状态")
        String specimenStatus
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchBlockSummary", description = "诊断工作台中的蜡块摘要")
    public record BlockSummary(
        @Schema(description = "蜡块 ID")
        String blockId,
        @Schema(description = "所属标本 ID")
        String specimenId,
        @Schema(description = "蜡块编码")
        String blockCode,
        @Schema(description = "包埋盒号")
        String embeddingBoxNo,
        @Schema(description = "描述")
        String description
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchSlideSummary", description = "诊断工作台中的玻片摘要")
    public record SlideSummary(
        @Schema(description = "玻片 ID")
        String slideId,
        @Schema(description = "所属标本 ID")
        String specimenId,
        @Schema(description = "所属包埋盒 ID")
        String embeddingBoxId,
        @Schema(description = "玻片号")
        String slideNo,
        @Schema(description = "玻片状态")
        String slideStatus,
        @Schema(description = "质控状态")
        String qualityStatus
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchCurrentReportSummary", description = "诊断工作台中的当前报告摘要")
    public record CurrentReportSummary(
        @Schema(description = "报告 ID")
        String reportId,
        @Schema(description = "报告编号")
        String reportNo,
        @Schema(description = "报告状态")
        String reportStatus,
        @Schema(description = "临床诊断")
        String clinicalDiagnosis,
        @Schema(description = "大体检查")
        String grossExam,
        @Schema(description = "镜下检查")
        String microscopicExam,
        @Schema(description = "最终诊断")
        String finalDiagnosis,
        @Schema(description = "富文本正文")
        String richTextContent,
        @Schema(description = "提交时间")
        String submittedAt,
        @Schema(description = "审核时间")
        String reviewedAt,
        @Schema(description = "签发时间")
        String signedAt,
        @Schema(description = "发布时间")
        String publishedAt,
        @Schema(description = "审核医生")
        String reviewerName,
        @Schema(description = "签发医生")
        String signedByName,
        @Schema(description = "当前版本号")
        int versionNo
    ) {
    }

    @Schema(name = "DiagnosticWorkbenchEventSummary", description = "诊断工作台中的事件摘要")
    public record EventSummary(
        @Schema(description = "节点编码")
        String nodeCode,
        @Schema(description = "事件类型")
        String eventType,
        @Schema(description = "事件状态")
        String eventStatus,
        @Schema(description = "事件时间")
        String eventTime,
        @Schema(description = "操作人姓名")
        String operatorName,
        @Schema(description = "事件内容")
        String eventContent
    ) {
    }
}
