package com.company.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.auth.login-protection")
public class AuthLoginProtectionProperties {

    private boolean enabled = true;
    private int maxFailedAttempts = 5;
    private Duration cooldownDuration = Duration.ofMinutes(15);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public Duration getCooldownDuration() {
        return cooldownDuration;
    }

    public void setCooldownDuration(Duration cooldownDuration) {
        this.cooldownDuration = cooldownDuration;
    }
}
