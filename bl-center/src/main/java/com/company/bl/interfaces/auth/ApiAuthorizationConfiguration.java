package com.company.bl.interfaces.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiAuthorizationConfiguration implements WebMvcConfigurer {

    private final ApiPermissionInterceptor apiPermissionInterceptor;
    private final ApiAuditInterceptor apiAuditInterceptor;

    public ApiAuthorizationConfiguration(ApiPermissionInterceptor apiPermissionInterceptor,
                                         ApiAuditInterceptor apiAuditInterceptor) {
        this.apiPermissionInterceptor = apiPermissionInterceptor;
        this.apiAuditInterceptor = apiAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiPermissionInterceptor).order(0);
        registry.addInterceptor(apiAuditInterceptor).order(1);
    }
}
