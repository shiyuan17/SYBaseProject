package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SpecimenManagementSummaryResponse", description = "Specimen management summary")
public record SpecimenManagementSummaryResponse(
    @Schema(description = "Total specimen count under current filters")
    long totalCount,
    @Schema(description = "Printed label count under current filters")
    long labelPrintedCount,
    @Schema(description = "Pending label count under current filters")
    long pendingLabelCount,
    @Schema(description = "Abnormal specimen count under current filters")
    long abnormalCount
) {
}