package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MedicalOrderTargetSnapshotResponse", description = "Medical order target snapshot result")
public record MedicalOrderTargetSnapshotResponse(
    @Schema(description = "Order ID")
    String orderId,
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "Order number")
    String orderNumber,
    @Schema(description = "Order status")
    String status,
    @Schema(description = "Target type")
    String targetType,
    @Schema(description = "Target specimen ID")
    String targetSpecimenId,
    @Schema(description = "Target specimen number")
    String targetSpecimenNo,
    @Schema(description = "Target block ID")
    String targetBlockId,
    @Schema(description = "Target block number")
    String targetBlockNo,
    @Schema(description = "Target slide ID")
    String targetSlideId,
    @Schema(description = "Target slide number")
    String targetSlideNo,
    @Schema(description = "Medical-order-only block ID when the target block is backed by medical_order_blocks")
    String medicalOrderBlockId
) {
}
