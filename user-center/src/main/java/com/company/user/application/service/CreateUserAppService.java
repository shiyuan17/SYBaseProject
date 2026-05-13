package com.company.user.application.service;

import com.company.user.application.command.CreateUserCommand;
import com.company.user.domain.model.User;
import com.company.user.domain.repository.UserRepository;
import com.company.user.domain.service.UserDomainService;
import com.company.user.domain.valueobject.UserId;
import com.company.user.infrastructure.config.ObservabilityConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserAppService {

    private final UserDomainService userDomainService;
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;

    public CreateUserAppService(UserDomainService userDomainService,
                                UserRepository userRepository,
                                MeterRegistry meterRegistry) {
        this.userDomainService = userDomainService;
        this.userRepository = userRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public UserId create(CreateUserCommand command) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Counter successCounter = Counter.builder("user_create_total")
            .tags(ObservabilityConfiguration.operationTags("create_user"))
            .register(meterRegistry);
        Counter failureCounter = Counter.builder("user_create_failed_total")
            .tags(ObservabilityConfiguration.operationTags("create_user"))
            .register(meterRegistry);

        try {
            User user = userDomainService.register(command.name(), command.email());
            UserId userId = userRepository.save(user).getId();
            successCounter.increment();
            return userId;
        } catch (RuntimeException exception) {
            failureCounter.increment();
            throw exception;
        } finally {
            sample.stop(Timer.builder("user_create_duration")
                .tags(ObservabilityConfiguration.operationTags("create_user"))
                .register(meterRegistry));
        }
    }
}
