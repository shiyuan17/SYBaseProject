package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EmbeddingQualityReviewResponse", description = "包埋质量评价调整响应")
public record EmbeddingQualityReviewResponse(
    TechnicalTrackingResponse.EmbeddingRecordSummary record,
    String reworkType,
    String reworkStatus
) {
}
