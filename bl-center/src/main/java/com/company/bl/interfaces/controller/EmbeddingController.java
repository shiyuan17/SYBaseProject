package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.EmbeddingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.EmbeddingResponse;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/embeddings")
public class EmbeddingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public EmbeddingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @RequirePermission(M3PermissionCodes.EMBEDDING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.TaskStartResult result = technicalWorkflowAppService.startEmbedding(
            new TechnicalWorkflowAppService.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @RequirePermission(M3PermissionCodes.EMBEDDING)
    @PostMapping("/complete")
    public EmbeddingResponse complete(@Valid @RequestBody EmbeddingCompleteRequest request,
                                      HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.EmbeddingResult result = technicalWorkflowAppService.completeEmbedding(
            new TechnicalWorkflowAppService.EmbeddingCompleteCommand(
                request.getTaskId(),
                request.getSamplingBlockId(),
                request.getEmbeddingBoxNo(),
                request.getBlockCount(),
                request.getSliceNotice(),
                request.getEvaluationLevel(),
                request.getSamplingEvaluation(),
                request.getDeviceCode(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new EmbeddingResponse(
            result.taskId(), result.embeddingId(), result.embeddingBoxId(), result.caseStatus(),
            result.markingSuccess(), result.markingMessage());
    }
}
