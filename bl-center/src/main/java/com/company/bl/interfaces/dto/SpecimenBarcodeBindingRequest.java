package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SpecimenBarcodeBindingRequest", description = "条码绑定请求")
@RejectLegacyOperatorFields
public class SpecimenBarcodeBindingRequest {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "目标条码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetBarcode;

    @Size(max = 64)
    @Schema(description = "终端编码")
    private String terminalCode;

    @Size(max = 500)
    @Schema(description = "备注")
    private String remarks;
}
