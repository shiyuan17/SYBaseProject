package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.MedicalOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
class MedicalOrderWorkflowService {

    private final MedicalOrderRepository medicalOrderRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;

    MedicalOrderWorkflowService(MedicalOrderRepository medicalOrderRepository,
                                DiagnosticReportSupport diagnosticReportSupport) {
        this.medicalOrderRepository = medicalOrderRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
    }

    @Transactional(readOnly = true)
    DiagnosticReportModels.PendingMedicalOrderPage listPendingMedicalOrders(DiagnosticReportModels.PendingMedicalOrderQuery query) {
        MedicalOrderRepository.PagedMedicalOrders paged = medicalOrderRepository.findMedicalOrders(
            new MedicalOrderRepository.PendingMedicalOrderQuery(query.page(), query.size(), query.pathologyNo(), query.status()));
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
        medicalOrderRepository.insertMedicalOrder(new MedicalOrderRepository.CreateMedicalOrderCommand(
            orderId,
            command.caseId(),
            orderId,
            command.orderContent(),
            command.orderType(),
            DiagnosticReportConstants.ORDER_SCOPE_TECHNICAL,
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

    private MedicalOrderRepository.MedicalOrder getOrder(String orderId) {
        return medicalOrderRepository.findMedicalOrderById(orderId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Medical order not found"));
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
}
