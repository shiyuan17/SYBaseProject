package com.company.bl.integration.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class M6BillingRows {

    private M6BillingRows() {
    }

    public record CreateBillingRecordRow(
        String id,
        String caseId,
        String orderId,
        String billingNo,
        String billingStage,
        String itemType,
        String itemName,
        BigDecimal quantity,
        BigDecimal amount,
        String billingStatus,
        LocalDateTime billedAt,
        String operatorUserId,
        String operatorName,
        String externalBillNo,
        String externalSystem,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record BillingRecordRow(
        String id,
        String caseId,
        String orderId,
        String billingNo,
        String billingStage,
        String itemType,
        String itemName,
        BigDecimal quantity,
        BigDecimal amount,
        String billingStatus,
        LocalDateTime billedAt,
        String operatorUserId,
        String operatorName,
        String externalBillNo,
        String externalSystem,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
