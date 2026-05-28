package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SpecimenRemovalPageResponse", description = "标本离体工作台分页响应")
public record SpecimenRemovalPageResponse(
    @Schema(description = "列表项")
    List<SpecimenRemovalItemResponse> items,
    @Schema(description = "当前页")
    int page,
    @Schema(description = "页大小")
    int size,
    @Schema(description = "总数")
    long total,
    @Schema(description = "统计信息")
    SpecimenRemovalSummaryResponse summary
) {
}
