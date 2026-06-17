package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.integration.application.BillingManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
class MedicalOrderWorkflowService {

    private final MedicalOrderRepository medicalOrderRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;
    private final BillingManagementService billingManagementService;

    MedicalOrderWorkflowService(MedicalOrderRepository medicalOrderRepository,
                                DiagnosticReportSupport diagnosticReportSupport,
                                BillingManagementService billingManagementService) {
        this.medicalOrderRepository = medicalOrderRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
        this.billingManagementService = billingManagementService;
    }

    @Transactional(readOnly = true)
    DiagnosticReportModels.PendingMedicalOrderPage listPendingMedicalOrders(DiagnosticReportModels.PendingMedicalOrderQuery query) {
        TechnicalWorkflowModels.LocalDateRange effectiveDateRange =
            resolveEffectiveDateRange(query.dateFrom(), query.dateTo(), query.workDate());
        MedicalOrderRepository.PagedMedicalOrders paged = medicalOrderRepository.findMedicalOrders(
            new MedicalOrderRepository.PendingMedicalOrderQuery(
                query.page(),
                query.size(),
                query.pathologyNo(),
                query.status(),
                query.orderCategoryCode(),
                effectiveDateRange.dateFrom() == null ? null : effectiveDateRange.dateFrom().atStartOfDay(),
                effectiveDateRange.dateTo() == null ? null : effectiveDateRange.dateTo().plusDays(1).atStartOfDay()));
        return new DiagnosticReportModels.PendingMedicalOrderPage(
            paged.items().stream().map(this::toView).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional
    DiagnosticReportModels.MedicalOrderResult createMedicalOrder(DiagnosticReportModels.CreateMedicalOrderCommand command) {
        diagnosticReportSupport.getCase(command.caseId());
        diagnosticReportSupport.ensureAssignedDoctor(diagnosticReportSupport.getLatestDiagnosticTask(command.caseId()), command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        String orderId = diagnosticReportSupport.nextId("MO");
        MedicalOrderRepository.MedicalOrderItemSnapshot orderItemSnapshot =
            medicalOrderRepository.findMedicalOrderItemSnapshotById(command.orderItemId()).orElse(null);
        medicalOrderRepository.insertMedicalOrder(new MedicalOrderRepository.CreateMedicalOrderCommand(
            orderId,
            command.caseId(),
            orderId,
            command.orderContent(),
            command.orderType(),
            orderItemSnapshot == null ? null : orderItemSnapshot.orderItemId(),
            orderItemSnapshot == null ? null : orderItemSnapshot.orderItemCode(),
            orderItemSnapshot == null ? null : orderItemSnapshot.orderItemName(),
            orderItemSnapshot == null ? null : orderItemSnapshot.orderCategoryId(),
            orderItemSnapshot == null ? null : orderItemSnapshot.orderCategoryCode(),
            orderItemSnapshot == null ? null : orderItemSnapshot.orderCategoryName(),
            firstPresent(orderItemSnapshot == null ? null : orderItemSnapshot.executionScope(), DiagnosticReportConstants.ORDER_SCOPE_TECHNICAL),
            DiagnosticReportConstants.ORDER_BILLING_PENDING,
            DiagnosticReportConstants.ORDER_PENDING,
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.remarks()));
        diagnosticReportSupport.insertWorkflowEvent(command.caseId(), "MEDICAL_ORDER_CREATE", "CREATE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.orderType() + ":" + command.orderContent());
        MedicalOrderRepository.MedicalOrder created = medicalOrderRepository.findMedicalOrderById(orderId).orElseThrow();
        return new DiagnosticReportModels.MedicalOrderResult(created.id(), created.caseId(), created.orderNumber(), created.status());
    }

    @Transactional
    DiagnosticReportModels.MedicalOrderResult acceptMedicalOrder(DiagnosticReportModels.MedicalOrderActionCommand command) {
        MedicalOrderRepository.MedicalOrder order = getOrder(command.orderId());
        if (!DiagnosticReportConstants.ORDER_PENDING.equals(order.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Medical order is not pending");
        }
        LocalDateTime now = LocalDateTime.now();
        medicalOrderRepository.acceptMedicalOrder(order.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        diagnosticReportSupport.insertWorkflowEvent(order.caseId(), "MEDICAL_ORDER_ACCEPT", "ACCEPT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), order.orderNumber());
        MedicalOrderRepository.MedicalOrder updated = getOrder(order.id());
        return new DiagnosticReportModels.MedicalOrderResult(updated.id(), updated.caseId(), updated.orderNumber(), updated.status());
    }

    @Transactional
    DiagnosticReportModels.MedicalOrderResult completeMedicalOrder(DiagnosticReportModels.MedicalOrderActionCommand command) {
        MedicalOrderRepository.MedicalOrder order = getOrder(command.orderId());
        if (!DiagnosticReportConstants.ORDER_IN_PROGRESS.equals(order.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Medical order is not in progress");
        }
        if (order.executorUserId() != null && !order.executorUserId().equals(command.operatorUserId())) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "Medical order is assigned to another executor");
        }
        LocalDateTime now = LocalDateTime.now();
        medicalOrderRepository.completeMedicalOrder(order.id(), command.remarks(), now);
        diagnosticReportSupport.insertWorkflowEvent(order.caseId(), "MEDICAL_ORDER_COMPLETE", "COMPLETE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), order.orderNumber());
        MedicalOrderRepository.MedicalOrder updated = getOrder(order.id());
        billingManagementService.triggerSpecialOrderBilling(
            updated.caseId(),
            updated.id(),
            updated.orderNumber(),
            updated.orderType(),
            updated.orderContent(),
            command.operatorUserId(),
            command.operatorName());
        return new DiagnosticReportModels.MedicalOrderResult(updated.id(), updated.caseId(), updated.orderNumber(), updated.status());
    }

    @Transactional
    DiagnosticReportModels.MedicalOrderResult cancelMedicalOrder(DiagnosticReportModels.MedicalOrderActionCommand command) {
        MedicalOrderRepository.MedicalOrder order = getOrder(command.orderId());
        if (!DiagnosticReportConstants.ORDER_PENDING.equals(order.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Medical order cannot be cancelled");
        }
        diagnosticReportSupport.ensureAssignedDoctor(diagnosticReportSupport.getLatestDiagnosticTask(order.caseId()), command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        medicalOrderRepository.cancelMedicalOrder(order.id(), command.remarks(), now);
        diagnosticReportSupport.insertWorkflowEvent(order.caseId(), "MEDICAL_ORDER_CANCEL", "CANCEL", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), order.orderNumber());
        MedicalOrderRepository.MedicalOrder updated = getOrder(order.id());
        return new DiagnosticReportModels.MedicalOrderResult(updated.id(), updated.caseId(), updated.orderNumber(), updated.status());
    }

    DiagnosticReportModels.MedicalOrderBillingResult executeMedicalOrderBilling(DiagnosticReportModels.MedicalOrderBillingCommand command) {
        List<MedicalOrderRepository.MedicalOrder> orders = resolveBillingTargetOrders(command);
        List<DiagnosticReportModels.MedicalOrderBillingItemResult> items = new ArrayList<>();
        int failureCount = 0;
        for (MedicalOrderRepository.MedicalOrder order : orders) {
            BillingManagementService.BillingRecordView billingRecord =
                billingManagementService.executeSpecialOrderBilling(
                    order.caseId(),
                    order.id(),
                    order.orderNumber(),
                    order.orderType(),
                    order.orderContent(),
                    command.operatorUserId(),
                    command.operatorName());
            if (isFailedBillingStatus(billingRecord.billingStatus())) {
                failureCount++;
            }
            items.add(toBillingItemResult(order.id(), billingRecord, "执行收费完成"));
        }
        return new DiagnosticReportModels.MedicalOrderBillingResult(
            items.size(),
            items.size() - failureCount,
            failureCount,
            items);
    }

    DiagnosticReportModels.MedicalOrderBillingResult confirmMedicalOrderBilling(DiagnosticReportModels.MedicalOrderBillingCommand command) {
        List<MedicalOrderRepository.MedicalOrder> orders = resolveBillingTargetOrders(command);
        List<DiagnosticReportModels.MedicalOrderBillingItemResult> items = new ArrayList<>();
        int failureCount = 0;
        for (MedicalOrderRepository.MedicalOrder order : orders) {
            BillingManagementService.BillingRecordView billingRecord =
                billingManagementService.confirmSpecialOrderBilling(
                    order.caseId(),
                    order.id(),
                    order.orderNumber(),
                    order.orderType(),
                    order.orderContent(),
                    command.operatorUserId(),
                    command.operatorName(),
                    command.remarks());
            if (isFailedBillingStatus(billingRecord.billingStatus())) {
                failureCount++;
            }
            items.add(toBillingItemResult(order.id(), billingRecord, "确认完成收费"));
        }
        return new DiagnosticReportModels.MedicalOrderBillingResult(
            items.size(),
            items.size() - failureCount,
            failureCount,
            items);
    }

    private MedicalOrderRepository.MedicalOrder getOrder(String orderId) {
        return medicalOrderRepository.findMedicalOrderById(orderId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order not found"));
    }

    private List<MedicalOrderRepository.MedicalOrder> resolveBillingTargetOrders(DiagnosticReportModels.MedicalOrderBillingCommand command) {
        String caseId = command.caseId();
        if (caseId == null || caseId.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Case ID is required");
        }
        diagnosticReportSupport.getCase(caseId);
        diagnosticReportSupport.ensureAssignedDoctor(diagnosticReportSupport.getLatestDiagnosticTask(caseId), command.operatorUserId());

        List<String> targetOrderIds = normalizeOrderIds(command.orderIds());
        if (!targetOrderIds.isEmpty()) {
            return targetOrderIds.stream()
                .map(this::getOrder)
                .peek(order -> ensureOrderInCase(order, caseId))
                .filter(order -> !isChargedBillingStatus(order.billingStatus()))
                .toList();
        }
        return medicalOrderRepository.findMedicalOrdersByCaseId(caseId).stream()
            .filter(order -> !isChargedBillingStatus(order.billingStatus()))
            .toList();
    }

    private List<String> normalizeOrderIds(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String orderId : orderIds) {
            if (orderId == null || orderId.isBlank()) {
                continue;
            }
            String trimmed = orderId.trim();
            if (seen.add(trimmed)) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private void ensureOrderInCase(MedicalOrderRepository.MedicalOrder order, String caseId) {
        if (!caseId.equals(order.caseId())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Medical order does not belong to the selected case");
        }
    }

    private DiagnosticReportModels.MedicalOrderBillingItemResult toBillingItemResult(String orderId,
                                                                                     BillingManagementService.BillingRecordView billingRecord,
                                                                                     String fallbackMessage) {
        String message = billingRecord.lastErrorMessage() == null || billingRecord.lastErrorMessage().isBlank()
            ? fallbackMessage
            : billingRecord.lastErrorMessage();
        return new DiagnosticReportModels.MedicalOrderBillingItemResult(
            orderId,
            billingRecord.billingStatus(),
            billingRecord.id(),
            message);
    }

    private boolean isChargedBillingStatus(String billingStatus) {
        return billingStatus != null && Set.of("CHARGED", "PAID", "SETTLED", "SUCCESS").contains(billingStatus.trim().toUpperCase());
    }

    private boolean isFailedBillingStatus(String billingStatus) {
        return billingStatus != null && "FAILED".equals(billingStatus.trim().toUpperCase());
    }

    private DiagnosticReportViews.MedicalOrderView toView(MedicalOrderRepository.MedicalOrder order) {
        return new DiagnosticReportViews.MedicalOrderView(
            order.id(),
            order.caseId(),
            order.pathologyNo(),
            order.applicationNo(),
            order.patientName(),
            order.orderNumber(),
            order.orderType(),
            order.orderContent(),
            order.orderItemId(),
            order.orderItemCode(),
            order.orderItemName(),
            order.orderCategoryId(),
            order.orderCategoryCode(),
            order.orderCategoryName(),
            order.executionScope(),
            order.billingStatus(),
            order.status(),
            order.doctorName(),
            order.executorName(),
            stringify(order.orderDate()),
            stringify(order.acceptedAt()),
            stringify(order.completedAt()),
            stringify(order.cancelledAt()),
            order.remarks());
    }

    private String stringify(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    private static String firstPresent(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private TechnicalWorkflowModels.LocalDateRange resolveEffectiveDateRange(
        java.time.LocalDate dateFrom,
        java.time.LocalDate dateTo,
        java.time.LocalDate workDate
    ) {
        if (dateFrom != null || dateTo != null) {
            return new TechnicalWorkflowModels.LocalDateRange(dateFrom, dateTo);
        }
        if (workDate != null) {
            return new TechnicalWorkflowModels.LocalDateRange(workDate, workDate);
        }
        return new TechnicalWorkflowModels.LocalDateRange(null, null);
    }
}
