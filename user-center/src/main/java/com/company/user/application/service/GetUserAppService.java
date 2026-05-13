package com.company.user.application.service;

import com.company.user.application.query.GetUserByIdQuery;
import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;
import com.company.user.domain.model.User;
import com.company.user.domain.repository.UserRepository;
import com.company.user.domain.valueobject.UserId;
import com.company.user.infrastructure.config.ObservabilityConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserAppService {

    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;

    public GetUserAppService(UserRepository userRepository, MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public User getById(GetUserByIdQuery query) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Counter successCounter = Counter.builder("user_query_total")
            .tags(ObservabilityConfiguration.operationTags("get_user"))
            .register(meterRegistry);
        Counter notFoundCounter = Counter.builder("user_query_not_found_total")
            .tags(ObservabilityConfiguration.operationTags("get_user"))
            .register(meterRegistry);

        try {
            User user = userRepository.findById(new UserId(query.userId()))
                .orElseThrow(() -> new UserDomainException(UserErrorCode.USER_NOT_FOUND, 404));
            successCounter.increment();
            return user;
        } catch (UserDomainException exception) {
            if (exception.getErrorCode() == UserErrorCode.USER_NOT_FOUND) {
                notFoundCounter.increment();
            }
            throw exception;
        } finally {
            sample.stop(Timer.builder("user_query_duration")
                .tags(ObservabilityConfiguration.operationTags("get_user"))
                .register(meterRegistry));
        }
    }
}
