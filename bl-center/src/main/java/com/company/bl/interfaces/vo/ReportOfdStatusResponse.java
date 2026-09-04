package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportOfdStatusResponse", description = "报告 OFD 文件准备状态")
public record ReportOfdStatusResponse(
    @Schema(description = "准备状态", allowableValues = {"READY", "GENERATING", "FAILED"})
    String status,
    @Schema(description = "建议再次查询的等待时间，单位毫秒")
    int retryAfterMs,
    @Schema(description = "可展示的状态说明")
    String message
) {
}
