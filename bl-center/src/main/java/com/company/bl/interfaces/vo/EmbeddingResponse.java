package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EmbeddingResponse", description = "包埋完成响应")
public record EmbeddingResponse(
    @Schema(description = "技术任务 ID")
    String taskId,
    @Schema(description = "包埋记录 ID")
    String embeddingId,
    @Schema(description = "包埋盒 ID")
    String embeddingBoxId,
    @Schema(description = "病例状态")
    String caseStatus,
    @Schema(description = "是否打号成功")
    boolean markingSuccess,
    @Schema(description = "打号结果说明")
    String markingMessage
) {
}
