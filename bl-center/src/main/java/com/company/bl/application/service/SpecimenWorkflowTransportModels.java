package com.company.bl.application.service;

import java.time.LocalDateTime;
import java.util.List;

public final class SpecimenWorkflowTransportModels {

    private SpecimenWorkflowTransportModels() {
    }

    public record CreateTransportOrderCommand(
        String applicationId,
        List<String> specimenBarcodes,
        String handoverUserId,
        String handoverUserName,
        String handoverDepartmentId,
        String handoverDepartmentName,
        String receiverDepartmentId,
        String receiverDepartmentName,
        String terminalCode,
        String remarks
    ) {
    }

    public record OperatorCommand(String operatorUserId, String operatorName, String terminalCode) {
    }

    public record HandoverTransportOrderCommand(
        String outboundUserId,
        String outboundUserName,
        String receiverUserId,
        String receiverUserName,
        String terminalCode,
        String remarks
    ) {
    }

    public record OutboundTransportOrderCommand(
        String outboundUserId,
        String outboundUserName,
        String terminalCode,
        String remarks
    ) {
    }

    public record PendingTransportOrderQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo,
        String departmentId,
        String dateFrom,
        String dateTo,
        String status
    ) {
    }

    public record PendingTransportOrderPage(
        List<PendingTransportOrderItem> items,
        int page,
        int size,
        long total
    ) {
    }

    public record PendingTransportOrderItem(
        String id,
        String transportOrderNo,
        String applicationId,
        String applicationNo,
        String patientName,
        String handoverDepartmentName,
        String receiverDepartmentName,
        String status,
        LocalDateTime toBeTransportedAt,
        LocalDateTime handedOverAt,
        String outboundUserId,
        String outboundUserName,
        List<String> specimenBarcodes
    ) {
    }
}
