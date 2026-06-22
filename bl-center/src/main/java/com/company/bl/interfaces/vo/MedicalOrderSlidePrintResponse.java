package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "MedicalOrderSlidePrintResponse", description = "Medical order slide print result")
public record MedicalOrderSlidePrintResponse(
    @Schema(description = "Order ID")
    String orderId,
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "Order number")
    String orderNumber,
    @Schema(description = "Order status")
    String status,
    @Schema(description = "Printed at")
    String printedAt,
    @Schema(description = "Printed by name")
    String printedByName,
    @Schema(description = "Labels")
    List<MedicalOrderSlidePrintLabelResponse> labels
) {
    public record MedicalOrderSlidePrintLabelResponse(
        @Schema(description = "Slide ID")
        String slideId,
        @Schema(description = "Slide number")
        String slideNo,
        @Schema(description = "Pathology number")
        String pathologyNo,
        @Schema(description = "Patient name")
        String patientName,
        @Schema(description = "Patient ID")
        String patientId,
        @Schema(description = "Specimen number")
        String specimenNo,
        @Schema(description = "Block number")
        String blockNo
    ) {
    }
}
