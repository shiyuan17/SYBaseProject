package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "FrozenRemainingTissueRequest", description = "剩余组织处理请求")
public class FrozenRemainingTissueRequest {

    @Schema(description = "终端编码")
    private String terminalCode;

    @Schema(description = "备注")
    private String remarks;

    @NotBlank
    @Schema(description = "剩余组织处理状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String remainingTissueStatus;
}
