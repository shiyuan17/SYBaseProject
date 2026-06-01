package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TechnicalSpecimenRegistrationMediaAssetDeleteResponse", description = "技术标本登记图片区删除结果")
public record TechnicalSpecimenRegistrationMediaAssetDeleteResponse(
    @Schema(description = "附件 ID")
    String assetId,
    @Schema(description = "是否已删除")
    boolean deleted
) {
}
