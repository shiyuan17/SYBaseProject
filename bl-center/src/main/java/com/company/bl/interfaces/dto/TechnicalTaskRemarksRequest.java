package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "TechnicalTaskRemarksRequest", description = "技术任务备注更新请求")
public class TechnicalTaskRemarksRequest {

    @Schema(description = "任务备注")
    private String remarks;

    @Schema(description = "主班备注")
    private String productionRemarks;

    @Schema(description = "终端编码")
    private String terminalCode;
}
