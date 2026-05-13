package com.company.user.domain.model;

import com.company.user.domain.enums.UserStatus;
import com.company.user.domain.valueobject.UserEmail;
import com.company.user.domain.valueobject.UserId;
import com.company.user.domain.valueobject.UserName;

import java.time.OffsetDateTime;

public class User {

    private final UserId id;
    private final UserName name;
    private final UserEmail email;
    private final UserStatus status;
    private final OffsetDateTime createdAt;

    public User(UserId id, UserName name, UserEmail email, UserStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UserId getId() {
        return id;
    }

    public UserName getName() {
        return name;
    }

    public UserEmail getEmail() {
        return email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
