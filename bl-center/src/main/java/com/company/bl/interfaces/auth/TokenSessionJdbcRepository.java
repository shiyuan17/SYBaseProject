package com.company.bl.interfaces.auth;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Repository
public class TokenSessionJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TokenSessionJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isAccessTokenActive(String tokenId, String userId, Instant expiresAt) {
        Long count = jdbcTemplate.queryForObject("""
            select count(*)
            from auth_access_tokens
            where jti = :tokenId
              and user_id = :userId
              and revoked_at is null
              and expires_at >= :expiresAt
            """, new MapSqlParameterSource()
            .addValue("tokenId", tokenId)
            .addValue("userId", userId)
            .addValue("expiresAt", LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)), Long.class);
        return count != null && count > 0;
    }
}
