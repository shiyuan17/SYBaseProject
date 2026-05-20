package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReworkOrderResponse", description = "返工单响应")
public record ReworkOrderResponse(
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "返工类型")
    String reworkType,
    @Schema(description = "返工单状态")
    String status
) {
}
