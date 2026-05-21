package com.company.bl.application.gateway;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface BillingGateway {

    BillingSubmitResult submit(BillingSubmitRequest request);

    List<ReconciliationResult> reconcile(ReconciliationRequest request);

    record BillingSubmitRequest(
        String billingNo,
        String caseId,
        String orderId,
        String billingStage,
        String itemType,
        String itemName,
        BigDecimal quantity,
        BigDecimal amount,
        String operatorUserId,
        String operatorName
    ) {
    }

    record BillingSubmitResult(
        boolean success,
        String externalBillNo,
        String message
    ) {
    }

    record ReconciliationRequest(
        LocalDateTime from,
        LocalDateTime to
    ) {
    }

    record ReconciliationResult(
        String billingNo,
        String externalBillNo,
        String remoteStatus,
        String message
    ) {
    }
}
