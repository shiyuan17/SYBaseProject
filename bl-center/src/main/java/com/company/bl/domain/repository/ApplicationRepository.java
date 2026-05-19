package com.company.bl.domain.repository;

import com.company.bl.domain.model.Application;
import com.company.bl.domain.valueobject.ApplicationId;

import java.util.Optional;

public interface ApplicationRepository {

    Application save(Application application);

    Optional<Application> findById(ApplicationId applicationId);

    boolean existsByApplicationNo(String applicationNo);
}
