package com.company.common.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class AccessLogFilter extends OncePerRequestFilter {
    private static final String METHOD = "method";
    private static final String URI = "uri";
    private static final String STATUS = "status";
    private static final String DURATION_MS = "durationMs";
    private static final String REMOTE_ADDR = "remoteAddr";
    private static final String MODULE = "module";
    private static final String OPERATION = "operation";

    private final boolean accessLogEnabled;
    private final String moduleName;

    public AccessLogFilter(@Value("${observability.logging.access.enabled:true}") boolean accessLogEnabled,
                           @Value("${observability.metrics.common-tags.module:unknown}") String moduleName) {
        this.accessLogEnabled = accessLogEnabled;
        this.moduleName = moduleName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!accessLogEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (!shouldSkipAccessLog(request)) {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
                putAccessLogContext(request, response, durationMs);
                try {
                    log.info("HTTP access");
                } finally {
                    clearAccessLogContext();
                }
            }
        }
    }

    private boolean shouldSkipAccessLog(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/prometheus") || uri.startsWith("/actuator/health");
    }

    private void putAccessLogContext(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        MDC.put(MODULE, moduleName);
        MDC.put(OPERATION, "http_access");
        MDC.put(METHOD, request.getMethod());
        MDC.put(URI, request.getRequestURI());
        MDC.put(STATUS, String.valueOf(response.getStatus()));
        MDC.put(DURATION_MS, String.valueOf(durationMs));
        MDC.put(REMOTE_ADDR, resolveRemoteAddress(request));
    }

    private void clearAccessLogContext() {
        MDC.remove(MODULE);
        MDC.remove(OPERATION);
        MDC.remove(METHOD);
        MDC.remove(URI);
        MDC.remove(STATUS);
        MDC.remove(DURATION_MS);
        MDC.remove(REMOTE_ADDR);
    }

    private String resolveRemoteAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
