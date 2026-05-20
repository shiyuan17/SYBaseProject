package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DiagnosticTaskOperationResponse", description = "诊断任务动作结果")
public record DiagnosticTaskOperationResponse(
    @Schema(description = "任务 ID")
    String taskId,
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病例状态")
    String caseStatus,
    @Schema(description = "任务状态")
    String taskStatus
) {
}
