package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LabelPrintRetryResponse", description = "重打标签结果")
public record LabelPrintRetryResponse(
    @Schema(description = "标签打印批次号")
    String labelPrintBatchNo,
    @Schema(description = "重试总数")
    int retriedCount,
    @Schema(description = "成功数量")
    int successCount,
    @Schema(description = "失败数量")
    int failedCount,
    @Schema(description = "是否全部成功")
    boolean allSuccessful,
    @Schema(description = "执行结果说明")
    String message
) {
}
