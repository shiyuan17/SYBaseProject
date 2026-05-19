package com.company.bl.domain.model;

import com.company.bl.domain.enums.ApplicationFormStatus;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.valueobject.ApplicationId;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Application {

    private final ApplicationId id;
    private final String applicationNo;
    private final String patientId;
    private final String applicationType;
    private final ApplicationStatus status;
    private final ApplicationFormStatus applicationFormStatus;
    private final String externalOrderNo;
    private final String thirdPartySource;
    private final String clinicalDiagnosis;
    private final String clinicalSymptom;
    private final String specimenSite;
    private final LocalDate applicationDate;
    private final LocalDate submissionDate;
    private final String remarks;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Application(ApplicationId id,
                       String applicationNo,
                       String patientId,
                       String applicationType,
                       ApplicationStatus status,
                       ApplicationFormStatus applicationFormStatus,
                       String externalOrderNo,
                       String thirdPartySource,
                       String clinicalDiagnosis,
                       String clinicalSymptom,
                       String specimenSite,
                       LocalDate applicationDate,
                       LocalDate submissionDate,
                       String remarks,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.applicationNo = applicationNo;
        this.patientId = patientId;
        this.applicationType = applicationType;
        this.status = status;
        this.applicationFormStatus = applicationFormStatus;
        this.externalOrderNo = externalOrderNo;
        this.thirdPartySource = thirdPartySource;
        this.clinicalDiagnosis = clinicalDiagnosis;
        this.clinicalSymptom = clinicalSymptom;
        this.specimenSite = specimenSite;
        this.applicationDate = applicationDate;
        this.submissionDate = submissionDate;
        this.remarks = remarks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
