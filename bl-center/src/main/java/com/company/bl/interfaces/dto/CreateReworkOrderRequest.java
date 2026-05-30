package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CreateReworkOrderRequest", description = "补做单创建请求")
@RejectLegacyOperatorFields
public class CreateReworkOrderRequest {

    @Schema(description = "病例 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Schema(description = "标本 ID")
    @Size(max = 64)
    private String specimenId;

    @Schema(description = "取材块 ID")
    @Size(max = 64)
    private String samplingBlockId;

    @Schema(description = "包埋盒 ID")
    @Size(max = 64)
    private String embeddingBoxId;

    @Schema(description = "切片 ID")
    @Size(max = 64)
    private String slideId;

    @Schema(description = "补做类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 50)
    private String reworkType;

    @Schema(description = "补做原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 500)
    private String reason;

    @Schema(description = "质控类型")
    @Size(max = 50)
    private String qcType;



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;
}