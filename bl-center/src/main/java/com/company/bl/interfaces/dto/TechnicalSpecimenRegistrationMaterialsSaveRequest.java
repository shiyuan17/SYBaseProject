package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "TechnicalSpecimenRegistrationMaterialsSaveRequest", description = "技术标本登记材料保存请求")
@RejectLegacyOperatorFields
public class TechnicalSpecimenRegistrationMaterialsSaveRequest {

    @Schema(description = "终端编码")
    @Size(max = 64)
    private String terminalCode;

    @Valid
    @NotEmpty
    @Schema(description = "有序材料列表")
    private List<MaterialItem> materials;

    @Getter
    @Setter
    @Schema(name = "TechnicalSpecimenRegistrationMaterialsSaveItem", description = "技术标本登记材料项")
    public static class MaterialItem {

        @Schema(description = "标本 ID，新增时为空")
        @Size(max = 64)
        private String specimenId;

        @Schema(description = "标本类型")
        @Size(max = 64)
        private String specimenType;

        @Schema(description = "标本名称")
        @Size(max = 255)
        private String specimenName;

        @Schema(description = "来源部位")
        @Size(max = 255)
        private String sourcePart;
    }
}
