package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "TransportOrderOperatorRequest", description = "转运单操作人请求")
@RejectLegacyOperatorFields
public class TransportOrderOperatorRequest {



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;
}