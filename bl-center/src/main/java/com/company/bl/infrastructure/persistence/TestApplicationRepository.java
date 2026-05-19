package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("test")
public class TestApplicationRepository implements ApplicationRepository {

    private final Map<String, Application> applications = new ConcurrentHashMap<>();
    private final Map<String, String> applicationNoIndex = new ConcurrentHashMap<>();

    @Override
    public Application save(Application application) {
        applications.put(application.getId().value(), application);
        applicationNoIndex.put(application.getApplicationNo(), application.getId().value());
        return application;
    }

    @Override
    public Optional<Application> findById(ApplicationId applicationId) {
        return Optional.ofNullable(applications.get(applicationId.value()));
    }

    @Override
    public boolean existsByApplicationNo(String applicationNo) {
        return applicationNoIndex.containsKey(applicationNo);
    }
}
