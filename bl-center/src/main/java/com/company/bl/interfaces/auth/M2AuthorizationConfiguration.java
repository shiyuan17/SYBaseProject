package com.company.bl.interfaces.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class M2AuthorizationConfiguration implements WebMvcConfigurer {

    private final M2PermissionInterceptor m2PermissionInterceptor;

    public M2AuthorizationConfiguration(M2PermissionInterceptor m2PermissionInterceptor) {
        this.m2PermissionInterceptor = m2PermissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(m2PermissionInterceptor).order(0);
    }
}
