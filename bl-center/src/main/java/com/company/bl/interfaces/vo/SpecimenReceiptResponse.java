package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SpecimenReceiptResponse", description = "标本签收结果")
public record SpecimenReceiptResponse(
    @Schema(description = "病例 ID")
    String caseId,
    @Schema(description = "病理号")
    String pathologyNo,
    @Schema(description = "签收状态")
    String receiptStatus,
    @Schema(description = "未签收标本数量")
    int unreceivedCount
) {
}
