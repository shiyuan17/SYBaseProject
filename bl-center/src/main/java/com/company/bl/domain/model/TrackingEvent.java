package com.company.bl.domain.model;

import java.time.LocalDateTime;

public record TrackingEvent(
    String id,
    String applicationId,
    String specimenId,
    String caseId,
    String transportOrderId,
    String nodeCode,
    String eventType,
    String eventStatus,
    LocalDateTime eventTime,
    String operatorUserId,
    String operatorName,
    String sourceTerminal,
    String eventContent,
    String operatorIp,
    String operatorDevice
) {
    public TrackingEvent(
        String id,
        String applicationId,
        String specimenId,
        String caseId,
        String transportOrderId,
        String nodeCode,
        String eventType,
        String eventStatus,
        LocalDateTime eventTime,
        String operatorUserId,
        String operatorName,
        String sourceTerminal,
        String eventContent,
        String operatorIp
    ) {
        this(
            id,
            applicationId,
            specimenId,
            caseId,
            transportOrderId,
            nodeCode,
            eventType,
            eventStatus,
            eventTime,
            operatorUserId,
            operatorName,
            sourceTerminal,
            eventContent,
            operatorIp,
            null);
    }
}
