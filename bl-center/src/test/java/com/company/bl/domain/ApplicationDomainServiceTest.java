package com.company.bl.domain;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.factory.ApplicationFactory;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.service.ApplicationDomainService;
import com.company.bl.domain.valueobject.ApplicationId;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationDomainServiceTest {

    private final ApplicationRepository applicationRepository = new ApplicationRepository() {
        private final Map<String, Application> applications = new HashMap<>();

        @Override
        public Application save(Application application) {
            applications.put(application.getId().value(), application);
            return application;
        }

        @Override
        public Optional<Application> findById(ApplicationId applicationId) {
            return Optional.ofNullable(applications.get(applicationId.value()));
        }

        @Override
        public boolean existsByApplicationNo(String applicationNo) {
            return applications.values().stream()
                .anyMatch(application -> application.getApplicationNo().equals(applicationNo));
        }
    };

    private final ApplicationDomainService applicationDomainService =
        new ApplicationDomainService(applicationRepository, new ApplicationFactory());

    @Test
    void shouldCreateApplicationWithDefaultStatuses() {
        Application application = applicationDomainService.register(
            "APP-001", null, "ROUTINE", null, null, null, null, null, null, null, null, null);

        assertNotNull(application.getId());
        assertEquals("APP-001", application.getApplicationNo());
        assertEquals(ApplicationStatus.DRAFT, application.getStatus());
        assertEquals("NOT_UPLOADED", application.getApplicationFormStatus().name());
    }

    @Test
    void shouldRejectDuplicateApplicationNumber() {
        applicationRepository.save(applicationDomainService.register(
            "APP-001", null, null, null, null, null, null, null, null, null, null, null));

        ApplicationDomainException exception = assertThrows(
            ApplicationDomainException.class,
            () -> applicationDomainService.register(
                "APP-001", null, null, null, null, null, null, null, null, null, null, null));

        assertEquals(ApplicationErrorCode.APPLICATION_NO_CONFLICT.code(), exception.getErrorCode().code());
    }

    @Test
    void shouldRejectBlankApplicationNumber() {
        ApplicationDomainException exception = assertThrows(
            ApplicationDomainException.class,
            () -> applicationDomainService.register(
                "   ", null, null, null, null, null, null, null, null, null, null, null));

        assertEquals(ApplicationErrorCode.INVALID_APPLICATION_NO.code(), exception.getErrorCode().code());
    }

    @Test
    void shouldRejectUnsupportedStatus() {
        ApplicationDomainException exception = assertThrows(
            ApplicationDomainException.class,
            () -> applicationDomainService.register(
                "APP-002", null, null, "UNKNOWN", null, null, null, null, null, null, null, null));

        assertEquals(ApplicationErrorCode.INVALID_APPLICATION_STATUS.code(), exception.getErrorCode().code());
    }
}
