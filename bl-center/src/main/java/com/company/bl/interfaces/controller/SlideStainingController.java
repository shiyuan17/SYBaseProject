package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SlideStainingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.SlideStainingResponse;
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
@RequestMapping("/api/v1/slide-stainings")
@Tag(name = "技术流程", description = "染色出片开始与完成接口")
public class SlideStainingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public SlideStainingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "开始染色出片", description = "将技术任务推进到染色中状态。")
    @RequirePermission(M3PermissionCodes.STAINING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TaskStartResult result = technicalWorkflowAppService.startSlideStaining(
            new TechnicalWorkflowModels.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @Operation(summary = "完成染色出片", description = "完成染色出片并返回出片结果。")
    @RequirePermission(M3PermissionCodes.STAINING)
    @PostMapping("/complete")
    public SlideStainingResponse complete(@Valid @RequestBody SlideStainingCompleteRequest request,
                                          HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.SlideStainingResult result = technicalWorkflowAppService.completeSlideStaining(
            new TechnicalWorkflowModels.SlideStainingCompleteCommand(
                request.getTaskId(),
                request.getSlideId(),
                request.getStainingType(),
                request.getQualityIssue(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new SlideStainingResponse(result.taskId(), result.slideId(), result.caseStatus());
    }
}
