package com.company.auth.infrastructure.config;

import com.company.auth.infrastructure.repository.AuthJdbcRepository;
import com.company.common.security.config.SecurityJwtProperties;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import com.company.common.security.jwt.Sm2JwtTokenService;
import com.company.common.security.session.TokenSessionValidator;
import com.company.common.security.web.BearerTokenAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SecurityJwtProperties.class)
public class AuthSecurityConfiguration {

    @Bean
    public Sm2JwtTokenService sm2JwtTokenService(ObjectMapper objectMapper, SecurityJwtProperties properties) {
        return new Sm2JwtTokenService(objectMapper, properties);
    }

    @Bean
    public Sm3PasswordEncoder sm3PasswordEncoder() {
        return new Sm3PasswordEncoder();
    }

    @Bean
    public TokenSessionValidator tokenSessionValidator(AuthJdbcRepository authJdbcRepository) {
        return authJdbcRepository::isAccessTokenActive;
    }

    @Bean
    public FilterRegistrationBean<BearerTokenAuthenticationFilter> bearerTokenAuthenticationFilter(
        ObjectMapper objectMapper,
        Sm2JwtTokenService tokenService,
        TokenSessionValidator tokenSessionValidator
    ) {
        FilterRegistrationBean<BearerTokenAuthenticationFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new BearerTokenAuthenticationFilter(tokenService, tokenSessionValidator, objectMapper));
        bean.addUrlPatterns("/*");
        bean.setOrder(20);
        return bean;
    }
}
