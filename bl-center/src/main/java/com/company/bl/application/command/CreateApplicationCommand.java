package com.company.bl.application.command;

import java.time.LocalDate;

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
    String remarks
) {
}
