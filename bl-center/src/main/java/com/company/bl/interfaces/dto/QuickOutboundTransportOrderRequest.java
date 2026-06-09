package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "QuickOutboundTransportOrderRequest", description = "按标本流水号快捷出库请求")
@RejectLegacyOperatorFields
public class QuickOutboundTransportOrderRequest {

    @Schema(description = "核对操作人登录确认 token；非当前登录人操作时传入")
    private String operatorVerificationToken;

    @Schema(description = "标识类型，仅支持 SPECIMEN_NO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Pattern(regexp = "SPECIMEN_NO")
    private String identifierType;

    @Schema(description = "标识值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String identifier;

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
