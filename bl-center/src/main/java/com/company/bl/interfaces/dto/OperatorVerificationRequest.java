package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "OperatorVerificationRequest", description = "核对操作人登录确认请求")
public class OperatorVerificationRequest {

    @Schema(description = "核对操作人用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String operatorUserId;

    @Schema(description = "核对操作人登录账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String loginName;

    @Schema(description = "核对操作人密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 255)
    private String password;
}
