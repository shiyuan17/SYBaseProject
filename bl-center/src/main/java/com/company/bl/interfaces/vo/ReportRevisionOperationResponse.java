package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportRevisionOperationResponse", description = "Report revision operation result")
public record ReportRevisionOperationResponse(
    @Schema(description = "Revision request ID")
    String requestId,
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "Report ID")
    String reportId,
    @Schema(description = "Request status")
    String requestStatus,
    @Schema(description = "Approved version number")
    Integer approvedVersionNo
) {
}
