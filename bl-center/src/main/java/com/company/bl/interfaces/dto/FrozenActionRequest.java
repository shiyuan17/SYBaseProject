package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "FrozenActionRequest", description = "冰冻流程动作请求")
public class FrozenActionRequest {

    @Schema(description = "终端编码")
    private String terminalCode;

    @Schema(description = "备注")
    private String remarks;
}
