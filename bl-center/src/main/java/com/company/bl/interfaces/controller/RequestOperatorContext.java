package com.company.bl.interfaces.controller;

import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.common.security.context.AuthenticatedPrincipal;
import com.company.common.security.context.AuthenticatedPrincipalContext;
import jakarta.servlet.http.HttpServletRequest;

final class RequestOperatorContext {

    private RequestOperatorContext() {
    }

    static String currentUserId(HttpServletRequest request) {
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        if (currentUserId instanceof String userId && !userId.isBlank()) {
            return userId.trim();
        }
        AuthenticatedPrincipal principal = AuthenticatedPrincipalContext.currentPrincipal(request);
        if (principal != null && principal.userId() != null && !principal.userId().isBlank()) {
            return principal.userId().trim();
        }
        return null;
    }

    static String currentOperatorName(HttpServletRequest request) {
        Object currentOperatorName = request.getAttribute(ApiPermissionContext.CURRENT_OPERATOR_NAME);
        if (currentOperatorName instanceof String operatorName && !operatorName.isBlank()) {
            return operatorName.trim();
        }
        Object currentLoginName = request.getAttribute(ApiPermissionContext.CURRENT_LOGIN_NAME);
        if (currentLoginName instanceof String loginName && !loginName.isBlank()) {
            return loginName.trim();
        }
        AuthenticatedPrincipal principal = AuthenticatedPrincipalContext.currentPrincipal(request);
        if (principal != null && principal.loginName() != null && !principal.loginName().isBlank()) {
            return principal.loginName().trim();
        }
        return null;
    }
}
