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
@Schema(name = "SpecimenRemovalQuickConfirmRequest", description = "按标识快捷离体确认请求")
@RejectLegacyOperatorFields
public class SpecimenRemovalQuickConfirmRequest {

    @Schema(description = "标识类型：BARCODE 或 SPECIMEN_NO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Pattern(regexp = "BARCODE|SPECIMEN_NO")
    private String identifierType;

    @Schema(description = "标识值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String identifier;



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}