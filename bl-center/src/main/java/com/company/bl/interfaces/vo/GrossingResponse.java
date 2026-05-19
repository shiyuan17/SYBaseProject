package com.company.bl.interfaces.vo;

public record GrossingResponse(
    String taskId,
    String caseId,
    String caseStatus,
    int createdDehydrationTaskCount
) {
}
