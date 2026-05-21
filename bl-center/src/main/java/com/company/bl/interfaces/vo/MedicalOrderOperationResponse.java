package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MedicalOrderOperationResponse", description = "Medical order operation result")
public record MedicalOrderOperationResponse(
    @Schema(description = "Order ID")
    String orderId,
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "Order number")
    String orderNumber,
    @Schema(description = "Order status")
    String status
) {
}
