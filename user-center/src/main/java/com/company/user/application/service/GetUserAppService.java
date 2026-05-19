package com.company.user.application.service;

import com.company.user.application.query.GetUserByIdQuery;
import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;
import com.company.user.domain.model.User;
import com.company.user.domain.repository.UserRepository;
import com.company.user.domain.valueobject.UserId;
import com.company.user.infrastructure.observability.ObservedOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserAppService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @ObservedOperation(
        operation = "get_user",
        successCounter = "user_query_total",
        failureCounter = "user_query_failed_total",
        durationMetric = "user_query_duration")
    public User getById(GetUserByIdQuery query) {
        return userRepository.findById(new UserId(query.userId()))
            .orElseThrow(() -> new UserDomainException(UserErrorCode.USER_NOT_FOUND, 404));
    }
}
