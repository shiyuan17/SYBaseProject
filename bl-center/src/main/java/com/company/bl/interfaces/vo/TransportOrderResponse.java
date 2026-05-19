package com.company.bl.interfaces.vo;

public record TransportOrderResponse(
    String id,
    String transportOrderNo,
    String applicationId,
    String status,
    String handoverUserName,
    String receiverUserName,
    String toBeTransportedAt,
    String handedOverAt
) {
}
