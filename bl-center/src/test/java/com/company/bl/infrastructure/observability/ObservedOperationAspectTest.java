package com.company.bl.infrastructure.observability;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.infrastructure.config.ObservabilityConfiguration;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservedOperationAspectTest {

    @Test
    void shouldRecordSuccessCounterAndDuration() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservabilityConfiguration configuration = new ObservabilityConfiguration("bl-center", "bl-center");
        ObservedOperationAspect aspect = new ObservedOperationAspect(meterRegistry, configuration, true);

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new SuccessObservedService());
        proxyFactory.addAspect(aspect);
        SuccessObservedService proxy = proxyFactory.getProxy();

        proxy.execute();

        assertThat(meterRegistry.get("application_create_total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("application_create_duration").timer().count()).isEqualTo(1L);
    }

    @Test
    void shouldRecordFailureAndNotFoundMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservabilityConfiguration configuration = new ObservabilityConfiguration("bl-center", "bl-center");
        ObservedOperationAspect aspect = new ObservedOperationAspect(meterRegistry, configuration, true);

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new NotFoundObservedService());
        proxyFactory.addAspect(aspect);
        NotFoundObservedService proxy = proxyFactory.getProxy();

        assertThatThrownBy(proxy::execute)
            .isInstanceOf(ApplicationDomainException.class);

        assertThat(meterRegistry.get("application_query_failed_total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("application_query_not_found_total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("application_query_duration").timer().count()).isEqualTo(1L);
    }

    static class SuccessObservedService {

        @ObservedOperation(
            operation = "create_application",
            successCounter = "application_create_total",
            failureCounter = "application_create_failed_total",
            durationMetric = "application_create_duration")
        public String execute() {
            return "ok";
        }
    }

    static class NotFoundObservedService {

        @ObservedOperation(
            operation = "get_application",
            successCounter = "application_query_total",
            failureCounter = "application_query_failed_total",
            durationMetric = "application_query_duration")
        public String execute() {
            throw new ApplicationDomainException(ApplicationErrorCode.APPLICATION_NOT_FOUND, 404);
        }
    }
}
