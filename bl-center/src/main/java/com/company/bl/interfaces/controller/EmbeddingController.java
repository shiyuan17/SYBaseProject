package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.EmbeddingCompleteRequest;
import com.company.bl.interfaces.dto.EmbeddingQualityReviewRequest;
import com.company.bl.interfaces.vo.EmbeddingQualityReviewResponse;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.EmbeddingWorkstationSummaryResponse;
import com.company.bl.interfaces.vo.EmbeddingResponse;
import com.company.bl.interfaces.vo.PendingTechnicalTaskResponse;
import com.company.bl.interfaces.vo.TechnicalTrackingResponse;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/embeddings")
@Tag(name = "技术流程", description = "包埋开始与完成接口")
public class EmbeddingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public EmbeddingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "查询包埋工作站当日汇总", description = "按工作日期返回包埋工作站待处理数、已处理数及对应任务明细。")
    @RequirePermission(M3PermissionCodes.EMBEDDING)
    @GetMapping("/workstation-summary")
    public EmbeddingWorkstationSummaryResponse getWorkstationSummary(
        @Parameter(description = "工作日期，默认服务端当天日期")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate
    ) {
        TechnicalWorkflowModels.EmbeddingWorkstationSummary result =
            technicalWorkflowAppService.getEmbeddingWorkstationSummary(workDate);
        return new EmbeddingWorkstationSummaryResponse(
            result.workDate() == null ? null : result.workDate().toString(),
            result.pendingCount(),
            result.completedCount(),
            result.pendingTasks().stream().map(task -> new PendingTechnicalTaskResponse(
                task.id(), task.applicationId(), task.applicationNo(), task.patientName(), task.patientId(),
                task.caseId(), task.pathologyNo(),
                task.specimenId(), task.taskType(), task.taskStatus(), task.objectType(), task.objectId(),
                task.samplingBlockCode(), task.samplingBlockDescription(), task.sampledByName(), task.sampledAt(),
                task.payload(), task.priority(), task.currentNode(), task.stationCode(), task.stationName(),
                task.assignedToUserId(), task.assignedToName(), task.expectedCompletedAt(), task.productionRemarks(),
                task.receivedAt(), task.remarks(), task.createdAt(), task.startedAt(), task.completedAt(),
                task.deadlineAt(), task.timeoutRuleCode(), task.timedOut()))
                .toList(),
            result.completedRecords().stream().map(item -> new TechnicalTrackingResponse.EmbeddingRecordSummary(
                item.taskId(), item.caseId(), item.pathologyNo(), item.specimenId(), item.specimenName(),
                item.samplingBlockId(), item.samplingBlockCode(), item.samplingBlockDescription(), item.grossDescription(),
                item.embeddingId(), item.embeddingBoxId(), item.embeddingBoxNo(), item.sliceNotice(),
                item.evaluationLevel(), item.samplingEvaluation(), item.embeddingRemarks(), item.sampledByName(),
                item.sampledAt(), item.embeddedByName(), item.startedAt(), item.endedAt(), item.taskStatus()))
                .toList());
    }

    @Operation(summary = "开始包埋", description = "将技术任务推进到包埋中状态。")
    @RequirePermission(M3PermissionCodes.EMBEDDING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TaskStartResult result = technicalWorkflowAppService.startEmbedding(
            new TechnicalWorkflowModels.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @Operation(summary = "完成包埋", description = "完成包埋并返回包埋盒与刻字结果。")
    @RequirePermission(M3PermissionCodes.EMBEDDING)
    @PostMapping("/complete")
    public EmbeddingResponse complete(@Valid @RequestBody EmbeddingCompleteRequest request,
                                      HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.EmbeddingResult result = technicalWorkflowAppService.completeEmbedding(
            new TechnicalWorkflowModels.EmbeddingCompleteCommand(
                request.getTaskId(),
                request.getSamplingBlockId(),
                request.getEmbeddingBoxNo(),
                request.getBlockCount(),
                request.getSliceNotice(),
                request.getEvaluationLevel(),
                request.getSamplingEvaluation(),
                request.getDeviceCode(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new EmbeddingResponse(
            result.taskId(), result.embeddingId(), result.embeddingBoxId(), result.caseStatus(),
            result.markingSuccess(), result.markingMessage());
    }

    @Operation(summary = "调整包埋质量评价", description = "调整已包埋记录的切片备注、取材评价，并可同步触发重新取材。")
    @RequirePermission(M3PermissionCodes.EMBEDDING)
    @PatchMapping("/{embeddingId}/quality-review")
    public EmbeddingQualityReviewResponse updateQualityReview(
        @Parameter(description = "包埋记录 ID") @PathVariable("embeddingId") String embeddingId,
        @Valid @RequestBody EmbeddingQualityReviewRequest request,
        HttpServletRequest httpServletRequest
    ) {
        TechnicalWorkflowModels.EmbeddingQualityReviewResult result =
            technicalWorkflowAppService.updateEmbeddingQualityReview(
                new TechnicalWorkflowModels.EmbeddingQualityReviewCommand(
                    embeddingId,
                    request.getSliceNotice(),
                    request.getEvaluationLevel(),
                    request.getSamplingEvaluation(),
                    request.getUnqualifiedReasons(),
                    request.getTreatmentAction(),
                    request.getTreatmentRemark(),
                    request.isNotifiedGrossingOperator(),
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    request.getTerminalCode(),
                    request.getRemarks()));
        return new EmbeddingQualityReviewResponse(
            toEmbeddingRecordSummary(result.record()),
            result.reworkType(),
            result.reworkStatus());
    }

    private TechnicalTrackingResponse.EmbeddingRecordSummary toEmbeddingRecordSummary(
        TechnicalWorkflowModels.TechnicalEmbeddingRecord item
    ) {
        return new TechnicalTrackingResponse.EmbeddingRecordSummary(
            item.taskId(), item.caseId(), item.pathologyNo(), item.specimenId(), item.specimenName(),
            item.samplingBlockId(), item.samplingBlockCode(), item.samplingBlockDescription(), item.grossDescription(),
            item.embeddingId(), item.embeddingBoxId(), item.embeddingBoxNo(), item.sliceNotice(),
            item.evaluationLevel(), item.samplingEvaluation(), item.embeddingRemarks(), item.sampledByName(),
            item.sampledAt(), item.embeddedByName(), item.startedAt(), item.endedAt(), item.taskStatus());
    }
}
