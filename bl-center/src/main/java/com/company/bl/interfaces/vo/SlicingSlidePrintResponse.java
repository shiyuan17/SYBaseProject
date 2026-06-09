package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SlicingSlidePrintResponse", description = "切片工作站玻片打印确认响应")
public record SlicingSlidePrintResponse(
    @Schema(description = "技术任务 ID")
    String taskId,
    @Schema(description = "切片记录 ID")
    String slicingId,
    @Schema(description = "打印后的玻片 ID 列表")
    List<String> slideIds,
    @Schema(description = "打印后的玻片号列表")
    List<String> slideNos,
    @Schema(description = "是否执行了近邻合并")
    boolean merged,
    @Schema(description = "打印后的玻片数量")
    int printedSlideCount
) {
}
