package com.company.bl.application.service;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
                query.priority(),
                query.assignedToUserId(),
                query.currentNode(),
                query.taskId(),
                query.applicationNo(),
                query.pathologyNo(),
                query.keyword(),
                query.objectType(),
                query.createdFrom(),
                query.createdTo(),
                query.timedOutOnly(),
                query.includeAllStatuses(),
                timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_GROSSING),
                timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_DEHYDRATION),
                timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_SLICING),
                timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_STAINING)));
        return new TechnicalWorkflowModels.PendingTechnicalTaskPage(
            paged.items().stream().map(task -> toTaskView(task, timeoutSnapshot)).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.EmbeddingWorkstationSummary getEmbeddingWorkstationSummary(LocalDate workDate) {
        LocalDate resolvedDate = workDate == null ? LocalDate.now() : workDate;
        LocalDateTime dayStart = resolvedDate.atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);
        LocalDateTime now = LocalDateTime.now();
        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot = technicalTaskTimeoutPolicy.snapshot(now);

        List<TechnicalWorkflowModels.TaskView> pendingTasks =
            technicalWorkflowRepository.findActiveTechnicalTasksByTypeAndCreatedRange(
                    TechnicalWorkflowConstants.NODE_EMBEDDING,
                    dayStart,
                    nextDayStart)
                .stream()
                .map(task -> toTaskView(task, timeoutSnapshot))
                .toList();

        List<TechnicalWorkflowModels.TechnicalEmbeddingRecord> completedRecords =
            technicalWorkflowRepository.findEmbeddingWorkstationRecordsByEndedAtRange(dayStart, nextDayStart).stream()
                .map(this::toTechnicalEmbeddingRecord)
                .toList();

        return new TechnicalWorkflowModels.EmbeddingWorkstationSummary(
            resolvedDate,
            pendingTasks.size(),
            completedRecords.size(),
            pendingTasks,
            completedRecords);
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.SlicingWorkbenchView getSlicingWorkbench(TechnicalWorkflowModels.SlicingWorkbenchQuery query) {
        LocalDateTime now = LocalDateTime.now();
        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot = technicalTaskTimeoutPolicy.snapshot(now);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        LocalDateTime dayAfterTomorrowStart = tomorrowStart.plusDays(1);
        TechnicalWorkflowRecords.SlicingWorkbenchQuery repositoryQuery =
            new TechnicalWorkflowRecords.SlicingWorkbenchQuery(
                query.keyword(),
                query.applicationType(),
                query.pendingTodayOnly(),
                query.overdueOnly(),
                query.pendingPage(),
                query.pendingSize(),
                query.completedPage(),
                query.completedSize(),
                query.currentUserId(),
                todayStart,
                tomorrowStart,
                dayAfterTomorrowStart,
                timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_SLICING));
        TechnicalWorkflowRecords.SlicingWorkbenchStats stats =
            technicalWorkflowRepository.summarizeSlicingWorkbench(repositoryQuery);
        TechnicalWorkflowRecords.PagedSlicingWorkbenchRows pendingRows =
            technicalWorkflowRepository.findPendingSlicingPrintRows(repositoryQuery);
        TechnicalWorkflowRecords.PagedSlicingWorkbenchRows pendingSliceRows =
            technicalWorkflowRepository.findPendingSlicingProcessRows(repositoryQuery);
        TechnicalWorkflowRecords.PagedSlicingWorkbenchRows completedRows =
            technicalWorkflowRepository.findCompletedSlicingWorkbenchRows(repositoryQuery);
        List<TechnicalWorkflowModels.SlicingWorkbenchRow> pendingPrintItems =
            pendingRows.items().stream().map(this::toSlicingWorkbenchRow).toList();
        List<TechnicalWorkflowModels.SlicingWorkbenchRow> pendingSliceItems =
            pendingSliceRows.items().stream().map(this::toSlicingWorkbenchRow).toList();
        return new TechnicalWorkflowModels.SlicingWorkbenchView(
            new TechnicalWorkflowModels.SlicingWorkbenchStats(
                stats.pendingTodayCount(),
                stats.pendingTomorrowCount(),
                stats.completedMineTodayCount(),
                stats.completedDeptTodayCount(),
                stats.overdueCount(),
                stats.pendingPrintCount()),
            pendingPrintItems,
            pendingPrintItems,
            pendingSliceItems,
            query.pendingPage(),
            query.pendingSize(),
            pendingRows.total(),
            pendingRows.total(),
            pendingSliceRows.total(),
            completedRows.items().stream().map(this::toSlicingWorkbenchRow).toList(),
            query.completedPage(),
            query.completedSize(),
            completedRows.total());
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalTrackingView getTechnicalTracking(String caseIdentifier) {
        LocalDateTime now = LocalDateTime.now();
        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot = technicalTaskTimeoutPolicy.snapshot(now);
        PathologyCase pathologyCase = resolveTrackingCase(caseIdentifier);
        String caseId = pathologyCase.id();
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        List<TechnicalWorkflowRecords.TechnicalTask> tasks = technicalWorkflowRepository.findActiveTechnicalTasksByCaseId(caseId);
        List<TechnicalWorkflowRecords.SamplingBlock> blocks = technicalWorkflowRepository.findSamplingBlocksByCaseId(caseId);
        List<TechnicalWorkflowRecords.EmbeddingBox> boxes = technicalWorkflowRepository.findEmbeddingBoxesByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.Slide> slides = technicalWorkflowRepository.findSlidesByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.SlideQcEvaluation> qcEvaluations =
            technicalWorkflowRepository.findSlideQcEvaluationsByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.ReworkOrder> reworkOrders = technicalWorkflowRepository.findReworkOrdersByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(caseId);
        List<TechnicalWorkflowRecords.EmbeddingWorkstationRecord> embeddingRecords =
            technicalWorkflowRepository.findEmbeddingWorkstationRecordsByCaseId(caseId);
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
                block.id(),
                block.specimenId(),
                block.blockCode(),
                block.embeddingBoxNo(),
                block.blockDescription(),
                block.specimenName(),
                block.grossDescription())).toList(),
            boxes.stream().map(box -> new TechnicalWorkflowModels.TechnicalEmbeddingBoxSummary(
                box.id(), box.specimenId(), box.embeddingBoxNo(), box.sliceNotice(), slidesByBox.getOrDefault(box.id(), List.of()).size())).toList(),
            embeddingRecords.stream().map(this::toTechnicalEmbeddingRecord).toList(),
            embeddingRecords.stream().map(this::toTechnicalEmbeddingEvaluationRecord).toList(),
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

    private PathologyCase resolveTrackingCase(String caseIdentifier) {
        if (caseIdentifier == null || caseIdentifier.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Pathology case identifier is required");
        }
        String normalizedIdentifier = caseIdentifier.trim();
        return technicalWorkflowRepository.findPathologyCaseById(normalizedIdentifier)
            .or(() -> technicalWorkflowRepository.findPathologyCaseByPathologyNo(normalizedIdentifier))
            .or(() -> technicalWorkflowRepository.findSpecimenById(normalizedIdentifier)
                .map(Specimen::caseId)
                .flatMap(technicalWorkflowRepository::findPathologyCaseById))
            .or(() -> technicalWorkflowRepository.findSamplingBlockById(normalizedIdentifier)
                .map(TechnicalWorkflowRecords.SamplingBlock::caseId)
                .flatMap(technicalWorkflowRepository::findPathologyCaseById))
            .or(() -> technicalWorkflowRepository.findEmbeddingBoxById(normalizedIdentifier)
                .map(TechnicalWorkflowRecords.EmbeddingBox::caseId)
                .flatMap(technicalWorkflowRepository::findPathologyCaseById))
            .or(() -> technicalWorkflowRepository.findSlideById(normalizedIdentifier)
                .map(TechnicalWorkflowProcessingRecords.Slide::caseId)
                .flatMap(technicalWorkflowRepository::findPathologyCaseById))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology case not found"));
    }

    private TechnicalWorkflowModels.TaskView toTaskView(TechnicalWorkflowRecords.TechnicalTask task,
                                                        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot) {
        return technicalWorkflowSupport.toTaskView(task, technicalTaskTimeoutPolicy.evaluate(task, timeoutSnapshot));
    }

    private TechnicalWorkflowModels.SlicingWorkbenchRow toSlicingWorkbenchRow(
        TechnicalWorkflowRecords.SlicingWorkbenchRow row
    ) {
        return new TechnicalWorkflowModels.SlicingWorkbenchRow(
            row.taskId(),
            row.caseId(),
            row.applicationType(),
            row.pathologyNo(),
            row.patientName(),
            row.patientId(),
            row.specimenId(),
            row.specimenName(),
            row.embeddingBoxId(),
            row.slideId(),
            row.slideNo(),
            row.slicingOperatorName(),
            row.slicingRemark(),
            stringify(row.completedAt()),
            row.grossingEvaluation(),
            row.embeddingEvaluation(),
            row.embeddingOperatorName(),
            row.embeddingClearRemark(),
            row.shiftRemark(),
            row.sliceNotice(),
            row.taskStatus(),
            row.slidePrintStatus(),
            row.printedSlideCount(),
            row.combinedSlide(),
            row.timedOut(),
            row.selectable());
    }

    private TechnicalWorkflowModels.TechnicalEmbeddingRecord toTechnicalEmbeddingRecord(
        TechnicalWorkflowRecords.EmbeddingWorkstationRecord record
    ) {
        return new TechnicalWorkflowModels.TechnicalEmbeddingRecord(
            record.taskId(),
            record.caseId(),
            record.pathologyNo(),
            record.specimenId(),
            record.specimenName(),
            record.samplingBlockId(),
            record.samplingBlockCode(),
            record.samplingBlockDescription(),
            record.grossDescription(),
            record.embeddingId(),
            record.embeddingBoxId(),
            record.embeddingBoxNo(),
            record.sliceNotice(),
            record.evaluationLevel(),
            record.samplingEvaluation(),
            record.embeddingRemarks(),
            record.sampledByName(),
            stringify(record.sampledAt()),
            record.embeddedByName(),
            stringify(record.startedAt()),
            stringify(record.endedAt()),
            record.taskStatus());
    }

    private TechnicalWorkflowModels.TechnicalEmbeddingEvaluationRecord toTechnicalEmbeddingEvaluationRecord(
        TechnicalWorkflowRecords.EmbeddingWorkstationRecord record
    ) {
        return new TechnicalWorkflowModels.TechnicalEmbeddingEvaluationRecord(
            record.embeddingId(),
            record.caseId(),
            record.pathologyNo(),
            record.specimenId(),
            record.specimenName(),
            record.samplingBlockId(),
            record.samplingBlockCode(),
            record.embeddingBoxNo(),
            record.evaluationLevel(),
            record.samplingEvaluation(),
            record.embeddingRemarks(),
            record.embeddedByName(),
            stringify(record.endedAt()));
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
