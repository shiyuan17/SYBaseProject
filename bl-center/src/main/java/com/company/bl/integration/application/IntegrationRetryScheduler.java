package com.company.bl.integration.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bl.integration.retry.enabled", havingValue = "true")
public class IntegrationRetryScheduler {

    private final BillingManagementService billingManagementService;

    public IntegrationRetryScheduler(BillingManagementService billingManagementService) {
        this.billingManagementService = billingManagementService;
    }

    @Scheduled(fixedDelayString = "${bl.integration.retry.fixed-delay-ms:300000}")
    public void retryPendingBillings() {
        billingManagementService.retryPendingBillings();
    }
}
