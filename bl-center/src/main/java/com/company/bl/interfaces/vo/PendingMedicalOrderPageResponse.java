package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PendingMedicalOrderPageResponse", description = "Pending medical order page")
public record PendingMedicalOrderPageResponse(
    @Schema(description = "Items")
    List<PendingMedicalOrderResponse> items,
    @Schema(description = "Page number")
    int page,
    @Schema(description = "Page size")
    int size,
    @Schema(description = "Total records")
    long total
) {
}
