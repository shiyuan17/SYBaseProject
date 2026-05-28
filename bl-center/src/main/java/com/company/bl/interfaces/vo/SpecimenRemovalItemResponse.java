package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SpecimenRemovalItemResponse", description = "标本离体工作台列表项")
public record SpecimenRemovalItemResponse(
    @Schema(description = "标本ID")
    String specimenId,
    @Schema(description = "标本条码")
    String barcode,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "标本编号")
    String specimenNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "性别")
    String patientGender,
    @Schema(description = "住院号")
    String inpatientNo,
    @Schema(description = "手术间")
    String surgeryName,
    @Schema(description = "标本名称")
    String specimenName,
    @Schema(description = "标本状态")
    String specimenStatus,
    @Schema(description = "类型")
    String specimenType,
    @Schema(description = "离体时间")
    String specimenRemovalAt,
    @Schema(description = "离体操作人")
    String specimenRemovalOperatorName,
    @Schema(description = "添加时间")
    String registeredAt,
    @Schema(description = "标签批次号")
    String labelPrintBatchNo,
    @Schema(description = "添加人")
    String registeredByName,
    @Schema(description = "最新轨迹时间")
    String latestTrackingAt,
    @Schema(description = "是否异常")
    boolean abnormalFlag
) {
}
