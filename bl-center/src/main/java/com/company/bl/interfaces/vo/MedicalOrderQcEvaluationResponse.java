package com.company.bl.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MedicalOrderQcEvaluationResponse", description = "Medical order QC evaluation response")
public record MedicalOrderQcEvaluationResponse(
    @Schema(description = "Order ID")
    String orderId,
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "QC aspect")
    String qcAspect,
    @Schema(description = "Total score")
    Integer totalScore,
    @Schema(description = "Grade")
    String grade,
    @Schema(description = "Evaluation reason")
    String evaluationReason,
    @Schema(description = "Processing action")
    String processingAction,
    @Schema(description = "Rework type")
    String reworkType,
    @Schema(description = "Rework order ID")
    String reworkOrderId,
    @Schema(description = "Remarks")
    String remarks,
    @Schema(description = "Evaluator name")
    String evaluatorName,
    @Schema(description = "Evaluated at")
    String evaluatedAt,
    @Schema(description = "Detail payload")
    JsonNode detailPayload
) {
}
