package com.company.user.application.service;

import com.company.user.application.command.CreateUserCommand;
import com.company.user.domain.model.User;
import com.company.user.domain.repository.UserRepository;
import com.company.user.domain.service.UserDomainService;
import com.company.user.domain.valueobject.UserId;
import com.company.user.infrastructure.observability.ObservedOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateUserAppService {

    private final UserDomainService userDomainService;
    private final UserRepository userRepository;

    @Transactional
    @ObservedOperation(
        operation = "create_user",
        successCounter = "user_create_total",
        failureCounter = "user_create_failed_total",
        durationMetric = "user_create_duration")
    public UserId create(CreateUserCommand command) {
        User user = userDomainService.register(command.name(), command.email());
        return userRepository.save(user).getId();
    }
}
