package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportOfdArtifactResponse", description = "病理报告 OFD 存档记录")
public record ReportOfdArtifactResponse(
    @Schema(description = "存档 ID")
    String artifactId,
    @Schema(description = "报告 ID")
    String reportId,
    @Schema(description = "报告版本号")
    Integer versionNo,
    @Schema(description = "存档格式")
    String artifactFormat,
    @Schema(description = "文件名")
    String fileName,
    @Schema(description = "内容类型")
    String contentType,
    @Schema(description = "文件字节数")
    long byteSize,
    @Schema(description = "文件 SHA-256")
    String sha256,
    @Schema(description = "生成时间")
    String generatedAt,
    @Schema(description = "历史存档下载地址")
    String downloadUrl
) {
}
