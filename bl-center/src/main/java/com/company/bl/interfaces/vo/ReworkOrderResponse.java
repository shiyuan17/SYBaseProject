package com.company.bl.interfaces.vo;

public record ReworkOrderResponse(
    String caseId,
    String reworkType,
    String status
) {
}
