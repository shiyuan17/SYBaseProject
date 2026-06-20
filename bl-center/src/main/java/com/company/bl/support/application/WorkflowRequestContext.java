package com.company.bl.support.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class WorkflowRequestContext {

    private WorkflowRequestContext() {
    }

    public static String resolveClientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return resolveRemoteAddress(servletRequestAttributes.getRequest());
    }

    private static String resolveRemoteAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
