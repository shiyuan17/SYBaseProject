package com.company.bl.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfiguration {

    public static final String SERVICE_TAG = "service";
    public static final String MODULE_TAG = "module";
    public static final String OPERATION_TAG = "operation";
    private final String serviceName;
    private final String moduleName;

    public ObservabilityConfiguration(@Value("${spring.application.name:bl-center}") String serviceName,
                                      @Value("${observability.metrics.common-tags.module:bl-center}") String moduleName) {
        this.serviceName = serviceName;
        this.moduleName = moduleName;
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> registry.config()
            .commonTags(
                SERVICE_TAG, serviceName,
                MODULE_TAG, moduleName)
            .meterFilter(MeterFilter.deny(id -> hasHighCardinalityTag(id.getTags())));
    }

    public Tags operationTags(String operation) {
        return Tags.of(OPERATION_TAG, operation);
    }

    public String moduleName() {
        return moduleName;
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
        return "applicationId".equalsIgnoreCase(key)
            || "patientId".equalsIgnoreCase(key)
            || "traceId".equalsIgnoreCase(key)
            || "applicationNo".equalsIgnoreCase(key);
    }
}
