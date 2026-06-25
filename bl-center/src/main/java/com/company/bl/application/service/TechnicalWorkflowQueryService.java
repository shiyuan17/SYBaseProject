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
    TechnicalWorkflowModels.EmbeddingWorkstationSummary getEmbeddingWorkstationSummary(
        LocalDate dateFrom,
        LocalDate dateTo,
        LocalDate workDate
    ) {
        TechnicalWorkflowModels.LocalDateRange effectiveDateRange =
            resolveEffectiveDateRange(dateFrom, dateTo, workDate);
        LocalDateTime dayStart =
            effectiveDateRange.dateFrom() == null ? null : effectiveDateRange.dateFrom().atStartOfDay();
        LocalDateTime nextDayStart =
            effectiveDateRange.dateTo() == null ? null : effectiveDateRange.dateTo().plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot = technicalTaskTimeoutPolicy.snapshot(now);

        List<TechnicalWorkflowModels.TaskView> pendingTasks = technicalWorkflowRepository
            .findTechnicalTasks(
                new TechnicalWorkflowRecords.PendingTechnicalTaskQuery(
                    1,
                    Integer.MAX_VALUE,
                    TechnicalWorkflowConstants.NODE_EMBEDDING,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    dayStart,
                    nextDayStart,
                    false,
                    false,
                    timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_GROSSING),
                    timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_DEHYDRATION),
                    timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_SLICING),
                    timeoutSnapshot.thresholdFor(TechnicalWorkflowConstants.NODE_STAINING)))
            .items()
            .stream()
            .map(task -> toTaskView(task, timeoutSnapshot))
            .toList();

        List<TechnicalWorkflowModels.TechnicalEmbeddingRecord> completedRecords = technicalWorkflowRepository
            .findEmbeddingWorkstationRecordsByEndedAtRange(dayStart, nextDayStart)
            .stream()
            .map(this::toTechnicalEmbeddingRecord)
            .toList();

        LocalDate today = LocalDate.now();
        TechnicalWorkflowModels.WorkstationDailyClearView dailyClear =
            toWorkstationDailyClearView(
                technicalWorkflowRepository.findWorkstationDailyClear(
                    TechnicalWorkflowConstants.NODE_EMBEDDING,
                    today));

        return new TechnicalWorkflowModels.EmbeddingWorkstationSummary(
            effectiveDateRange.dateFrom(),
            pendingTasks.size(),
            completedRecords.size(),
            pendingTasks,
            completedRecords,
            dailyClear);
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.SlicingWorkbenchView getSlicingWorkbench(TechnicalWorkflowModels.SlicingWorkbenchQuery query) {
        LocalDateTime now = LocalDateTime.now();
        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot = technicalTaskTimeoutPolicy.snapshot(now);
        TechnicalWorkflowModels.LocalDateRange effectiveDateRange =
            resolveEffectiveDateRange(query.dateFrom(), query.dateTo(), query.workDate());
        LocalDateTime dateFrom = effectiveDateRange.dateFrom() == null ? null : effectiveDateRange.dateFrom().atStartOfDay();
        LocalDateTime dateToExclusive =
            effectiveDateRange.dateTo() == null ? null : effectiveDateRange.dateTo().plusDays(1).atStartOfDay();
        LocalDate baseDate =
            effectiveDateRange.dateFrom() != null
                ? effectiveDateRange.dateFrom()
                : (query.workDate() != null ? query.workDate() : LocalDate.now());
        LocalDateTime todayStart = baseDate.atStartOfDay();
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
                dateFrom,
                dateToExclusive,
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
    TechnicalWorkflowModels.TechnicalTrackingCaseListPage listTechnicalTrackingCases(
        TechnicalWorkflowModels.TechnicalTrackingCaseListQuery query
    ) {
        TechnicalWorkflowModels.LocalDateRange effectiveDateRange =
            resolveEffectiveDateRange(query.dateFrom(), query.dateTo(), query.workDate());
        if (effectiveDateRange.dateFrom() == null && effectiveDateRange.dateTo() == null) {
            throw new BlBusinessException(
                BlErrorCode.INVALID_ARGUMENT,
                400,
                "Technical tracking case list requires at least one date filter");
        }
        TechnicalWorkflowRecords.PagedTechnicalTrackingCases paged =
            technicalWorkflowRepository.findTechnicalTrackingCases(
                new TechnicalWorkflowRecords.TechnicalTrackingCaseListQuery(
                    query.page(),
                    query.size(),
                    effectiveDateRange.dateFrom() == null ? null : effectiveDateRange.dateFrom().atStartOfDay(),
                    effectiveDateRange.dateTo() == null ? null : effectiveDateRange.dateTo().plusDays(1).atStartOfDay()));
        return new TechnicalWorkflowModels.TechnicalTrackingCaseListPage(
            paged.items().stream().map(item -> new TechnicalWorkflowModels.TechnicalTrackingCaseListItem(
                item.caseId(),
                item.pathologyNo(),
                item.patientName(),
                item.patientIdDisplay(),
                item.applicationNo(),
                item.applicationType(),
                item.submittingDepartmentName(),
                item.caseStatus(),
                stringify(item.latestActivityAt()),
                item.matchedActivityTypes())).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.TechnicalTrackingView getTechnicalTracking(
        String caseIdentifier,
        LocalDate dateFrom,
        LocalDate dateTo,
        LocalDate workDate
    ) {
        LocalDateTime now = LocalDateTime.now();
        TechnicalTaskTimeoutPolicy.TimeoutSnapshot timeoutSnapshot = technicalTaskTimeoutPolicy.snapshot(now);
        PathologyCase pathologyCase = resolveTrackingCase(caseIdentifier);
        String caseId = pathologyCase.id();
        TechnicalWorkflowModels.LocalDateRange effectiveDateRange =
            resolveEffectiveDateRange(dateFrom, dateTo, workDate);
        LocalDateTime workDateStart =
            effectiveDateRange.dateFrom() == null ? null : effectiveDateRange.dateFrom().atStartOfDay();
        LocalDateTime workDateEnd =
            effectiveDateRange.dateTo() == null ? null : effectiveDateRange.dateTo().plusDays(1).atStartOfDay();
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
        if (workDateStart != null || workDateEnd != null) {
            tasks = tasks.stream()
                .filter(task -> isWithinDateRange(task.createdAt(), workDateStart, workDateEnd)
                    || isWithinDateRange(task.startedAt(), workDateStart, workDateEnd)
                    || isWithinDateRange(task.completedAt(), workDateStart, workDateEnd))
                .toList();
            embeddingRecords = embeddingRecords.stream()
                .filter(item -> isWithinDateRange(item.endedAt(), workDateStart, workDateEnd)
                    || isWithinDateRange(item.startedAt(), workDateStart, workDateEnd)
                    || isWithinDateRange(item.sampledAt(), workDateStart, workDateEnd))
                .toList();
            slides = slides.stream()
                .filter(slide -> isWithinDateRange(slide.createdAt(), workDateStart, workDateEnd))
                .toList();
            qcEvaluations = qcEvaluations.stream()
                .filter(item -> isWithinDateRange(item.evaluatedAt(), workDateStart, workDateEnd))
                .toList();
            reworkOrders = reworkOrders.stream()
                .filter(item -> isWithinDateRange(item.requestedAt(), workDateStart, workDateEnd)
                    || isWithinDateRange(item.executedAt(), workDateStart, workDateEnd)
                    || isWithinDateRange(item.createdAt(), workDateStart, workDateEnd))
                .toList();
            events = events.stream()
                .filter(item -> isWithinDateRange(item.eventTime(), workDateStart, workDateEnd))
                .toList();
        }
        List<String> referencedSlideIds = java.util.stream.Stream.of(
                slides.stream().map(TechnicalWorkflowProcessingRecords.Slide::id),
                qcEvaluations.stream().map(TechnicalWorkflowProcessingRecords.SlideQcEvaluation::slideId),
                reworkOrders.stream().map(TechnicalWorkflowProcessingRecords.ReworkOrder::slideId))
            .flatMap(stream -> stream)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();
        List<String> referencedSpecimenIds = java.util.stream.Stream.of(
                tasks.stream().map(TechnicalWorkflowRecords.TechnicalTask::specimenId),
                embeddingRecords.stream().map(TechnicalWorkflowRecords.EmbeddingWorkstationRecord::specimenId),
                slides.stream().map(TechnicalWorkflowProcessingRecords.Slide::specimenId),
                qcEvaluations.stream().map(TechnicalWorkflowProcessingRecords.SlideQcEvaluation::specimenId),
                reworkOrders.stream().map(TechnicalWorkflowProcessingRecords.ReworkOrder::specimenId))
            .flatMap(stream -> stream)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();
        List<String> referencedBlockIds = java.util.stream.Stream.concat(
                embeddingRecords.stream().map(TechnicalWorkflowRecords.EmbeddingWorkstationRecord::samplingBlockId),
                reworkOrders.stream().map(TechnicalWorkflowProcessingRecords.ReworkOrder::samplingBlockId))
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();
        List<String> referencedEmbeddingBoxIds = java.util.stream.Stream.of(
                embeddingRecords.stream().map(TechnicalWorkflowRecords.EmbeddingWorkstationRecord::embeddingBoxId),
                slides.stream().map(TechnicalWorkflowProcessingRecords.Slide::embeddingBoxId),
                reworkOrders.stream().map(TechnicalWorkflowProcessingRecords.ReworkOrder::embeddingBoxId))
            .flatMap(stream -> stream)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();
        if (workDateStart != null || workDateEnd != null) {
            specimens = specimens.stream()
                .filter(item -> referencedSpecimenIds.contains(item.id()))
                .toList();
            blocks = blocks.stream()
                .filter(item -> referencedBlockIds.contains(item.id()))
                .toList();
            boxes = boxes.stream()
                .filter(item -> referencedEmbeddingBoxIds.contains(item.id()))
                .toList();
        }
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
                block.embeddingBoxName(),
                block.blockDescription(),
                block.specimenName(),
                block.grossDescription(),
                block.embeddingRemarks())).toList(),
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
            row.patientIdDisplay(),
            row.specimenId(),
            row.specimenName(),
            row.embeddingBoxId(),
            row.embeddingBoxNo(),
            row.slideId(),
            row.slideNo(),
            row.slicingOperatorName(),
            row.slicingRemark(),
            stringify(row.completedAt()),
            row.grossingEvaluation(),
            row.embeddingEvaluation(),
            row.embeddingOperatorName(),
            row.embeddingClearRemark(),
            row.embeddingRemarks(),
            row.shiftRemark(),
            row.sliceNotice(),
            row.submittingDepartmentName(),
            row.taskStatus(),
            row.slidePrintStatus(),
            row.printedSlideCount(),
            row.combinedSlide(),
            row.timedOut(),
            row.selectable(),
            row.printGroupId(),
            row.mergedPrintGroup(),
            row.taskIds(),
            row.embeddingBoxIds());
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

    private TechnicalWorkflowModels.WorkstationDailyClearView toWorkstationDailyClearView(
        java.util.Optional<TechnicalWorkflowRecords.WorkstationDailyClearRecord> record
    ) {
        LocalDate today = LocalDate.now();
        return record
            .map(item -> new TechnicalWorkflowModels.WorkstationDailyClearView(
                item.workDate(),
                true,
                item.operatorUserId(),
                item.operatorName(),
                item.clearedAt(),
                item.clearStatus()))
            .orElseGet(() -> new TechnicalWorkflowModels.WorkstationDailyClearView(
                today,
                false,
                null,
                null,
                null,
                null));
    }

    private TechnicalWorkflowModels.LocalDateRange resolveEffectiveDateRange(
        LocalDate dateFrom,
        LocalDate dateTo,
        LocalDate workDate
    ) {
        if (dateFrom != null || dateTo != null) {
            return new TechnicalWorkflowModels.LocalDateRange(dateFrom, dateTo);
        }
        if (workDate != null) {
            return new TechnicalWorkflowModels.LocalDateRange(workDate, workDate);
        }
        return new TechnicalWorkflowModels.LocalDateRange(null, null);
    }

    private boolean isWithinDateRange(
        LocalDateTime value,
        LocalDateTime workDateStart,
        LocalDateTime workDateEnd
    ) {
        if (value == null) {
            return false;
        }
        if (workDateStart != null && value.isBefore(workDateStart)) {
            return false;
        }
        if (workDateEnd != null && !value.isBefore(workDateEnd)) {
            return false;
        }
        return true;
    }
}
