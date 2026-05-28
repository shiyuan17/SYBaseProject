package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SpecimenRemovalConfirmResponse", description = "标本离体确认结果")
public record SpecimenRemovalConfirmResponse(
    @Schema(description = "标本ID")
    String specimenId,
    @Schema(description = "标本条码")
    String barcode,
    @Schema(description = "离体时间")
    String specimenRemovalAt,
    @Schema(description = "离体操作人")
    String operatorName
) {
}
