package com.company.bl.integration.application;

import com.company.bl.integration.infrastructure.M6JdbcRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class IntegrationManagementService {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final M6JdbcRepository repository;

    public IntegrationManagementService(M6JdbcRepository repository) {
        this.repository = repository;
    }

    public String openTask(CreateIntegrationTaskCommand command) {
        LocalDateTime now = LocalDateTime.now();
        String taskId = "IT-" + UUID.randomUUID();
        repository.insertIntegrationTask(new M6JdbcRepository.CreateIntegrationTaskRow(
            taskId,
            command.taskType(),
            command.businessType(),
            command.businessId(),
            command.stageCode(),
            command.externalSystem(),
            command.requestPayload(),
            null,
            "RUNNING",
            0,
            DEFAULT_MAX_RETRY_COUNT,
            null,
            now,
            null,
            null,
            "NONE",
            "PENDING",
            null,
            now,
            now));
        return taskId;
    }

    public void markSuccess(String taskId, String responsePayload) {
        M6JdbcRepository.IntegrationTaskRow current = requireTask(taskId);
        repository.updateIntegrationTask(new M6JdbcRepository.IntegrationTaskRow(
            current.id(),
            current.taskType(),
            current.businessType(),
            current.businessId(),
            current.stageCode(),
            current.externalSystem(),
            current.requestPayload(),
            responsePayload,
            "SUCCESS",
            current.retryCount(),
            current.maxRetryCount(),
            null,
            LocalDateTime.now(),
            null,
            null,
            current.compensationStatus(),
            current.reconciliationStatus(),
            LocalDateTime.now(),
            current.createdAt(),
            LocalDateTime.now()));
    }

    public void markFailure(String taskId, String errorCode, String errorMessage, String responsePayload, boolean retryable) {
        M6JdbcRepository.IntegrationTaskRow current = requireTask(taskId);
        int nextRetryCount = current.retryCount() + 1;
        boolean exhausted = !retryable || nextRetryCount >= current.maxRetryCount();
        repository.updateIntegrationTask(new M6JdbcRepository.IntegrationTaskRow(
            current.id(),
            current.taskType(),
            current.businessType(),
            current.businessId(),
            current.stageCode(),
            current.externalSystem(),
            current.requestPayload(),
            responsePayload,
            exhausted ? "FAILED" : "RETRY_PENDING",
            nextRetryCount,
            current.maxRetryCount(),
            exhausted ? null : LocalDateTime.now().plusMinutes(5),
            LocalDateTime.now(),
            errorCode,
            errorMessage,
            exhausted ? "MANUAL_REQUIRED" : "RETRY_PENDING",
            current.reconciliationStatus(),
            exhausted ? LocalDateTime.now() : null,
            current.createdAt(),
            LocalDateTime.now()));
    }

    public void markRetryStarted(String taskId, String requestPayload) {
        M6JdbcRepository.IntegrationTaskRow current = requireTask(taskId);
        repository.updateIntegrationTask(new M6JdbcRepository.IntegrationTaskRow(
            current.id(),
            current.taskType(),
            current.businessType(),
            current.businessId(),
            current.stageCode(),
            current.externalSystem(),
            requestPayload,
            current.responsePayload(),
            "RUNNING",
            current.retryCount(),
            current.maxRetryCount(),
            null,
            LocalDateTime.now(),
            current.lastErrorCode(),
            current.lastErrorMessage(),
            "RETRYING",
            current.reconciliationStatus(),
            null,
            current.createdAt(),
            LocalDateTime.now()));
    }

    public void markReconciled(String taskId, String reconciliationStatus) {
        M6JdbcRepository.IntegrationTaskRow current = requireTask(taskId);
        repository.updateIntegrationTask(new M6JdbcRepository.IntegrationTaskRow(
            current.id(),
            current.taskType(),
            current.businessType(),
            current.businessId(),
            current.stageCode(),
            current.externalSystem(),
            current.requestPayload(),
            current.responsePayload(),
            current.taskStatus(),
            current.retryCount(),
            current.maxRetryCount(),
            current.nextRetryAt(),
            current.lastAttemptAt(),
            current.lastErrorCode(),
            current.lastErrorMessage(),
            current.compensationStatus(),
            reconciliationStatus,
            "MATCHED".equals(reconciliationStatus) ? LocalDateTime.now() : current.resolvedAt(),
            current.createdAt(),
            LocalDateTime.now()));
    }

    public List<IntegrationTaskView> listTasks(String taskType, String businessType, String taskStatus) {
        return repository.findIntegrationTasks(taskType, businessType, taskStatus).stream()
            .map(this::toView)
            .toList();
    }

    public IntegrationTaskView findLatestTask(String businessType, String businessId, String stageCode) {
        M6JdbcRepository.IntegrationTaskRow row = repository.findLatestIntegrationTask(businessType, businessId, stageCode);
        return row == null ? null : toView(row);
    }

    public M6JdbcRepository.IntegrationTaskRow requireTask(String taskId) {
        M6JdbcRepository.IntegrationTaskRow task = repository.findIntegrationTaskById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Integration task not found: " + taskId);
        }
        return task;
    }

    private IntegrationTaskView toView(M6JdbcRepository.IntegrationTaskRow row) {
        return new IntegrationTaskView(
            row.id(),
            row.taskType(),
            row.businessType(),
            row.businessId(),
            row.stageCode(),
            row.externalSystem(),
            row.taskStatus(),
            row.retryCount(),
            row.maxRetryCount(),
            row.nextRetryAt() == null ? null : row.nextRetryAt().toString(),
            row.lastAttemptAt() == null ? null : row.lastAttemptAt().toString(),
            row.lastErrorCode(),
            row.lastErrorMessage(),
            row.compensationStatus(),
            row.reconciliationStatus(),
            row.createdAt() == null ? null : row.createdAt().toString(),
            row.updatedAt() == null ? null : row.updatedAt().toString());
    }

    public record CreateIntegrationTaskCommand(
        String taskType,
        String businessType,
        String businessId,
        String stageCode,
        String externalSystem,
        String requestPayload
    ) {
    }

    public record IntegrationTaskView(
        String id,
        String taskType,
        String businessType,
        String businessId,
        String stageCode,
        String externalSystem,
        String taskStatus,
        int retryCount,
        int maxRetryCount,
        String nextRetryAt,
        String lastAttemptAt,
        String lastErrorCode,
        String lastErrorMessage,
        String compensationStatus,
        String reconciliationStatus,
        String createdAt,
        String updatedAt
    ) {
    }
}
