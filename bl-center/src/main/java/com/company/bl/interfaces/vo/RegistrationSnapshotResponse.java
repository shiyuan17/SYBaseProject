package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegistrationSnapshotResponse", description = "最近一次标本登记快照")
public record RegistrationSnapshotResponse(
    @Schema(description = "采集场景")
    String collectionScene,
    @Schema(description = "登记人用户 ID")
    String operatorUserId,
    @Schema(description = "登记人姓名")
    String operatorName,
    @Schema(description = "打印机编码")
    String printerCode,
    @Schema(description = "终端编码")
    String terminalCode,
    @Schema(description = "登记备注")
    String remarks
) {
}
