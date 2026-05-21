package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PendingMedicalOrderResponse", description = "Pending medical order item")
public record PendingMedicalOrderResponse(
    @Schema(description = "Order ID")
    String orderId,
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "Pathology number")
    String pathologyNo,
    @Schema(description = "Application number")
    String applicationNo,
    @Schema(description = "Patient name")
    String patientName,
    @Schema(description = "Order number")
    String orderNumber,
    @Schema(description = "Order type")
    String orderType,
    @Schema(description = "Order content")
    String orderContent,
    @Schema(description = "Execution scope")
    String executionScope,
    @Schema(description = "Billing status")
    String billingStatus,
    @Schema(description = "Order status")
    String status,
    @Schema(description = "Doctor name")
    String doctorName,
    @Schema(description = "Executor name")
    String executorName,
    @Schema(description = "Order date")
    String orderDate,
    @Schema(description = "Accepted at")
    String acceptedAt,
    @Schema(description = "Completed at")
    String completedAt,
    @Schema(description = "Cancelled at")
    String cancelledAt,
    @Schema(description = "Remarks")
    String remarks
) {
}
