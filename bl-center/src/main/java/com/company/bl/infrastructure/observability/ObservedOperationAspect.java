package com.company.bl.infrastructure.observability;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.common.core.exception.BaseException;
import com.company.bl.infrastructure.config.ObservabilityConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ObservedOperationAspect {

    private final MeterRegistry meterRegistry;
    private final ObservabilityConfiguration observabilityConfiguration;
    private final boolean metricsEnabled;

    public ObservedOperationAspect(MeterRegistry meterRegistry,
                                   ObservabilityConfiguration observabilityConfiguration,
                                   @Value("${observability.metrics.enabled:true}") boolean metricsEnabled) {
        this.meterRegistry = meterRegistry;
        this.observabilityConfiguration = observabilityConfiguration;
        this.metricsEnabled = metricsEnabled;
    }

    @Around("@annotation(observedOperation)")
    public Object observe(ProceedingJoinPoint joinPoint, ObservedOperation observedOperation) throws Throwable {
        Timer.Sample sample = metricsEnabled ? Timer.start(meterRegistry) : null;
        long startNanos = System.nanoTime();
        Logger logger = LoggerFactory.getLogger(((MethodSignature) joinPoint.getSignature()).getDeclaringType());
        String outcome = "success";
        Throwable failure = null;

        putBusinessContext(observedOperation, joinPoint);

        try {
            Object result = joinPoint.proceed();
            incrementCounter(observedOperation.successCounter(), observedOperation.operation());
            return result;
        } catch (BaseException exception) {
            incrementCounter(observedOperation.failureCounter(), observedOperation.operation());
            if (exception instanceof ApplicationDomainException applicationException) {
                incrementAdditionalCounter(observedOperation.operation(), applicationException);
            }
            outcome = "business_failure";
            failure = exception;
            throw exception;
        } catch (Throwable throwable) {
            incrementCounter(observedOperation.failureCounter(), observedOperation.operation());
            outcome = "unexpected_failure";
            failure = throwable;
            throw throwable;
        } finally {
            stopTimer(sample, observedOperation);
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            MDC.put(ObservedLogFields.OUTCOME, outcome);
            MDC.put(ObservedLogFields.DURATION_MS, String.valueOf(durationMs));
            logObservation(logger, failure);
            clearBusinessContext();
        }
    }

    private void putBusinessContext(ObservedOperation observedOperation, ProceedingJoinPoint joinPoint) {
        MDC.put(ObservedLogFields.MODULE, observabilityConfiguration.moduleName());
        MDC.put(ObservedLogFields.OPERATION, observedOperation.operation());
        if (observedOperation.logArgs()) {
            MDC.put(ObservedLogFields.ARG_COUNT, String.valueOf(joinPoint.getArgs().length));
        }
    }

    private void clearBusinessContext() {
        MDC.remove(ObservedLogFields.MODULE);
        MDC.remove(ObservedLogFields.OPERATION);
        MDC.remove(ObservedLogFields.OUTCOME);
        MDC.remove(ObservedLogFields.DURATION_MS);
        MDC.remove(ObservedLogFields.ARG_COUNT);
    }

    private void logObservation(Logger logger, Throwable failure) {
        if (failure == null) {
            logger.info("Observed business operation success");
            return;
        }
        if (failure instanceof BaseException) {
            logger.warn("Observed business operation failure", failure);
            return;
        }
        logger.error("Observed business operation unexpected failure", failure);
    }

    private void incrementCounter(String metricName, String operation) {
        if (!metricsEnabled || metricName == null || metricName.isBlank()) {
            return;
        }
        Counter.builder(metricName)
            .tags(observabilityConfiguration.operationTags(operation))
            .register(meterRegistry)
            .increment();
    }

    private void incrementAdditionalCounter(String operation, ApplicationDomainException exception) {
        if (!metricsEnabled) {
            return;
        }
        if ("get_application".equals(operation)
            && exception.getErrorCode() == ApplicationErrorCode.APPLICATION_NOT_FOUND) {
            Counter.builder("application_query_not_found_total")
                .tags(observabilityConfiguration.operationTags(operation))
                .register(meterRegistry)
                .increment();
        }
    }

    private void stopTimer(Timer.Sample sample, ObservedOperation observedOperation) {
        if (!metricsEnabled || sample == null) {
            return;
        }
        sample.stop(Timer.builder(observedOperation.durationMetric())
            .tags(observabilityConfiguration.operationTags(observedOperation.operation()))
            .register(meterRegistry));
    }
}
