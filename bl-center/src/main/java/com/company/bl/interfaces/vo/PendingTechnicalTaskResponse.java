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
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "患者 ID")
    String patientId,
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
    @Schema(description = "对象展示编号")
    String objectDisplayNo,
    @Schema(description = "蜡块号")
    String samplingBlockCode,
    @Schema(description = "蜡块名称")
    String samplingBlockDescription,
    @Schema(description = "取材操作人")
    String sampledByName,
    @Schema(description = "取材时间")
    String sampledAt,
    @Schema(description = "扩展载荷")
    String payload,
    @Schema(description = "任务优先级")
    String priority,
    @Schema(description = "当前节点")
    String currentNode,
    @Schema(description = "工作台编码")
    String stationCode,
    @Schema(description = "工作台名称")
    String stationName,
    @Schema(description = "责任技师用户 ID")
    String assignedToUserId,
    @Schema(description = "责任技师姓名")
    String assignedToName,
    @Schema(description = "期望完成时间")
    String expectedCompletedAt,
    @Schema(description = "生产备注")
    String productionRemarks,
    @Schema(description = "接收时间")
    String receivedAt,
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
