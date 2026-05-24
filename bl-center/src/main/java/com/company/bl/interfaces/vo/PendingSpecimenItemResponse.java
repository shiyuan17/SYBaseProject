package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PendingSpecimenItemResponse", description = "待处理标本条目")
public record PendingSpecimenItemResponse(
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "送检科室 ID")
    String submittingDepartmentId,
    @Schema(description = "送检科室名称")
    String submittingDepartmentName,
    @Schema(description = "转运单 ID")
    String transportOrderId,
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "标本号")
    String specimenNo,
    @Schema(description = "标本条码")
    String barcode,
    @Schema(description = "容器名称")
    String containerName,
    @Schema(description = "容器数量")
    Integer containerCount,
    @Schema(description = "标本状态")
    String specimenStatus,
    @Schema(description = "固定状态")
    String fixationStatus,
    @Schema(description = "登记时间")
    String registeredAt,
    @Schema(description = "最近追踪时间")
    String latestTrackingAt,
    @Schema(description = "是否异常")
    boolean abnormalFlag
) {
}
