package com.company.bl.interfaces.vo;

public record DehydrationBatchResponse(
    String batchId,
    String batchNo,
    String batchStatus,
    int taskCount
) {
}
