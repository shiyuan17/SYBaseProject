package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import com.company.common.security.exception.SecurityAuthenticationException;
import com.company.common.security.jwt.JwtAccessTokenClaims;
import com.company.common.security.jwt.Sm2JwtTokenService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OperatorVerificationService {

    private static final String TOKEN_PREFIX = "OV";
    private static final long TOKEN_TTL_SECONDS = 300;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Sm2JwtTokenService tokenService;
    private final Sm3PasswordEncoder passwordEncoder;

    public OperatorVerificationService(NamedParameterJdbcTemplate jdbcTemplate,
                                       Sm2JwtTokenService tokenService,
                                       Sm3PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public OperatorVerificationResult verify(String currentUserId,
                                             String operatorUserId,
                                             String loginName,
                                             String password) {
        String normalizedCurrentUserId = requireCurrentUser(currentUserId);
        String normalizedOperatorUserId = requireText(operatorUserId, "请选择核对操作人");
        String normalizedLoginName = requireText(loginName, "请输入核对人账号");
        String normalizedPassword = requireText(password, "请输入核对人密码");
        if (normalizedCurrentUserId.equals(normalizedOperatorUserId)) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409,
                "登录人跟核对人不能是同一个人！");
        }

        OperatorCredential credential = findCredentialById(normalizedOperatorUserId);
        if (credential == null
            || !credential.enabled()
            || !credential.loginName().equals(normalizedLoginName)
            || !isPasswordMatched(credential, normalizedPassword)) {
            throw new BlBusinessException(BlErrorCode.AUTHENTICATION_REQUIRED, 401,
                "核对人账号或密码错误");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(TOKEN_TTL_SECONDS);
        String token = tokenService.generateToken(new JwtAccessTokenClaims(
            TOKEN_PREFIX + ":" + normalizedCurrentUserId + ":" + UUID.randomUUID(),
            credential.id(),
            credential.loginName(),
            issuedAt,
            expiresAt));
        return new OperatorVerificationResult(
            token,
            expiresAt.toString(),
            credential.id(),
            credential.loginName(),
            credential.name());
    }

    public VerifiedOperator resolveVerifiedOperator(String token, String currentUserId) {
        String normalizedCurrentUserId = requireCurrentUser(currentUserId);
        String normalizedToken = requireText(token, "请先完成核对人登录确认");
        JwtAccessTokenClaims claims;
        try {
            claims = tokenService.parseAndValidate(normalizedToken);
        } catch (SecurityAuthenticationException exception) {
            throw new BlBusinessException(BlErrorCode.AUTHENTICATION_REQUIRED, 401,
                "核对人登录确认已失效，请重新确认");
        }

        String tokenId = claims.tokenId();
        String expectedPrefix = TOKEN_PREFIX + ":" + normalizedCurrentUserId + ":";
        if (tokenId == null || !tokenId.startsWith(expectedPrefix)) {
            throw new BlBusinessException(BlErrorCode.AUTHENTICATION_REQUIRED, 401,
                "核对人登录确认与当前登录人不匹配，请重新确认");
        }
        if (normalizedCurrentUserId.equals(claims.userId())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409,
                "登录人跟核对人不能是同一个人！");
        }

        OperatorCredential credential = findCredentialById(claims.userId());
        if (credential == null
            || !credential.enabled()
            || !credential.loginName().equals(claims.loginName())) {
            throw new BlBusinessException(BlErrorCode.AUTHENTICATION_REQUIRED, 401,
                "核对人登录确认已失效，请重新确认");
        }
        return new VerifiedOperator(credential.id(), credential.loginName(), credential.name());
    }

    private OperatorCredential findCredentialById(String userId) {
        List<OperatorCredential> rows = jdbcTemplate.query("""
            select id, login_name, name, password, password_algo, password_salt, enabled
            from users
            where id = :userId
            """, new MapSqlParameterSource().addValue("userId", userId), this::mapCredential);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private OperatorCredential mapCredential(ResultSet rs, int rowNum) throws SQLException {
        return new OperatorCredential(
            rs.getString("id"),
            rs.getString("login_name"),
            rs.getString("name"),
            rs.getString("password"),
            rs.getString("password_algo"),
            rs.getString("password_salt"),
            rs.getInt("enabled") == 1);
    }

    private boolean isPasswordMatched(OperatorCredential credential, String rawPassword) {
        String passwordAlgo = credential.passwordAlgo();
        if (passwordAlgo == null || passwordAlgo.isBlank()
            || Sm3PasswordEncoder.PASSWORD_ALGO_PLAIN.equalsIgnoreCase(passwordAlgo)) {
            return passwordEncoder.matchesPlain(rawPassword, credential.password());
        }
        if (Sm3PasswordEncoder.PASSWORD_ALGO_SM3.equalsIgnoreCase(passwordAlgo)) {
            return passwordEncoder.matchesSm3(
                rawPassword,
                credential.passwordSalt(),
                credential.password());
        }
        return false;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value.trim();
    }

    private String requireCurrentUser(String value) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.AUTHENTICATION_REQUIRED, 401,
                "Authorization bearer token is required");
        }
        return value.trim();
    }

    private record OperatorCredential(
        String id,
        String loginName,
        String name,
        String password,
        String passwordAlgo,
        String passwordSalt,
        boolean enabled
    ) {
    }

    public record OperatorVerificationResult(
        String operatorVerificationToken,
        String expiresAt,
        String operatorUserId,
        String loginName,
        String operatorName
    ) {
    }

    public record VerifiedOperator(
        String operatorUserId,
        String loginName,
        String operatorName
    ) {
    }
}
