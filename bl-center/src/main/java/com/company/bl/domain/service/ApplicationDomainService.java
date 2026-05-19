package com.company.bl.domain.service;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.factory.ApplicationFactory;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.repository.ApplicationRepository;

import java.time.LocalDate;

public class ApplicationDomainService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationFactory applicationFactory;

    public ApplicationDomainService(ApplicationRepository applicationRepository,
                                    ApplicationFactory applicationFactory) {
        this.applicationRepository = applicationRepository;
        this.applicationFactory = applicationFactory;
    }

    public Application register(String applicationNo,
                                String patientId,
                                String applicationType,
                                String status,
                                String externalOrderNo,
                                String thirdPartySource,
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
            trimToNull(applicationType),
            ApplicationStatus.from(status),
            trimToNull(externalOrderNo),
            trimToNull(thirdPartySource),
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
