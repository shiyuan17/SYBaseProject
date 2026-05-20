package com.company.common.security.session;

import java.time.Instant;

public interface TokenSessionValidator {

    boolean isTokenActive(String tokenId, String userId, Instant expiresAt);
}
