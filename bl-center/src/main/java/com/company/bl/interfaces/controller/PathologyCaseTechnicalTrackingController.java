package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.vo.PendingTechnicalTaskResponse;
import com.company.bl.interfaces.vo.TechnicalTrackingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pathology-cases")
public class PathologyCaseTechnicalTrackingController {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public PathologyCaseTechnicalTrackingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @RequirePermission(M3PermissionCodes.TECHNICAL_TRACKING_QUERY)
    @GetMapping("/{id}/technical-tracking")
    public TechnicalTrackingResponse getTracking(@PathVariable("id") String caseId) {
        TechnicalWorkflowAppService.TechnicalTrackingView result = technicalWorkflowAppService.getTechnicalTracking(caseId);
        return new TechnicalTrackingResponse(
            result.caseId(),
            result.pathologyNo(),
            result.caseStatus(),
            result.technicalTasks().stream().map(task -> new PendingTechnicalTaskResponse(
                task.id(), task.applicationId(), task.applicationNo(), task.caseId(), task.pathologyNo(),
                task.specimenId(), task.taskType(), task.taskStatus(), task.objectType(), task.objectId(),
                task.payload(), task.remarks(), task.createdAt(), task.startedAt(), task.completedAt()))
                .toList(),
            result.specimens().stream().map(item -> new TechnicalTrackingResponse.SpecimenSummary(
                item.specimenId(), item.specimenNo(), item.barcode(), item.specimenName(), item.specimenStatus()))
                .toList(),
            result.blocks().stream().map(item -> new TechnicalTrackingResponse.BlockSummary(
                item.blockId(), item.specimenId(), item.blockCode(), item.embeddingBoxNo(), item.description()))
                .toList(),
            result.embeddingBoxes().stream().map(item -> new TechnicalTrackingResponse.EmbeddingBoxSummary(
                item.embeddingBoxId(), item.specimenId(), item.embeddingBoxNo(), item.sliceNotice(), item.slideCount()))
                .toList(),
            result.slides().stream().map(item -> new TechnicalTrackingResponse.SlideSummary(
                item.slideId(), item.specimenId(), item.embeddingBoxId(), item.slideNo(), item.slideStatus(), item.qualityStatus()))
                .toList(),
            result.reworks().stream().map(item -> new TechnicalTrackingResponse.ReworkSummary(
                item.reworkOrderId(), item.reworkType(), item.status(), item.reason()))
                .toList(),
            result.events().stream().map(item -> new TechnicalTrackingResponse.EventSummary(
                item.nodeCode(), item.eventType(), item.eventStatus(), item.eventTime(), item.operatorName(), item.eventContent()))
                .toList());
    }
}
