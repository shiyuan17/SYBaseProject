package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "FrozenPhoneBackRequest", description = "冰冻初步结果/电话回报请求")
public class FrozenPhoneBackRequest {

    @Schema(description = "终端编码")
    private String terminalCode;

    @Schema(description = "备注")
    private String remarks;

    @NotBlank
    @Schema(description = "初步结果", requiredMode = Schema.RequiredMode.REQUIRED)
    private String preliminaryResult;
}
