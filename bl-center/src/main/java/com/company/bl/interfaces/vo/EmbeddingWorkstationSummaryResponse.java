package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "EmbeddingWorkstationSummaryResponse", description = "包埋工作站汇总视图")
public record EmbeddingWorkstationSummaryResponse(
    @Schema(description = "工作日期")
    String workDate,
    @Schema(description = "待包埋数")
    int pendingCount,
    @Schema(description = "已包埋数")
    int completedCount,
    @Schema(description = "当日待处理任务")
    List<PendingTechnicalTaskResponse> pendingTasks,
    @Schema(description = "当日已包埋记录")
    List<TechnicalTrackingResponse.EmbeddingRecordSummary> completedRecords,
    @Schema(description = "当日日结清零状态")
    WorkstationDailyClearResponse dailyClear
) {
}
