package com.company.common.security.context;

public record AuthenticatedPrincipal(
    String userId,
    String loginName,
    String tokenId
) {
}
