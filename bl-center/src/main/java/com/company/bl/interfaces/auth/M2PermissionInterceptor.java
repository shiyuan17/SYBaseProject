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
public class M2PermissionInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ID = "CURRENT_USER_ID";
    public static final String USER_ID_HEADER = "X-User-Id";

    private final RbacPermissionRepository permissionRepository;

    public M2PermissionInterceptor(RbacPermissionRepository permissionRepository) {
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
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.isBlank()) {
            throw new BlBusinessException(BlErrorCode.AUTHENTICATION_REQUIRED, 401, "X-User-Id header is required");
        }
        String normalizedUserId = userId.trim();
        if (!permissionRepository.hasPermission(normalizedUserId, permission.value())) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "User does not have permission: " + permission.value());
        }
        request.setAttribute(CURRENT_USER_ID, normalizedUserId);
        return true;
    }
}
