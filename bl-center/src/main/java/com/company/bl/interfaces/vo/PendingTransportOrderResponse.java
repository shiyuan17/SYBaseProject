package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PendingTransportOrderResponse", description = "待处理转运单条目")
public record PendingTransportOrderResponse(
    @Schema(description = "转运单 ID")
    String id,
    @Schema(description = "转运单号")
    String transportOrderNo,
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "交接科室名称")
    String handoverDepartmentName,
    @Schema(description = "接收科室名称")
    String receiverDepartmentName,
    @Schema(description = "转运状态")
    String status,
    @Schema(description = "提醒计数")
    int reminderCount,
    @Schema(description = "未接收数量")
    int unreceivedCount,
    @Schema(description = "批次级异常标记")
    boolean batchAbnormalFlag,
    @Schema(description = "待转运时间")
    String toBeTransportedAt,
    @Schema(description = "交接完成时间")
    String handedOverAt,
    @Schema(description = "出库人用户 ID")
    String outboundUserId,
    @Schema(description = "出库人姓名")
    String outboundUserName,
    @Schema(description = "标本条码列表")
    List<String> specimenBarcodes
) {
}
