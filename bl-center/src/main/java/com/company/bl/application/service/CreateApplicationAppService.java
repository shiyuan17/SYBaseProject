package com.company.bl.application.service;

import com.company.bl.application.command.CreateApplicationCommand;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.service.ApplicationDomainService;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.infrastructure.observability.ObservedOperation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateApplicationAppService {

    private final ApplicationDomainService applicationDomainService;
    private final ApplicationRepository applicationRepository;

    public CreateApplicationAppService(ApplicationDomainService applicationDomainService,
                                       ApplicationRepository applicationRepository) {
        this.applicationDomainService = applicationDomainService;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    @ObservedOperation(
        operation = "create_application",
        successCounter = "application_create_total",
        failureCounter = "application_create_failed_total",
        durationMetric = "application_create_duration")
    public ApplicationId create(CreateApplicationCommand command) {
        Application application = applicationDomainService.register(
            command.applicationNo(),
            command.patientId(),
            command.applicationType(),
            command.status(),
            command.externalOrderNo(),
            command.thirdPartySource(),
            command.clinicalDiagnosis(),
            command.clinicalSymptom(),
            command.specimenSite(),
            command.applicationDate(),
            command.submissionDate(),
            command.remarks());
        return applicationRepository.save(application).getId();
    }
}
