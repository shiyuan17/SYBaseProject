package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PendingTechnicalTaskResponse", description = "待处理技术任务")
public record PendingTechnicalTaskResponse(
    @Schema(description = "技术任务 ID")
    String id,
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "任务类型")
    String taskType,
    @Schema(description = "任务状态")
    String taskStatus,
    @Schema(description = "对象类型")
    String objectType,
    @Schema(description = "对象 ID")
    String objectId,
    @Schema(description = "扩展载荷")
    String payload,
    @Schema(description = "备注")
    String remarks,
    @Schema(description = "创建时间")
    String createdAt,
    @Schema(description = "开始时间")
    String startedAt,
    @Schema(description = "完成时间")
    String completedAt,
    @Schema(description = "超时截止时间")
    String deadlineAt,
    @Schema(description = "超时规则编码")
    String timeoutRuleCode,
    @Schema(description = "是否已超时")
    boolean timedOut
) {
}
