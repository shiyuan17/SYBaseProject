package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FormalReportVersionListItemResponse", description = "病例正式报告列表项")
public record FormalReportVersionListItemResponse(
    @Schema(description = "版本 ID")
    String versionId,
    @Schema(description = "报告 ID")
    String reportId,
    @Schema(description = "报告号")
    String reportNo,
    @Schema(description = "版本号")
    Integer versionNo,
    @Schema(description = "生命周期状态")
    String versionStatus,
    @Schema(description = "签发医生")
    String signedByName,
    @Schema(description = "签发时间")
    String signedAt,
    @Schema(description = "发布时间")
    String publishedAt,
    @Schema(description = "打印状态")
    String printStatus,
    @Schema(description = "打印时间")
    String printedAt,
    @Schema(description = "发放状态")
    String deliveryStatus,
    @Schema(description = "发放时间")
    String issuedAt,
    @Schema(description = "回收时间")
    String recalledAt
) {
}
