package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "TechnicalTrackingResponse", description = "病理技术流程追踪视图")
public record TechnicalTrackingResponse(
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "病例状态")
    String caseStatus,
    @Schema(description = "技术任务列表")
    List<PendingTechnicalTaskResponse> technicalTasks,
    @Schema(description = "标本摘要列表")
    List<SpecimenSummary> specimens,
    @Schema(description = "蜡块摘要列表")
    List<BlockSummary> blocks,
    @Schema(description = "包埋盒摘要列表")
    List<EmbeddingBoxSummary> embeddingBoxes,
    @Schema(description = "包埋记录列表")
    List<EmbeddingRecordSummary> embeddingRecords,
    @Schema(description = "包埋评价记录列表")
    List<EmbeddingEvaluationRecordSummary> embeddingEvaluationRecords,
    @Schema(description = "玻片摘要列表")
    List<SlideSummary> slides,
    @Schema(description = "玻片质控历史")
    List<SlideQcEvaluationSummary> qcEvaluations,
    @Schema(description = "返工摘要列表")
    List<ReworkSummary> reworks,
    @Schema(description = "追踪事件列表")
    List<EventSummary> events
) {
    @Schema(name = "TechnicalTrackingSpecimenSummary", description = "技术追踪中的标本摘要")
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

    @Schema(name = "TechnicalTrackingBlockSummary", description = "技术追踪中的蜡块摘要")
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
        String description,
        @Schema(description = "标本名称")
        String specimenName,
        @Schema(description = "大体所见")
        String grossDescription
    ) {
    }

    @Schema(name = "TechnicalTrackingEmbeddingBoxSummary", description = "技术追踪中的包埋盒摘要")
    public record EmbeddingBoxSummary(
        @Schema(description = "包埋盒 ID")
        String embeddingBoxId,
        @Schema(description = "所属标本 ID")
        String specimenId,
        @Schema(description = "包埋盒号")
        String embeddingBoxNo,
        @Schema(description = "切片提示")
        String sliceNotice,
        @Schema(description = "玻片数量")
        int slideCount
    ) {
    }

    @Schema(name = "TechnicalTrackingEmbeddingRecordSummary", description = "技术追踪中的包埋记录摘要")
    public record EmbeddingRecordSummary(
        @Schema(description = "技术任务 ID")
        String taskId,
        @Schema(description = "病例 ID")
        String caseId,
        @Schema(description = "病理号")
        String pathologyNo,
        @Schema(description = "标本 ID")
        String specimenId,
        @Schema(description = "标本名称")
        String specimenName,
        @Schema(description = "蜡块 ID")
        String samplingBlockId,
        @Schema(description = "蜡块号")
        String samplingBlockCode,
        @Schema(description = "蜡块名称")
        String samplingBlockDescription,
        @Schema(description = "大体所见")
        String grossDescription,
        @Schema(description = "包埋记录 ID")
        String embeddingId,
        @Schema(description = "包埋盒 ID")
        String embeddingBoxId,
        @Schema(description = "包埋盒号")
        String embeddingBoxNo,
        @Schema(description = "切片提示")
        String sliceNotice,
        @Schema(description = "评估等级")
        String evaluationLevel,
        @Schema(description = "取材评价")
        String samplingEvaluation,
        @Schema(description = "包埋备注")
        String embeddingRemarks,
        @Schema(description = "取材操作人")
        String sampledByName,
        @Schema(description = "取材时间")
        String sampledAt,
        @Schema(description = "包埋操作人")
        String embeddedByName,
        @Schema(description = "包埋开始时间")
        String startedAt,
        @Schema(description = "包埋完成时间")
        String endedAt,
        @Schema(description = "任务状态")
        String taskStatus
    ) {
    }

    @Schema(name = "TechnicalTrackingEmbeddingEvaluationRecordSummary", description = "技术追踪中的包埋评价摘要")
    public record EmbeddingEvaluationRecordSummary(
        @Schema(description = "包埋记录 ID")
        String embeddingId,
        @Schema(description = "病例 ID")
        String caseId,
        @Schema(description = "病理号")
        String pathologyNo,
        @Schema(description = "标本 ID")
        String specimenId,
        @Schema(description = "标本名称")
        String specimenName,
        @Schema(description = "蜡块 ID")
        String samplingBlockId,
        @Schema(description = "蜡块号")
        String samplingBlockCode,
        @Schema(description = "包埋盒号")
        String embeddingBoxNo,
        @Schema(description = "评估等级")
        String evaluationLevel,
        @Schema(description = "取材评价")
        String samplingEvaluation,
        @Schema(description = "包埋备注")
        String embeddingRemarks,
        @Schema(description = "包埋操作人")
        String embeddedByName,
        @Schema(description = "包埋完成时间")
        String endedAt
    ) {
    }

    @Schema(name = "TechnicalTrackingSlideSummary", description = "技术追踪中的玻片摘要")
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

    @Schema(name = "TechnicalTrackingSlideQcEvaluationSummary", description = "技术追踪中的玻片质控摘要")
    public record SlideQcEvaluationSummary(
        @Schema(description = "质控记录 ID")
        String qcEvaluationId,
        @Schema(description = "所属标本 ID")
        String specimenId,
        @Schema(description = "玻片 ID")
        String slideId,
        @Schema(description = "玻片号")
        String slideNo,
        @Schema(description = "质控类型")
        String qcType,
        @Schema(description = "质控结果")
        String evaluationResult,
        @Schema(description = "问题描述")
        String issueDescription,
        @Schema(description = "改进建议")
        String improvementSuggestion,
        @Schema(description = "评估人")
        String evaluatorName,
        @Schema(description = "评估时间")
        String evaluatedAt,
        @Schema(description = "备注")
        String remarks
    ) {
    }

    @Schema(name = "TechnicalTrackingReworkSummary", description = "技术追踪中的返工摘要")
    public record ReworkSummary(
        @Schema(description = "返工单 ID")
        String reworkOrderId,
        @Schema(description = "返工类型")
        String reworkType,
        @Schema(description = "返工状态")
        String status,
        @Schema(description = "返工原因")
        String reason
    ) {
    }

    @Schema(name = "TechnicalTrackingEventSummary", description = "技术追踪中的事件摘要")
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
