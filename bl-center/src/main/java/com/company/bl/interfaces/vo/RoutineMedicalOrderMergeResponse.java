package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "RoutineMedicalOrderMergeResponse", description = "常规医嘱合片/取消合片结果")
public record RoutineMedicalOrderMergeResponse(
    @Schema(description = "本次处理的合片组 ID")
    List<String> printGroupIds
) {
}
