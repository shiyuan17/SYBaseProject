package com.company.bl.domain.service;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.factory.ApplicationFactory;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

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
                                String remarks) {
        String normalizedApplicationNo = normalizeApplicationNo(applicationNo);
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
            trimToNull(remarks));
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
