package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TechnicalSpecimenRegistrationMaterialResponse", description = "技术标本登记送检材料")
public record TechnicalSpecimenRegistrationMaterialResponse(
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "序号")
    int sequenceNo,
    @Schema(description = "标本类型")
    String specimenType,
    @Schema(description = "名称")
    String specimenName,
    @Schema(description = "来源部位")
    String sourcePart
) {
}
