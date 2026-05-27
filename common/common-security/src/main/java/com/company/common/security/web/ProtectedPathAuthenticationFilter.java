package com.company.common.security.web;

import com.company.common.security.context.AuthenticatedPrincipalContext;
import com.company.common.core.i18n.HttpMessageTranslator;
import com.company.common.security.exception.SecurityErrorCode;
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

public class ProtectedPathAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public ProtectedPathAuthenticationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (AuthenticatedPrincipalContext.currentPrincipal(request) == null) {
            writeAuthenticationFailure(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeAuthenticationFailure(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", SecurityErrorCode.AUTHENTICATION_REQUIRED.code());
        body.put("message", HttpMessageTranslator.translate(SecurityErrorCode.AUTHENTICATION_REQUIRED.message()));
        body.put("traceId", traceId(request));
        body.put("data", null);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute("traceId");
        return traceId == null ? "" : traceId.toString();
    }
}
