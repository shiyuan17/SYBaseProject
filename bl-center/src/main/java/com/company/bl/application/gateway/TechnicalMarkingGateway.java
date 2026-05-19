package com.company.bl.application.gateway;

public interface TechnicalMarkingGateway {

    MarkingResult mark(MarkingRequest request);

    record MarkingRequest(
        String caseId,
        String objectType,
        String objectId,
        String deviceCode,
        String label
    ) {
    }

    record MarkingResult(
        boolean success,
        String message
    ) {
    }
}
