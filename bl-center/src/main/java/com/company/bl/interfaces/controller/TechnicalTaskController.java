package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.vo.PendingTechnicalTaskPageResponse;
import com.company.bl.interfaces.vo.PendingTechnicalTaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/technical-tasks")
@Tag(name = "技术流程", description = "技术任务待办查询接口")
public class TechnicalTaskController {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public TechnicalTaskController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "查询待处理技术任务", description = "分页查询技术流程待处理任务列表。")
    @RequirePermission(M3PermissionCodes.TECHNICAL_TASK_QUERY)
    @GetMapping("/pending")
    public PendingTechnicalTaskPageResponse listPending(
        @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "任务类型") @RequestParam(required = false) String taskType,
        @Parameter(description = "任务状态") @RequestParam(required = false) String taskStatus,
        @Parameter(description = "申请单号") @RequestParam(required = false) String applicationNo,
        @Parameter(description = "病理号") @RequestParam(required = false) String pathologyNo,
        @Parameter(description = "对象类型") @RequestParam(required = false) String objectType,
        @Parameter(description = "创建时间起点，ISO-8601") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
        @Parameter(description = "创建时间终点，ISO-8601") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
        @Parameter(description = "是否只查询超时任务") @RequestParam(defaultValue = "false") boolean timedOutOnly
    ) {
        TechnicalWorkflowModels.PendingTechnicalTaskPage result = technicalWorkflowAppService.listPendingTasks(
            new TechnicalWorkflowModels.PendingTechnicalTaskQuery(
                page, size, taskType, taskStatus, applicationNo, pathologyNo, objectType, createdFrom, createdTo, timedOutOnly));
        return new PendingTechnicalTaskPageResponse(
            result.items().stream().map(this::toResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    private PendingTechnicalTaskResponse toResponse(TechnicalWorkflowModels.TaskView item) {
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
            item.completedAt(),
            item.deadlineAt(),
            item.timeoutRuleCode(),
            item.timedOut());
    }
}
