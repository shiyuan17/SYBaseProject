package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SpecimenVerificationRecordResponse", description = "标本核对记录")
public record SpecimenVerificationRecordResponse(
    @Schema(description = "申请单 ID")
    String applicationId,
    @Schema(description = "标本条码")
    String barcode,
    @Schema(description = "操作人")
    String operatorName,
    @Schema(description = "备注")
    String remarks,
    @Schema(description = "结果")
    String result,
    @Schema(description = "标本 ID")
    String specimenId,
    @Schema(description = "终端编码")
    String terminalCode,
    @Schema(description = "核对类型")
    String verificationType,
    @Schema(description = "核对时间")
    String verifiedAt
) {
}
