package com.company.bl.interfaces.controller;

import com.company.bl.application.service.OperatorVerificationService;
import com.company.bl.interfaces.auth.RequireAuthenticated;
import com.company.bl.interfaces.dto.OperatorVerificationRequest;
import com.company.bl.interfaces.vo.OperatorVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operator-verifications")
@RequiredArgsConstructor
@RequireAuthenticated
@Tag(name = "Operator Verification", description = "核对操作人二次登录确认")
public class OperatorVerificationController {

    private final OperatorVerificationService operatorVerificationService;

    @Operation(summary = "Verify operator credential", description = "Verify selected operator credential and issue a short-lived verification token.")
    @PostMapping
    public OperatorVerificationResponse verify(
        @Valid @RequestBody OperatorVerificationRequest request,
        HttpServletRequest httpServletRequest
    ) {
        OperatorVerificationService.OperatorVerificationResult result =
            operatorVerificationService.verify(
                RequestOperatorContext.currentUserId(httpServletRequest),
                request.getOperatorUserId(),
                request.getLoginName(),
                request.getPassword());
        return new OperatorVerificationResponse(
            result.operatorVerificationToken(),
            result.expiresAt(),
            result.operatorUserId(),
            result.loginName(),
            result.operatorName());
    }
}
