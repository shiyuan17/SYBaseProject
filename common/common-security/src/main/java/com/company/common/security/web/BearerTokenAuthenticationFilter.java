package com.company.common.security.web;

import com.company.common.security.context.AuthenticatedPrincipal;
import com.company.common.security.context.AuthenticatedPrincipalContext;
import com.company.common.core.i18n.HttpMessageTranslator;
import com.company.common.security.exception.SecurityAuthenticationException;
import com.company.common.security.exception.SecurityErrorCode;
import com.company.common.security.jwt.JwtAccessTokenClaims;
import com.company.common.security.jwt.Sm2JwtTokenService;
import com.company.common.security.session.TokenSessionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final Sm2JwtTokenService tokenService;
    private final TokenSessionValidator tokenSessionValidator;
    private final ObjectMapper objectMapper;

    public BearerTokenAuthenticationFilter(
        Sm2JwtTokenService tokenService,
        TokenSessionValidator tokenSessionValidator,
        ObjectMapper objectMapper
    ) {
        this.tokenService = tokenService;
        this.tokenSessionValidator = tokenSessionValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String accessToken = authorizationHeader.substring("Bearer ".length()).trim();
            JwtAccessTokenClaims claims = tokenService.parseAndValidate(accessToken);
            if (!tokenSessionValidator.isTokenActive(claims.tokenId(), claims.userId(), claims.expiresAt())) {
                throw new SecurityAuthenticationException(
                    SecurityErrorCode.ACCESS_TOKEN_REVOKED,
                    401,
                    "Access token is revoked or unavailable");
            }

            request.setAttribute(
                AuthenticatedPrincipalContext.REQUEST_ATTRIBUTE,
                new AuthenticatedPrincipal(claims.userId(), claims.loginName(), claims.tokenId()));

            filterChain.doFilter(request, response);
        } catch (SecurityAuthenticationException exception) {
            writeAuthenticationFailure(request, response, exception);
        }
    }

    private void writeAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        SecurityAuthenticationException exception
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(exception.getHttpStatus());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", exception.getErrorCode().code());
        body.put("message", HttpMessageTranslator.translate(exception.getMessage()));
        body.put("traceId", traceId(request));
        body.put("data", null);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute("traceId");
        return traceId == null ? "" : traceId.toString();
    }
}
