package com.company.bl.domain.factory;

import com.company.bl.domain.enums.ApplicationFormStatus;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.valueobject.ApplicationId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ApplicationFactory {

    public Application create(String applicationNo,
                              String patientId,
                              String patientName,
                              String patientGender,
                              String patientAge,
                              String applicationType,
                              ApplicationStatus status,
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
        LocalDateTime now = LocalDateTime.now();
        return new Application(
            new ApplicationId(UUID.randomUUID().toString()),
            applicationNo,
            patientId,
            patientName,
            patientGender,
            patientAge,
            applicationType,
            status,
            ApplicationFormStatus.NOT_UPLOADED,
            externalOrderNo,
            thirdPartySource,
            sourceHospitalId,
            sourceHospitalName,
            submittingDepartmentId,
            submittingDepartmentName,
            submittingDoctorUserId,
            submittingDoctorName,
            clinicalDiagnosis,
            clinicalSymptom,
            specimenSite,
            applicationDate,
            submissionDate,
            remarks,
            now,
            now);
    }
}
