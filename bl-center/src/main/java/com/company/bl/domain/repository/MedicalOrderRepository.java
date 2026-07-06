package com.company.bl.domain.repository;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MedicalOrderRepository {

    void insertMedicalOrder(CreateMedicalOrderCommand command);

    Optional<MedicalOrder> findMedicalOrderById(String orderId);

    Optional<MedicalOrderItemSnapshot> findMedicalOrderItemSnapshotById(String orderItemId);

    List<MedicalOrder> findMedicalOrdersByCaseId(String caseId);

    void insertMedicalOrderBlock(CreateMedicalOrderBlockCommand command);

    List<MedicalOrderBlock> findMedicalOrderBlocksByCaseId(String caseId);

    Optional<MedicalOrderBlock> findMedicalOrderBlockByCaseIdAndBlockNo(String caseId, String blockNo);

    List<MedicalOrder> findMedicalOrdersByIds(List<String> orderIds);

    PagedMedicalOrders findMedicalOrders(PendingMedicalOrderQuery query);

    List<MedicalOrder> findMedicalOrdersForExport(PendingMedicalOrderQuery query);

    List<MedicalOrderSlicingLink> findPendingSlicingLinksByOrderIds(List<String> orderIds);

    List<SlicingMergeGroup> findSlicingMergeGroupsByIds(List<String> printGroupIds);

    void acceptMedicalOrder(String orderId,
                            String executorUserId,
                            String executorName,
                            String remarks,
                            LocalDateTime acceptedAt);

    void markMedicalOrderPrinted(String orderId,
                                 String printedByUserId,
                                 String printedByName,
                                 String remarks,
                                 LocalDateTime printedAt);

    void completeMedicalOrder(String orderId, String remarks, LocalDateTime completedAt);

    void terminateMedicalOrder(String orderId,
                               String terminatedByUserId,
                               String terminatedByName,
                               String terminationReasonCode,
                               String terminationReasonLabel,
                               String remarks,
                               LocalDateTime terminatedAt);

    void cancelMedicalOrder(String orderId, String remarks, LocalDateTime cancelledAt);

    void insertMedicalOrderQcEvaluation(CreateMedicalOrderQcEvaluationCommand command);

    Optional<MedicalOrderQcEvaluation> findLatestMedicalOrderQcEvaluation(String orderId);

    int updateMedicalOrderTargetSnapshot(UpdateMedicalOrderTargetSnapshotCommand command);

    record PendingMedicalOrderQuery(
        int page,
        int size,
        String pathologyNo,
        String status,
        String orderCategoryCode,
        LocalDateTime orderDateFrom,
        LocalDateTime orderDateTo
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
        String orderItemId,
        String orderItemCode,
        String orderItemName,
        String orderCategoryId,
        String orderCategoryCode,
        String orderCategoryName,
        String executionScope,
        String billingStatus,
        String status,
        String doctorUserId,
        String doctorName,
        String targetType,
        String targetSpecimenId,
        String targetSpecimenNo,
        String targetBlockId,
        String targetBlockNo,
        String targetSlideId,
        String targetSlideNo,
        LocalDateTime orderDate,
        String remarks
    ) {
    }

    record CreateMedicalOrderQcEvaluationCommand(
        String id,
        String orderId,
        String caseId,
        String qcAspect,
        Integer totalScore,
        String grade,
        String evaluationReason,
        String processingAction,
        String reworkType,
        String reworkOrderId,
        String remarks,
        String evaluatorUserId,
        String evaluatorName,
        LocalDateTime evaluatedAt,
        JsonNode detailPayload
    ) {
    }

    record CreateMedicalOrderBlockCommand(
        String id,
        String caseId,
        String blockNo,
        String createdByUserId,
        String createdByName,
        LocalDateTime createdAt
    ) {
    }

    record UpdateMedicalOrderTargetSnapshotCommand(
        String orderId,
        String targetType,
        String targetSpecimenId,
        String targetSpecimenNo,
        String targetBlockId,
        String targetBlockNo,
        String targetSlideId,
        String targetSlideNo,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }

    record MedicalOrderQcEvaluation(
        String id,
        String orderId,
        String caseId,
        String qcAspect,
        Integer totalScore,
        String grade,
        String evaluationReason,
        String processingAction,
        String reworkType,
        String reworkOrderId,
        String remarks,
        String evaluatorUserId,
        String evaluatorName,
        LocalDateTime evaluatedAt,
        JsonNode detailPayload
    ) {
    }

    record MedicalOrderItemSnapshot(
        String orderItemId,
        String orderItemCode,
        String orderItemName,
        String orderCategoryId,
        String orderCategoryCode,
        String orderCategoryName,
        String orderType,
        String defaultContent,
        String executionScope
    ) {
    }

    record MedicalOrderBlock(
        String id,
        String caseId,
        String blockNo,
        String createdByUserId,
        String createdByName,
        LocalDateTime createdAt
    ) {
    }

    record MedicalOrder(
        String id,
        String caseId,
        String pathologyNo,
        String applicationNo,
        String inpatientNo,
        String slicingTaskId,
        String slicingPrintGroupId,
        boolean slicingMergedPrintGroup,
        List<String> slicingTaskIds,
        String patientName,
        String patientId,
        String patientIdDisplay,
        String submittingDepartmentName,
        String orderNumber,
        String orderContent,
        String orderType,
        String orderItemId,
        String orderItemCode,
        String orderItemName,
        String orderCategoryId,
        String orderCategoryCode,
        String orderCategoryName,
        String executionScope,
        String billingStatus,
        String status,
        String doctorUserId,
        String doctorName,
        String executorUserId,
        String executorName,
        LocalDateTime orderDate,
        LocalDateTime acceptedAt,
        String printedByUserId,
        String printedByName,
        LocalDateTime printedAt,
        String releasedByUserId,
        String releasedByName,
        LocalDateTime releasedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String terminatedByUserId,
        String terminatedByName,
        LocalDateTime terminatedAt,
        String terminationReasonCode,
        String terminationReasonLabel,
        String targetType,
        String targetSpecimenId,
        String targetSpecimenNo,
        String targetBlockId,
        String targetBlockNo,
        String targetSlideId,
        String targetSlideNo,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record MedicalOrderSlicingLink(
        String orderId,
        String slicingTaskId,
        String slicingPrintGroupId,
        boolean slicingMergedPrintGroup,
        List<String> slicingTaskIds
    ) {
    }

    record SlicingMergeGroup(
        String printGroupId,
        String caseId,
        String groupStatus,
        String slicingId
    ) {
    }
}
