package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SlideQcEvaluationCreateRequest", description = "切片质控评价请求")
@RejectLegacyOperatorFields
public class SlideQcEvaluationCreateRequest {

    @Schema(description = "病例 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Schema(description = "标本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String specimenId;

    @Schema(description = "玻片 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String slideId;

    @Schema(description = "质控类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 32)
    private String qcType;

    @Schema(description = "评价结果", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 32)
    private String evaluationResult;

    @Schema(description = "问题描述")
    @Size(max = 500)
    private String issueDescription;

    @Schema(description = "改进建议")
    @Size(max = 500)
    private String improvementSuggestion;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
