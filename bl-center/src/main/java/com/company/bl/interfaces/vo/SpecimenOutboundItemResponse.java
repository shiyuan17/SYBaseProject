package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SpecimenOutboundItemResponse", description = "标本出库列表项")
public record SpecimenOutboundItemResponse(
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "转运单 ID")
    String transportOrderId,
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "标本条码")
    String barcode,
    @Schema(description = "标本编号")
    String specimenNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "患者性别")
    String patientGender,
    @Schema(description = "病人 ID")
    String patientId,
    @Schema(description = "住院号")
    String inpatientNo,
    @Schema(description = "手术间")
    String surgeryName,
    @Schema(description = "标本名称")
    String specimenName,
    @Schema(description = "标本状态")
    String specimenStatus,
    @Schema(description = "固定状态")
    String fixationStatus,
    @Schema(description = "入库状态")
    String checkInStatus,
    @Schema(description = "标本确认时间")
    String specimenConfirmedAt,
    @Schema(description = "送检科室 ID")
    String submittingDepartmentId,
    @Schema(description = "送检科室")
    String submittingDepartmentName,
    @Schema(description = "添加时间")
    String registeredAt,
    @Schema(description = "添加人")
    String registeredByName,
    @Schema(description = "出库时间")
    String outboundAt,
    @Schema(description = "出库人")
    String outboundUserName
) {
}
