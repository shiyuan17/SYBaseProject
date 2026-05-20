package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PathologyReportOperationResponse", description = "病理报告动作结果")
public record PathologyReportOperationResponse(
    @Schema(description = "报告 ID")
    String reportId,
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "报告编号")
    String reportNo,
    @Schema(description = "报告状态")
    String reportStatus,
    @Schema(description = "版本号")
    Integer versionNo,
    @Schema(description = "版本状态")
    String versionStatus
) {
}
