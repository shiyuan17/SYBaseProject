package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PendingDiagnosticTaskResponse", description = "待处理诊断任务")
public record PendingDiagnosticTaskResponse(
    @Schema(description = "任务 ID")
    String id,
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "患者 ID")
    String patientId,
    @Schema(description = "展示用患者 ID，来源于申请登记工作台 ID号")
    String patientIdDisplay,
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "送检类型")
    String applicationType,
    @Schema(description = "检查项目")
    String checkItem,
    @Schema(description = "蜡块数")
    Integer blockCount,
    @Schema(description = "送检科室")
    String submittingDepartmentName,
    @Schema(description = "送检标本")
    String specimenName,
    @Schema(description = "任务类型")
    String taskType,
    @Schema(description = "任务状态")
    String taskStatus,
    @Schema(description = "报告生命周期状态")
    String reportStatus,
    @Schema(description = "责任诊断医生用户 ID")
    String diagnosisDoctorUserId,
    @Schema(description = "责任诊断医生姓名")
    String diagnosisDoctorName,
    @Schema(description = "初诊医生用户 ID")
    String primaryDoctorUserId,
    @Schema(description = "初诊医生姓名")
    String primaryDoctorName,
    @Schema(description = "审核医生用户 ID")
    String reviewerUserId,
    @Schema(description = "审核医生姓名")
    String reviewerName,
    @Schema(description = "派发时间")
    String assignedAt,
    @Schema(description = "接单时间")
    String acceptedAt,
    @Schema(description = "完成时间")
    String completedAt,
    @Schema(description = "备注")
    String remarks
) {
}
