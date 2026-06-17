package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TechnicalSpecimenRegistrationBasicInfoResponse", description = "技术标本登记基础信息")
public record TechnicalSpecimenRegistrationBasicInfoResponse(
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "性别")
    String patientGender,
    @Schema(description = "年龄")
    String patientAge,
    @Schema(description = "患者 ID")
    String patientId,
    @Schema(description = "患者展示 ID")
    String patientIdDisplay,
    @Schema(description = "住院号")
    String inpatientNo,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "申请科室")
    String submittingDepartmentName,
    @Schema(description = "申请医生")
    String submittingDoctorName,
    @Schema(description = "送检日期")
    String submissionDate,
    @Schema(description = "离体时间")
    String specimenRemovalTime,
    @Schema(description = "固定时间")
    String fixationTime,
    @Schema(description = "送检类型")
    String applicationType,
    @Schema(description = "病理检查号")
    String pathologyNo,
    @Schema(description = "登记状态")
    String registrationStatus
) {
}
