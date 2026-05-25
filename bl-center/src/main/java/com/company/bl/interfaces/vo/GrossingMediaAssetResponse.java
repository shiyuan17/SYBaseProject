package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GrossingMediaAssetResponse", description = "取材影像上传响应")
public record GrossingMediaAssetResponse(
    @Schema(description = "可访问的文件 URL")
    String fileUrl,
    @Schema(description = "原始文件名")
    String fileName,
    @Schema(description = "文件内容类型")
    String contentType,
    @Schema(description = "文件大小，单位字节")
    long size
) {
}
