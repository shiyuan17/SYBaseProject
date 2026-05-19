package com.company.bl.interfaces.vo;

import java.util.List;

public record ApplicationDetailResponse(
    String id,
    String applicationNo,
    String patientId,
    String patientName,
    String patientGender,
    String patientAge,
    String applicationType,
    String status,
    String applicationFormStatus,
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
    String applicationDate,
    String submissionDate,
    String currentNode,
    boolean abnormalFlag,
    List<SpecimenSummaryResponse> specimens,
    List<TrackingEventResponse> recentEvents,
    String remarks,
    String createdAt,
    String updatedAt
) {
}
