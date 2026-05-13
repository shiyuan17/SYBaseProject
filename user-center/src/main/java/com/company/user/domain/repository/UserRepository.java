package com.company.user.domain.repository;

import com.company.user.domain.model.User;
import com.company.user.domain.valueobject.UserId;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);

    boolean existsByEmail(String email);
}
