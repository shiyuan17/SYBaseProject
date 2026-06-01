package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SlideQcEvaluationResponse", description = "切片质控评价结果")
public record SlideQcEvaluationResponse(
    @Schema(description = "质控记录 ID")
    String qcEvaluationId,
    @Schema(description = "玻片 ID")
    String slideId,
    @Schema(description = "评价结果")
    String evaluationResult,
    @Schema(description = "玻片质控状态")
    String qualityStatus
) {
}
