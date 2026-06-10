package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SpecimenFixationRequest", description = "标本固定请求")
@RejectLegacyOperatorFields
public class SpecimenFixationRequest {

    @Schema(description = "标本 ID；优先于标本条码和标本编号")
    @Size(max = 64)
    private String specimenId;

    @Schema(description = "标本条码")
    @Size(max = 128)
    private String specimenBarcode;

    @Schema(description = "标本编号；仅在唯一命中时使用")
    @Size(max = 64)
    private String specimenNo;

    @Schema(description = "固定液类型")
    @Size(max = 100)
    private String fixationLiquidType;



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
