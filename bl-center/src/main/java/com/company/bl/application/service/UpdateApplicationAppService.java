package com.company.bl.application.service;

import com.company.bl.application.command.UpdateApplicationCommand;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.service.ApplicationDomainService;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.common.web.observability.ObservedOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateApplicationAppService {

    private static final String DOWNSTREAM_STARTED_MESSAGE = "申请单已进入下游流程，不能再编辑或作废";
    private static final String VOIDED_MESSAGE = "申请单已作废，不能再编辑或作废";

    private final ApplicationDomainService applicationDomainService;
    private final ApplicationRepository applicationRepository;
    private final ApplicationRegistrationWorkbenchRepository workbenchRepository;
    private final ApplicationPatientIdentityResolver patientIdentityResolver;

    @Transactional
    @ObservedOperation(
        operation = "update_application",
        successCounter = "application_update_total",
        failureCounter = "application_update_failed_total",
        durationMetric = "application_update_duration")
    public Application update(String applicationId, UpdateApplicationCommand command) {
        Application application = loadEditableApplication(applicationId);
        String resolvedPatientId = patientIdentityResolver.resolveOrCreate(
            command.patientId(),
            command.patientName(),
            command.patientGender(),
            command.patientAge());
        Application updated = applicationDomainService.revise(
            application,
            command.applicationNo(),
            resolvedPatientId,
            command.patientName(),
            command.patientGender(),
            command.patientAge(),
            command.applicationType(),
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
        return applicationRepository.update(updated);
    }

    @Transactional
    @ObservedOperation(
        operation = "void_application",
        successCounter = "application_void_total",
        failureCounter = "application_void_failed_total",
        durationMetric = "application_void_duration")
    public Application voidApplication(String applicationId) {
        Application application = loadEditableApplication(applicationId);
        workbenchRepository.clearPreDownstreamRegistrationData(applicationId);
        Application voided = new Application(
            application.getId(),
            application.getApplicationNo(),
            application.getPatientId(),
            application.getPatientName(),
            application.getPatientGender(),
            application.getPatientAge(),
            application.getApplicationType(),
            ApplicationStatus.VOIDED,
            application.getApplicationFormStatus(),
            application.getExternalOrderNo(),
            application.getThirdPartySource(),
            application.getSourceHospitalId(),
            application.getSourceHospitalName(),
            application.getSubmittingDepartmentId(),
            application.getSubmittingDepartmentName(),
            application.getSubmittingDoctorUserId(),
            application.getSubmittingDoctorName(),
            application.getClinicalDiagnosis(),
            application.getClinicalSymptom(),
            application.getSpecimenSite(),
            application.getApplicationDate(),
            application.getSubmissionDate(),
            application.getSpecimenRemovalTime(),
            application.getRemarks(),
            application.getCreatedAt(),
            java.time.LocalDateTime.now());
        return applicationRepository.update(voided);
    }

    private Application loadEditableApplication(String applicationId) {
        Application application = applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new ApplicationDomainException(
                com.company.bl.domain.enums.ApplicationErrorCode.APPLICATION_NOT_FOUND,
                404));
        if (application.getStatus() == ApplicationStatus.VOIDED) {
            throw new com.company.bl.domain.exception.ApplicationDomainException(
                com.company.bl.domain.enums.ApplicationErrorCode.INVALID_APPLICATION_STATUS,
                409,
                VOIDED_MESSAGE);
        }
        if (workbenchRepository.hasStartedDownstreamWorkflow(applicationId)) {
            throw new com.company.bl.domain.exception.ApplicationDomainException(
                com.company.bl.domain.enums.ApplicationErrorCode.INVALID_APPLICATION_STATUS,
                409,
                DOWNSTREAM_STARTED_MESSAGE);
        }
        return application;
    }
}
