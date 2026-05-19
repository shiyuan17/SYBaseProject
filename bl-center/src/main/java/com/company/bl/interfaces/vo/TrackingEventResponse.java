package com.company.bl.interfaces.vo;

public record TrackingEventResponse(
    String nodeCode,
    String eventType,
    String eventStatus,
    String eventTime,
    String operatorName,
    String sourceTerminal,
    String eventContent
) {
}
