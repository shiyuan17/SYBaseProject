package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FixationResponse", description = "固定处理响应")
public record FixationResponse(
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "标本条码")
    String barcode,
    @Schema(description = "固定状态")
    String fixationStatus,
    @Schema(description = "固定完成时间")
    String fixationCompletedAt,
    @Schema(description = "固定核对人用户 ID")
    String operatorUserId,
    @Schema(description = "固定核对人姓名")
    String operatorName,
    @Schema(description = "固定液类型")
    String fixationLiquidType
) {
}
