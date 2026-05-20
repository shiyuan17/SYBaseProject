package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.GrossingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.GrossingResponse;
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
@RequestMapping("/api/v1/grossings")
@Tag(name = "技术流程", description = "取材开始与完成接口")
public class GrossingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public GrossingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "开始取材", description = "将技术任务推进到取材中状态。")
    @RequirePermission(M3PermissionCodes.GROSSING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TaskStartResult result = technicalWorkflowAppService.startGrossing(
            new TechnicalWorkflowModels.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @Operation(summary = "完成取材", description = "完成取材并生成后续脱水任务。")
    @RequirePermission(M3PermissionCodes.GROSSING)
    @PostMapping("/complete")
    public GrossingResponse complete(@Valid @RequestBody GrossingCompleteRequest request,
                                     HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.GrossingResult result = technicalWorkflowAppService.completeGrossing(
            new TechnicalWorkflowModels.GrossingCompleteCommand(
                request.getTaskId(),
                request.getCaseId(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getSpecimens().stream().map(item -> new TechnicalWorkflowModels.GrossingSpecimenItem(
                    item.getSpecimenId(),
                    item.getSpecimenType(),
                    item.getBodyPartId(),
                    item.getSamplingTemplateId(),
                    item.getGrossDescription(),
                    item.getBlocks().stream().map(block -> new TechnicalWorkflowModels.GrossingBlockItem(
                        block.getBlockSite(),
                        block.getBlockDescription(),
                        block.getSpecialRequirement()))
                        .toList(),
                    item.getMediaAssets() == null ? java.util.List.of() : item.getMediaAssets().stream()
                        .map(asset -> new TechnicalWorkflowModels.MediaAssetInput(asset.getFileUrl(), asset.getFileName()))
                        .toList()))
                    .toList()));
        return new GrossingResponse(result.taskId(), result.caseId(), result.caseStatus(), result.createdDehydrationTaskCount());
    }
}
