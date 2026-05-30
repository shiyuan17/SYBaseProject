package com.company.bl.interfaces.controller;

import com.company.bl.interfaces.auth.ApiPermissionContext;
import jakarta.servlet.http.HttpServletRequest;

abstract class TechnicalControllerSupport {

    protected String resolveUserId(String bodyUserId, HttpServletRequest request) {
        return resolveUserId(request);
    }

    protected String resolveUserId(HttpServletRequest request) {
        return RequestOperatorContext.currentUserId(request);
    }

    protected String resolveOperatorName(String bodyOperatorName, HttpServletRequest request) {
        return resolveOperatorName(request);
    }

    protected String resolveOperatorName(HttpServletRequest request) {
        return RequestOperatorContext.currentOperatorName(request);
    }

    protected String resolveRoleCode(HttpServletRequest request) {
        Object currentRoleCode = request.getAttribute(ApiPermissionContext.CURRENT_ROLE_CODE);
        return currentRoleCode == null ? null : currentRoleCode.toString();
    }
}
