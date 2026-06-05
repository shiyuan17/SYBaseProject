package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.notification.application.WorkflowNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
class TechnicalTaskManagementService {

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy;
    private final WorkflowNotificationService workflowNotificationService;

    TechnicalTaskManagementService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                   TechnicalWorkflowSupport technicalWorkflowSupport,
                                   TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy,
                                   WorkflowNotificationService workflowNotificationService) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.technicalTaskTimeoutPolicy = technicalTaskTimeoutPolicy;
        this.workflowNotificationService = workflowNotificationService;
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
        TechnicalWorkflowModels.TaskView view = reloadTaskView(task.id());
        workflowNotificationService.notifyUsers(new WorkflowNotificationService.BulkNotificationCommand(
            WorkflowNotificationService.TOPIC_TECH_TASK_ASSIGN,
            WorkflowNotificationService.CATEGORY_TODO_TASK,
            WorkflowNotificationService.LEVEL_MEDIUM,
            "技术任务已分派",
            "病理号 %s 的 %s 任务已分派给你，请及时处理。".formatted(task.pathologyNo(), task.taskType()),
            "病理号 %s 的 %s 任务已分派给你".formatted(task.pathologyNo(), task.taskType()),
            null,
            "/technical-workflow/tasks",
            buildTaskQuery(view),
            "查看任务",
            command.operatorUserId(),
            false,
            java.util.List.of(new WorkflowNotificationService.Recipient(view.assignedToUserId(), view.assignedToName()))
        ));
        return view;
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
        TechnicalWorkflowModels.TaskView view = reloadTaskView(task.id());
        workflowNotificationService.notifyUsers(new WorkflowNotificationService.BulkNotificationCommand(
            WorkflowNotificationService.TOPIC_TECH_TASK_RELEASE,
            WorkflowNotificationService.CATEGORY_SYSTEM_MESSAGE,
            WorkflowNotificationService.LEVEL_MEDIUM,
            "技术任务已释放",
            "病理号 %s 的 %s 任务已从你的名下释放，请以最新任务分派为准。".formatted(task.pathologyNo(), task.taskType()),
            "病理号 %s 的 %s 任务已从你的名下释放".formatted(task.pathologyNo(), task.taskType()),
            null,
            "/technical-workflow/tasks",
            buildTaskQuery(view),
            "查看任务",
            command.operatorUserId(),
            false,
            java.util.List.of(new WorkflowNotificationService.Recipient(task.assignedToUserId(), task.assignedToName()))
        ));
        return view;
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
        TechnicalWorkflowModels.TaskView view = reloadTaskView(task.id());
        workflowNotificationService.notifyUsers(new WorkflowNotificationService.BulkNotificationCommand(
            WorkflowNotificationService.TOPIC_TECH_TASK_PRIORITY,
            WorkflowNotificationService.CATEGORY_TODO_TASK,
            WorkflowNotificationService.LEVEL_MEDIUM,
            "技术任务优先级已调整",
            buildPriorityContent(task, view.priority(), command.productionRemarks()),
            "病理号 %s 的 %s 任务优先级已调整为 %s".formatted(task.pathologyNo(), task.taskType(), view.priority()),
            null,
            "/technical-workflow/tasks",
            buildTaskQuery(view),
            "查看任务",
            command.operatorUserId(),
            false,
            java.util.List.of(new WorkflowNotificationService.Recipient(task.assignedToUserId(), task.assignedToName()))
        ));
        return view;
    }

    @Transactional
    TechnicalWorkflowModels.TaskView updateTechnicalTaskRemarks(TechnicalWorkflowModels.TechnicalTaskRemarksCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = requireTask(command.taskId());
        technicalWorkflowRepository.updateTechnicalTaskRemarks(
            task.id(),
            trimToNull(command.remarks()),
            trimToNull(command.productionRemarks()));
        technicalWorkflowSupport.insertWorkflowEvent(task, task.currentNode(), "REMARKS", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Technical task remarks updated");
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, String> buildTaskQuery(TechnicalWorkflowModels.TaskView task) {
        Map<String, String> query = new LinkedHashMap<>();
        putIfPresent(query, "taskId", task.id());
        putIfPresent(query, "caseId", task.caseId());
        putIfPresent(query, "pathologyNo", task.pathologyNo());
        return query;
    }

    private String buildPriorityContent(
        TechnicalWorkflowRecords.TechnicalTask task,
        String priority,
        String productionRemarks
    ) {
        StringBuilder builder = new StringBuilder()
            .append("病理号 ")
            .append(task.pathologyNo())
            .append(" 的 ")
            .append(task.taskType())
            .append(" 任务优先级已调整为 ")
            .append(priority)
            .append("。");
        if (productionRemarks != null && !productionRemarks.isBlank()) {
            builder.append(" 备注：").append(productionRemarks.trim());
        }
        return builder.toString();
    }

    private void putIfPresent(Map<String, String> query, String key, String value) {
        if (value != null && !value.isBlank()) {
            query.put(key, value);
        }
    }
}
