package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DehydrationBatchResponse", description = "脱水批次响应")
public record DehydrationBatchResponse(
    @Schema(description = "批次 ID")
    String batchId,
    @Schema(description = "批次号")
    String batchNo,
    @Schema(description = "批次状态")
    String batchStatus,
    @Schema(description = "批次内任务数量")
    int taskCount
) {
}
