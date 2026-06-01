package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TransportOrderResponse", description = "转运单响应")
public record TransportOrderResponse(
    @Schema(description = "转运单 ID")
    String id,
    @Schema(description = "转运单号")
    String transportOrderNo,
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "转运状态")
    String status,
    @Schema(description = "交接人姓名")
    String handoverUserName,
    @Schema(description = "接收人姓名")
    String receiverUserName,
    @Schema(description = "出库人用户 ID")
    String outboundUserId,
    @Schema(description = "出库人姓名")
    String outboundUserName,
    @Schema(description = "待转运时间")
    String toBeTransportedAt,
    @Schema(description = "交接完成时间")
    String handedOverAt
) {
}
