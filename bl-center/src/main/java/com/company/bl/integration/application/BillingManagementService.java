package com.company.bl.integration.application;

import com.company.bl.application.gateway.BillingGateway;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.infrastructure.config.ObservabilityConfiguration;
import com.company.bl.integration.infrastructure.M6BillingRows;
import com.company.bl.integration.infrastructure.M6JdbcRepository;
import com.company.bl.integration.infrastructure.M6IntegrationTaskRows;
import com.company.common.web.observability.ObservedOperation;
import com.company.bl.support.application.OperationAuditService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BillingManagementService {

    private static final String BUSINESS_TYPE_BILLING_RECORD = "BILLING_RECORD";
    private static final String EXTERNAL_SYSTEM = "MOCK_BILLING";

    private final M6JdbcRepository repository;
    private final IntegrationManagementService integrationManagementService;
    private final BillingGateway billingGateway;
    private final OperationAuditService operationAuditService;
    private final MeterRegistry meterRegistry;
    private final ObservabilityConfiguration observabilityConfiguration;

    public BillingManagementService(M6JdbcRepository repository,
                                    IntegrationManagementService integrationManagementService,
                                    BillingGateway billingGateway,
                                    OperationAuditService operationAuditService,
                                    MeterRegistry meterRegistry,
                                    ObservabilityConfiguration observabilityConfiguration) {
        this.repository = repository;
        this.integrationManagementService = integrationManagementService;
        this.billingGateway = billingGateway;
        this.operationAuditService = operationAuditService;
        this.meterRegistry = meterRegistry;
        this.observabilityConfiguration = observabilityConfiguration;
    }

    @Transactional
    public void triggerSpecialOrderBilling(String caseId,
                                           String orderId,
                                           String orderNumber,
                                           String itemType,
                                           String itemName,
                                           String operatorUserId,
                                           String operatorName) {
        submitBilling(new SubmitBillingCommand(
            caseId,
            orderId,
            "SPECIAL_ORDER",
            itemType == null ? orderNumber : itemType,
            itemName == null ? orderNumber : itemName,
            BigDecimal.ONE,
            BigDecimal.ONE,
            operatorUserId,
            operatorName));
    }

    @Transactional
    public BillingRecordView executeSpecialOrderBilling(String caseId,
                                                        String orderId,
                                                        String orderNumber,
                                                        String itemType,
                                                        String itemName,
                                                        String operatorUserId,
                                                        String operatorName) {
        BillingRecordView latest = findLatestSpecialOrderBilling(orderId);
        if (latest == null) {
            return submitBilling(new SubmitBillingCommand(
                caseId,
                orderId,
                "SPECIAL_ORDER",
                itemType == null ? orderNumber : itemType,
                itemName == null ? orderNumber : itemName,
                BigDecimal.ONE,
                BigDecimal.ONE,
                operatorUserId,
                operatorName));
        }
        if ("FAILED".equals(latest.billingStatus())) {
            return retryBilling(latest.id(), operatorUserId, operatorName);
        }
        return latest;
    }

    @Transactional
    public BillingRecordView confirmSpecialOrderBilling(String caseId,
                                                        String orderId,
                                                        String orderNumber,
                                                        String itemType,
                                                        String itemName,
                                                        String operatorUserId,
                                                        String operatorName,
                                                        String remarks) {
        BillingRecordView latest = findLatestSpecialOrderBilling(orderId);
        if (latest == null) {
            latest = executeSpecialOrderBilling(caseId, orderId, orderNumber, itemType, itemName, operatorUserId, operatorName);
        }
        if ("SUCCESS".equals(latest.billingStatus())) {
            return latest;
        }
        return receiveBillingReceipt(latest.id(), latest.externalBillNo(), "SUCCESS", operatorUserId, operatorName, remarks);
    }

    @Transactional
    public void triggerReportPublishBilling(String caseId,
                                            String reportId,
                                            String reportNo,
                                            String finalDiagnosis,
                                            String operatorUserId,
                                            String operatorName) {
        submitBilling(new SubmitBillingCommand(
            caseId,
            null,
            "REPORT_PUBLISH",
            reportNo,
            finalDiagnosis == null || finalDiagnosis.isBlank() ? reportId : finalDiagnosis,
            BigDecimal.ONE,
            BigDecimal.ONE,
            operatorUserId,
            operatorName));
    }

    @Transactional(readOnly = true)
    public List<BillingRecordView> listBillingRecords(String billingStatus,
                                                      String billingStage,
                                                      String externalSystem,
                                                      String caseId,
                                                      String orderId,
                                                      LocalDateTime from, LocalDateTime to) {
        return repository.findBillingRecords(billingStatus, billingStage, externalSystem, caseId, orderId, from, to).stream()
            .map(this::toView)
            .toList();
    }

    @ObservedOperation(
        operation = "billing_receipt",
        successCounter = "billing_receipt_total",
        failureCounter = "billing_receipt_failed_total",
        durationMetric = "billing_receipt_duration")
    @Transactional
    public BillingRecordView receiveBillingReceipt(String id, String externalBillNo, String billingStatus,
                                                   String operatorUserId, String operatorName, String remarks) {
        return operationAuditService.audit("M6", "BILLING", "billing_receipt", () -> {
            M6BillingRows.BillingRecordRow current = requireBillingRecord(id);
            LocalDateTime now = LocalDateTime.now();
            repository.updateBillingRecord(new M6BillingRows.BillingRecordRow(
                current.id(), current.caseId(), current.orderId(), current.billingNo(), current.billingStage(), current.itemType(), current.itemName(),
                current.quantity(), current.amount(), billingStatus, now, operatorUserId, operatorName,
                externalBillNo, current.externalSystem(), remarks, current.createdAt(), now));
            if (current.orderId() != null) {
                repository.updateMedicalOrderBillingStatus(current.orderId(), billingStatus);
            }
            IntegrationManagementService.IntegrationTaskView task =
                integrationManagementService.findLatestTask(BUSINESS_TYPE_BILLING_RECORD, current.id(), current.billingStage());
            if (task != null) {
                integrationManagementService.markSuccess(task.id(), "{\"receipt\":true}");
                integrationManagementService.markReconciled(task.id(), "SUCCESS".equals(billingStatus) ? "MATCHED" : "DISCREPANCY");
            }
            return toView(requireBillingRecord(id));
        }, BillingRecordView::id, () -> id, operatorUserId, operatorName, () -> "billing receipt");
    }

    @Transactional
    public BillingRecordView retryBilling(String id, String operatorUserId, String operatorName) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return operationAuditService.audit("M6", "BILLING", "billing_retry", () -> {
                M6BillingRows.BillingRecordRow current = requireBillingRecord(id);
                M6IntegrationTaskRows.IntegrationTaskRow task = repository.findLatestIntegrationTask(BUSINESS_TYPE_BILLING_RECORD, current.id(), current.billingStage());
                if (task == null) {
                    throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Integration task not found for billing record");
                }
                BillingGateway.BillingSubmitRequest request = toGatewayRequest(current, operatorUserId, operatorName);
                integrationManagementService.markRetryStarted(task.id(), request.toString());
                BillingGateway.BillingSubmitResult result = billingGateway.submit(request);
                LocalDateTime now = LocalDateTime.now();
                if (result.success()) {
                    repository.updateBillingRecord(new M6BillingRows.BillingRecordRow(
                        current.id(), current.caseId(), current.orderId(), current.billingNo(), current.billingStage(), current.itemType(), current.itemName(),
                        current.quantity(), current.amount(), "SUCCESS", now, operatorUserId, operatorName,
                        result.externalBillNo(), EXTERNAL_SYSTEM, result.message(), current.createdAt(), now));
                    if (current.orderId() != null) {
                        repository.updateMedicalOrderBillingStatus(current.orderId(), "SUCCESS");
                    }
                    incrementCounter("billing_retry_total", "billing_retry");
                    integrationManagementService.markSuccess(task.id(), "{\"retry\":true,\"message\":\"" + safe(result.message()) + "\"}");
                } else {
                    repository.updateBillingRecord(new M6BillingRows.BillingRecordRow(
                        current.id(), current.caseId(), current.orderId(), current.billingNo(), current.billingStage(), current.itemType(), current.itemName(),
                        current.quantity(), current.amount(), "FAILED", current.billedAt(), operatorUserId, operatorName,
                        current.externalBillNo(), EXTERNAL_SYSTEM, result.message(), current.createdAt(), now));
                    if (current.orderId() != null) {
                        repository.updateMedicalOrderBillingStatus(current.orderId(), "FAILED");
                    }
                    incrementCounter("billing_retry_failed_total", "billing_retry");
                    integrationManagementService.markFailure(task.id(), "BILLING_RETRY_FAILED", result.message(),
                        "{\"retry\":true,\"message\":\"" + safe(result.message()) + "\"}", true);
                }
                return toView(requireBillingRecord(id));
            }, BillingRecordView::id, () -> id, operatorUserId, operatorName, () -> "billing retry");
        } catch (RuntimeException exception) {
            incrementCounter("billing_retry_failed_total", "billing_retry");
            throw exception;
        } finally {
            stopTimer(sample, "billing_retry_duration", "billing_retry");
        }
    }

    @ObservedOperation(
        operation = "billing_reconcile",
        successCounter = "billing_reconcile_total",
        failureCounter = "billing_reconcile_failed_total",
        durationMetric = "billing_reconcile_duration")
    @Transactional
    public ReconciliationResult reconcile(LocalDateTime from, LocalDateTime to, String operatorUserId, String operatorName) {
        return operationAuditService.audit("M6", "BILLING", "billing_reconcile", () -> {
            List<M6BillingRows.BillingRecordRow> rows = repository.findBillingRecords(null, null, null, null, null, from, to);
            billingGateway.reconcile(new BillingGateway.ReconciliationRequest(from, to));
            int matched = 0;
            int discrepancy = 0;
            for (M6BillingRows.BillingRecordRow row : rows) {
                M6IntegrationTaskRows.IntegrationTaskRow task =
                    repository.findLatestIntegrationTask(BUSINESS_TYPE_BILLING_RECORD, row.id(), row.billingStage());
                if ("SUCCESS".equals(row.billingStatus()) && row.externalBillNo() != null && !row.externalBillNo().isBlank()) {
                    matched++;
                    if (task != null) {
                        integrationManagementService.markReconciled(task.id(), "MATCHED");
                    }
                } else {
                    discrepancy++;
                    if (task != null) {
                        integrationManagementService.markReconciled(task.id(), "DISCREPANCY");
                    }
                }
            }
            return new ReconciliationResult(rows.size(), matched, discrepancy, from == null ? null : from.toString(), to == null ? null : to.toString());
        }, value -> "billing-reconcile", () -> "billing-reconcile", operatorUserId, operatorName, () -> "billing reconcile");
    }

    @Transactional
    public void retryPendingBillings() {
        List<IntegrationManagementService.IntegrationTaskView> pendingTasks =
            integrationManagementService.listTasks("BILLING_SUBMIT", BUSINESS_TYPE_BILLING_RECORD, null,
                "RETRY_PENDING", null, null, null, null);
        for (IntegrationManagementService.IntegrationTaskView task : pendingTasks) {
            try {
                retryBilling(task.businessId(), "system", "system");
            } catch (RuntimeException ignored) {
                // Keep background retry resilient so a single failed record does not block the rest.
            }
        }
    }

    private BillingRecordView submitBilling(SubmitBillingCommand command) {
        Timer.Sample sample = Timer.start(meterRegistry);
        LocalDateTime now = LocalDateTime.now();
        String recordId = "BR-" + UUID.randomUUID();
        String billingNo = "BL-" + UUID.randomUUID();
        try {
            repository.insertBillingRecord(new M6BillingRows.CreateBillingRecordRow(
                recordId,
                command.caseId(),
                command.orderId(),
                billingNo,
                command.billingStage(),
                command.itemType(),
                command.itemName(),
                command.quantity(),
                command.amount(),
                "PENDING",
                null,
                command.operatorUserId(),
                command.operatorName(),
                null,
                EXTERNAL_SYSTEM,
                null,
                now,
                now));
            String taskId = integrationManagementService.openTask(new IntegrationManagementService.CreateIntegrationTaskCommand(
                "BILLING_SUBMIT",
                BUSINESS_TYPE_BILLING_RECORD,
                recordId,
                command.billingStage(),
                EXTERNAL_SYSTEM,
                command.toString()));
            BillingGateway.BillingSubmitResult result = billingGateway.submit(new BillingGateway.BillingSubmitRequest(
                billingNo,
                command.caseId(),
                command.orderId(),
                command.billingStage(),
                command.itemType(),
                command.itemName(),
                command.quantity(),
                command.amount(),
                command.operatorUserId(),
                command.operatorName()));
            if (result.success()) {
                repository.updateBillingRecord(new M6BillingRows.BillingRecordRow(
                    recordId, command.caseId(), command.orderId(), billingNo, command.billingStage(), command.itemType(), command.itemName(),
                    command.quantity(), command.amount(), "SUCCESS", now, command.operatorUserId(), command.operatorName(),
                    result.externalBillNo(), EXTERNAL_SYSTEM, result.message(), now, now));
                if (command.orderId() != null) {
                    repository.updateMedicalOrderBillingStatus(command.orderId(), "SUCCESS");
                }
                incrementCounter("billing_submit_total", "billing_submit");
                integrationManagementService.markSuccess(taskId, "{\"message\":\"" + safe(result.message()) + "\"}");
            } else {
                repository.updateBillingRecord(new M6BillingRows.BillingRecordRow(
                    recordId, command.caseId(), command.orderId(), billingNo, command.billingStage(), command.itemType(), command.itemName(),
                    command.quantity(), command.amount(), "FAILED", null, command.operatorUserId(), command.operatorName(),
                    null, EXTERNAL_SYSTEM, result.message(), now, now));
                if (command.orderId() != null) {
                    repository.updateMedicalOrderBillingStatus(command.orderId(), "FAILED");
                }
                incrementCounter("billing_submit_failed_total", "billing_submit");
                integrationManagementService.markFailure(taskId, "BILLING_SUBMIT_FAILED", result.message(),
                    "{\"message\":\"" + safe(result.message()) + "\"}", true);
            }
        } catch (RuntimeException exception) {
            incrementCounter("billing_submit_failed_total", "billing_submit");
            throw exception;
        } finally {
            stopTimer(sample, "billing_submit_duration", "billing_submit");
        }
        return toView(requireBillingRecord(recordId));
    }

    private BillingRecordView findLatestSpecialOrderBilling(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return null;
        }
        List<BillingRecordView> rows = listBillingRecords(null, "SPECIAL_ORDER", null, null, orderId, null, null);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private BillingGateway.BillingSubmitRequest toGatewayRequest(M6BillingRows.BillingRecordRow row,
                                                                 String operatorUserId,
                                                                 String operatorName) {
        return new BillingGateway.BillingSubmitRequest(
            row.billingNo(),
            row.caseId(),
            row.orderId(),
            row.billingStage(),
            row.itemType(),
            row.itemName(),
            row.quantity(),
            row.amount(),
            operatorUserId,
            operatorName);
    }

    private M6BillingRows.BillingRecordRow requireBillingRecord(String id) {
        M6BillingRows.BillingRecordRow row = repository.findBillingRecordById(id);
        if (row == null) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Billing record not found");
        }
        return row;
    }

    private BillingRecordView toView(M6BillingRows.BillingRecordRow row) {
        IntegrationManagementService.IntegrationTaskView task =
            integrationManagementService.findLatestTask(BUSINESS_TYPE_BILLING_RECORD, row.id(), row.billingStage());
        return new BillingRecordView(
            row.id(),
            row.caseId(),
            row.orderId(),
            row.billingNo(),
            row.billingStage(),
            row.itemType(),
            row.itemName(),
            row.quantity() == null ? null : row.quantity().toPlainString(),
            row.amount() == null ? null : row.amount().toPlainString(),
            row.billingStatus(),
            row.billedAt() == null ? null : row.billedAt().toString(),
            row.operatorUserId(),
            row.operatorName(),
            row.externalBillNo(),
            row.externalSystem(),
            row.remarks(),
            task == null ? null : task.id(),
            task == null ? 0 : task.retryCount(),
            task == null ? 0 : task.maxRetryCount(),
            task == null ? null : task.lastAttemptAt(),
            task == null ? null : task.lastErrorCode(),
            task == null ? null : task.lastErrorMessage(),
            task == null ? null : task.compensationStatus(),
            task == null ? null : task.reconciliationStatus(),
            task == null ? null : task.resolvedAt(),
            row.createdAt() == null ? null : row.createdAt().toString(),
            row.updatedAt() == null ? null : row.updatedAt().toString());
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private void incrementCounter(String metricName, String operation) {
        Counter.builder(metricName)
            .tags(observabilityConfiguration.operationTags(operation))
            .register(meterRegistry)
            .increment();
    }

    private void stopTimer(Timer.Sample sample, String metricName, String operation) {
        sample.stop(Timer.builder(metricName)
            .tags(observabilityConfiguration.operationTags(operation))
            .register(meterRegistry));
    }

    private record SubmitBillingCommand(
        String caseId,
        String orderId,
        String billingStage,
        String itemType,
        String itemName,
        BigDecimal quantity,
        BigDecimal amount,
        String operatorUserId,
        String operatorName
    ) {
    }

    public record BillingRecordView(
        String id,
        String caseId,
        String orderId,
        String billingNo,
        String billingStage,
        String itemType,
        String itemName,
        String quantity,
        String amount,
        String billingStatus,
        String billedAt,
        String operatorUserId,
        String operatorName,
        String externalBillNo,
        String externalSystem,
        String remarks,
        String integrationTaskId,
        int retryCount,
        int maxRetryCount,
        String lastAttemptAt,
        String lastErrorCode,
        String lastErrorMessage,
        String compensationStatus,
        String reconciliationStatus,
        String resolvedAt,
        String createdAt,
        String updatedAt
    ) {
    }

    public record ReconciliationResult(
        int totalCount,
        int matchedCount,
        int discrepancyCount,
        String from,
        String to
    ) {
    }
}
