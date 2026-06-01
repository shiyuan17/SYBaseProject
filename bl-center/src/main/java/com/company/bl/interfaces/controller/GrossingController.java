package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.GrossingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.GrossingWorkbenchContextResponse;
import com.company.bl.interfaces.vo.GrossingResponse;
import com.company.bl.interfaces.vo.PendingTechnicalTaskResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationCheckItemResponse;
import com.company.bl.interfaces.vo.TechnicalTrackingResponse;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(summary = "查询取材工作台上下文", description = "汇总当前取材任务的病例、标本、临床病史、相关检查和历史影像。")
    @RequirePermission(M3PermissionCodes.GROSSING)
    @GetMapping("/{taskId}/context")
    public GrossingWorkbenchContextResponse getContext(@PathVariable("taskId") String taskId) {
        TechnicalWorkflowModels.GrossingWorkbenchContext result =
            technicalWorkflowAppService.getGrossingWorkbenchContext(taskId);
        return new GrossingWorkbenchContextResponse(
            new GrossingWorkbenchContextResponse.TaskSummary(
                result.task().taskId(),
                result.task().taskStatus(),
                result.task().objectType(),
                result.task().objectId()),
            new GrossingWorkbenchContextResponse.CaseSummary(
                result.caseSummary().caseId(),
                result.caseSummary().applicationId(),
                result.caseSummary().applicationNo(),
                result.caseSummary().pathologyNo(),
                result.caseSummary().caseStatus(),
                result.caseSummary().patientName(),
                result.caseSummary().patientId(),
                result.caseSummary().inpatientNo(),
                result.caseSummary().applicationType(),
                result.caseSummary().submittingDepartmentName()),
            toTrackingResponse(result.tracking()),
            result.clinicalDiagnosis(),
            result.clinicalHistory(),
            result.relatedExaminations(),
            result.contextSummary(),
            result.checkItems().stream().map(item -> new TechnicalSpecimenRegistrationCheckItemResponse(
                item.sequenceNo(),
                item.name()))
                .toList(),
            result.mediaAssets().stream().map(item -> new GrossingWorkbenchContextResponse.MediaAssetSummary(
                item.assetId(),
                item.specimenId(),
                item.fileName(),
                item.fileUrl(),
                item.capturedAt(),
                item.capturedByName()))
                .toList());
    }

    @Operation(summary = "开始取材", description = "将技术任务推进到取材中状态。")
    @RequirePermission(M3PermissionCodes.GROSSING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TaskStartResult result = technicalWorkflowAppService.startGrossing(
            new TechnicalWorkflowModels.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
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
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getSpecimens().stream().map(item -> new TechnicalWorkflowModels.GrossingSpecimenItem(
                    item.getSpecimenId(),
                    item.getSpecimenType(),
                    item.getBodyPartId(),
                    item.getSamplingTemplateId(),
                    item.getSizeText(),
                    item.getCutSurfaceFeature(),
                    item.getMarginMarking(),
                    item.getBlockCount(),
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

    private TechnicalTrackingResponse toTrackingResponse(TechnicalWorkflowModels.TechnicalTrackingView result) {
        return new TechnicalTrackingResponse(
            result.caseId(),
            result.pathologyNo(),
            result.caseStatus(),
            result.technicalTasks().stream().map(task -> new PendingTechnicalTaskResponse(
                task.id(), task.applicationId(), task.applicationNo(), task.caseId(), task.pathologyNo(),
                task.specimenId(), task.taskType(), task.taskStatus(), task.objectType(), task.objectId(),
                task.samplingBlockCode(), task.samplingBlockDescription(), task.sampledByName(), task.sampledAt(),
                task.payload(), task.priority(), task.currentNode(), task.stationCode(), task.stationName(),
                task.assignedToUserId(), task.assignedToName(), task.expectedCompletedAt(), task.productionRemarks(),
                task.receivedAt(), task.remarks(), task.createdAt(), task.startedAt(), task.completedAt(),
                task.deadlineAt(), task.timeoutRuleCode(), task.timedOut()))
                .toList(),
            result.specimens().stream().map(item -> new TechnicalTrackingResponse.SpecimenSummary(
                item.specimenId(), item.specimenNo(), item.barcode(), item.specimenName(), item.specimenStatus()))
                .toList(),
            result.blocks().stream().map(item -> new TechnicalTrackingResponse.BlockSummary(
                item.blockId(), item.specimenId(), item.blockCode(), item.embeddingBoxNo(), item.description(),
                item.specimenName(), item.grossDescription()))
                .toList(),
            result.embeddingBoxes().stream().map(item -> new TechnicalTrackingResponse.EmbeddingBoxSummary(
                item.embeddingBoxId(), item.specimenId(), item.embeddingBoxNo(), item.sliceNotice(), item.slideCount()))
                .toList(),
            result.embeddingRecords().stream().map(item -> new TechnicalTrackingResponse.EmbeddingRecordSummary(
                item.taskId(), item.caseId(), item.pathologyNo(), item.specimenId(), item.specimenName(),
                item.samplingBlockId(), item.samplingBlockCode(), item.samplingBlockDescription(), item.grossDescription(),
                item.embeddingId(), item.embeddingBoxId(), item.embeddingBoxNo(), item.sliceNotice(),
                item.evaluationLevel(), item.samplingEvaluation(), item.embeddingRemarks(), item.sampledByName(),
                item.sampledAt(), item.embeddedByName(), item.startedAt(), item.endedAt(), item.taskStatus()))
                .toList(),
            result.embeddingEvaluationRecords().stream().map(item -> new TechnicalTrackingResponse.EmbeddingEvaluationRecordSummary(
                item.embeddingId(), item.caseId(), item.pathologyNo(), item.specimenId(), item.specimenName(),
                item.samplingBlockId(), item.samplingBlockCode(), item.embeddingBoxNo(), item.evaluationLevel(),
                item.samplingEvaluation(), item.embeddingRemarks(), item.embeddedByName(), item.endedAt()))
                .toList(),
            result.slides().stream().map(item -> new TechnicalTrackingResponse.SlideSummary(
                item.slideId(), item.specimenId(), item.embeddingBoxId(), item.slideNo(), item.slideStatus(), item.qualityStatus()))
                .toList(),
            result.qcEvaluations().stream().map(item -> new TechnicalTrackingResponse.SlideQcEvaluationSummary(
                item.qcEvaluationId(), item.specimenId(), item.slideId(), item.slideNo(), item.qcType(),
                item.evaluationResult(), item.issueDescription(), item.improvementSuggestion(), item.evaluatorName(),
                item.evaluatedAt(), item.remarks()))
                .toList(),
            result.reworks().stream().map(item -> new TechnicalTrackingResponse.ReworkSummary(
                item.reworkOrderId(), item.reworkType(), item.status(), item.reason()))
                .toList(),
            result.events().stream().map(item -> new TechnicalTrackingResponse.EventSummary(
                item.nodeCode(), item.eventType(), item.eventStatus(), item.eventTime(), item.operatorName(), item.eventContent()))
                .toList());
    }
}
