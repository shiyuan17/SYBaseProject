package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CreateMedicalOrderQcEvaluationRequest", description = "Create medical order QC evaluation request")
@RejectLegacyOperatorFields
public class CreateMedicalOrderQcEvaluationRequest {

    @Schema(description = "Target slide ID; omit for legacy order-level evaluation")
    @Size(max = 64)
    private String slideId;

    @Schema(description = "Expected current evaluation version for optimistic locking")
    private Integer expectedVersion;

    @Schema(description = "QC aspect", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 32)
    private String qcAspect;

    @Schema(description = "Total score", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer totalScore;

    @Schema(description = "Grade", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 32)
    private String grade;

    @Schema(description = "Evaluation reason")
    @Size(max = 1000)
    private String evaluationReason;

    @Schema(description = "Processing action")
    @Size(max = 32)
    private String processingAction;

    @Schema(description = "Detail payload")
    private JsonNode detailPayload;

    @Schema(description = "Terminal code")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "Remarks")
    @Size(max = 500)
    private String remarks;
}
