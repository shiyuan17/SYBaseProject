package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "TransportOrderOperatorRequest", description = "转运单操作人请求")
public class TransportOrderOperatorRequest {

    @Schema(description = "操作人用户 ID")
    @Size(max = 64)
    private String operatorUserId;

    @Schema(description = "操作人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;
}
