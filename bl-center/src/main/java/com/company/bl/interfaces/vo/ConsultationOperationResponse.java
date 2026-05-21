package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConsultationOperationResponse", description = "Consultation operation result")
public record ConsultationOperationResponse(
    @Schema(description = "Consultation ID")
    String consultationId,
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "Consultation status")
    String status
) {
}
