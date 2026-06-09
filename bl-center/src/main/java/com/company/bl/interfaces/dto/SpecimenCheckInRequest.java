package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SpecimenCheckInRequest", description = "标本入库请求")
public class SpecimenCheckInRequest {

    @Schema(description = "核对操作人登录确认 token；非当前登录人操作时传入")
    private String operatorVerificationToken;

    @Schema(description = "标本条码")
    @Size(max = 128)
    private String specimenBarcode;

    @Schema(description = "入库人用户 ID")
    @Size(max = 64)
    private String operatorUserId;

    @Schema(description = "入库人姓名")
    @Size(max = 100)
    private String operatorName;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
