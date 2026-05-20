package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GrossingResponse", description = "取材完成响应")
public record GrossingResponse(
    @Schema(description = "技术任务 ID")
    String taskId,
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病例状态")
    String caseStatus,
    @Schema(description = "新建脱水任务数量")
    int createdDehydrationTaskCount
) {
}
