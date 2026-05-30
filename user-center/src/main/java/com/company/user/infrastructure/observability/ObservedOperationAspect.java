package com.company.user.infrastructure.observability;

import com.company.common.web.observability.ObservedOperation;
import com.company.common.web.observability.ObservedOperationAspectSupport;
import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;
import com.company.user.infrastructure.config.ObservabilityConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ObservedOperationAspect extends ObservedOperationAspectSupport {

    private final ObservabilityConfiguration observabilityConfiguration;
    private final MeterRegistry meterRegistry;
    private final boolean metricsEnabled;

    public ObservedOperationAspect(MeterRegistry meterRegistry,
                                   ObservabilityConfiguration observabilityConfiguration,
                                   @Value("${observability.metrics.enabled:true}") boolean metricsEnabled) {
        super(meterRegistry, observabilityConfiguration, metricsEnabled);
        this.meterRegistry = meterRegistry;
        this.observabilityConfiguration = observabilityConfiguration;
        this.metricsEnabled = metricsEnabled;
    }

    @Around("@annotation(observedOperation)")
    public Object observe(ProceedingJoinPoint joinPoint, ObservedOperation observedOperation) throws Throwable {
        return observeOperation(joinPoint, observedOperation);
    }

    @Override
    protected boolean isBusinessFailure(Throwable throwable) {
        return throwable instanceof UserDomainException;
    }

    @Override
    protected void onBusinessFailure(String operation, Throwable throwable) {
        if (!metricsEnabled || !(throwable instanceof UserDomainException exception)) {
            return;
        }
        if ("get_user".equals(operation) && exception.getErrorCode() == UserErrorCode.USER_NOT_FOUND) {
            Counter.builder("user_query_not_found_total")
                .tags(observabilityConfiguration.operationTags(operation))
                .register(meterRegistry)
                .increment();
        }
    }
}
