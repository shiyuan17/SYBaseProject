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
    @Schema(description = "待转运时间")
    String toBeTransportedAt,
    @Schema(description = "交接完成时间")
    String handedOverAt,
    @Schema(description = "标本条码列表")
    List<String> specimenBarcodes
) {
}
