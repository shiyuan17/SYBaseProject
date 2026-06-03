package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PendingTechnicalSpecimenRegistrationResponse", description = "技术登记病例")
public record PendingTechnicalSpecimenRegistrationResponse(
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "患者 ID")
    String patientId,
    @Schema(description = "住院号")
    String inpatientNo,
    @Schema(description = "送检类型")
    String applicationType,
    @Schema(description = "申请科室")
    String submittingDepartmentName,
    @Schema(description = "检查项目")
    String checkItem,
    @Schema(description = "登记人")
    String registeredByName,
    @Schema(description = "登记状态")
    String registrationStatus,
    @Schema(description = "接收时间")
    String receivedAt,
    @Schema(description = "登记时间")
    String registeredAt
) {
}
