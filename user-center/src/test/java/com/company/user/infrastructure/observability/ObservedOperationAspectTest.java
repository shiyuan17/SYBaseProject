package com.company.user.infrastructure.observability;

import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;
import com.company.user.infrastructure.config.ObservabilityConfiguration;
import com.company.common.web.observability.ObservedOperation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservedOperationAspectTest {

    @Test
    void shouldRecordSuccessCounterAndDuration() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservabilityConfiguration configuration = new ObservabilityConfiguration("user-center", "user-center");
        ObservedOperationAspect aspect = new ObservedOperationAspect(meterRegistry, configuration, true);

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new SuccessObservedService());
        proxyFactory.addAspect(aspect);
        SuccessObservedService proxy = proxyFactory.getProxy();

        proxy.execute();

        assertThat(meterRegistry.get("user_create_total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("user_create_duration").timer().count()).isEqualTo(1L);
    }

    @Test
    void shouldRecordFailureAndNotFoundMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservabilityConfiguration configuration = new ObservabilityConfiguration("user-center", "user-center");
        ObservedOperationAspect aspect = new ObservedOperationAspect(meterRegistry, configuration, true);

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new NotFoundObservedService());
        proxyFactory.addAspect(aspect);
        NotFoundObservedService proxy = proxyFactory.getProxy();

        assertThatThrownBy(proxy::execute)
            .isInstanceOf(UserDomainException.class);

        assertThat(meterRegistry.get("user_query_failed_total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("user_query_not_found_total").counter().count()).isEqualTo(1.0d);
        assertThat(meterRegistry.get("user_query_duration").timer().count()).isEqualTo(1L);
    }

    static class SuccessObservedService {

        @ObservedOperation(
            operation = "create_user",
            successCounter = "user_create_total",
            failureCounter = "user_create_failed_total",
            durationMetric = "user_create_duration")
        public String execute() {
            return "ok";
        }
    }

    static class NotFoundObservedService {

        @ObservedOperation(
            operation = "get_user",
            successCounter = "user_query_total",
            failureCounter = "user_query_failed_total",
            durationMetric = "user_query_duration")
        public String execute() {
            throw new UserDomainException(UserErrorCode.USER_NOT_FOUND, 404);
        }
    }
}
