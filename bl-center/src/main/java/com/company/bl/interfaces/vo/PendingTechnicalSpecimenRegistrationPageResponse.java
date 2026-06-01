package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PendingTechnicalSpecimenRegistrationPageResponse", description = "待技术登记病例分页结果")
public record PendingTechnicalSpecimenRegistrationPageResponse(
    @Schema(description = "分页数据")
    List<PendingTechnicalSpecimenRegistrationResponse> items,
    @Schema(description = "页码")
    int page,
    @Schema(description = "页大小")
    int size,
    @Schema(description = "总条数")
    long total
) {
}
