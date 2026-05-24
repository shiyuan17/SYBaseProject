package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ApplicationDuplicateCheckItemResponse", description = "疑似重复申请命中记录")
public record ApplicationDuplicateCheckItemResponse(
    @Schema(description = "申请单 ID")
    String id,
    @Schema(description = "申请单号")
    String applicationNo,
    @Schema(description = "患者姓名")
    String patientName,
    @Schema(description = "申请日期")
    String applicationDate,
    @Schema(description = "送检部位")
    String specimenSite,
    @Schema(description = "申请单状态")
    String status,
    @Schema(description = "当前节点")
    String currentNode,
    @Schema(description = "命中规则")
    List<String> matchedBy
) {
}
