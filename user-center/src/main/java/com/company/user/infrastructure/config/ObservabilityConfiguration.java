package com.company.user.infrastructure.config;

import com.company.common.web.observability.ObservabilityConfigurationSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ObservabilityConfiguration extends ObservabilityConfigurationSupport {

    public ObservabilityConfiguration(@Value("${spring.application.name:user-center}") String serviceName,
                                      @Value("${observability.metrics.common-tags.module:user-center}") String moduleName) {
        super(serviceName, moduleName, Set.of("userId", "orderId", "traceId", "email"));
    }
}
