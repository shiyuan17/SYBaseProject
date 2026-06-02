package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "EmbeddingQualityReviewRequest", description = "包埋质量评价调整请求")
@RejectLegacyOperatorFields
public class EmbeddingQualityReviewRequest {

    @Schema(description = "切片备注")
    @Size(max = 500)
    private String sliceNotice;

    @Schema(description = "评价等级")
    @Size(max = 32)
    private String evaluationLevel;

    @Schema(description = "取材评价")
    @Size(max = 500)
    private String samplingEvaluation;

    @Schema(description = "不合格原因")
    private List<@Size(max = 100) String> unqualifiedReasons;

    @Schema(description = "处理措施，示例：REGROSSING/OTHER")
    @Size(max = 32)
    private String treatmentAction;

    @Schema(description = "处理说明")
    @Size(max = 500)
    private String treatmentRemark;

    @Schema(description = "是否已通知取材人")
    private boolean notifiedGrossingOperator;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
