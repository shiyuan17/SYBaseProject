package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "TechnicalSpecimenRegistrationWorkspaceResponse", description = "技术标本登记工作台")
public record TechnicalSpecimenRegistrationWorkspaceResponse(
    @Schema(description = "左侧当前病例摘要")
    PendingTechnicalSpecimenRegistrationResponse pendingSummary,
    @Schema(description = "基础信息")
    TechnicalSpecimenRegistrationBasicInfoResponse basicInfo,
    @Schema(description = "详情分区")
    TechnicalSpecimenRegistrationDetailSectionsResponse detailSections,
    @Schema(description = "材料列表")
    List<TechnicalSpecimenRegistrationMaterialResponse> materials,
    @Schema(description = "检查项目")
    List<TechnicalSpecimenRegistrationCheckItemResponse> checkItems,
    @Schema(description = "图片区附件")
    List<TechnicalSpecimenRegistrationMediaAssetResponse> mediaAssets,
    @Schema(description = "动作可用性")
    TechnicalSpecimenRegistrationActionFlagsResponse actionFlags
) {
}
