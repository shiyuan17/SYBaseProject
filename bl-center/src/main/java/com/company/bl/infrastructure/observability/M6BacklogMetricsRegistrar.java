package com.company.bl.infrastructure.observability;

import com.company.bl.infrastructure.config.ObservabilityConfiguration;
import com.company.bl.integration.infrastructure.M6JdbcRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class M6BacklogMetricsRegistrar {

    public M6BacklogMetricsRegistrar(MeterRegistry meterRegistry,
                                     ObservabilityConfiguration observabilityConfiguration,
                                     M6JdbcRepository repository,
                                     @Value("${observability.metrics.enabled:true}") boolean metricsEnabled) {
        if (!metricsEnabled) {
            return;
        }
        registerGauge(meterRegistry, observabilityConfiguration, repository,
            "integration_retry_pending_count", "integration_retry_pending",
            value -> value.countIntegrationTasksByStatus("RETRY_PENDING"));
        registerGauge(meterRegistry, observabilityConfiguration, repository,
            "integration_manual_required_count", "integration_manual_required",
            value -> value.countIntegrationTasksByCompensationStatus("MANUAL_REQUIRED"));
        registerGauge(meterRegistry, observabilityConfiguration, repository,
            "billing_reconciliation_discrepancy_count", "billing_reconciliation_discrepancy",
            value -> value.countIntegrationTasksByBusinessAndReconciliationStatus("BILLING_RECORD", "DISCREPANCY"));
        registerGauge(meterRegistry, observabilityConfiguration, repository,
            "historical_import_running_count", "historical_import_running",
            value -> value.countHistoricalImportJobsByStatus("RUNNING"));
    }

    private void registerGauge(MeterRegistry meterRegistry,
                               ObservabilityConfiguration observabilityConfiguration,
                               M6JdbcRepository repository,
                               String metricName,
                               String operation,
                               GaugeValueProvider valueProvider) {
        Gauge.builder(metricName, repository, value -> (double) valueProvider.provide(value))
            .tags(observabilityConfiguration.operationTags(operation))
            .register(meterRegistry);
    }

    @FunctionalInterface
    private interface GaugeValueProvider {

        long provide(M6JdbcRepository repository);
    }
}
