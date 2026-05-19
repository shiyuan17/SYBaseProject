package com.company.bl.interfaces.vo;

public record LabelPrintRetryResponse(
    String labelPrintBatchNo,
    int retriedCount,
    int successCount,
    int failedCount,
    boolean allSuccessful,
    String message
) {
}
