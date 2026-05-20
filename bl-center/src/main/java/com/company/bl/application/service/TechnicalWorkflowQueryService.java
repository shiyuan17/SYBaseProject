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

    TechnicalWorkflowQueryService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                  TechnicalWorkflowSupport technicalWorkflowSupport) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.PendingTechnicalTaskPage listPendingTasks(TechnicalWorkflowModels.PendingTechnicalTaskQuery query) {
        TechnicalWorkflowRecords.PagedTechnicalTasks paged = technicalWorkflowRepository.findTechnicalTasks(
            new TechnicalWorkflowRecords.PendingTechnicalTaskQuery(
                query.page(),
                query.size(),
                query.taskType(),
                query.taskStatus(),
                query.applicationNo(),
                query.pathologyNo(),
                query.objectType()));
        return new TechnicalWorkflowModels.PendingTechnicalTaskPage(
            paged.items().stream().map(technicalWorkflowSupport::toTaskView).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalTrackingView getTechnicalTracking(String caseId) {
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(caseId);
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        List<TechnicalWorkflowRecords.TechnicalTask> tasks = technicalWorkflowRepository.findActiveTechnicalTasksByCaseId(caseId);
        List<TechnicalWorkflowRecords.SamplingBlock> blocks = technicalWorkflowRepository.findSamplingBlocksByCaseId(caseId);
        List<TechnicalWorkflowRecords.EmbeddingBox> boxes = technicalWorkflowRepository.findEmbeddingBoxesByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.Slide> slides = technicalWorkflowRepository.findSlidesByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.ReworkOrder> reworkOrders = technicalWorkflowRepository.findReworkOrdersByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(caseId);
        Map<String, List<TechnicalWorkflowProcessingRecords.Slide>> slidesByBox = slides.stream()
            .collect(Collectors.groupingBy(TechnicalWorkflowProcessingRecords.Slide::embeddingBoxId));
        return new TechnicalWorkflowModels.TechnicalTrackingView(
            pathologyCase.id(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            tasks.stream().map(technicalWorkflowSupport::toTaskView).toList(),
            specimens.stream().map(specimen -> new TechnicalWorkflowModels.TechnicalSpecimenSummary(
                specimen.id(), specimen.specimenNo(), specimen.barcode(), specimen.specimenNameStandardized(), specimen.specimenStatus().name())).toList(),
            blocks.stream().map(block -> new TechnicalWorkflowModels.TechnicalBlockSummary(
                block.id(), block.specimenId(), block.blockCode(), block.embeddingBoxNo(), block.blockDescription())).toList(),
            boxes.stream().map(box -> new TechnicalWorkflowModels.TechnicalEmbeddingBoxSummary(
                box.id(), box.specimenId(), box.embeddingBoxNo(), box.sliceNotice(), slidesByBox.getOrDefault(box.id(), List.of()).size())).toList(),
            slides.stream().map(slide -> new TechnicalWorkflowModels.TechnicalSlideSummary(
                slide.id(), slide.specimenId(), slide.embeddingBoxId(), slide.slideNo(), slide.slideStatus(), slide.qualityStatus())).toList(),
            reworkOrders.stream().map(order -> new TechnicalWorkflowModels.ReworkSummary(
                order.id(), order.reworkType(), order.status(), order.reason())).toList(),
            events.stream()
                .sorted(Comparator.comparing(TrackingEvent::eventTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTrackingEvent)
                .toList());
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
