package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SpecimenRemovalSummaryResponse", description = "标本离体工作台统计")
public record SpecimenRemovalSummaryResponse(
    @Schema(description = "总数")
    long totalCount,
    @Schema(description = "已离体")
    long confirmedCount,
    @Schema(description = "待离体")
    long pendingCount,
    @Schema(description = "异常数")
    long abnormalCount
) {
}
