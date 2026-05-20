package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SlideStainingResponse", description = "染色完成响应")
public record SlideStainingResponse(
    @Schema(description = "技术任务 ID")
    String taskId,
    @Schema(description = "玻片 ID")
    String slideId,
    @Schema(description = "病例状态")
    String caseStatus
) {
}
