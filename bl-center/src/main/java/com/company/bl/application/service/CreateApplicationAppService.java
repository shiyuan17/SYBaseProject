package com.company.bl.application.service;

import com.company.bl.application.command.CreateApplicationCommand;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.service.ApplicationDomainService;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.infrastructure.observability.ObservedOperation;
import com.company.bl.support.application.NumberingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateApplicationAppService {

    private final ApplicationDomainService applicationDomainService;
    private final ApplicationRepository applicationRepository;
    private final NumberingService numberingService;

    @Transactional
    @ObservedOperation(
        operation = "create_application",
        successCounter = "application_create_total",
        failureCounter = "application_create_failed_total",
        durationMetric = "application_create_duration")
    public ApplicationId create(CreateApplicationCommand command) {
        String applicationNo = command.applicationNo();
        if (applicationNo == null || applicationNo.isBlank()) {
            applicationNo = numberingService.generateApplicationNo();
        }
        Application application = applicationDomainService.register(
            applicationNo,
            command.patientId(),
            command.patientName(),
            command.patientGender(),
            command.patientAge(),
            command.applicationType(),
            command.status(),
            command.externalOrderNo(),
            command.thirdPartySource(),
            command.sourceHospitalId(),
            command.sourceHospitalName(),
            command.submittingDepartmentId(),
            command.submittingDepartmentName(),
            command.submittingDoctorUserId(),
            command.submittingDoctorName(),
            command.clinicalDiagnosis(),
            command.clinicalSymptom(),
            command.specimenSite(),
            command.applicationDate(),
            command.submissionDate(),
            command.specimenRemovalTime(),
            command.applicationFormStatus(),
            command.remarks());
        return applicationRepository.save(application).getId();
    }
}
