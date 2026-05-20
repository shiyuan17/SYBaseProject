package com.company.common.security.jwt;

import java.time.Instant;

public record JwtAccessTokenClaims(
    String tokenId,
    String userId,
    String loginName,
    Instant issuedAt,
    Instant expiresAt
) {
}
