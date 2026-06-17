package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import java.util.List;

@Schema(name = "SlicingWorkbenchResponse", description = "切片工作站聚合视图")
public record SlicingWorkbenchResponse(
    @Schema(description = "顶部统计")
    Stats stats,
    @Schema(description = "待切列表")
    List<Row> pendingList,
    @Schema(description = "待打印玻片列表")
    List<Row> pendingPrintList,
    @Schema(description = "已打印待切列表")
    List<Row> pendingSliceList,
    @Schema(description = "待切页码")
    int pendingPage,
    @Schema(description = "待切分页大小")
    int pendingSize,
    @Schema(description = "待切总数")
    long pendingTotal,
    @Schema(description = "待打印总数")
    long pendingPrintTotal,
    @Schema(description = "已打印待切总数")
    long pendingSliceTotal,
    @Schema(description = "今日已完成列表")
    List<Row> completedTodayList,
    @Schema(description = "已完成页码")
    int completedPage,
    @Schema(description = "已完成分页大小")
    int completedSize,
    @Schema(description = "已完成总数")
    long completedTotal
) {
    @Schema(name = "SlicingWorkbenchStats", description = "切片工作站统计摘要")
    public record Stats(
        @Schema(description = "今天待切数")
        long pendingTodayCount,
        @Schema(description = "明天待切数")
        long pendingTomorrowCount,
        @Schema(description = "我今日已切数")
        long completedMineTodayCount,
        @Schema(description = "全科今日已切数")
        long completedDeptTodayCount,
        @Schema(description = "过期任务数")
        long overdueCount,
        @Schema(description = "待打印玻片数")
        long pendingPrintCount
    ) {
    }

    @Schema(name = "SlicingWorkbenchRow", description = "切片工作站列表行")
    public record Row(
        @Schema(description = "任务 ID")
        String taskId,
        @Schema(description = "病例 ID")
        String caseId,
        @Schema(description = "申请类型")
        String applicationType,
        @Schema(description = "病理号")
        String pathologyNo,
        @Schema(description = "患者姓名")
        String patientName,
        @Schema(description = "患者 ID")
        String patientId,
        @Schema(description = "患者展示 ID")
        String patientIdDisplay,
        @Schema(description = "标本 ID")
        String specimenId,
        @Schema(description = "标本名称")
        String specimenName,
        @Schema(description = "包埋盒 ID")
        String embeddingBoxId,
        @Schema(description = "包埋盒号")
        String embeddingBoxNo,
        @Schema(description = "玻片 ID")
        String slideId,
        @Schema(description = "玻片号")
        String slideNo,
        @Schema(description = "切片操作人")
        String slicingOperatorName,
        @Schema(description = "切片备注")
        String slicingRemark,
        @Schema(description = "切片完成时间")
        String completedAt,
        @Schema(description = "取材评价")
        String grossingEvaluation,
        @Schema(description = "包埋评价")
        String embeddingEvaluation,
        @Schema(description = "包埋操作人")
        String embeddingOperatorName,
        @Schema(description = "包埋清零备注")
        String embeddingClearRemark,
        @Schema(description = "包埋备注")
        String embeddingRemarks,
        @Schema(description = "主班备注")
        String shiftRemark,
        @Schema(description = "切片提示")
        String sliceNotice,
        @Schema(description = "申请科室")
        String submittingDepartmentName,
        @Schema(description = "任务状态")
        String taskStatus,
        @Schema(description = "玻片打印状态")
        String slidePrintStatus,
        @Schema(description = "已打印玻片数量")
        int printedSlideCount,
        @Schema(description = "是否包含近邻合并玻片")
        boolean combinedSlide,
        @Schema(description = "是否超时")
        boolean timedOut,
        @Schema(description = "是否可选")
        boolean selectable,
        @Schema(description = "未打印合片组 ID")
        String printGroupId,
        @Schema(description = "是否为未打印合片组行")
        boolean mergedPrintGroup,
        @Schema(description = "合片组内技术任务 ID 列表")
        List<String> taskIds,
        @Schema(description = "合片组内包埋盒 ID 列表")
        List<String> embeddingBoxIds
    ) {
    }
}
