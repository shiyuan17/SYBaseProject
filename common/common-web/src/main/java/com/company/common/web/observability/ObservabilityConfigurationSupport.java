package com.company.common.web.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ObservabilityConfigurationSupport {

    public static final String SERVICE_TAG = "service";
    public static final String MODULE_TAG = "module";
    public static final String OPERATION_TAG = "operation";

    private final String serviceName;
    private final String moduleName;
    private final Set<String> highCardinalityTagKeys;

    protected ObservabilityConfigurationSupport(String serviceName,
                                                String moduleName,
                                                Set<String> highCardinalityTagKeys) {
        this.serviceName = serviceName;
        this.moduleName = moduleName;
        this.highCardinalityTagKeys = highCardinalityTagKeys.stream()
            .map(key -> key.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
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
        return highCardinalityTagKeys.contains(key.toLowerCase(Locale.ROOT));
    }
}
