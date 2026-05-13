package com.company.user.domain.factory;

import com.company.user.domain.enums.UserStatus;
import com.company.user.domain.model.User;
import com.company.user.domain.valueobject.UserEmail;
import com.company.user.domain.valueobject.UserId;
import com.company.user.domain.valueobject.UserName;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class UserFactory {

    public User create(UserName userName, UserEmail userEmail) {
        return new User(
            new UserId(UUID.randomUUID().toString()),
            userName,
            userEmail,
            UserStatus.ACTIVE,
            OffsetDateTime.now(ZoneOffset.UTC));
    }
}
