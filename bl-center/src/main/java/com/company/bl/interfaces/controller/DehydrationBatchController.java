package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.BatchOperatorRequest;
import com.company.bl.interfaces.dto.CompleteDehydrationBatchRequest;
import com.company.bl.interfaces.dto.CreateDehydrationBatchRequest;
import com.company.bl.interfaces.vo.DehydrationBatchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "技术流程", description = "脱水批次创建、开始与完成接口")
public class DehydrationBatchController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public DehydrationBatchController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "创建脱水批次", description = "根据病例和取材块创建脱水批次。")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "创建成功", useReturnTypeSchema = true))
    @RequirePermission(M3PermissionCodes.DEHYDRATION)
    @PostMapping
    public ResponseEntity<DehydrationBatchResponse> create(@Valid @RequestBody CreateDehydrationBatchRequest request,
                                                           HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.DehydrationBatchResult result = technicalWorkflowAppService.createDehydrationBatch(
            new TechnicalWorkflowModels.CreateDehydrationBatchCommand(
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

    @Operation(summary = "开始脱水批次", description = "将指定脱水批次推进到处理中状态。")
    @RequirePermission(M3PermissionCodes.DEHYDRATION)
    @PostMapping("/{id}/start")
    public DehydrationBatchResponse start(@Parameter(description = "脱水批次 ID") @PathVariable("id") String id,
                                          @Valid @RequestBody BatchOperatorRequest request,
                                          HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.DehydrationBatchResult result = technicalWorkflowAppService.startDehydrationBatch(
            new TechnicalWorkflowModels.BatchOperatorCommand(
                id,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new DehydrationBatchResponse(result.batchId(), result.batchNo(), result.batchStatus(), result.taskCount());
    }

    @Operation(summary = "完成脱水批次", description = "完成指定脱水批次并记录附件。")
    @RequirePermission(M3PermissionCodes.DEHYDRATION)
    @PostMapping("/{id}/complete")
    public DehydrationBatchResponse complete(@Parameter(description = "脱水批次 ID") @PathVariable("id") String id,
                                             @Valid @RequestBody CompleteDehydrationBatchRequest request,
                                             HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.DehydrationBatchResult result = technicalWorkflowAppService.completeDehydrationBatch(
            new TechnicalWorkflowModels.CompleteDehydrationBatchCommand(
                id,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getMediaAssets() == null ? java.util.List.of() : request.getMediaAssets().stream()
                    .map(item -> new TechnicalWorkflowModels.MediaAssetInput(item.getFileUrl(), item.getFileName()))
                    .toList()));
        return new DehydrationBatchResponse(result.batchId(), result.batchNo(), result.batchStatus(), result.taskCount());
    }
}
