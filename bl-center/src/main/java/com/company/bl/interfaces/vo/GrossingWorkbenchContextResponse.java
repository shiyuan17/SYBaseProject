package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "GrossingWorkbenchContextResponse", description = "取材工作台上下文")
public record GrossingWorkbenchContextResponse(
    @Schema(description = "当前取材任务摘要")
    TaskSummary task,
    @Schema(description = "病例与申请单摘要")
    CaseSummary caseSummary,
    @Schema(description = "技术追踪摘要")
    TechnicalTrackingResponse tracking,
    @Schema(description = "临床诊断")
    String clinicalDiagnosis,
    @Schema(description = "临床病史")
    String clinicalHistory,
    @Schema(description = "相关检查")
    String relatedExaminations,
    @Schema(description = "上下文摘要")
    String contextSummary,
    @Schema(description = "临床送检要求")
    String clinicalSubmissionRequirements,
    @Schema(description = "传染/既往信息摘要")
    String infectiousAndPastHistorySummary,
    @Schema(description = "外院病理诊断")
    String externalPathologyDiagnosis,
    @Schema(description = "检查项目")
    List<TechnicalSpecimenRegistrationCheckItemResponse> checkItems,
    @Schema(description = "已采影像")
    List<MediaAssetSummary> mediaAssets
) {
    @Schema(name = "GrossingWorkbenchTaskSummaryResponse", description = "取材任务摘要")
    public record TaskSummary(
        @Schema(description = "任务 ID")
        String taskId,
        @Schema(description = "任务状态")
        String taskStatus,
        @Schema(description = "对象类型")
        String objectType,
        @Schema(description = "对象编号")
        String objectId
    ) {
    }

    @Schema(name = "GrossingWorkbenchCaseSummaryResponse", description = "取材工作台病例摘要")
    public record CaseSummary(
        @Schema(description = "病例 ID")
        String caseId,
        @Schema(description = "申请单 ID")
        String applicationId,
        @Schema(description = "申请单号")
        String applicationNo,
        @Schema(description = "病理号")
        String pathologyNo,
        @Schema(description = "病例状态")
        String caseStatus,
        @Schema(description = "患者姓名")
        String patientName,
        @Schema(description = "患者 ID")
        String patientId,
        @Schema(description = "患者展示 ID")
        String patientIdDisplay,
        @Schema(description = "住院号")
        String inpatientNo,
        @Schema(description = "申请类型")
        String applicationType,
        @Schema(description = "送检科室")
        String submittingDepartmentName
    ) {
    }

    @Schema(name = "GrossingWorkbenchMediaAssetSummaryResponse", description = "取材工作台影像摘要")
    public record MediaAssetSummary(
        @Schema(description = "影像 ID")
        String assetId,
        @Schema(description = "标本 ID")
        String specimenId,
        @Schema(description = "文件名")
        String fileName,
        @Schema(description = "文件地址")
        String fileUrl,
        @Schema(description = "采图时间")
        String capturedAt,
        @Schema(description = "采图人")
        String capturedByName
    ) {
    }
}
