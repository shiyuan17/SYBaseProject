package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.vo.PendingTechnicalTaskPageResponse;
import com.company.bl.interfaces.vo.PendingTechnicalTaskResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/technical-tasks")
public class TechnicalTaskController {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public TechnicalTaskController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @RequirePermission(M3PermissionCodes.TECHNICAL_TASK_QUERY)
    @GetMapping("/pending")
    public PendingTechnicalTaskPageResponse listPending(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(required = false) String taskType,
                                                        @RequestParam(required = false) String taskStatus,
                                                        @RequestParam(required = false) String applicationNo,
                                                        @RequestParam(required = false) String pathologyNo,
                                                        @RequestParam(required = false) String objectType) {
        TechnicalWorkflowAppService.PendingTechnicalTaskPage result = technicalWorkflowAppService.listPendingTasks(
            new TechnicalWorkflowAppService.PendingTechnicalTaskQuery(page, size, taskType, taskStatus, applicationNo, pathologyNo, objectType));
        return new PendingTechnicalTaskPageResponse(
            result.items().stream().map(this::toResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    private PendingTechnicalTaskResponse toResponse(TechnicalWorkflowAppService.TaskView item) {
        return new PendingTechnicalTaskResponse(
            item.id(),
            item.applicationId(),
            item.applicationNo(),
            item.caseId(),
            item.pathologyNo(),
            item.specimenId(),
            item.taskType(),
            item.taskStatus(),
            item.objectType(),
            item.objectId(),
            item.payload(),
            item.remarks(),
            item.createdAt(),
            item.startedAt(),
            item.completedAt());
    }
}
