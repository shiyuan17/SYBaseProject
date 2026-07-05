package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "FrozenParaffinCompareRequest", description = "冰石对比请求")
public class FrozenParaffinCompareRequest {

    @Schema(description = "终端编码")
    private String terminalCode;

    @Schema(description = "备注")
    private String remarks;

    @NotBlank
    @Schema(description = "对比结果", requiredMode = Schema.RequiredMode.REQUIRED)
    private String compareStatus;

    @NotBlank
    @Schema(description = "对比结论", requiredMode = Schema.RequiredMode.REQUIRED)
    private String compareSummary;
}
