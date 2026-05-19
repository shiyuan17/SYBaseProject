package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SlicingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.SlicingResponse;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slicings")
public class SlicingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public SlicingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.TaskStartResult result = technicalWorkflowAppService.startSlicing(
            new TechnicalWorkflowAppService.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/complete")
    public SlicingResponse complete(@Valid @RequestBody SlicingCompleteRequest request,
                                    HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.SlicingResult result = technicalWorkflowAppService.completeSlicing(
            new TechnicalWorkflowAppService.SlicingCompleteCommand(
                request.getTaskId(),
                request.getEmbeddingBoxId(),
                request.getSlideCount(),
                request.getSliceCountPerSlide(),
                request.getSliceThickness(),
                request.getQualityIssue(),
                request.getDeviceCode(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new SlicingResponse(result.taskId(), result.slicingId(), result.slideIds(), result.caseStatus());
    }
}
