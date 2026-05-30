package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "RegisterSpecimensRequest", description = "标本登记请求")
@RejectLegacyOperatorFields
public class RegisterSpecimensRequest {

    @Schema(description = "申请单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String applicationId;

    @Schema(description = "打印机编码")
    @Size(max = 64)
    private String printerCode;

    @Schema(description = "采集场景")
    @Size(max = 100)
    private String collectionScene;



    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remarks;

    @Schema(description = "标本明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty
    private List<SpecimenItem> items;

    @Getter
    @Setter
    @Schema(name = "RegisterSpecimenItem", description = "标本登记明细")
    public static class SpecimenItem {
        @Schema(description = "标准化标本名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        private String specimenNameStandardized;

        @Schema(description = "标本类型")
        @Size(max = 100)
        private String specimenType;

        @Schema(description = "标本部位", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        private String specimenSite;

        @Schema(description = "采样方式")
        @Size(max = 50)
        private String collectionMode;

        @Schema(description = "标本数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1)
        private Integer specimenCount;

        @Schema(description = "容器名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        private String containerName;

        @Schema(description = "容器数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1)
        private Integer containerCount;

        @Schema(description = "标本条码")
        @Size(max = 128)
        private String barcode;

        @Schema(description = "临床症状")
        @Size(max = 500)
        private String clinicalSymptom;
    }
}