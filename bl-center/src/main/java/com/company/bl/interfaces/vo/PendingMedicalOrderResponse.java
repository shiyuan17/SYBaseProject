package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PendingMedicalOrderResponse", description = "Pending medical order item")
public record PendingMedicalOrderResponse(
    @Schema(description = "Order ID")
    String orderId,
    @Schema(description = "Case ID")
    String caseId,
    @Schema(description = "Pathology number")
    String pathologyNo,
    @Schema(description = "Application number")
    String applicationNo,
    @Schema(description = "Patient name")
    String patientName,
    @Schema(description = "Patient ID")
    String patientId,
    @Schema(description = "Patient ID display")
    String patientIdDisplay,
    @Schema(description = "Order number")
    String orderNumber,
    @Schema(description = "Order type")
    String orderType,
    @Schema(description = "Order content")
    String orderContent,
    @Schema(description = "Medical order dictionary item ID")
    String orderItemId,
    @Schema(description = "Medical order dictionary item code")
    String orderItemCode,
    @Schema(description = "Medical order dictionary item name")
    String orderItemName,
    @Schema(description = "Medical order category ID")
    String orderCategoryId,
    @Schema(description = "Medical order category code")
    String orderCategoryCode,
    @Schema(description = "Medical order category name")
    String orderCategoryName,
    @Schema(description = "Execution scope")
    String executionScope,
    @Schema(description = "Billing status")
    String billingStatus,
    @Schema(description = "Order status")
    String status,
    @Schema(description = "Doctor name")
    String doctorName,
    @Schema(description = "Executor name")
    String executorName,
    @Schema(description = "Order date")
    String orderDate,
    @Schema(description = "Accepted at")
    String acceptedAt,
    @Schema(description = "Printed at")
    String printedAt,
    @Schema(description = "Printed by name")
    String printedByName,
    @Schema(description = "Released at")
    String releasedAt,
    @Schema(description = "Released by name")
    String releasedByName,
    @Schema(description = "Completed at")
    String completedAt,
    @Schema(description = "Cancelled at")
    String cancelledAt,
    @Schema(description = "Terminated at")
    String terminatedAt,
    @Schema(description = "Terminated by name")
    String terminatedByName,
    @Schema(description = "Termination reason code")
    String terminationReasonCode,
    @Schema(description = "Termination reason label")
    String terminationReasonLabel,
    @Schema(description = "Termination remarks")
    String terminationRemarks,
    @Schema(description = "Remarks")
    String remarks,
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
    @Schema(description = "Specimen number")
    String specimenNo,
    @Schema(description = "Block number")
    String blockNo,
    @Schema(description = "Slide number")
    String slideNo,
    @Schema(description = "Can confirm")
    boolean canConfirm,
    @Schema(description = "Can print")
    boolean canPrint,
    @Schema(description = "Can release")
    boolean canRelease,
    @Schema(description = "Can terminate")
    boolean canTerminate,
    @Schema(description = "Can QC")
    boolean canQc
) {
}
