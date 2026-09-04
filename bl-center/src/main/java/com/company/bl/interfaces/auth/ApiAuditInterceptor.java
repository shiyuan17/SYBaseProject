package com.company.bl.interfaces.auth;

import com.company.bl.support.infrastructure.SupportJdbcRepository;
import com.company.common.security.context.AuthenticatedPrincipal;
import com.company.common.security.context.AuthenticatedPrincipalContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class ApiAuditInterceptor implements HandlerInterceptor {

    private static final int CONTENT_MAX_LENGTH = 2000;
    private static final int FAILURE_MAX_LENGTH = 500;
    private static final Pattern SENSITIVE_PAIR = Pattern.compile(
        "(?i)(password|token|authorization|accessToken|refreshToken|secret|salt)([\"'=:\\s]+)([^,&\\s\"'}]+)");

    private final SupportJdbcRepository supportJdbcRepository;

    public ApiAuditInterceptor(SupportJdbcRepository supportJdbcRepository) {
        this.supportJdbcRepository = supportJdbcRepository;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception exception) {
        if (!(handler instanceof HandlerMethod handlerMethod) || shouldSkip(request, handlerMethod)) {
            return;
        }
        try {
            AuditOperation auditOperation = findAuditOperation(handlerMethod);
            supportJdbcRepository.insertOperationLog(new SupportJdbcRepository.OperationLogRow(
                "OP-" + UUID.randomUUID(),
                resolveModuleCode(auditOperation, request),
                resolveBusinessType(auditOperation, request),
                request.getParameter("id"),
                resolveOperationName(auditOperation, request),
                exception == null && response.getStatus() < 400 ? "SUCCESS" : "FAILED",
                attributeValue(request, ApiPermissionContext.CURRENT_USER_ID),
                resolveOperatorName(request),
                resolveRemoteAddress(request),
                LocalDateTime.now(),
                buildOperationContent(request, response),
                sanitizeFailure(exception == null ? null : exception.getMessage())));
        } catch (RuntimeException ignored) {
            // Audit must never change the original request outcome.
        }
    }

    private boolean shouldSkip(HttpServletRequest request, HandlerMethod handlerMethod) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/") || uri.startsWith("/actuator/") || Boolean.TRUE.equals(
            request.getAttribute(ApiPermissionContext.OPERATION_AUDIT_RECORDED))) {
            return true;
        }
        if (!hasAuthorizationRequirement(handlerMethod)
            || AuthenticatedPrincipalContext.currentPrincipal(request) == null) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        AuditOperation auditOperation = findAuditOperation(handlerMethod);
        return auditOperation == null || !auditOperation.sensitiveQuery();
    }

    private RequirePermission findPermission(HandlerMethod handlerMethod) {
        RequirePermission permission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequirePermission.class);
        if (permission != null) {
            return permission;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
    }

    private boolean hasAuthorizationRequirement(HandlerMethod handlerMethod) {
        if (findPermission(handlerMethod) != null) {
            return true;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireAnyPermission.class) != null
            || AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireAnyPermission.class) != null;
    }

    private AuditOperation findAuditOperation(HandlerMethod handlerMethod) {
        AuditOperation operation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), AuditOperation.class);
        if (operation != null) {
            return operation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), AuditOperation.class);
    }

    private String resolveModuleCode(AuditOperation auditOperation, HttpServletRequest request) {
        if (auditOperation != null && !auditOperation.moduleCode().isBlank()) {
            return auditOperation.moduleCode();
        }
        String uri = request.getRequestURI();
        if (uri.contains("/m6/") || uri.contains("/stat-") || uri.contains("/billing")) {
            return "M6";
        }
        if (uri.contains("/archive") || uri.contains("/reagent") || uri.contains("/equipment")) {
            return "M5";
        }
        if (uri.contains("/report") || uri.contains("/diagnostic") || uri.contains("/consultation")) {
            return "M4";
        }
        if (uri.contains("/technical") || uri.contains("/grossing") || uri.contains("/slicing")
            || uri.contains("/staining")) {
            return "M3";
        }
        if (uri.contains("/applications") || uri.contains("/specimens") || uri.contains("/transport")) {
            return "M2";
        }
        return "SYSTEM";
    }

    private String resolveBusinessType(AuditOperation auditOperation, HttpServletRequest request) {
        if (auditOperation != null && !auditOperation.businessType().isBlank()) {
            return auditOperation.businessType();
        }
        String[] parts = request.getRequestURI().split("/");
        return parts.length >= 4 ? truncate(parts[3].replace('-', '_').toUpperCase(), 64) : "HTTP";
    }

    private String resolveOperationName(AuditOperation auditOperation, HttpServletRequest request) {
        if (auditOperation != null && !auditOperation.operationName().isBlank()) {
            return auditOperation.operationName();
        }
        return truncate(request.getMethod().toLowerCase() + " " + request.getRequestURI(), 100);
    }

    private String resolveOperatorName(HttpServletRequest request) {
        String operatorName = attributeValue(request, ApiPermissionContext.CURRENT_OPERATOR_NAME);
        if (operatorName != null) {
            return operatorName;
        }
        AuthenticatedPrincipal principal = AuthenticatedPrincipalContext.currentPrincipal(request);
        return principal == null ? "system" : principal.loginName();
    }

    private String attributeValue(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        return value == null ? null : value.toString();
    }

    private String resolveRemoteAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildOperationContent(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("method", request.getMethod());
        content.put("uri", request.getRequestURI());
        content.put("query", sanitize(request.getQueryString()));
        content.put("status", response.getStatus());
        return truncate(content.toString(), CONTENT_MAX_LENGTH);
    }

    private String sanitizeFailure(String value) {
        return truncate(sanitize(value), FAILURE_MAX_LENGTH);
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return SENSITIVE_PAIR.matcher(value).replaceAll("$1$2***");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
