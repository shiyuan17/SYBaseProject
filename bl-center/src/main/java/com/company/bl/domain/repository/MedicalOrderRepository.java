package com.company.bl.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicalOrderRepository {

    void insertMedicalOrder(CreateMedicalOrderCommand command);

    Optional<MedicalOrder> findMedicalOrderById(String orderId);

    List<MedicalOrder> findMedicalOrdersByCaseId(String caseId);

    PagedMedicalOrders findMedicalOrders(PendingMedicalOrderQuery query);

    void acceptMedicalOrder(String orderId,
                            String executorUserId,
                            String executorName,
                            String remarks,
                            LocalDateTime acceptedAt);

    void completeMedicalOrder(String orderId, String remarks, LocalDateTime completedAt);

    void cancelMedicalOrder(String orderId, String remarks, LocalDateTime cancelledAt);

    record PendingMedicalOrderQuery(
        int page,
        int size,
        String pathologyNo,
        String status
    ) {
    }

    record PagedMedicalOrders(List<MedicalOrder> items, long total) {
    }

    record CreateMedicalOrderCommand(
        String id,
        String caseId,
        String orderNumber,
        String orderContent,
        String orderType,
        String executionScope,
        String billingStatus,
        String status,
        String doctorUserId,
        String doctorName,
        LocalDateTime orderDate,
        String remarks
    ) {
    }

    record MedicalOrder(
        String id,
        String caseId,
        String pathologyNo,
        String applicationNo,
        String patientName,
        String orderNumber,
        String orderContent,
        String orderType,
        String executionScope,
        String billingStatus,
        String status,
        String doctorUserId,
        String doctorName,
        String executorUserId,
        String executorName,
        LocalDateTime orderDate,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
