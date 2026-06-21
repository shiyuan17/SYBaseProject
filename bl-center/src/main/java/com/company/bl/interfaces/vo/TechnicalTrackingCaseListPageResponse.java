package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "TechnicalTrackingCaseListPageResponse", description = "技术追踪病例列表分页结果")
public record TechnicalTrackingCaseListPageResponse(
    @Schema(description = "当前页数据")
    List<TechnicalTrackingCaseListItemResponse> items,
    @Schema(description = "页码，从 1 开始")
    int page,
    @Schema(description = "每页条数")
    int size,
    @Schema(description = "总记录数")
    long total
) {
}
