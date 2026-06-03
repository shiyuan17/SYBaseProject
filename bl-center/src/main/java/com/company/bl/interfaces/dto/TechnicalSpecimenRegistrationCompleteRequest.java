package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "TechnicalSpecimenRegistrationCompleteRequest", description = "技术标本登记完成请求")
@RejectLegacyOperatorFields
public class TechnicalSpecimenRegistrationCompleteRequest {

    @Schema(description = "送检类型")
    @NotBlank
    @Size(max = 64)
    private String applicationType;

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}
