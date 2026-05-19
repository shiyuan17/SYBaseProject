package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.BatchOperatorRequest;
import com.company.bl.interfaces.dto.CompleteDehydrationBatchRequest;
import com.company.bl.interfaces.dto.CreateDehydrationBatchRequest;
import com.company.bl.interfaces.vo.DehydrationBatchResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dehydration-batches")
public class DehydrationBatchController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public DehydrationBatchController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @RequirePermission(M3PermissionCodes.DEHYDRATION)
    @PostMapping
    public ResponseEntity<DehydrationBatchResponse> create(@Valid @RequestBody CreateDehydrationBatchRequest request,
                                                           HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.DehydrationBatchResult result = technicalWorkflowAppService.createDehydrationBatch(
            new TechnicalWorkflowAppService.CreateDehydrationBatchCommand(
                request.getCaseId(),
                request.getBasketNo(),
                request.getDeviceNo(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getSamplingBlockIds()));
        return ResponseEntity.status(201).body(new DehydrationBatchResponse(
            result.batchId(), result.batchNo(), result.batchStatus(), result.taskCount()));
    }

    @RequirePermission(M3PermissionCodes.DEHYDRATION)
    @PostMapping("/{id}/start")
    public DehydrationBatchResponse start(@PathVariable("id") String id,
                                          @Valid @RequestBody BatchOperatorRequest request,
                                          HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.DehydrationBatchResult result = technicalWorkflowAppService.startDehydrationBatch(
            new TechnicalWorkflowAppService.BatchOperatorCommand(
                id,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new DehydrationBatchResponse(result.batchId(), result.batchNo(), result.batchStatus(), result.taskCount());
    }

    @RequirePermission(M3PermissionCodes.DEHYDRATION)
    @PostMapping("/{id}/complete")
    public DehydrationBatchResponse complete(@PathVariable("id") String id,
                                             @Valid @RequestBody CompleteDehydrationBatchRequest request,
                                             HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.DehydrationBatchResult result = technicalWorkflowAppService.completeDehydrationBatch(
            new TechnicalWorkflowAppService.CompleteDehydrationBatchCommand(
                id,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getMediaAssets() == null ? java.util.List.of() : request.getMediaAssets().stream()
                    .map(item -> new TechnicalWorkflowAppService.MediaAssetInput(item.getFileUrl(), item.getFileName()))
                    .toList()));
        return new DehydrationBatchResponse(result.batchId(), result.batchNo(), result.batchStatus(), result.taskCount());
    }
}
