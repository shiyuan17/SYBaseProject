package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TechnicalSpecimenRegistrationActionFlagsResponse", description = "技术标本登记动作权限标记")
public record TechnicalSpecimenRegistrationActionFlagsResponse(
    @Schema(description = "是否可完成登记")
    boolean canCompleteRegistration,
    @Schema(description = "是否可保存摘要分区")
    boolean canSaveDetailSections,
    @Schema(description = "是否可保存材料")
    boolean canSaveMaterials,
    @Schema(description = "是否可上传图片")
    boolean canUploadMediaAssets,
    @Schema(description = "是否可删除图片")
    boolean canDeleteMediaAssets
) {
}
