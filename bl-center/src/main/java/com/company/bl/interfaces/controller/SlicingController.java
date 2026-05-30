package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SlicingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.SlicingResponse;
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
@RequestMapping("/api/v1/slicings")
@Tag(name = "技术流程", description = "切片开始与完成接口")
public class SlicingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public SlicingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "开始切片", description = "将技术任务推进到切片中状态。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TaskStartResult result = technicalWorkflowAppService.startSlicing(
            new TechnicalWorkflowModels.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @Operation(summary = "完成切片", description = "完成切片并返回生成的切片信息。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/complete")
    public SlicingResponse complete(@Valid @RequestBody SlicingCompleteRequest request,
                                    HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.SlicingResult result = technicalWorkflowAppService.completeSlicing(
            new TechnicalWorkflowModels.SlicingCompleteCommand(
                request.getTaskId(),
                request.getEmbeddingBoxId(),
                request.getSlideCount(),
                request.getSliceCountPerSlide(),
                request.getSliceThickness(),
                request.getQualityIssue(),
                request.getDeviceCode(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new SlicingResponse(result.taskId(), result.slicingId(), result.slideIds(), result.caseStatus());
    }
}
