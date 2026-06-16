package com.company.bl.interfaces.auth;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;
import com.company.common.security.context.AuthenticatedPrincipal;
import com.company.common.security.context.AuthenticatedPrincipalContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiPermissionInterceptor implements HandlerInterceptor {

    private final RbacPermissionRepository permissionRepository;
    private final SystemUserJdbcRepository systemUserJdbcRepository;

    public ApiPermissionInterceptor(RbacPermissionRepository permissionRepository,
                                    SystemUserJdbcRepository systemUserJdbcRepository) {
        this.permissionRepository = permissionRepository;
        this.systemUserJdbcRepository = systemUserJdbcRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission permission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequirePermission.class);
        if (permission == null) {
            permission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
        }
        RequireAnyPermission anyPermission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireAnyPermission.class);
        if (anyPermission == null) {
            anyPermission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireAnyPermission.class);
        }
        if (permission == null && anyPermission == null) {
            return true;
        }
        AuthenticatedPrincipal principal = AuthenticatedPrincipalContext.currentPrincipal(request);
        if (principal == null) {
            throw new BlBusinessException(BlErrorCode.AUTHENTICATION_REQUIRED, 401,
                "Authorization bearer token is required");
        }
        String normalizedUserId = principal.userId().trim();
        if (permission != null && !permissionRepository.hasPermission(normalizedUserId, permission.value())) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403,
                "User does not have permission: " + permission.value());
        }
        if (anyPermission != null && !permissionRepository.hasAnyPermission(normalizedUserId, anyPermission.value())) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403,
                "User does not have any required permission: " + String.join(", ", anyPermission.value()));
        }
        request.setAttribute(ApiPermissionContext.CURRENT_USER_ID, normalizedUserId);
        String loginName = principal.loginName() == null ? null : principal.loginName().trim();
        request.setAttribute(ApiPermissionContext.CURRENT_LOGIN_NAME, loginName);
        request.setAttribute(ApiPermissionContext.CURRENT_OPERATOR_NAME, resolveOperatorName(normalizedUserId, loginName));
        request.setAttribute(ApiPermissionContext.CURRENT_ROLE_CODE, permissionRepository.findPrimaryRoleCode(normalizedUserId));
        return true;
    }

    private String resolveOperatorName(String userId, String loginName) {
        SystemJdbcRepository.UserRow user = systemUserJdbcRepository.findUserById(userId);
        if (user != null && user.name() != null && !user.name().isBlank()) {
            return user.name().trim();
        }
        return loginName;
    }
}
