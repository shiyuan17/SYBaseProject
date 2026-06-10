package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SpecimenConfirmRequest", description = "标本确认请求")
public class SpecimenConfirmRequest {

    @Schema(description = "标本 ID；优先于路径条码、请求条码和标本编号")
    @Size(max = 64)
    private String specimenId;

    @Schema(description = "标本条码")
    @Size(max = 128)
    private String specimenBarcode;

    @Schema(description = "标本编号；仅在唯一命中时使用")
    @Size(max = 64)
    private String specimenNo;

    @Schema(description = "核对操作人登录确认 token", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String operatorVerificationToken;

    @Schema(description = "确认人用户 ID")
    @Size(max = 64)
    private String operatorUserId;

    @Schema(description = "确认人姓名")
    @Size(max = 100)
    private String operatorName;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
