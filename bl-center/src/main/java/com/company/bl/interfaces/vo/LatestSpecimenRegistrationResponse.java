package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "LatestSpecimenRegistrationResponse", description = "最近一次标本登记结果")
public record LatestSpecimenRegistrationResponse(
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "标签打印批次号")
    String labelPrintBatchNo,
    @Schema(description = "是否打印成功")
    boolean labelPrintSuccess,
    @Schema(description = "打印结果说明")
    String labelPrintMessage,
    @Schema(description = "最近一次登记的标本列表")
    List<SpecimenSummaryResponse> specimens
) {
}
