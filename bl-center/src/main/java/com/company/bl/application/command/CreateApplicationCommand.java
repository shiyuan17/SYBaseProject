package com.company.bl.application.command;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateApplicationCommand(
    String applicationNo,
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
    String remarks
) {
}
