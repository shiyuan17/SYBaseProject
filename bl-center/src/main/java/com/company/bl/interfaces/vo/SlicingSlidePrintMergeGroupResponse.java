package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "SlicingSlidePrintMergeGroupResponse", description = "切片工作站未打印合片组操作结果")
public record SlicingSlidePrintMergeGroupResponse(
    @Schema(description = "本次创建或取消的合片组 ID")
    List<String> printGroupIds
) {
}
