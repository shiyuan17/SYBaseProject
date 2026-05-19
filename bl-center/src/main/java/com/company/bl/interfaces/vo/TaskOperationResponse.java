package com.company.bl.interfaces.vo;

public record TaskOperationResponse(
    String taskId,
    String caseId,
    String caseStatus,
    String taskStatus
) {
}
