package com.company.bl.interfaces.controller;

import com.company.bl.interfaces.auth.ApiPermissionContext;
import jakarta.servlet.http.HttpServletRequest;

abstract class TechnicalControllerSupport {

    protected String resolveUserId(String bodyUserId, HttpServletRequest request) {
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }

    protected String resolveRoleCode(HttpServletRequest request) {
        Object currentRoleCode = request.getAttribute(ApiPermissionContext.CURRENT_ROLE_CODE);
        return currentRoleCode == null ? null : currentRoleCode.toString();
    }
}
