package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "MedicalOrderBillingResponse", description = "Medical order billing action result")
public record MedicalOrderBillingResponse(
    @Schema(description = "Total processed count")
    int totalCount,
    @Schema(description = "Successful count")
    int successCount,
    @Schema(description = "Failed count")
    int failureCount,
    @Schema(description = "Per-order billing results")
    List<MedicalOrderBillingItemResponse> items
) {
    @Schema(name = "MedicalOrderBillingItemResponse", description = "Per-order billing result")
    public record MedicalOrderBillingItemResponse(
        @Schema(description = "Order ID")
        String orderId,
        @Schema(description = "Billing status")
        String billingStatus,
        @Schema(description = "Billing record ID")
        String billingRecordId,
        @Schema(description = "Result message")
        String message
    ) {
    }
}
