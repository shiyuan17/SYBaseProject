package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.GrossingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.GrossingResponse;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/grossings")
public class GrossingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public GrossingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @RequirePermission(M3PermissionCodes.GROSSING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.TaskStartResult result = technicalWorkflowAppService.startGrossing(
            new TechnicalWorkflowAppService.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @RequirePermission(M3PermissionCodes.GROSSING)
    @PostMapping("/complete")
    public GrossingResponse complete(@Valid @RequestBody GrossingCompleteRequest request,
                                     HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.GrossingResult result = technicalWorkflowAppService.completeGrossing(
            new TechnicalWorkflowAppService.GrossingCompleteCommand(
                request.getTaskId(),
                request.getCaseId(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getSpecimens().stream().map(item -> new TechnicalWorkflowAppService.GrossingSpecimenItem(
                    item.getSpecimenId(),
                    item.getSpecimenType(),
                    item.getBodyPartId(),
                    item.getSamplingTemplateId(),
                    item.getGrossDescription(),
                    item.getBlocks().stream().map(block -> new TechnicalWorkflowAppService.GrossingBlockItem(
                        block.getBlockSite(),
                        block.getBlockDescription(),
                        block.getSpecialRequirement()))
                        .toList(),
                    item.getMediaAssets() == null ? java.util.List.of() : item.getMediaAssets().stream()
                        .map(asset -> new TechnicalWorkflowAppService.MediaAssetInput(asset.getFileUrl(), asset.getFileName()))
                        .toList()))
                    .toList()));
        return new GrossingResponse(result.taskId(), result.caseId(), result.caseStatus(), result.createdDehydrationTaskCount());
    }
}
