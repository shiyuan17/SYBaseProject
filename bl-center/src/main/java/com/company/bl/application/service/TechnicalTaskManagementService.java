package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.interfaces.auth.RbacPermissionRepository;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.notification.application.WorkflowNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
class TechnicalTaskManagementService {

    private static final String ROLE_PATHOLOGY_ADMIN = "PATHOLOGY_ADMIN";

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy;
    private final WorkflowNotificationService workflowNotificationService;
    private final RbacPermissionRepository permissionRepository;

    TechnicalTaskManagementService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                   TechnicalWorkflowSupport technicalWorkflowSupport,
                                   TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy,
                                   WorkflowNotificationService workflowNotificationService,
                                   RbacPermissionRepository permissionRepository) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.technicalTaskTimeoutPolicy = technicalTaskTimeoutPolicy;
        this.workflowNotificationService = workflowNotificationService;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    TechnicalWorkflowModels.TaskView assignTechnicalTask(TechnicalWorkflowModels.TechnicalTaskAssignCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = requireTask(command.taskId());
        requireAssignable(task, command.operatorUserId(), command.operatorRoleCode());
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
        requireNodeRole(task, command.operatorRoleCode());
        requireOperatorIdentity(command.assignedToUserId(), command.assignedToName(), command.operatorUserId(), command.operatorName());
        requireClaimable(task, command.operatorUserId(), command.operatorRoleCode());
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
        requireReleasable(task, command.operatorUserId(), command.operatorRoleCode());
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
        requireAdmin(command.operatorRoleCode(), "Only admin can update technical task priority");
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
        requireRemarksEditable(task, command.operatorUserId(), command.operatorRoleCode(), command.remarks(), command.productionRemarks());
        String nextRemarks = command.remarks() != null ? trimToNull(command.remarks()) : task.remarks();
        String nextProductionRemarks = command.productionRemarks() != null
            ? trimToNull(command.productionRemarks())
            : task.productionRemarks();
        technicalWorkflowRepository.updateTechnicalTaskRemarks(
            task.id(),
            nextRemarks,
            nextProductionRemarks);
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

    private void requireAssignable(TechnicalWorkflowRecords.TechnicalTask task, String operatorUserId, String operatorRoleCode) {
        if (isAdmin(operatorRoleCode)) {
            return;
        }
        requireNodeRole(task, operatorRoleCode);
        String assignedToUserId = trimToNull(task.assignedToUserId());
        if (assignedToUserId != null && !Objects.equals(assignedToUserId, trimToNull(operatorUserId))) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "Only admin can reassign tasks already owned by another user");
        }
    }

    private void requireClaimable(TechnicalWorkflowRecords.TechnicalTask task, String operatorUserId, String operatorRoleCode) {
        if (isAdmin(operatorRoleCode)) {
            return;
        }
        String assignedToUserId = trimToNull(task.assignedToUserId());
        String normalizedOperatorUserId = requireText(operatorUserId, "Operator user is required");
        if (assignedToUserId != null && !Objects.equals(assignedToUserId, normalizedOperatorUserId)) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Technical task is already assigned to another user");
        }
    }

    private void requireReleasable(TechnicalWorkflowRecords.TechnicalTask task, String operatorUserId, String operatorRoleCode) {
        String assignedToUserId = trimToNull(task.assignedToUserId());
        if (assignedToUserId == null) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Technical task is already unassigned");
        }
        if (isAdmin(operatorRoleCode)) {
            return;
        }
        requireNodeRole(task, operatorRoleCode);
        if (!Objects.equals(assignedToUserId, requireText(operatorUserId, "Operator user is required"))) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "Only the owner can release their own assigned task");
        }
    }

    private void requireRemarksEditable(
        TechnicalWorkflowRecords.TechnicalTask task,
        String operatorUserId,
        String operatorRoleCode,
        String remarks,
        String productionRemarks
    ) {
        if (isAdmin(operatorRoleCode)) {
            return;
        }
        if (productionRemarks != null) {
            requireProductionRemarksEditable(task, operatorUserId, operatorRoleCode);
        }
        if (remarks != null) {
            requireNodeRole(task, operatorRoleCode);
            if (!Objects.equals(trimToNull(task.assignedToUserId()), requireText(operatorUserId, "Operator user is required"))) {
                throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "Only the owner can update their own assigned task remarks");
            }
        }
    }

    private void requireProductionRemarksEditable(
        TechnicalWorkflowRecords.TechnicalTask task,
        String operatorUserId,
        String operatorRoleCode
    ) {
        String workstationPermission = workstationPermissionForNode(task.currentNode());
        if (workstationPermission != null
            && permissionRepository.hasPermission(requireText(operatorUserId, "Operator user is required"), workstationPermission)) {
            return;
        }
        requireNodeRole(task, operatorRoleCode);
    }

    private void requireOperatorIdentity(
        String assignedToUserId,
        String assignedToName,
        String operatorUserId,
        String operatorName
    ) {
        String normalizedAssignedToUserId = requireText(assignedToUserId, "Assigned user is required");
        String normalizedAssignedToName = requireText(assignedToName, "Assigned name is required");
        if (!Objects.equals(normalizedAssignedToUserId, requireText(operatorUserId, "Operator user is required"))
            || !Objects.equals(normalizedAssignedToName, requireText(operatorName, "Operator name is required"))) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "Technical task claim must be performed by the same operator");
        }
    }

    private void requireNodeRole(TechnicalWorkflowRecords.TechnicalTask task, String operatorRoleCode) {
        if (isAdmin(operatorRoleCode)) {
            return;
        }
        String expectedRoleCode = roleCodeForNode(task.currentNode());
        if (expectedRoleCode == null || !expectedRoleCode.equals(trimToNull(operatorRoleCode))) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "Operator role does not match technical task node");
        }
    }

    private void requireAdmin(String operatorRoleCode, String message) {
        if (!isAdmin(operatorRoleCode)) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, message);
        }
    }

    private boolean isAdmin(String operatorRoleCode) {
        return ROLE_PATHOLOGY_ADMIN.equals(trimToNull(operatorRoleCode));
    }

    private String roleCodeForNode(String currentNode) {
        if (currentNode == null || currentNode.isBlank()) {
            return null;
        }
        return switch (currentNode.trim()) {
            case TechnicalWorkflowConstants.NODE_GROSSING -> "M3_GROSSING";
            case TechnicalWorkflowConstants.NODE_DEHYDRATION -> "M3_DEHYDRATION";
            case TechnicalWorkflowConstants.NODE_EMBEDDING -> "M3_EMBEDDING";
            case TechnicalWorkflowConstants.NODE_SLICING -> "M3_SLICING";
            case TechnicalWorkflowConstants.NODE_STAINING -> "M3_STAINING";
            case TechnicalWorkflowConstants.NODE_REWORK -> "M3_REWORK";
            default -> null;
        };
    }

    private String workstationPermissionForNode(String currentNode) {
        if (currentNode == null || currentNode.isBlank()) {
            return null;
        }
        return switch (currentNode.trim()) {
            case TechnicalWorkflowConstants.NODE_EMBEDDING -> M3PermissionCodes.EMBEDDING;
            case TechnicalWorkflowConstants.NODE_SLICING -> M3PermissionCodes.SLICING;
            default -> null;
        };
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
