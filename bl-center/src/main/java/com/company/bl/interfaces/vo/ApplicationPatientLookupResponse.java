package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApplicationPatientLookupResponse", description = "申请单患者查询结果")
public record ApplicationPatientLookupResponse(
    @Schema(description = "患者主键 ID")
    String patientId,
    @Schema(description = "患者展示编号")
    String patientIdentifier,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "患者性别")
    String patientGender,
    @Schema(description = "患者年龄")
    String patientAge
) {
}
