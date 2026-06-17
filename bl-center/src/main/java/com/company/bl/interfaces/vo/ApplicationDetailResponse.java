package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ApplicationDetailResponse", description = "病理申请单详情")
public record ApplicationDetailResponse(
    @Schema(description = "申请单 ID")
    String id,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "患者 ID")
    String patientId,
    @Schema(description = "展示用患者 ID，来源于申请登记工作台 ID号")
    String patientIdDisplay,
    @Schema(description = "患者展示编号")
    String patientIdentifier,
    @Schema(description = "患者核对状态")
    String patientCheckStatus,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "患者性别")
    String patientGender,
    @Schema(description = "患者年龄")
    String patientAge,
    @Schema(description = "申请类型")
    String applicationType,
    @Schema(description = "申请单状态")
    String status,
    @Schema(description = "申请单表单状态")
    String applicationFormStatus,
    @Schema(description = "外部单号")
    String externalOrderNo,
    @Schema(description = "第三方来源标识")
    String thirdPartySource,
    @Schema(description = "来源医院 ID")
    String sourceHospitalId,
    @Schema(description = "来源医院名称")
    String sourceHospitalName,
    @Schema(description = "送检科室 ID")
    String submittingDepartmentId,
    @Schema(description = "送检科室名称")
    String submittingDepartmentName,
    @Schema(description = "送检医生用户 ID")
    String submittingDoctorUserId,
    @Schema(description = "送检医生姓名")
    String submittingDoctorName,
    @Schema(description = "临床诊断")
    String clinicalDiagnosis,
    @Schema(description = "临床症状")
    String clinicalSymptom,
    @Schema(description = "送检部位")
    String specimenSite,
    @Schema(description = "申请日期")
    String applicationDate,
    @Schema(description = "送检日期")
    String submissionDate,
    @Schema(description = "离体时间")
    String specimenRemovalTime,
    @Schema(description = "固定完成时间")
    String fixationCompletedAt,
    @Schema(description = "标本确认时间")
    String specimenConfirmedAt,
    @Schema(description = "当前流程节点")
    String currentNode,
    @Schema(description = "是否存在异常标记")
    boolean abnormalFlag,
    @Schema(description = "是否可编辑")
    boolean editable,
    @Schema(description = "是否可删除")
    boolean deletable,
    @Schema(description = "是否已作废")
    boolean voided,
    @Schema(description = "操作禁用原因")
    String operationDisabledReason,
    @Schema(description = "报告状态")
    String reportStatus,
    @Schema(description = "是否已签发报告")
    boolean reportIssued,
    @Schema(description = "接收异常摘要")
    String receiptAbnormalSummary,
    @Schema(description = "未接收数量")
    int unreceivedCount,
    @Schema(description = "标本摘要列表")
    List<SpecimenSummaryResponse> specimens,
    @Schema(description = "最近追踪事件列表")
    List<TrackingEventResponse> recentEvents,
    @Schema(description = "备注")
    String remarks,
    @Schema(description = "创建时间")
    String createdAt,
    @Schema(description = "更新时间")
    String updatedAt
) {
}
