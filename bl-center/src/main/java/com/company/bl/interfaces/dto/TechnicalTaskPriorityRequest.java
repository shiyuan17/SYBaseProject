package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "TechnicalTaskPriorityRequest", description = "技术任务优先级调整请求")
@RejectLegacyOperatorFields
public class TechnicalTaskPriorityRequest {

    @Schema(description = "任务优先级", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 32)
    private String priority;

    @Schema(description = "生产备注")
    @Size(max = 500)
    private String productionRemarks;



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;
}