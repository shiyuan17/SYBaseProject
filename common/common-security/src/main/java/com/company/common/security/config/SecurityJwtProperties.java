package com.company.common.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.auth.jwt")
public class SecurityJwtProperties {

    private Duration accessTokenTtl = Duration.ofHours(8);
    private String issuer = "sy-base-project";
    private final Sm2Properties sm2 = new Sm2Properties();

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Sm2Properties getSm2() {
        return sm2;
    }

    public static class Sm2Properties {
        private String privateKey;
        private String publicKey;

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }
    }
}
