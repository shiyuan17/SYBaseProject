package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TechnicalSpecimenRegistrationCheckItemResponse", description = "技术标本登记检查项目")
public record TechnicalSpecimenRegistrationCheckItemResponse(
    @Schema(description = "序号")
    int sequenceNo,
    @Schema(description = "检查项目")
    String name
) {
}
