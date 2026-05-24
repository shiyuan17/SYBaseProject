package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SpecimenManagementPageResponse", description = "Specimen management page response")
public record SpecimenManagementPageResponse(
    @Schema(description = "List items")
    List<SpecimenManagementItemResponse> items,
    @Schema(description = "Current page")
    int page,
    @Schema(description = "Page size")
    int size,
    @Schema(description = "Total records")
    long total,
    @Schema(description = "Summary cards")
    SpecimenManagementSummaryResponse summary
) {
}