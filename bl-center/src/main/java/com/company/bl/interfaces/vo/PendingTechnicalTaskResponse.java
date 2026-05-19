package com.company.bl.interfaces.vo;

public record PendingTechnicalTaskResponse(
    String id,
    String applicationId,
    String applicationNo,
    String caseId,
    String pathologyNo,
    String specimenId,
    String taskType,
    String taskStatus,
    String objectType,
    String objectId,
    String payload,
    String remarks,
    String createdAt,
    String startedAt,
    String completedAt
) {
}
