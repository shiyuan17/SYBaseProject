package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dehydrations")
@Tag(name = "技术流程", description = "脱水任务开始与完成接口")
public class DehydrationController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public DehydrationController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "开始脱水", description = "将选中的脱水技术任务推进到处理中状态。")
    @RequirePermission(M3PermissionCodes.DEHYDRATION)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TaskStartResult result = technicalWorkflowAppService.startDehydration(
            new TechnicalWorkflowModels.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @Operation(summary = "完成脱水", description = "完成选中的脱水技术任务并创建后续包埋任务。")
    @RequirePermission(M3PermissionCodes.DEHYDRATION)
    @PostMapping("/complete")
    public TaskOperationResponse complete(@Valid @RequestBody TechnicalTaskStartRequest request,
                                          HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TaskStartResult result = technicalWorkflowAppService.completeDehydration(
            new TechnicalWorkflowModels.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }
}
