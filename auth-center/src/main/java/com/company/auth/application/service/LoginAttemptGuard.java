package com.company.auth.application.service;

import com.company.auth.infrastructure.config.AuthLoginProtectionProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptGuard {

    private final AuthLoginProtectionProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, AttemptState> states = new ConcurrentHashMap<>();

    public LoginAttemptGuard(AuthLoginProtectionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean isBlocked(String loginName, String clientIp) {
        if (!properties.isEnabled()) {
            return false;
        }
        return isKeyBlocked(attemptKey(loginName, clientIp));
    }

    public void recordFailure(String loginName, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }
        registerFailure(attemptKey(loginName, clientIp));
    }

    public void recordSuccess(String loginName, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }
        states.remove(attemptKey(loginName, clientIp));
    }

    private boolean isKeyBlocked(String key) {
        AttemptState state = states.get(key);
        if (state == null) {
            return false;
        }
        Instant now = clock.instant();
        if (state.lockedUntil != null && state.lockedUntil.isAfter(now)) {
            return true;
        }
        if (state.lockedUntil != null && !state.lockedUntil.isAfter(now)) {
            states.remove(key, state);
        }
        return false;
    }

    private void registerFailure(String key) {
        Instant now = clock.instant();
        states.compute(key, (ignored, current) -> {
            AttemptState next = current;
            if (next == null || next.lockedUntilExpired(now)) {
                next = new AttemptState();
            }
            next.failedAttempts++;
            if (next.failedAttempts >= properties.getMaxFailedAttempts()) {
                next.lockedUntil = now.plus(properties.getCooldownDuration());
            }
            return next;
        });
    }

    private String attemptKey(String loginName, String clientIp) {
        String normalizedLogin = normalize(loginName);
        if ("unknown".equals(normalizedLogin)) {
            return "ip:" + normalize(clientIp);
        }
        return "login-ip:" + normalizedLogin + "|" + normalize(clientIp);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class AttemptState {
        private int failedAttempts;
        private Instant lockedUntil;

        private boolean lockedUntilExpired(Instant now) {
            return lockedUntil != null && !lockedUntil.isAfter(now);
        }
    }
}
