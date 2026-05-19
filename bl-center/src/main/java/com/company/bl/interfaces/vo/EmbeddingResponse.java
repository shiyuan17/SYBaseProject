package com.company.bl.interfaces.vo;

public record EmbeddingResponse(
    String taskId,
    String embeddingId,
    String embeddingBoxId,
    String caseStatus,
    boolean markingSuccess,
    String markingMessage
) {
}
