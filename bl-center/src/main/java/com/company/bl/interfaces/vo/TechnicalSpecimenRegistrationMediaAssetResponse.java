package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TechnicalSpecimenRegistrationMediaAssetResponse", description = "技术标本登记图片区附件")
public record TechnicalSpecimenRegistrationMediaAssetResponse(
    @Schema(description = "附件 ID")
    String assetId,
    @Schema(description = "文件名")
    String fileName,
    @Schema(description = "文件 URL")
    String fileUrl,
    @Schema(description = "上传时间")
    String capturedAt
) {
}
