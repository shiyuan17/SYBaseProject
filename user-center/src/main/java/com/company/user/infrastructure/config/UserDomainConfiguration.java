package com.company.user.infrastructure.config;

import com.company.user.domain.factory.UserFactory;
import com.company.user.domain.repository.UserRepository;
import com.company.user.domain.service.UserDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfiguration {

    @Bean
    public UserFactory userFactory() {
        return new UserFactory();
    }

    @Bean
    public UserDomainService userDomainService(UserRepository userRepository, UserFactory userFactory) {
        return new UserDomainService(userRepository, userFactory);
    }
}
