package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TaskOperationResponse", description = "技术任务操作结果")
public record TaskOperationResponse(
    @Schema(description = "技术任务 ID")
    String taskId,
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病例状态")
    String caseStatus,
    @Schema(description = "任务状态")
    String taskStatus
) {
}
