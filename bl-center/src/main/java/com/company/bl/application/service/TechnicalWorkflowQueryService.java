package com.company.bl.application.service;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class TechnicalWorkflowQueryService {

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy;

    TechnicalWorkflowQueryService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                  TechnicalWorkflowSupport technicalWorkflowSupport,
                                  TechnicalTaskTimeoutPolicy technicalTaskTimeoutPolicy) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.technicalTaskTimeoutPolicy = technicalTaskTimeoutPolicy;
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.PendingTechnicalTaskPage listPendingTasks(TechnicalWorkflowModels.PendingTechnicalTaskQuery query) {
        LocalDateTime now = LocalDateTime.now();
        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot = technicalTaskTimeoutPolicy.snapshot(now);
        TechnicalWorkflowRecords.PagedTechnicalTasks paged = technicalWorkflowRepository.findTechnicalTasks(
            new TechnicalWorkflowRecords.PendingTechnicalTaskQuery(
                query.page(),
                query.size(),
                query.taskType(),
                query.taskStatus(),
                query.applicationNo(),
                query.pathologyNo(),
                query.objectType(),
                query.createdFrom(),
                query.createdTo(),
                query.timedOutOnly(),
                timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_GROSSING),
                timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_DEHYDRATION),
                timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_STAINING)));
        return new TechnicalWorkflowModels.PendingTechnicalTaskPage(
            paged.items().stream().map(task -> toTaskView(task, timeoutSnapshot)).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalTrackingView getTechnicalTracking(String caseId) {
        LocalDateTime now = LocalDateTime.now();
        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot = technicalTaskTimeoutPolicy.snapshot(now);
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(caseId);
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        List<TechnicalWorkflowRecords.TechnicalTask> tasks = technicalWorkflowRepository.findActiveTechnicalTasksByCaseId(caseId);
        List<TechnicalWorkflowRecords.SamplingBlock> blocks = technicalWorkflowRepository.findSamplingBlocksByCaseId(caseId);
        List<TechnicalWorkflowRecords.EmbeddingBox> boxes = technicalWorkflowRepository.findEmbeddingBoxesByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.Slide> slides = technicalWorkflowRepository.findSlidesByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.SlideQcEvaluation> qcEvaluations =
            technicalWorkflowRepository.findSlideQcEvaluationsByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.ReworkOrder> reworkOrders = technicalWorkflowRepository.findReworkOrdersByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(caseId);
        Map<String, List<TechnicalWorkflowProcessingRecords.Slide>> slidesByBox = slides.stream()
            .collect(Collectors.groupingBy(TechnicalWorkflowProcessingRecords.Slide::embeddingBoxId));
        return new TechnicalWorkflowModels.TechnicalTrackingView(
            pathologyCase.id(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            tasks.stream().map(task -> toTaskView(task, timeoutSnapshot)).toList(),
            specimens.stream().map(specimen -> new TechnicalWorkflowModels.TechnicalSpecimenSummary(
                specimen.id(), specimen.specimenNo(), specimen.barcode(), specimen.specimenNameStandardized(), specimen.specimenStatus().name())).toList(),
            blocks.stream().map(block -> new TechnicalWorkflowModels.TechnicalBlockSummary(
                block.id(), block.specimenId(), block.blockCode(), block.embeddingBoxNo(), block.blockDescription())).toList(),
            boxes.stream().map(box -> new TechnicalWorkflowModels.TechnicalEmbeddingBoxSummary(
                box.id(), box.specimenId(), box.embeddingBoxNo(), box.sliceNotice(), slidesByBox.getOrDefault(box.id(), List.of()).size())).toList(),
            slides.stream().map(slide -> new TechnicalWorkflowModels.TechnicalSlideSummary(
                slide.id(), slide.specimenId(), slide.embeddingBoxId(), slide.slideNo(), slide.slideStatus(), slide.qualityStatus())).toList(),
            qcEvaluations.stream().map(item -> new TechnicalWorkflowModels.SlideQcEvaluationSummary(
                item.id(), item.specimenId(), item.slideId(), item.slideNo(), item.qcType(), item.evaluationResult(),
                item.issueDescription(), item.improvementSuggestion(), item.evaluatorName(), stringify(item.evaluatedAt()), item.remarks())).toList(),
            reworkOrders.stream().map(order -> new TechnicalWorkflowModels.ReworkSummary(
                order.id(), order.reworkType(), order.status(), order.reason())).toList(),
            events.stream()
                .sorted(Comparator.comparing(TrackingEvent::eventTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTrackingEvent)
                .toList());
    }

    private TechnicalWorkflowModels.TaskView toTaskView(TechnicalWorkflowRecords.TechnicalTask task,
                                                        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot) {
        return technicalWorkflowSupport.toTaskView(task, technicalTaskTimeoutPolicy.evaluate(task, timeoutSnapshot));
    }

    private TechnicalWorkflowModels.TechnicalTrackingEvent toTrackingEvent(TrackingEvent event) {
        return new TechnicalWorkflowModels.TechnicalTrackingEvent(
            event.nodeCode(),
            event.eventType(),
            event.eventStatus(),
            stringify(event.eventTime()),
            event.operatorName(),
            event.eventContent());
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
