package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.vo.PendingTechnicalTaskResponse;
import com.company.bl.interfaces.vo.TechnicalTrackingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pathology-cases")
@Tag(name = "技术流程", description = "病例级技术流程追踪接口")
public class PathologyCaseTechnicalTrackingController {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public PathologyCaseTechnicalTrackingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "查询病例技术追踪", description = "按病例 ID、病理号或技术对象 ID 查询技术任务、蜡块、包埋盒、玻片、质控和返工追踪信息。")
    @RequirePermission(M3PermissionCodes.TECHNICAL_TRACKING_QUERY)
    @GetMapping("/{id}/technical-tracking")
    public TechnicalTrackingResponse getTracking(@Parameter(description = "病例 ID、病理号或技术对象 ID") @PathVariable("id") String caseIdentifier,
                                                 @Parameter(description = "开始日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate dateFrom,
                                                 @Parameter(description = "结束日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate dateTo,
                                                 @Parameter(description = "工作日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate workDate) {
        TechnicalWorkflowModels.TechnicalTrackingView result =
            technicalWorkflowAppService.getTechnicalTracking(caseIdentifier, dateFrom, dateTo, workDate);
        return new TechnicalTrackingResponse(
            result.caseId(),
            result.pathologyNo(),
            result.caseStatus(),
            result.technicalTasks().stream().map(task -> new PendingTechnicalTaskResponse(
                task.id(), task.applicationId(), task.applicationNo(), task.patientName(), task.patientId(), task.patientIdDisplay(),
                task.caseId(), task.pathologyNo(),
                task.specimenId(), task.taskType(), task.taskStatus(), task.objectType(), task.objectId(),
                task.objectDisplayNo(), task.samplingBlockCode(), task.samplingBlockDescription(), task.embeddingRemarks(), task.specimenName(), task.grossDescription(), task.sampledByName(), task.sampledAt(),
                task.payload(), task.priority(), task.currentNode(), task.stationCode(), task.stationName(),
                task.assignedToUserId(), task.assignedToName(), task.expectedCompletedAt(), task.productionRemarks(),
                task.receivedAt(), task.remarks(), task.createdAt(), task.startedAt(), task.completedAt(),
                task.deadlineAt(), task.timeoutRuleCode(), task.timedOut()))
                .toList(),
            result.specimens().stream().map(item -> new TechnicalTrackingResponse.SpecimenSummary(
                item.specimenId(), item.specimenNo(), item.barcode(), item.specimenName(), item.specimenStatus()))
                .toList(),
            result.blocks().stream().map(item -> new TechnicalTrackingResponse.BlockSummary(
                item.blockId(), item.specimenId(), item.blockCode(), item.embeddingBoxNo(), item.embeddingBoxName(), item.description(),
                item.specimenName(), item.grossDescription(), item.embeddingRemarks()))
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
