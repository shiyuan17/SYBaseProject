package com.company.bl.interfaces.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiAuthorizationConfiguration implements WebMvcConfigurer {

    private final ApiPermissionInterceptor apiPermissionInterceptor;

    public ApiAuthorizationConfiguration(ApiPermissionInterceptor apiPermissionInterceptor) {
        this.apiPermissionInterceptor = apiPermissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiPermissionInterceptor).order(0);
    }
}
