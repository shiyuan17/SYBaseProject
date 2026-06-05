package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "OutboundTransportOrderRequest", description = "转运单出库请求")
public class OutboundTransportOrderRequest {

    @Schema(description = "核对操作人登录确认 token", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String operatorVerificationToken;

    @Schema(description = "出库人用户 ID")
    @Size(max = 64)
    private String outboundUserId;

    @Schema(description = "出库人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String outboundUserName;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
