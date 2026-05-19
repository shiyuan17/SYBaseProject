package com.company.bl.infrastructure.config;

import com.company.bl.domain.factory.ApplicationFactory;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.service.ApplicationDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationDomainConfiguration {

    @Bean
    public ApplicationFactory applicationFactory() {
        return new ApplicationFactory();
    }

    @Bean
    public ApplicationDomainService applicationDomainService(ApplicationRepository applicationRepository,
                                                             ApplicationFactory applicationFactory) {
        return new ApplicationDomainService(applicationRepository, applicationFactory);
    }
}
