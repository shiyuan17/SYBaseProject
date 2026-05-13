package com.company.user.domain;

import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;
import com.company.user.domain.factory.UserFactory;
import com.company.user.domain.model.User;
import com.company.user.domain.repository.UserRepository;
import com.company.user.domain.service.UserDomainService;
import com.company.user.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserDomainServiceTest {

    private final UserRepository userRepository = new UserRepository() {
        private final Map<String, User> users = new HashMap<>();

        @Override
        public User save(User user) {
            users.put(user.getId().value(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UserId userId) {
            return Optional.ofNullable(users.get(userId.value()));
        }

        @Override
        public boolean existsByEmail(String email) {
            return users.values().stream().anyMatch(user -> user.getEmail().value().equals(email));
        }
    };

    private final UserDomainService userDomainService = new UserDomainService(userRepository, new UserFactory());

    @Test
    void shouldCreateUserWhenInputIsValid() {
        User user = userDomainService.register("Alice", "alice@example.com");

        assertNotNull(user.getId());
        assertEquals("Alice", user.getName().value());
        assertEquals("alice@example.com", user.getEmail().value());
    }

    @Test
    void shouldRejectDuplicateEmailWhenEmailAlreadyExists() {
        userRepository.save(userDomainService.register("Alice", "alice@example.com"));

        UserDomainException exception = assertThrows(
            UserDomainException.class,
            () -> userDomainService.register("Bob", "alice@example.com"));

        assertEquals(UserErrorCode.USER_EMAIL_CONFLICT.code(), exception.getErrorCode().code());
    }

    @Test
    void shouldRejectInvalidEmailWhenEmailFormatIsWrong() {
        UserDomainException exception = assertThrows(
            UserDomainException.class,
            () -> userDomainService.register("Alice", "invalid-email"));

        assertEquals(UserErrorCode.INVALID_USER_EMAIL.code(), exception.getErrorCode().code());
    }
}
