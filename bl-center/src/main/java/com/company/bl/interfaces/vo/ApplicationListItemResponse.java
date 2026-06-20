package com.company.bl.interfaces.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApplicationListItemResponse", description = "病理申请单列表项")
public record ApplicationListItemResponse(
    @Schema(description = "申请单 ID")
    String id,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "患者性别")
    String patientGender,
    @Schema(description = "患者年龄")
    String patientAge,
    @Schema(description = "申请单状态")
    String status,
    @Schema(description = "送检科室名称")
    String submittingDepartmentName,
    @Schema(description = "送检医生姓名")
    String submittingDoctorName,
    @Schema(description = "申请类型")
    String applicationType,
    @Schema(description = "申请单表单状态")
    String applicationFormStatus,
    @Schema(description = "当前流程节点")
    String currentNode,
    @Schema(description = "是否存在异常标记")
    boolean abnormalFlag,
    @Schema(description = "已登记标本数")
    int registeredSpecimenCount,
    @Schema(description = "标本号列表")
    List<String> specimenNos,
    @Schema(description = "最近一次标签状态")
    String latestLabelPrintStatus,
    @Schema(description = "是否可编辑")
    boolean editable,
    @Schema(description = "是否可删除")
    boolean deletable,
    @Schema(description = "是否已作废")
    boolean voided,
    @Schema(description = "操作禁用原因")
    String operationDisabledReason,
    @Schema(description = "申请日期")
    String applicationDate,
    @Schema(description = "送检日期")
    String submissionDate,
    @Schema(description = "创建时间")
    String createdAt,
    @Schema(description = "更新时间")
    String updatedAt
) {
}


