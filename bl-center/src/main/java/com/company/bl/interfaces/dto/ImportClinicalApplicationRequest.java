package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ImportClinicalApplicationRequest", description = "第三方临床申请导入请求")
public class ImportClinicalApplicationRequest {

    @Schema(description = "第三方来源编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String thirdPartySource;

    @Schema(description = "外部申请单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String externalOrderNo;
}
