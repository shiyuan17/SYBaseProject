package com.company.bl.domain.model;

import com.company.bl.domain.enums.TransportOrderStatus;

import java.time.LocalDateTime;

public record TransportOrder(
    String id,
    String transportOrderNo,
    String applicationId,
    TransportOrderStatus status,
    String handoverUserId,
    String handoverUserName,
    String handoverDepartmentId,
    String handoverDepartmentName,
    String receiverDepartmentId,
    String receiverDepartmentName,
    String receiverUserId,
    String receiverUserName,
    String outboundUserId,
    String outboundUserName,
    LocalDateTime printedAt,
    LocalDateTime toBeTransportedAt,
    LocalDateTime handedOverAt,
    String terminalCode,
    String remarks
) {
}
