package com.company.bl.interfaces.auth;

import com.company.common.security.config.SecurityJwtProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BlRuntimeConfigurationGuard {

    private final Environment environment;
    private final SecurityJwtProperties securityJwtProperties;
    private final DataSourceProperties dataSourceProperties;

    public BlRuntimeConfigurationGuard(Environment environment,
                                       SecurityJwtProperties securityJwtProperties,
                                       DataSourceProperties dataSourceProperties) {
        this.environment = environment;
        this.securityJwtProperties = securityJwtProperties;
        this.dataSourceProperties = dataSourceProperties;
    }

    @PostConstruct
    void validate() {
        if (!securityJwtProperties.isAllowGeneratedKeys()) {
            requireText(securityJwtProperties.getSm2().getPrivateKey(),
                "Missing required JWT private key: SECURITY_AUTH_JWT_SM2_PRIVATE_KEY");
            requireText(securityJwtProperties.getSm2().getPublicKey(),
                "Missing required JWT public key: SECURITY_AUTH_JWT_SM2_PUBLIC_KEY");
        }
        if (!environment.acceptsProfiles(Profiles.of("test", "local"))) {
            requireText(dataSourceProperties.getUrl(), "Missing required datasource URL: BL_CENTER_DATASOURCE_URL");
            requireText(dataSourceProperties.getUsername(), "Missing required datasource username: BL_CENTER_DATASOURCE_USERNAME");
            requireText(dataSourceProperties.getPassword(), "Missing required datasource password: BL_CENTER_DATASOURCE_PASSWORD");
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }
}
