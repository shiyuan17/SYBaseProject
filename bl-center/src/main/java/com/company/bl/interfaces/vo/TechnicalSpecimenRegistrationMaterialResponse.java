package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "TechnicalSpecimenRegistrationMaterialResponse", description = "技术标本登记送检材料")
public record TechnicalSpecimenRegistrationMaterialResponse(
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "标本条码")
    String specimenBarcode,
    @Schema(description = "序号")
    int sequenceNo,
    @Schema(description = "标本类型")
    String specimenType,
    @Schema(description = "名称")
    String specimenName,
    @Schema(description = "来源部位")
    String sourcePart,
    @Schema(description = "组织数量")
    int tissueCount,
    @Schema(description = "标本大小")
    String specimenSize,
    @Schema(description = "是否冰冻")
    boolean frozen,
    @Schema(description = "技术登记评价项")
    List<String> evaluationItems,
    @Schema(description = "核对状态：UNVERIFIED/VERIFYING/VERIFIED")
    String verificationStatus,
    @Schema(description = "核对完成时间")
    String verificationCompletedAt,
    @Schema(description = "核对人")
    String verifiedByName
) {
}
