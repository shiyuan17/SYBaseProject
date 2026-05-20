package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FixationResponse", description = "固定处理响应")
public record FixationResponse(
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "标本条码")
    String barcode,
    @Schema(description = "固定状态")
    String fixationStatus
) {
}
