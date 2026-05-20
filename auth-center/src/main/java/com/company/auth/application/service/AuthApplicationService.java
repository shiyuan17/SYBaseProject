package com.company.auth.application.service;

import com.company.auth.domain.enums.AuthCenterErrorCode;
import com.company.auth.domain.exception.AuthCenterException;
import com.company.auth.infrastructure.repository.AuthJdbcRepository;
import com.company.common.security.context.AuthenticatedPrincipal;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import com.company.common.security.jwt.JwtAccessTokenClaims;
import com.company.common.security.jwt.Sm2JwtTokenService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class AuthApplicationService {

    private final AuthJdbcRepository authJdbcRepository;
    private final Sm3PasswordEncoder sm3PasswordEncoder;
    private final Sm2JwtTokenService tokenService;
    private final LoginAttemptGuard loginAttemptGuard;

    public AuthApplicationService(
        AuthJdbcRepository authJdbcRepository,
        Sm3PasswordEncoder sm3PasswordEncoder,
        Sm2JwtTokenService tokenService,
        LoginAttemptGuard loginAttemptGuard
    ) {
        this.authJdbcRepository = authJdbcRepository;
        this.sm3PasswordEncoder = sm3PasswordEncoder;
        this.tokenService = tokenService;
        this.loginAttemptGuard = loginAttemptGuard;
    }

    public LoginResult login(String loginName, String password, String clientIp, String clientDevice) {
        LocalDateTime loginAt = LocalDateTime.now();
        if (loginAttemptGuard.isBlocked(loginName, clientIp)) {
            authJdbcRepository.recordLogin(new AuthJdbcRepository.LoginLogRow(
                "LOGIN-" + UUID.randomUUID(),
                null,
                loginName,
                "FAILED",
                clientIp,
                clientDevice,
                loginAt,
                "Login temporarily locked",
                "login failed"));
            throw new AuthCenterException(
                AuthCenterErrorCode.INVALID_CREDENTIALS,
                401,
                "Login name or password is incorrect");
        }
        AuthJdbcRepository.AuthUserRow user = authJdbcRepository.findUserByLoginName(loginName);
        if (user == null || !isPasswordMatched(user, password)) {
            loginAttemptGuard.recordFailure(loginName, clientIp);
            authJdbcRepository.recordLogin(new AuthJdbcRepository.LoginLogRow(
                "LOGIN-" + UUID.randomUUID(),
                user == null ? null : user.id(),
                loginName,
                "FAILED",
                clientIp,
                clientDevice,
                loginAt,
                "Bad credentials",
                "login failed"));
            throw new AuthCenterException(
                AuthCenterErrorCode.INVALID_CREDENTIALS,
                401,
                "Login name or password is incorrect");
        }
        if (!user.enabled()) {
            loginAttemptGuard.recordFailure(loginName, clientIp);
            authJdbcRepository.recordLogin(new AuthJdbcRepository.LoginLogRow(
                "LOGIN-" + UUID.randomUUID(),
                user.id(),
                loginName,
                "FAILED",
                clientIp,
                clientDevice,
                loginAt,
                "Account disabled",
                "login failed"));
            throw new AuthCenterException(AuthCenterErrorCode.USER_DISABLED, 403, "Current account is disabled");
        }

        if (needsPasswordUpgrade(user)) {
            String salt = sm3PasswordEncoder.generateSalt();
            authJdbcRepository.updatePassword(
                user.id(),
                sm3PasswordEncoder.encode(password, salt),
                Sm3PasswordEncoder.PASSWORD_ALGO_SM3,
                salt);
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenService.getProperties().getAccessTokenTtl());
        String tokenId = "AT-" + UUID.randomUUID();
        String accessToken = tokenService.generateToken(new JwtAccessTokenClaims(
            tokenId,
            user.id(),
            user.loginName(),
            issuedAt,
            expiresAt));

        authJdbcRepository.saveAccessToken(new AuthJdbcRepository.AccessTokenRow(
            tokenId,
            user.id(),
            LocalDateTime.ofInstant(issuedAt, ZoneOffset.UTC),
            LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC),
            null,
            clientIp,
            clientDevice));
        authJdbcRepository.updateLastLogin(user.id(), clientIp, clientDevice, loginAt);
        authJdbcRepository.recordLogin(new AuthJdbcRepository.LoginLogRow(
            "LOGIN-" + UUID.randomUUID(),
            user.id(),
            user.loginName(),
            "SUCCESS",
            clientIp,
            clientDevice,
            loginAt,
            null,
            "login success"));
        loginAttemptGuard.recordSuccess(loginName, clientIp);

        return new LoginResult(accessToken, expiresAt.toString());
    }

    public CurrentUserResult currentUser(AuthenticatedPrincipal principal) {
        AuthJdbcRepository.AuthUserRow user = authJdbcRepository.findUserById(principal.userId());
        List<String> roles = authJdbcRepository.findRoleCodes(principal.userId());
        return new CurrentUserResult(
            principal.userId(),
            principal.loginName(),
            user == null ? principal.loginName() : user.name(),
            roles,
            user == null ? null : user.avatar(),
            null);
    }

    public List<String> accessCodes(AuthenticatedPrincipal principal) {
        return authJdbcRepository.findAccessCodes(principal.userId());
    }

    public void logout(AuthenticatedPrincipal principal) {
        authJdbcRepository.revokeAccessToken(principal.tokenId());
    }

    private boolean isPasswordMatched(AuthJdbcRepository.AuthUserRow user, String rawPassword) {
        String passwordAlgo = user.passwordAlgo();
        if (passwordAlgo == null || passwordAlgo.isBlank() || Sm3PasswordEncoder.PASSWORD_ALGO_PLAIN.equalsIgnoreCase(passwordAlgo)) {
            return sm3PasswordEncoder.matchesPlain(rawPassword, user.password());
        }
        if (Sm3PasswordEncoder.PASSWORD_ALGO_SM3.equalsIgnoreCase(passwordAlgo)) {
            return sm3PasswordEncoder.matchesSm3(rawPassword, user.passwordSalt(), user.password());
        }
        return false;
    }

    private boolean needsPasswordUpgrade(AuthJdbcRepository.AuthUserRow user) {
        String passwordAlgo = user.passwordAlgo();
        return passwordAlgo == null
            || passwordAlgo.isBlank()
            || Sm3PasswordEncoder.PASSWORD_ALGO_PLAIN.equalsIgnoreCase(passwordAlgo);
    }

    public record LoginResult(String accessToken, String expiresAt) {
    }

    public record CurrentUserResult(
        String userId,
        String loginName,
        String realName,
        List<String> roles,
        String avatar,
        String homePath
    ) {
    }
}
