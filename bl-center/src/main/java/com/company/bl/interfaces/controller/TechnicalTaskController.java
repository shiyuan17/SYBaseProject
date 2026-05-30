package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.TechnicalTaskAssignRequest;
import com.company.bl.interfaces.dto.TechnicalTaskClaimRequest;
import com.company.bl.interfaces.dto.TechnicalTaskPriorityRequest;
import com.company.bl.interfaces.dto.TechnicalTaskReleaseRequest;
import com.company.bl.interfaces.vo.PendingTechnicalTaskPageResponse;
import com.company.bl.interfaces.vo.PendingTechnicalTaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/technical-tasks")
@Tag(name = "技术流程", description = "技术任务待办查询与分派接口")
public class TechnicalTaskController extends TechnicalControllerSupport {

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
        @Parameter(description = "任务优先级") @RequestParam(required = false) String priority,
        @Parameter(description = "责任技师用户 ID") @RequestParam(required = false) String assignedToUserId,
        @Parameter(description = "当前节点") @RequestParam(required = false) String currentNode,
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
                page, size, taskType, taskStatus, priority, assignedToUserId, currentNode, applicationNo, pathologyNo,
                objectType, createdFrom, createdTo, timedOutOnly));
        return new PendingTechnicalTaskPageResponse(
            result.items().stream().map(this::toResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "分派技术任务", description = "设置任务优先级、工作台、责任技师与期望完成时间。")
    @RequirePermission(M3PermissionCodes.TECHNICAL_TASK_QUERY)
    @PostMapping("/{id}/assign")
    public PendingTechnicalTaskResponse assign(@PathVariable String id,
                                               @Valid @RequestBody TechnicalTaskAssignRequest request,
                                               HttpServletRequest httpServletRequest) {
        return toResponse(technicalWorkflowAppService.assignTechnicalTask(
            new TechnicalWorkflowModels.TechnicalTaskAssignCommand(
                id,
                request.getPriority(),
                request.getStationCode(),
                request.getStationName(),
                request.getAssignedToUserId(),
                request.getAssignedToName(),
                request.getExpectedCompletedAt(),
                request.getProductionRemarks(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode())));
    }

    @Operation(summary = "领取技术任务", description = "当前技师接单并写入工作台信息。")
    @RequirePermission(M3PermissionCodes.TECHNICAL_TASK_QUERY)
    @PostMapping("/{id}/claim")
    public PendingTechnicalTaskResponse claim(@PathVariable String id,
                                              @Valid @RequestBody TechnicalTaskClaimRequest request,
                                              HttpServletRequest httpServletRequest) {
        return toResponse(technicalWorkflowAppService.claimTechnicalTask(
            new TechnicalWorkflowModels.TechnicalTaskClaimCommand(
                id,
                request.getAssignedToUserId(),
                request.getAssignedToName(),
                request.getStationCode(),
                request.getStationName(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks())));
    }

    @Operation(summary = "释放技术任务", description = "清空任务责任技师，回到未分派状态。")
    @RequirePermission(M3PermissionCodes.TECHNICAL_TASK_QUERY)
    @PostMapping("/{id}/release")
    public PendingTechnicalTaskResponse release(@PathVariable String id,
                                                @Valid @RequestBody TechnicalTaskReleaseRequest request,
                                                HttpServletRequest httpServletRequest) {
        return toResponse(technicalWorkflowAppService.releaseTechnicalTask(
            new TechnicalWorkflowModels.TechnicalTaskReleaseCommand(
                id,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks())));
    }

    @Operation(summary = "调整技术任务优先级", description = "调整任务优先级并同步生产备注。")
    @RequirePermission(M3PermissionCodes.TECHNICAL_TASK_QUERY)
    @PostMapping("/{id}/priority")
    public PendingTechnicalTaskResponse priority(@PathVariable String id,
                                                 @Valid @RequestBody TechnicalTaskPriorityRequest request,
                                                 HttpServletRequest httpServletRequest) {
        return toResponse(technicalWorkflowAppService.updateTechnicalTaskPriority(
            new TechnicalWorkflowModels.TechnicalTaskPriorityCommand(
                id,
                request.getPriority(),
                request.getProductionRemarks(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode())));
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
            item.priority(),
            item.currentNode(),
            item.stationCode(),
            item.stationName(),
            item.assignedToUserId(),
            item.assignedToName(),
            item.expectedCompletedAt(),
            item.productionRemarks(),
            item.receivedAt(),
            item.remarks(),
            item.createdAt(),
            item.startedAt(),
            item.completedAt(),
            item.deadlineAt(),
            item.timeoutRuleCode(),
            item.timedOut());
    }
}
