package com.company.bl.interfaces.vo;

public record SpecimenReceiptResponse(
    String caseId,
    String pathologyNo,
    String receiptStatus,
    int unreceivedCount
) {
}
