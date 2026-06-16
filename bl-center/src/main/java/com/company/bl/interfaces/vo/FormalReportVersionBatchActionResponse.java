package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "FormalReportVersionBatchActionResponse", description = "正式报告批量动作结果")
public record FormalReportVersionBatchActionResponse(
    @Schema(description = "总数")
    int totalCount,
    @Schema(description = "成功数")
    int successCount,
    @Schema(description = "失败数")
    int failureCount,
    @Schema(description = "逐项结果")
    List<ItemResult> items
) {
    @Schema(name = "FormalReportVersionBatchActionItemResult", description = "正式报告批量动作逐项结果")
    public record ItemResult(
        @Schema(description = "版本 ID")
        String versionId,
        @Schema(description = "是否成功")
        boolean success,
        @Schema(description = "结果说明")
        String message
    ) {
    }
}
