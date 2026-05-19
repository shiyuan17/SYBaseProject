package com.company.bl.application.command;

import java.time.LocalDate;

public record CreateApplicationCommand(
    String applicationNo,
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
    String remarks
) {
}
