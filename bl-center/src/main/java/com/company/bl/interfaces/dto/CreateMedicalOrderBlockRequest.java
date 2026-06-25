package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CreateMedicalOrderBlockRequest", description = "Create medical-order-only block request")
@RejectLegacyOperatorFields
public class CreateMedicalOrderBlockRequest {

    @Schema(description = "Medical-order-only block number", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String blockNo;
}
