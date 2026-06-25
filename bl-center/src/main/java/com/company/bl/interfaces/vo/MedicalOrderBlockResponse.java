package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MedicalOrderBlockResponse", description = "Medical-order-only block result")
public record MedicalOrderBlockResponse(
    @Schema(description = "Medical-order-only block ID")
    String medicalOrderBlockId,
    @Schema(description = "Normalized block number")
    String blockNo
) {
}
