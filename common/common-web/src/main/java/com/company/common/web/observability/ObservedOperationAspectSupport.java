package com.company.common.web.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class ObservedOperationAspectSupport {

    private final MeterRegistry meterRegistry;
    private final ObservabilityConfigurationSupport observabilityConfiguration;
    private final boolean metricsEnabled;

    protected ObservedOperationAspectSupport(MeterRegistry meterRegistry,
                                            ObservabilityConfigurationSupport observabilityConfiguration,
                                            boolean metricsEnabled) {
        this.meterRegistry = meterRegistry;
        this.observabilityConfiguration = observabilityConfiguration;
        this.metricsEnabled = metricsEnabled;
    }

    protected Object observeOperation(ProceedingJoinPoint joinPoint, ObservedOperation observedOperation) throws Throwable {
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
        } catch (Throwable throwable) {
            incrementCounter(observedOperation.failureCounter(), observedOperation.operation());
            if (isBusinessFailure(throwable)) {
                onBusinessFailure(observedOperation.operation(), throwable);
                outcome = "business_failure";
            } else {
                outcome = "unexpected_failure";
            }
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

    protected abstract boolean isBusinessFailure(Throwable throwable);

    protected void onBusinessFailure(String operation, Throwable throwable) {
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
        if (isBusinessFailure(failure)) {
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

    private void stopTimer(Timer.Sample sample, ObservedOperation observedOperation) {
        if (!metricsEnabled || sample == null) {
            return;
        }
        sample.stop(Timer.builder(observedOperation.durationMetric())
            .tags(observabilityConfiguration.operationTags(observedOperation.operation()))
            .register(meterRegistry));
    }
}
