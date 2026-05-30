package com.company.bl.infrastructure.config;

import com.company.common.web.observability.ObservabilityConfigurationSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ObservabilityConfiguration extends ObservabilityConfigurationSupport {

    public ObservabilityConfiguration(@Value("${spring.application.name:bl-center}") String serviceName,
                                      @Value("${observability.metrics.common-tags.module:bl-center}") String moduleName) {
        super(serviceName, moduleName, Set.of("applicationId", "patientId", "traceId", "applicationNo"));
    }
}
