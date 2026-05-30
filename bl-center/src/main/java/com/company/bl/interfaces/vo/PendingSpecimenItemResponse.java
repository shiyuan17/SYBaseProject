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
    @Schema(description = "固定开始时间")
    String fixationStartedAt,
    @Schema(description = "固定完成时间")
    String fixationCompletedAt,
    @Schema(description = "固定液类型")
    String fixationLiquidType,
    @Schema(description = "固定核对人用户 ID")
    String fixationOperatorUserId,
    @Schema(description = "固定核对人姓名")
    String fixationOperatorName,
    @Schema(description = "核对状态")
    String verificationStatus,
    @Schema(description = "开始核对时间")
    String verificationStartedAt,
    @Schema(description = "完成核对时间")
    String verificationCompletedAt,
    @Schema(description = "标本确认时间")
    String specimenConfirmedAt,
    @Schema(description = "入库状态")
    String checkInStatus,
    @Schema(description = "入库时间")
    String checkedInAt,
    @Schema(description = "入库操作人")
    String checkedInByName,
    @Schema(description = "异常类型")
    String abnormalType,
    @Schema(description = "提醒计数")
    int reminderCount,
    @Schema(description = "未接收数量")
    int unreceivedCount,
    @Schema(description = "批次级异常标记")
    boolean batchAbnormalFlag,
    @Schema(description = "登记时间")
    String registeredAt,
    @Schema(description = "最近追踪时间")
    String latestTrackingAt,
    @Schema(description = "是否异常")
    boolean abnormalFlag
) {
}
