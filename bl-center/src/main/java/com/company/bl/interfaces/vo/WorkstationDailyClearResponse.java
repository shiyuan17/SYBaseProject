package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WorkstationDailyClearResponse", description = "工作站日结清零状态")
public record WorkstationDailyClearResponse(
    @Schema(description = "工作日期")
    String workDate,
    @Schema(description = "是否已完成清零")
    boolean cleared,
    @Schema(description = "操作人用户 ID")
    String operatorUserId,
    @Schema(description = "操作人姓名")
    String operatorName,
    @Schema(description = "清零时间")
    String clearedAt,
    @Schema(description = "清零状态")
    String clearStatus
) {
}
