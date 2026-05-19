package com.company.bl.interfaces.vo;

public record PendingSpecimenItemResponse(
    String applicationId,
    String applicationNo,
    String patientName,
    String submittingDepartmentId,
    String submittingDepartmentName,
    String specimenId,
    String specimenNo,
    String barcode,
    String specimenStatus,
    String fixationStatus,
    String registeredAt,
    String latestTrackingAt,
    boolean abnormalFlag
) {
}
