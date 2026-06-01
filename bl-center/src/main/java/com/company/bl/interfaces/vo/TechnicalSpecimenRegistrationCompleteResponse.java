package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TechnicalSpecimenRegistrationCompleteResponse", description = "技术标本登记完成结果")
public record TechnicalSpecimenRegistrationCompleteResponse(
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "登记状态")
    String registrationStatus,
    @Schema(description = "是否新建取材任务")
    boolean grossingTaskCreated
) {
}
