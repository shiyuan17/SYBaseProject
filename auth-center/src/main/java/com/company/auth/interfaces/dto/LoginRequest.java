package com.company.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 64, message = "登录账号长度不能超过 64")
    String loginName,
    @NotBlank(message = "密码不能为空")
    @Size(max = 255, message = "密码长度不能超过 255")
    String password
) {
}
