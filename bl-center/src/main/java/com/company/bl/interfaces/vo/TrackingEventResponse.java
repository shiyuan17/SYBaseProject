package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrackingEventResponse", description = "申请单追踪事件")
public record TrackingEventResponse(
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
    @Schema(description = "来源终端")
    String sourceTerminal,
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "标本编号")
    String specimenNo,
    @Schema(description = "标本条码")
    String specimenBarcode,
    @Schema(description = "事件内容")
    String eventContent,
    @Schema(description = "操作人 IP")
    String operatorIp,
    @Schema(description = "操作设备浏览器信息")
    String operatorDevice
) {
}
