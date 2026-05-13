package com.company.user.infrastructure.persistence;

import com.company.user.domain.model.User;
import com.company.user.domain.repository.UserRepository;
import com.company.user.domain.valueobject.UserId;
import com.company.user.infrastructure.convert.UserInfrastructureConverter;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, UserDataObject> users = new ConcurrentHashMap<>();
    private final Map<String, String> emailIndex = new ConcurrentHashMap<>();
    private final UserInfrastructureConverter userInfrastructureConverter;

    public InMemoryUserRepository(UserInfrastructureConverter userInfrastructureConverter) {
        this.userInfrastructureConverter = userInfrastructureConverter;
    }

    @Override
    public User save(User user) {
        UserDataObject dataObject = userInfrastructureConverter.toDataObject(user);
        users.put(dataObject.getId(), dataObject);
        emailIndex.put(dataObject.getEmail(), dataObject.getId());
        return userInfrastructureConverter.toDomain(dataObject);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(users.get(userId.value()))
            .map(userInfrastructureConverter::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return emailIndex.containsKey(email);
    }
}
