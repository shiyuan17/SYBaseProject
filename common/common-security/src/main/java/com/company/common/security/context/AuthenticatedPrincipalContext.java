package com.company.common.security.context;

import com.company.common.security.exception.SecurityAuthenticationException;
import com.company.common.security.exception.SecurityErrorCode;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthenticatedPrincipalContext {

    public static final String REQUEST_ATTRIBUTE = AuthenticatedPrincipal.class.getName();

    private AuthenticatedPrincipalContext() {
    }

    public static AuthenticatedPrincipal currentPrincipal(HttpServletRequest request) {
        Object principal = request.getAttribute(REQUEST_ATTRIBUTE);
        return principal instanceof AuthenticatedPrincipal authenticatedPrincipal ? authenticatedPrincipal : null;
    }

    public static AuthenticatedPrincipal requirePrincipal(HttpServletRequest request) {
        AuthenticatedPrincipal principal = currentPrincipal(request);
        if (principal == null) {
            throw new SecurityAuthenticationException(
                SecurityErrorCode.AUTHENTICATION_REQUIRED,
                401,
                "Authorization bearer token is required");
        }
        return principal;
    }
}
