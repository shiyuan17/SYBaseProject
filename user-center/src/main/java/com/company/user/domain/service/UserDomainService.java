package com.company.user.domain.service;

import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;
import com.company.user.domain.factory.UserFactory;
import com.company.user.domain.model.User;
import com.company.user.domain.repository.UserRepository;
import com.company.user.domain.valueobject.UserEmail;
import com.company.user.domain.valueobject.UserName;

public class UserDomainService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;

    public UserDomainService(UserRepository userRepository, UserFactory userFactory) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
    }

    public User register(String name, String email) {
        UserName userName = new UserName(name);
        UserEmail userEmail = new UserEmail(email);
        if (userRepository.existsByEmail(userEmail.value())) {
            throw new UserDomainException(UserErrorCode.USER_EMAIL_CONFLICT, 409);
        }
        return userFactory.create(userName, userEmail);
    }
}
