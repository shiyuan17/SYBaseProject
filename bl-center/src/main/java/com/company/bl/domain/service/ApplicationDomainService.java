package com.company.bl.domain.service;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.enums.ApplicationFormStatus;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.factory.ApplicationFactory;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ApplicationDomainService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationFactory applicationFactory;

    public Application register(String applicationNo,
                                String patientId,
                                String patientName,
                                String patientGender,
                                String patientAge,
                                String applicationType,
                                String status,
                                String externalOrderNo,
                                String thirdPartySource,
                                String sourceHospitalId,
                                String sourceHospitalName,
                                String submittingDepartmentId,
                                String submittingDepartmentName,
                                String submittingDoctorUserId,
                                String submittingDoctorName,
                                String clinicalDiagnosis,
                                String clinicalSymptom,
                                String specimenSite,
                                LocalDate applicationDate,
                                LocalDate submissionDate,
                                LocalDateTime specimenRemovalTime,
                                String applicationFormStatus,
                                String remarks) {
        String normalizedApplicationNo = normalizeApplicationNo(applicationNo);
        validateRequiredFields(
            patientId,
            patientName,
            applicationType,
            submittingDepartmentId,
            submittingDepartmentName,
            submittingDoctorUserId,
            submittingDoctorName,
            clinicalDiagnosis,
            specimenSite);
        if (applicationRepository.existsByApplicationNo(normalizedApplicationNo)) {
            throw new ApplicationDomainException(ApplicationErrorCode.APPLICATION_NO_CONFLICT, 409);
        }
        return applicationFactory.create(
            normalizedApplicationNo,
            trimToNull(patientId),
            trimToNull(patientName),
            trimToNull(patientGender),
            trimToNull(patientAge),
            trimToNull(applicationType),
            ApplicationStatus.from(status),
            trimToNull(externalOrderNo),
            trimToNull(thirdPartySource),
            trimToNull(sourceHospitalId),
            trimToNull(sourceHospitalName),
            trimToNull(submittingDepartmentId),
            trimToNull(submittingDepartmentName),
            trimToNull(submittingDoctorUserId),
            trimToNull(submittingDoctorName),
            trimToNull(clinicalDiagnosis),
            trimToNull(clinicalSymptom),
            trimToNull(specimenSite),
            applicationDate,
            submissionDate,
            specimenRemovalTime,
            ApplicationFormStatus.from(applicationFormStatus),
            trimToNull(remarks));
    }

    public Application revise(Application existing,
                              String applicationNo,
                              String patientId,
                              String patientName,
                              String patientGender,
                              String patientAge,
                              String applicationType,
                              String externalOrderNo,
                              String thirdPartySource,
                              String sourceHospitalId,
                              String sourceHospitalName,
                              String submittingDepartmentId,
                              String submittingDepartmentName,
                              String submittingDoctorUserId,
                              String submittingDoctorName,
                              String clinicalDiagnosis,
                              String clinicalSymptom,
                              String specimenSite,
                              LocalDate applicationDate,
                              LocalDate submissionDate,
                              LocalDateTime specimenRemovalTime,
                              String applicationFormStatus,
                              String remarks) {
        String normalizedApplicationNo = normalizeApplicationNo(applicationNo);
        validateRequiredFields(
            patientId,
            patientName,
            applicationType,
            submittingDepartmentId,
            submittingDepartmentName,
            submittingDoctorUserId,
            submittingDoctorName,
            clinicalDiagnosis,
            specimenSite);
        applicationRepository.findByApplicationNo(normalizedApplicationNo)
            .filter(found -> !found.getId().equals(existing.getId()))
            .ifPresent((found) -> {
                throw new ApplicationDomainException(ApplicationErrorCode.APPLICATION_NO_CONFLICT, 409);
            });
        return new Application(
            new ApplicationId(existing.getId().value()),
            normalizedApplicationNo,
            trimToNull(patientId),
            trimToNull(patientName),
            trimToNull(patientGender),
            trimToNull(patientAge),
            trimToNull(applicationType),
            existing.getStatus(),
            ApplicationFormStatus.from(applicationFormStatus),
            trimToNull(externalOrderNo),
            trimToNull(thirdPartySource),
            trimToNull(sourceHospitalId),
            trimToNull(sourceHospitalName),
            trimToNull(submittingDepartmentId),
            trimToNull(submittingDepartmentName),
            trimToNull(submittingDoctorUserId),
            trimToNull(submittingDoctorName),
            trimToNull(clinicalDiagnosis),
            trimToNull(clinicalSymptom),
            trimToNull(specimenSite),
            applicationDate,
            submissionDate,
            specimenRemovalTime,
            trimToNull(remarks),
            existing.getCreatedAt(),
            LocalDateTime.now());
    }

    private void validateRequiredFields(String patientId,
                                        String patientName,
                                        String applicationType,
                                        String submittingDepartmentId,
                                        String submittingDepartmentName,
                                        String submittingDoctorUserId,
                                        String submittingDoctorName,
                                        String clinicalDiagnosis,
                                        String specimenSite) {
        if (trimToNull(patientId) == null && trimToNull(patientName) == null) {
            throw invalidField("Patient id or patient name must be provided");
        }
        requireText(applicationType, "Application type must not be blank");
        requireText(submittingDepartmentId, "Submitting department id must not be blank");
        requireText(submittingDepartmentName, "Submitting department name must not be blank");
        requireText(submittingDoctorUserId, "Submitting doctor user id must not be blank");
        requireText(submittingDoctorName, "Submitting doctor name must not be blank");
        requireText(clinicalDiagnosis, "Clinical diagnosis must not be blank");
        requireText(specimenSite, "Specimen site must not be blank");
    }

    private void requireText(String value, String message) {
        if (trimToNull(value) == null) {
            throw invalidField(message);
        }
    }

    private ApplicationDomainException invalidField(String message) {
        return new ApplicationDomainException(ApplicationErrorCode.INVALID_APPLICATION_FIELD, 400, message);
    }

    private String normalizeApplicationNo(String applicationNo) {
        if (applicationNo == null) {
            throw new ApplicationDomainException(ApplicationErrorCode.INVALID_APPLICATION_NO, 400);
        }
        String normalized = applicationNo.trim();
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new ApplicationDomainException(ApplicationErrorCode.INVALID_APPLICATION_NO, 400);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
