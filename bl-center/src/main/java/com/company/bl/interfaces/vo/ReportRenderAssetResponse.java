package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportRenderAssetResponse", description = "报告版式图片资源")
public record ReportRenderAssetResponse(
    String assetId,
    String caseId,
    String fileName,
    String fileUrl,
    String contentType,
    long byteSize
) {
}
