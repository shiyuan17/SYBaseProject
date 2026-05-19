package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SlideStainingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.SlideStainingResponse;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slide-stainings")
public class SlideStainingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public SlideStainingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @RequirePermission(M3PermissionCodes.STAINING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.TaskStartResult result = technicalWorkflowAppService.startSlideStaining(
            new TechnicalWorkflowAppService.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @RequirePermission(M3PermissionCodes.STAINING)
    @PostMapping("/complete")
    public SlideStainingResponse complete(@Valid @RequestBody SlideStainingCompleteRequest request,
                                          HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.SlideStainingResult result = technicalWorkflowAppService.completeSlideStaining(
            new TechnicalWorkflowAppService.SlideStainingCompleteCommand(
                request.getTaskId(),
                request.getSlideId(),
                request.getStainingType(),
                request.getQualityIssue(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new SlideStainingResponse(result.taskId(), result.slideId(), result.caseStatus());
    }
}
