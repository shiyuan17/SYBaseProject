package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "TechnicalTrackingCaseListItemResponse", description = "技术追踪病例列表项")
public record TechnicalTrackingCaseListItemResponse(
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "患者展示 ID")
    String patientIdDisplay,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "送检类型")
    String applicationType,
    @Schema(description = "送检科室")
    String submittingDepartmentName,
    @Schema(description = "病例状态")
    String caseStatus,
    @Schema(description = "最近技术活动时间，ISO-8601")
    String latestActivityAt,
    @Schema(description = "命中的技术活动类型")
    List<String> matchedActivityTypes
) {
}
