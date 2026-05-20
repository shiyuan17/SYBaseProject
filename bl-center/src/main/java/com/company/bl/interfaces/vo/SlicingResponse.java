package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SlicingResponse", description = "切片完成响应")
public record SlicingResponse(
    @Schema(description = "技术任务 ID")
    String taskId,
    @Schema(description = "切片记录 ID")
    String slicingId,
    @Schema(description = "生成的玻片 ID 列表")
    List<String> slideIds,
    @Schema(description = "病例状态")
    String caseStatus
) {
}
