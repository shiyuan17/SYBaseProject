package com.company.bl.domain.model;

import com.company.bl.domain.enums.TransportItemStatus;

import java.time.LocalDateTime;

public record TransportOrderItem(
    String id,
    String transportOrderId,
    String applicationId,
    String specimenId,
    TransportItemStatus status,
    String verificationResult,
    String verifiedByUserId,
    String verifiedByName,
    LocalDateTime verifiedAt,
    String remarks
) {
}
