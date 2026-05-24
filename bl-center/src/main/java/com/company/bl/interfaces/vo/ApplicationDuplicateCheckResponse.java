package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ApplicationDuplicateCheckResponse", description = "重复申请预警结果")
public record ApplicationDuplicateCheckResponse(
    @Schema(description = "命中记录")
    List<ApplicationDuplicateCheckItemResponse> items,
    @Schema(description = "建议动作，ALLOW/BLOCK/CONFIRM")
    String suggestedAction
) {
}
