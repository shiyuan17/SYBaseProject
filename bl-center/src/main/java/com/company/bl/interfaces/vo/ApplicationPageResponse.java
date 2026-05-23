package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ApplicationPageResponse", description = "病理申请单分页结果")
public record ApplicationPageResponse(
    @Schema(description = "列表数据")
    List<ApplicationListItemResponse> items,
    @Schema(description = "当前页码")
    int page,
    @Schema(description = "每页条数")
    int size,
    @Schema(description = "总记录数")
    long total
) {
}
