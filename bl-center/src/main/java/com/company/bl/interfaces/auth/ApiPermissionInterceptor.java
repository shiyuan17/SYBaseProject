package com.company.bl.interfaces.auth;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiPermissionInterceptor implements HandlerInterceptor {

    private final RbacPermissionRepository permissionRepository;

    public ApiPermissionInterceptor(RbacPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
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
        if (permission == null) {
            return true;
        }
        String userId = request.getHeader(ApiPermissionContext.USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new BlBusinessException(BlErrorCode.AUTHENTICATION_REQUIRED, 401,
                ApiPermissionContext.USER_ID_HEADER + " header is required");
        }
        String normalizedUserId = userId.trim();
        if (!permissionRepository.hasPermission(normalizedUserId, permission.value())) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403,
                "User does not have permission: " + permission.value());
        }
        request.setAttribute(ApiPermissionContext.CURRENT_USER_ID, normalizedUserId);
        return true;
    }
}
