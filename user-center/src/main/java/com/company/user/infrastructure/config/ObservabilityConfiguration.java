package com.company.user.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfiguration {

    public static final String SERVICE_NAME = "user-center";
    public static final String SERVICE_TAG = "service";
    public static final String MODULE_TAG = "module";
    public static final String OPERATION_TAG = "operation";

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> registry.config()
            .commonTags(SERVICE_TAG, SERVICE_NAME)
            .meterFilter(MeterFilter.deny(id -> hasHighCardinalityTag(id.getTags())));
    }

    public static Tags operationTags(String operation) {
        return Tags.of(
            MODULE_TAG, SERVICE_NAME,
            OPERATION_TAG, operation);
    }

    private boolean hasHighCardinalityTag(Iterable<io.micrometer.core.instrument.Tag> tags) {
        for (io.micrometer.core.instrument.Tag tag : tags) {
            if (isHighCardinalityTag(tag.getKey())) {
                return true;
            }
        }
        return false;
    }

    private boolean isHighCardinalityTag(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return "userId".equalsIgnoreCase(key)
            || "orderId".equalsIgnoreCase(key)
            || "traceId".equalsIgnoreCase(key)
            || "email".equalsIgnoreCase(key);
    }
}
