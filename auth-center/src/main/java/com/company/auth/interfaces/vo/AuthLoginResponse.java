package com.company.auth.interfaces.vo;

public record AuthLoginResponse(
    String accessToken,
    String expiresAt
) {
}
