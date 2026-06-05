package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OperatorVerificationResponse", description = "核对操作人登录确认结果")
public record OperatorVerificationResponse(
    String operatorVerificationToken,
    String expiresAt,
    String operatorUserId,
    String loginName,
    String operatorName
) {
}
