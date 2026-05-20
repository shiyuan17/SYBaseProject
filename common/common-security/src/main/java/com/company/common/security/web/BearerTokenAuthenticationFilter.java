package com.company.common.security.web;

import com.company.common.security.context.AuthenticatedPrincipal;
import com.company.common.security.context.AuthenticatedPrincipalContext;
import com.company.common.security.exception.SecurityAuthenticationException;
import com.company.common.security.exception.SecurityErrorCode;
import com.company.common.security.jwt.JwtAccessTokenClaims;
import com.company.common.security.jwt.Sm2JwtTokenService;
import com.company.common.security.session.TokenSessionValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final Sm2JwtTokenService tokenService;
    private final TokenSessionValidator tokenSessionValidator;

    public BearerTokenAuthenticationFilter(
        Sm2JwtTokenService tokenService,
        TokenSessionValidator tokenSessionValidator
    ) {
        this.tokenService = tokenService;
        this.tokenSessionValidator = tokenSessionValidator;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
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
    }
}
