package com.company.auth.interfaces.vo;

import java.util.List;

public record CurrentUserResponse(
    String userId,
    String loginName,
    String realName,
    List<String> roles,
    String avatar,
    String homePath
) {
}
