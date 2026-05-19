package com.company.bl.domain.model;

import java.time.LocalDateTime;

public record PathologyCase(
    String id,
    String applicationId,
    String pathologyNo,
    String caseStatus,
    String sourceHospitalId,
    String sourceHospitalName,
    String sourceDepartmentId,
    String sourceDepartmentName,
    String receivedByUserId,
    String receivedByName,
    LocalDateTime receivedAt
) {
}
