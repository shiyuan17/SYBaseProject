package com.company.bl.interfaces.vo;

public record ApplicationDetailResponse(
    String id,
    String applicationNo,
    String patientId,
    String applicationType,
    String status,
    String applicationFormStatus,
    String externalOrderNo,
    String thirdPartySource,
    String clinicalDiagnosis,
    String clinicalSymptom,
    String specimenSite,
    String applicationDate,
    String submissionDate,
    String remarks,
    String createdAt,
    String updatedAt
) {
}
