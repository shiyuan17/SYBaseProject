package com.company.auth.interfaces.controller;

import com.company.auth.application.service.AuthApplicationService;
import com.company.auth.interfaces.dto.LoginRequest;
import com.company.auth.interfaces.vo.AuthLoginResponse;
import com.company.auth.interfaces.vo.CurrentUserResponse;
import com.company.common.security.context.AuthenticatedPrincipal;
import com.company.common.security.context.AuthenticatedPrincipalContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AuthApplicationService.LoginResult result = authApplicationService.login(
            request.loginName(),
            request.password(),
            clientIp(servletRequest),
            clientDevice(servletRequest));
        return new AuthLoginResponse(result.accessToken(), result.expiresAt());
    }

    @PostMapping("/logout")
    public Void logout(HttpServletRequest servletRequest) {
        AuthenticatedPrincipal principal = AuthenticatedPrincipalContext.requirePrincipal(servletRequest);
        authApplicationService.logout(principal);
        return null;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(HttpServletRequest servletRequest) {
        AuthenticatedPrincipal principal = AuthenticatedPrincipalContext.requirePrincipal(servletRequest);
        AuthApplicationService.CurrentUserResult result = authApplicationService.currentUser(principal);
        return new CurrentUserResponse(
            result.userId(),
            result.loginName(),
            result.realName(),
            result.roles(),
            result.avatar(),
            result.homePath());
    }

    @GetMapping("/access-codes")
    public List<String> accessCodes(HttpServletRequest servletRequest) {
        AuthenticatedPrincipal principal = AuthenticatedPrincipalContext.requirePrincipal(servletRequest);
        return authApplicationService.accessCodes(principal);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String clientDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null || userAgent.isBlank() ? "Unknown" : userAgent;
    }
}
