package com.company.bl.interfaces;

import com.company.common.security.jwt.JwtAccessTokenClaims;
import com.company.common.security.jwt.Sm2JwtTokenService;
import com.company.common.test.BaseJdbcWebIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

abstract class AuthenticatedWebIntegrationTest extends BaseJdbcWebIntegrationTest {

    @Autowired
    private Sm2JwtTokenService tokenService;

    protected MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder requestBuilder, String userId) {
        String loginName = jdbcTemplate.queryForObject("""
            select login_name
            from users
            where id = :userId
            """, Map.of("userId", userId), String.class);
        if (loginName == null || loginName.isBlank()) {
            throw new IllegalArgumentException("Unknown test user: " + userId);
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenService.getProperties().getAccessTokenTtl());
        String tokenId = "TEST-" + UUID.randomUUID();
        String accessToken = tokenService.generateToken(new JwtAccessTokenClaims(
            tokenId,
            userId,
            loginName,
            issuedAt,
            expiresAt));

        jdbcTemplate.update("""
            insert into auth_access_tokens
                (jti, user_id, issued_at, expires_at, revoked_at, client_ip, client_device)
            values
                (:tokenId, :userId, :issuedAt, :expiresAt, null, :clientIp, :clientDevice)
            """, new MapSqlParameterSource()
            .addValue("tokenId", tokenId)
            .addValue("userId", userId)
            .addValue("issuedAt", LocalDateTime.ofInstant(issuedAt, ZoneOffset.UTC))
            .addValue("expiresAt", LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC))
            .addValue("clientIp", "127.0.0.1")
            .addValue("clientDevice", "MockMvc"));

        return requestBuilder.header("Authorization", "Bearer " + accessToken);
    }
}
