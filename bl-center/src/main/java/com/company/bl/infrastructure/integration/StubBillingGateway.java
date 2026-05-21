package com.company.bl.infrastructure.integration;

import com.company.bl.application.gateway.BillingGateway;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StubBillingGateway implements BillingGateway {

    private final Set<String> failedOnceBills = ConcurrentHashMap.newKeySet();

    @Override
    public BillingSubmitResult submit(BillingSubmitRequest request) {
        String itemName = request.itemName() == null ? "" : request.itemName().trim().toUpperCase();
        if (itemName.contains("FAIL_ALWAYS")) {
            return new BillingSubmitResult(false, null, "Billing placeholder forced permanent failure");
        }
        if (itemName.contains("FAIL_ONCE") && failedOnceBills.add(request.billingNo())) {
            return new BillingSubmitResult(false, null, "Billing placeholder forced one-time failure");
        }
        return new BillingSubmitResult(true, "EXT-" + request.billingNo(), "Billing placeholder succeeded");
    }

    @Override
    public List<ReconciliationResult> reconcile(ReconciliationRequest request) {
        return List.of();
    }
}
