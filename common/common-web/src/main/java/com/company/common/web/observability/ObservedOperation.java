package com.company.common.web.observability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ObservedOperation {

    String operation();

    String successCounter();

    String failureCounter() default "";

    String durationMetric();

    boolean logArgs() default false;
}
