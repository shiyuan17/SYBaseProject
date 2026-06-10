package com.company.bl.interfaces.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditOperation {

    String businessType() default "";

    String moduleCode() default "";

    String operationName() default "";

    boolean sensitiveQuery() default false;
}
