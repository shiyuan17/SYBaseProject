package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TechnicalTaskManagementService {

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy;

    TechnicalTaskManagementService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                   TechnicalWorkflowSupport technicalWorkflowSupport,
                                   TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.technicalTaskTimeoutPolicy = technicalTaskTimeoutPolicy;
    }

    @Transactional
    TechnicalWorkflowModels.TaskView assignTechnicalTask(TechnicalWorkflowModels.TechnicalTaskAssignCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = requireTask(command.taskId());
        technicalWorkflowRepository.assignTechnicalTask(
            task.id(),
            normalizePriority(command.priority(), task.priority()),
            command.stationCode(),
            command.stationName(),
            command.assignedToUserId(),
            command.assignedToName(),
            command.expectedCompletedAt(),
            command.productionRemarks());
        technicalWorkflowSupport.insertWorkflowEvent(task, task.currentNode(), "ASSIGN", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Technical task assigned");
        return reloadTaskView(task.id());
    }

    @Transactional
    TechnicalWorkflowModels.TaskView claimTechnicalTask(TechnicalWorkflowModels.TechnicalTaskClaimCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = requireTask(command.taskId());
        technicalWorkflowRepository.claimTechnicalTask(
            task.id(),
            requireText(command.assignedToUserId(), "Assigned user is required"),
            requireText(command.assignedToName(), "Assigned name is required"),
            command.stationCode(),
            command.stationName(),
            command.remarks());
        technicalWorkflowSupport.insertWorkflowEvent(task, task.currentNode(), "CLAIM", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Technical task claimed");
        return reloadTaskView(task.id());
    }

    @Transactional
    TechnicalWorkflowModels.TaskView releaseTechnicalTask(TechnicalWorkflowModels.TechnicalTaskReleaseCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = requireTask(command.taskId());
        technicalWorkflowRepository.releaseTechnicalTask(task.id(), command.remarks());
        technicalWorkflowSupport.insertWorkflowEvent(task, task.currentNode(), "RELEASE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Technical task released");
        return reloadTaskView(task.id());
    }

    @Transactional
    TechnicalWorkflowModels.TaskView updateTechnicalTaskPriority(TechnicalWorkflowModels.TechnicalTaskPriorityCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = requireTask(command.taskId());
        technicalWorkflowRepository.updateTechnicalTaskPriority(
            task.id(),
            normalizePriority(command.priority(), null),
            command.productionRemarks());
        technicalWorkflowSupport.insertWorkflowEvent(task, task.currentNode(), "PRIORITY", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Technical task priority updated");
        return reloadTaskView(task.id());
    }

    private TechnicalWorkflowRecords.TechnicalTask requireTask(String taskId) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowRepository.findTechnicalTaskById(taskId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Technical task not found"));
        if (TechnicalWorkflowConstants.TASK_COMPLETED.equals(task.taskStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Technical task is completed");
        }
        return task;
    }

    private TechnicalWorkflowModels.TaskView reloadTaskView(String taskId) {
        TechnicalWorkflowRecords.TechnicalTask task = requireTask(taskId);
        return technicalWorkflowSupport.toTaskView(
            task,
            technicalTaskTimeoutPolicy.evaluate(task, technicalTaskTimeoutPolicy.snapshot(java.time.LocalDateTime.now())));
    }

    private String normalizePriority(String priority, String fallback) {
        String candidate = priority == null || priority.isBlank() ? fallback : priority.trim();
        if (candidate == null || candidate.isBlank()) {
            candidate = "NORMAL";
        }
        if (!"NORMAL".equals(candidate) && !"PRIORITY".equals(candidate) && !"STAT".equals(candidate)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported technical task priority");
        }
        return candidate;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value.trim();
    }
}
