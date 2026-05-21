package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CreateMedicalOrderRequest", description = "Create medical order request")
public class CreateMedicalOrderRequest {

    @Schema(description = "Case ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Schema(description = "Order type", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 32)
    private String orderType;

    @Schema(description = "Order content", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 1000)
    private String orderContent;

    @Schema(description = "Operator user ID")
    @Size(max = 64)
    private String operatorUserId;

    @Schema(description = "Operator name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Schema(description = "Terminal code")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "Remarks")
    @Size(max = 500)
    private String remarks;
}
