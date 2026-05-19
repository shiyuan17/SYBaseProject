package com.company.bl.interfaces.controller;

import com.company.bl.interfaces.auth.ApiPermissionContext;
import jakarta.servlet.http.HttpServletRequest;

abstract class TechnicalControllerSupport {

    protected String resolveUserId(String bodyUserId, HttpServletRequest request) {
        if (bodyUserId != null && !bodyUserId.isBlank()) {
            return bodyUserId.trim();
        }
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }
}
