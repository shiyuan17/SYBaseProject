package com.company.bl.application.service;

import com.company.bl.application.gateway.TechnicalMarkingGateway;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.support.application.WorkflowRequestContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
class TechnicalProcessingWorkflowService {

    private static final int MAX_EMBEDDING_BOX_NO_LENGTH = 64;
    private static final int MAX_EMBEDDING_BOX_NO_RETRY = 100;
    private static final Pattern EMBEDDING_BOX_NO_PATTERN = Pattern.compile("^([A-Za-z]+)(\\d+).*$");

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final DiagnosticReportAppService diagnosticReportAppService;
    private final TechnicalReworkWorkflowService technicalReworkWorkflowService;

    TechnicalProcessingWorkflowService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                       TechnicalWorkflowSupport technicalWorkflowSupport,
                                       DiagnosticReportAppService diagnosticReportAppService,
                                       TechnicalReworkWorkflowService technicalReworkWorkflowService) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.diagnosticReportAppService = diagnosticReportAppService;
        this.technicalReworkWorkflowService = technicalReworkWorkflowService;
    }

    @Transactional
    TechnicalWorkflowModels.TaskStartResult startEmbedding(TechnicalWorkflowModels.TaskStartCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.startPendingTaskWithStatus(
            command,
            TechnicalWorkflowConstants.NODE_EMBEDDING,
            TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK,
            TechnicalWorkflowConstants.TASK_EMBEDDING_CONFIRM_PENDING);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "EMBEDDING");
        technicalWorkflowSupport.insertWorkflowEvent(task, TechnicalWorkflowConstants.NODE_EMBEDDING, "START", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Embedding started");
        return new TechnicalWorkflowModels.TaskStartResult(
            task.id(),
            task.caseId(),
            "EMBEDDING",
            TechnicalWorkflowConstants.TASK_EMBEDDING_CONFIRM_PENDING);
    }

    @Transactional
    TechnicalWorkflowModels.EmbeddingResult completeEmbedding(TechnicalWorkflowModels.EmbeddingCompleteCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTask(
            command.taskId(), TechnicalWorkflowConstants.NODE_EMBEDDING, TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK);
        if (!TechnicalWorkflowConstants.TASK_EMBEDDING_CONFIRM_PENDING.equals(task.taskStatus())
            && !TechnicalWorkflowConstants.TASK_IN_PROGRESS.equals(task.taskStatus())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Embedding must be confirmed before completion");
        }
        TechnicalWorkflowRecords.SamplingBlock block = technicalWorkflowSupport.getSamplingBlock(command.samplingBlockId());
        technicalWorkflowSupport.validateTaskObject(task, block.id());
        LocalDateTime now = LocalDateTime.now();
        String embeddingBoxNo = resolveEmbeddingBoxNo(command, task, block);
        String embeddingId = technicalWorkflowSupport.nextId("EMB");
        technicalWorkflowRepository.insertEmbedding(new TechnicalWorkflowRecords.CreateEmbeddingCommand(
            embeddingId,
            task.caseId(),
            block.specimenId(),
            block.samplingId(),
            block.id(),
            TechnicalWorkflowConstants.TASK_COMPLETED,
            command.evaluationLevel(),
            command.samplingEvaluation(),
            task.startedAt() == null ? now : task.startedAt(),
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.remarks()));
        String embeddingBoxId = technicalWorkflowSupport.nextId("BOX");
        technicalWorkflowRepository.insertEmbeddingBox(new TechnicalWorkflowRecords.CreateEmbeddingBoxCommand(
            embeddingBoxId,
            task.caseId(),
            block.specimenId(),
            block.id(),
            embeddingId,
            embeddingBoxNo,
            command.blockCount(),
            false,
            command.sliceNotice(),
            "ACTIVE"));
        TechnicalMarkingGateway.MarkingResult markingResult = technicalWorkflowSupport.markObject(
            task.caseId(),
            TechnicalWorkflowConstants.OBJECT_EMBEDDING_BOX,
            embeddingBoxId,
            command.deviceCode(),
            embeddingBoxNo,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            TechnicalWorkflowConstants.NODE_EMBEDDING);
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TechnicalWorkflowConstants.TASK_COMPLETED, command.remarks(), now);
        technicalWorkflowSupport.createTechnicalTaskIfAbsent(
            task.applicationId(),
            task.caseId(),
            block.specimenId(),
            TechnicalWorkflowConstants.NODE_SLICING,
            TechnicalWorkflowConstants.OBJECT_EMBEDDING_BOX,
            embeddingBoxId,
            task.id(),
            "embeddingBoxNo=" + embeddingBoxNo);
        return new TechnicalWorkflowModels.EmbeddingResult(
            task.id(), embeddingId, embeddingBoxId, "EMBEDDING", markingResult.success(), markingResult.message());
    }

    @Transactional
    TechnicalWorkflowModels.TaskStartResult cancelEmbedding(TechnicalWorkflowModels.TaskStartCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTask(
            command.taskId(), TechnicalWorkflowConstants.NODE_EMBEDDING, TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK);
        if (!TechnicalWorkflowConstants.TASK_EMBEDDING_CONFIRM_PENDING.equals(task.taskStatus())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Only embedding confirmation pending tasks can be cancelled");
        }
        technicalWorkflowRepository.resetTechnicalTaskToPending(task.id(), command.remarks(), LocalDateTime.now());
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "EMBEDDING");
        technicalWorkflowSupport.insertWorkflowEvent(task, TechnicalWorkflowConstants.NODE_EMBEDDING, "CANCEL", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Embedding confirmation cancelled");
        return new TechnicalWorkflowModels.TaskStartResult(
            task.id(),
            task.caseId(),
            "EMBEDDING",
            TechnicalWorkflowConstants.TASK_PENDING);
    }

    @Transactional
    TechnicalWorkflowModels.WorkstationDailyClearView confirmEmbeddingWorkstationClear(
        TechnicalWorkflowModels.WorkstationClearCommand command
    ) {
        LocalDate today = LocalDate.now();
        if (technicalWorkflowRepository.findWorkstationDailyClear(
            TechnicalWorkflowConstants.NODE_EMBEDDING, today).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "今日已完成清零");
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            TechnicalWorkflowRecords.WorkstationDailyClearRecord record =
                technicalWorkflowRepository.insertWorkstationDailyClear(
                    new TechnicalWorkflowRecords.CreateWorkstationDailyClearCommand(
                        technicalWorkflowSupport.nextId("WDC"),
                        TechnicalWorkflowConstants.NODE_EMBEDDING,
                        today,
                        command.operatorUserId(),
                        command.operatorName(),
                        now,
                        TechnicalWorkflowConstants.WORKSTATION_CLEAR_STATUS_CLEARED,
                        WorkflowRequestContext.resolveClientIp()));
            return toWorkstationDailyClearView(record);
        } catch (DuplicateKeyException exception) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "今日已完成清零");
        }
    }

    private TechnicalWorkflowModels.WorkstationDailyClearView toWorkstationDailyClearView(
        TechnicalWorkflowRecords.WorkstationDailyClearRecord record
    ) {
        return new TechnicalWorkflowModels.WorkstationDailyClearView(
            record.workDate(),
            true,
            record.operatorUserId(),
            record.operatorName(),
            record.clearedAt(),
            record.clearStatus());
    }

    private String resolveEmbeddingBoxNo(TechnicalWorkflowModels.EmbeddingCompleteCommand command,
                                         TechnicalWorkflowRecords.TechnicalTask task,
                                         TechnicalWorkflowRecords.SamplingBlock block) {
        String requestedEmbeddingBoxNo = trimToNull(command.embeddingBoxNo());
        if (requestedEmbeddingBoxNo != null) {
            ensureEmbeddingBoxNoAvailable(task.caseId(), requestedEmbeddingBoxNo);
            return requestedEmbeddingBoxNo;
        }

        String grossingEmbeddingBoxNo = trimToNull(block.embeddingBoxNo());
        if (grossingEmbeddingBoxNo != null
            && technicalWorkflowRepository.findEmbeddingBoxByCaseIdAndNo(task.caseId(), grossingEmbeddingBoxNo).isEmpty()) {
            return grossingEmbeddingBoxNo;
        }

        return generateAvailableEmbeddingBoxNo(task, block);
    }

    private void ensureEmbeddingBoxNoAvailable(String caseId, String embeddingBoxNo) {
        if (technicalWorkflowRepository.findEmbeddingBoxByCaseIdAndNo(caseId, embeddingBoxNo).isPresent()) {
            throw new BlBusinessException(
                BlErrorCode.RESOURCE_CONFLICT,
                409,
                "Embedding box number already exists");
        }
    }

    private String generateAvailableEmbeddingBoxNo(TechnicalWorkflowRecords.TechnicalTask task,
                                                   TechnicalWorkflowRecords.SamplingBlock block) {
        String scopeToken = normalizeEmbeddingBoxNoToken(firstNonBlank(task.pathologyNo(), task.caseId()), "CASE");
        String blockToken = normalizeEmbeddingBoxNoToken(firstNonBlank(task.samplingBlockCode(), block.blockCode(), block.id()), "BLOCK");
        String baseEmbeddingBoxNo = truncateEmbeddingBoxNo("BX-" + scopeToken + "-" + blockToken, "");

        for (int retryIndex = 0; retryIndex < MAX_EMBEDDING_BOX_NO_RETRY; retryIndex++) {
            String suffix = retryIndex == 0 ? "" : "-" + retryIndex;
            String candidate = truncateEmbeddingBoxNo(baseEmbeddingBoxNo, suffix);
            if (technicalWorkflowRepository.findEmbeddingBoxByCaseIdAndNo(task.caseId(), candidate).isEmpty()) {
                return candidate;
            }
        }

        throw new BlBusinessException(
            BlErrorCode.NUMBERING_GENERATION_FAILED,
            500,
            "Failed to generate available embedding box number");
    }

    private String truncateEmbeddingBoxNo(String baseEmbeddingBoxNo, String suffix) {
        int maxBaseLength = Math.max(1, MAX_EMBEDDING_BOX_NO_LENGTH - suffix.length());
        String truncatedBase = baseEmbeddingBoxNo.length() <= maxBaseLength
            ? baseEmbeddingBoxNo
            : baseEmbeddingBoxNo.substring(0, maxBaseLength).replaceAll("-+$", "");
        return (truncatedBase.isBlank() ? "BX-CASE-BLOCK" : truncatedBase) + suffix;
    }

    private String normalizeEmbeddingBoxNoToken(String value, String fallback) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return fallback;
        }
        normalizedValue = normalizedValue
            .toUpperCase()
            .replaceAll("[^A-Z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        return normalizedValue.isBlank() ? fallback : normalizedValue;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalizedValue = trimToNull(value);
            if (normalizedValue != null) {
                return normalizedValue;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    TechnicalWorkflowModels.EmbeddingQualityReviewResult updateEmbeddingQualityReview(
        TechnicalWorkflowModels.EmbeddingQualityReviewCommand command
    ) {
        TechnicalWorkflowRecords.EmbeddingWorkstationRecord currentRecord =
            technicalWorkflowRepository.findEmbeddingWorkstationRecordByEmbeddingId(command.embeddingId())
                .orElseThrow(() -> new BlBusinessException(
                    BlErrorCode.RESOURCE_NOT_FOUND,
                    404,
                    "Embedding record not found"));
        String samplingEvaluation = buildSamplingEvaluation(command);
        technicalWorkflowRepository.updateEmbeddingQualityReview(
            currentRecord.embeddingId(),
            normalizeText(command.evaluationLevel()),
            samplingEvaluation);
        technicalWorkflowRepository.updateEmbeddingBoxSliceNoticeByEmbeddingId(
            currentRecord.embeddingId(),
            normalizeText(command.sliceNotice()));

        TechnicalWorkflowModels.ReworkOrderResult reworkResult = null;
        if ("REGROSSING".equals(command.treatmentAction())) {
            reworkResult = technicalReworkWorkflowService.createAndExecuteReworkOrder(
                new TechnicalWorkflowModels.CreateReworkOrderCommand(
                    currentRecord.caseId(),
                    currentRecord.specimenId(),
                    currentRecord.samplingBlockId(),
                    null,
                    null,
                    "REGROSSING",
                    samplingEvaluation == null || samplingEvaluation.isBlank()
                        ? "取材评价不合格，需重新取材"
                        : samplingEvaluation,
                    null,
                    command.operatorUserId(),
                    command.operatorName(),
                    command.terminalCode(),
                    command.remarks()));
        }

        TechnicalWorkflowRecords.EmbeddingWorkstationRecord updatedRecord =
            technicalWorkflowRepository.findEmbeddingWorkstationRecordByEmbeddingId(currentRecord.embeddingId())
                .orElseThrow(() -> new BlBusinessException(
                    BlErrorCode.RESOURCE_NOT_FOUND,
                    404,
                    "Embedding record not found"));
        return new TechnicalWorkflowModels.EmbeddingQualityReviewResult(
            toTechnicalEmbeddingRecord(updatedRecord),
            reworkResult == null ? null : reworkResult.reworkType(),
            reworkResult == null ? null : reworkResult.status());
    }

    private String buildSamplingEvaluation(TechnicalWorkflowModels.EmbeddingQualityReviewCommand command) {
        List<String> parts = new ArrayList<>();
        String samplingEvaluation = normalizeText(command.samplingEvaluation());
        if (samplingEvaluation != null) {
            parts.add(samplingEvaluation);
        }
        List<String> unqualifiedReasons = command.unqualifiedReasons() == null
            ? List.of()
            : command.unqualifiedReasons().stream()
                .map(this::normalizeText)
                .filter(item -> item != null)
                .toList();
        if (!unqualifiedReasons.isEmpty()) {
            parts.add("不合格原因：" + String.join("、", unqualifiedReasons));
        }
        if ("REGROSSING".equals(command.treatmentAction())) {
            parts.add("处理措施：重新取材");
        } else if ("OTHER".equals(command.treatmentAction())) {
            parts.add("处理措施：其他");
        }
        String treatmentRemark = normalizeText(command.treatmentRemark());
        if (treatmentRemark != null) {
            parts.add("处理说明：" + treatmentRemark);
        }
        if (command.notifiedGrossingOperator()) {
            parts.add("已通知取材人");
        }
        return parts.isEmpty() ? null : String.join("；", parts);
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

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    @Transactional
    TechnicalWorkflowModels.TaskStartResult startSlicing(TechnicalWorkflowModels.TaskStartCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.startTask(
            command, TechnicalWorkflowConstants.NODE_SLICING, TechnicalWorkflowConstants.OBJECT_EMBEDDING_BOX);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "SLICING");
        technicalWorkflowSupport.insertWorkflowEvent(task, TechnicalWorkflowConstants.NODE_SLICING, "START", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Slicing started");
        return new TechnicalWorkflowModels.TaskStartResult(task.id(), task.caseId(), "SLICING", TechnicalWorkflowConstants.TASK_IN_PROGRESS);
    }

    @Transactional
    TechnicalWorkflowModels.SlicingResult completeSlicing(TechnicalWorkflowModels.SlicingCompleteCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTask(
            command.taskId(), TechnicalWorkflowConstants.NODE_SLICING, TechnicalWorkflowConstants.OBJECT_EMBEDDING_BOX);
        TechnicalWorkflowRecords.EmbeddingBox box = technicalWorkflowSupport.getEmbeddingBox(command.embeddingBoxId());
        technicalWorkflowSupport.validateTaskObject(task, box.id());
        TechnicalWorkflowProcessingRecords.Slicing slicing = technicalWorkflowRepository
            .findSlicingByTaskIdAndEmbeddingBoxId(task.id(), box.id())
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.RESOURCE_CONFLICT,
                409,
                "Slide printing must be completed before slicing"));
        List<TechnicalWorkflowProcessingRecords.Slide> slides =
            technicalWorkflowRepository.findSlidesBySlicingId(slicing.id());
        if (slides.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Printed slides are required before slicing");
        }
        LocalDateTime now = LocalDateTime.now();
        technicalWorkflowRepository.completeSlicingRecord(
            slicing.id(),
            TechnicalWorkflowConstants.TASK_COMPLETED,
            command.sliceCountPerSlide(),
            command.sliceThickness(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.qualityIssue(),
            command.remarks());
        List<String> slideIds = new ArrayList<>();
        for (TechnicalWorkflowProcessingRecords.Slide slide : slides) {
            technicalWorkflowSupport.createTechnicalTaskIfAbsent(
                task.applicationId(),
                task.caseId(),
                box.specimenId(),
                TechnicalWorkflowConstants.NODE_STAINING,
                TechnicalWorkflowConstants.OBJECT_SLIDE,
                slide.id(),
                task.id(),
                "slideNo=" + slide.slideNo());
            slideIds.add(slide.id());
        }
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TechnicalWorkflowConstants.TASK_COMPLETED, command.remarks(), now);
        technicalWorkflowSupport.insertWorkflowEvent(task.applicationId(), box.specimenId(), task.caseId(),
            TechnicalWorkflowConstants.NODE_SLICING, "COMPLETE", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), "Slicing completed");
        return new TechnicalWorkflowModels.SlicingResult(task.id(), slicing.id(), slideIds, "SLICING");
    }

    @Transactional
    TechnicalWorkflowModels.SlicingSlidePrintResult printSlicingSlides(
        TechnicalWorkflowModels.SlicingSlidePrintCommand command
    ) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTask(
            command.taskId(), TechnicalWorkflowConstants.NODE_SLICING, TechnicalWorkflowConstants.OBJECT_EMBEDDING_BOX);
        TechnicalWorkflowRecords.EmbeddingBox box = technicalWorkflowSupport.getEmbeddingBox(command.embeddingBoxId());
        technicalWorkflowSupport.validateTaskObject(task, box.id());
        if (technicalWorkflowRepository.findSlicingByTaskIdAndEmbeddingBoxId(task.id(), box.id()).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Slide labels have already been printed");
        }

        List<String> slideNos = buildSlideNos(command.sourceSlideCount(), command.mergeAdjacent());
        LocalDateTime now = LocalDateTime.now();
        String slicingId = technicalWorkflowSupport.nextId("SLC");
        String slicingBatchNo = "SLC-" + UUID.randomUUID().toString().substring(0, 8);
        technicalWorkflowRepository.insertSlicing(new TechnicalWorkflowProcessingRecords.CreateSlicingCommand(
            slicingId,
            task.id(),
            task.caseId(),
            box.specimenId(),
            box.embeddingId(),
            box.id(),
            slicingBatchNo,
            "PRINTED",
            slideNos.size(),
            null,
            null,
            null,
            null,
            null,
            null,
            command.remarks()));

        List<String> slideIds = new ArrayList<>();
        for (String slideNo : slideNos) {
            String slideId = technicalWorkflowSupport.nextId("SLD");
            boolean combined = slideNo.contains("-");
            technicalWorkflowRepository.insertSlide(new TechnicalWorkflowProcessingRecords.CreateSlideCommand(
                slideId,
                task.caseId(),
                box.specimenId(),
                slicingId,
                box.id(),
                box.samplingBlockId(),
                slideNo,
                slideNo,
                combined,
                "PENDING",
                "PRINTED",
                null));
            TechnicalMarkingGateway.MarkingResult result = technicalWorkflowSupport.markObject(
                task.caseId(),
                TechnicalWorkflowConstants.OBJECT_SLIDE,
                slideId,
                command.printerCode(),
                slideNo,
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                TechnicalWorkflowConstants.NODE_SLICING);
            if (!result.success()) {
                technicalWorkflowSupport.insertWorkflowEvent(
                    task.applicationId(),
                    box.specimenId(),
                    task.caseId(),
                    TechnicalWorkflowConstants.NODE_SLICING,
                    "SLIDE_PRINT",
                    "FAILED",
                    command.operatorUserId(),
                    command.operatorName(),
                    command.terminalCode(),
                    result.message());
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, result.message());
            }
            slideIds.add(slideId);
        }
        technicalWorkflowSupport.insertWorkflowEvent(task.applicationId(), box.specimenId(), task.caseId(),
            TechnicalWorkflowConstants.NODE_SLICING, "SLIDE_PRINT", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), "Slide labels printed");
        return new TechnicalWorkflowModels.SlicingSlidePrintResult(
            task.id(),
            slicingId,
            slideIds,
            slideNos,
            command.mergeAdjacent(),
            slideNos.size());
    }

    @Transactional
    TechnicalWorkflowModels.SlicingSlidePrintMergeGroupResult createSlicingSlidePrintMergeGroups(
        TechnicalWorkflowModels.SlicingSlidePrintMergeGroupCommand command
    ) {
        List<String> taskIds = normalizeIds(command.taskIds());
        if (taskIds.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "At least one slicing task is required");
        }
        List<TechnicalWorkflowRecords.SlicingWorkbenchRow> rows =
            technicalWorkflowRepository.findPendingSlicingPrintRowsByTaskIds(taskIds);
        if (rows.size() != taskIds.size()) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Only unprinted slicing tasks can be merged");
        }

        Map<String, List<TechnicalWorkflowRecords.SlicingWorkbenchRow>> buckets = new LinkedHashMap<>();
        for (TechnicalWorkflowRecords.SlicingWorkbenchRow row : rows) {
            String prefix = embeddingBoxPrefix(row.embeddingBoxNo());
            if (prefix == null) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Embedding box number is required for merging");
            }
            if (row.patientId() == null || row.patientId().isBlank()) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Patient ID is required for merging");
            }
            String bucketKey = row.patientId() + "|" + row.caseId() + "|" + nullToEmpty(row.pathologyNo()) + "|" + prefix;
            buckets.computeIfAbsent(bucketKey, ignored -> new ArrayList<>()).add(row);
        }

        List<String> groupIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (List<TechnicalWorkflowRecords.SlicingWorkbenchRow> bucketRows : buckets.values()) {
            bucketRows.sort(Comparator.comparing(TechnicalWorkflowRecords.SlicingWorkbenchRow::embeddingBoxNo, this::compareEmbeddingBoxNos));
            for (int index = 0; index + 1 < bucketRows.size(); index += 2) {
                List<TechnicalWorkflowRecords.SlicingWorkbenchRow> pair = bucketRows.subList(index, index + 2);
                String groupId = technicalWorkflowSupport.nextId("SPG");
                String embeddingBoxNo = pair.get(0).embeddingBoxNo() + "+" + pair.get(1).embeddingBoxNo();
                TechnicalWorkflowRecords.SlicingWorkbenchRow first = pair.get(0);
                technicalWorkflowRepository.insertSlicingSlidePrintMergeGroup(
                    groupId,
                    first.caseId(),
                    first.pathologyNo(),
                    first.patientId(),
                    embeddingBoxNo,
                    command.operatorUserId(),
                    command.operatorName(),
                    command.remarks(),
                    now);
                for (int pairIndex = 0; pairIndex < pair.size(); pairIndex++) {
                    TechnicalWorkflowRecords.SlicingWorkbenchRow row = pair.get(pairIndex);
                    technicalWorkflowRepository.insertSlicingSlidePrintMergeGroupItem(
                        technicalWorkflowSupport.nextId("SPGI"),
                        groupId,
                        row.taskId(),
                        row.embeddingBoxId(),
                        row.embeddingBoxNo(),
                        pairIndex + 1);
                }
                groupIds.add(groupId);
            }
        }
        if (groupIds.isEmpty()) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "No mergeable slicing task pairs found");
        }
        return new TechnicalWorkflowModels.SlicingSlidePrintMergeGroupResult(groupIds);
    }

    @Transactional
    TechnicalWorkflowModels.SlicingSlidePrintMergeGroupResult cancelSlicingSlidePrintMergeGroups(
        TechnicalWorkflowModels.SlicingSlidePrintMergeGroupCancelCommand command
    ) {
        List<String> printGroupIds = normalizeIds(command.printGroupIds());
        if (printGroupIds.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "At least one merge group is required");
        }
        technicalWorkflowRepository.cancelSlicingSlidePrintMergeGroups(printGroupIds, LocalDateTime.now());
        return new TechnicalWorkflowModels.SlicingSlidePrintMergeGroupResult(printGroupIds);
    }

    @Transactional
    TechnicalWorkflowModels.SlicingSlidePrintResult printSlicingSlideMergeGroup(
        TechnicalWorkflowModels.SlicingSlidePrintMergeGroupPrintCommand command
    ) {
        List<TechnicalWorkflowRecords.SlicingSlidePrintMergeGroupItem> items =
            technicalWorkflowRepository.findPendingSlicingPrintMergeGroupItems(command.printGroupId());
        if (items.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Slicing slide print merge group not found");
        }

        List<String> slideIds = new ArrayList<>();
        List<String> slideNos = new ArrayList<>();
        String firstSlicingId = null;
        for (TechnicalWorkflowRecords.SlicingSlidePrintMergeGroupItem item : items) {
            TechnicalWorkflowModels.SlicingSlidePrintResult result = printSlicingSlides(
                new TechnicalWorkflowModels.SlicingSlidePrintCommand(
                    item.taskId(),
                    item.embeddingBoxId(),
                    1,
                    false,
                    command.printerCode(),
                    command.operatorUserId(),
                    command.operatorName(),
                    command.terminalCode(),
                    command.remarks()));
            if (firstSlicingId == null) {
                firstSlicingId = result.slicingId();
            }
            slideIds.addAll(result.slideIds());
            slideNos.addAll(result.slideNos());
        }
        technicalWorkflowRepository.markSlicingSlidePrintMergeGroupPrinted(
            command.printGroupId(),
            firstSlicingId,
            command.operatorUserId(),
            command.operatorName(),
            command.remarks(),
            LocalDateTime.now());
        return new TechnicalWorkflowModels.SlicingSlidePrintResult(
            command.printGroupId(),
            firstSlicingId,
            slideIds,
            slideNos,
            true,
            slideNos.size());
    }

    private List<String> normalizeIds(List<String> ids) {
        return ids == null
            ? List.of()
            : ids.stream()
                .map(this::normalizeText)
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private String embeddingBoxPrefix(String embeddingBoxNo) {
        String normalizedValue = normalizeText(embeddingBoxNo);
        if (normalizedValue == null) {
            return null;
        }
        Matcher matcher = EMBEDDING_BOX_NO_PATTERN.matcher(normalizedValue);
        return matcher.matches() ? matcher.group(1).toUpperCase() : null;
    }

    private int compareEmbeddingBoxNos(String left, String right) {
        Matcher leftMatcher = EMBEDDING_BOX_NO_PATTERN.matcher(nullToEmpty(left));
        Matcher rightMatcher = EMBEDDING_BOX_NO_PATTERN.matcher(nullToEmpty(right));
        if (leftMatcher.matches() && rightMatcher.matches()) {
            int prefixCompare = leftMatcher.group(1).compareToIgnoreCase(rightMatcher.group(1));
            if (prefixCompare != 0) {
                return prefixCompare;
            }
            return Integer.compare(Integer.parseInt(leftMatcher.group(2)), Integer.parseInt(rightMatcher.group(2)));
        }
        return nullToEmpty(left).compareToIgnoreCase(nullToEmpty(right));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> buildSlideNos(int sourceSlideCount, boolean mergeAdjacent) {
        if (sourceSlideCount < 1) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Slide count must be at least 1");
        }
        List<String> baseSlideNos = new ArrayList<>();
        for (int i = 0; i < sourceSlideCount; i++) {
            baseSlideNos.add(technicalWorkflowSupport.generateSlideNo());
        }
        if (!mergeAdjacent) {
            return baseSlideNos;
        }
        List<String> mergedSlideNos = new ArrayList<>();
        for (int i = 0; i < baseSlideNos.size(); i += 2) {
            String current = baseSlideNos.get(i);
            if (i + 1 >= baseSlideNos.size()) {
                mergedSlideNos.add(current);
            } else {
                mergedSlideNos.add(current + "-" + baseSlideNos.get(i + 1));
            }
        }
        return mergedSlideNos;
    }

    @Transactional
    TechnicalWorkflowModels.SlideQcEvaluationResult createSlideQcEvaluation(
        TechnicalWorkflowModels.CreateSlideQcEvaluationCommand command
    ) {
        TechnicalWorkflowProcessingRecords.Slide slide = technicalWorkflowSupport.getSlide(command.slideId());
        technicalWorkflowSupport.ensureSameCase(command.caseId(), slide.caseId());
        if (command.specimenId() != null
            && !command.specimenId().isBlank()
            && !command.specimenId().trim().equals(slide.specimenId())) {
            throw new BlBusinessException(
                BlErrorCode.INVALID_ARGUMENT,
                400,
                "Slide specimen mismatch");
        }
        LocalDateTime now = LocalDateTime.now();
        String qcEvaluationId = technicalWorkflowSupport.nextId("QC");
        technicalWorkflowRepository.insertSlideQcEvaluation(
            new TechnicalWorkflowProcessingRecords.CreateSlideQcEvaluationCommand(
                qcEvaluationId,
                slide.caseId(),
                command.specimenId() == null || command.specimenId().isBlank()
                    ? slide.specimenId()
                    : command.specimenId(),
                slide.id(),
                command.qcType(),
                command.evaluationResult(),
                command.issueDescription(),
                command.improvementSuggestion(),
                command.operatorUserId(),
                command.operatorName(),
                now,
                command.remarks()));
        String qualityStatus = switch (command.evaluationResult()) {
            case "UNQUALIFIED", "REWORK_REQUIRED" -> "UNQUALIFIED";
            default -> "QUALIFIED";
        };
        technicalWorkflowRepository.updateSlideStatus(slide.id(), slide.slideStatus(), qualityStatus);
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(slide.caseId());
        technicalWorkflowSupport.insertWorkflowEvent(
            pathologyCase.applicationId(),
            slide.specimenId(),
            slide.caseId(),
            TechnicalWorkflowConstants.NODE_QC,
            "EVALUATE",
            command.evaluationResult(),
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            command.issueDescription() == null || command.issueDescription().isBlank()
                ? "Slide QC evaluated"
                : command.issueDescription());
        return new TechnicalWorkflowModels.SlideQcEvaluationResult(
            qcEvaluationId,
            slide.id(),
            command.evaluationResult(),
            qualityStatus);
    }

    @Transactional
    TechnicalWorkflowModels.TaskStartResult startSlideStaining(TechnicalWorkflowModels.TaskStartCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.startTask(
            command, TechnicalWorkflowConstants.NODE_STAINING, TechnicalWorkflowConstants.OBJECT_SLIDE);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "STAINING");
        technicalWorkflowSupport.insertWorkflowEvent(task, TechnicalWorkflowConstants.NODE_STAINING, "START", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Staining started");
        return new TechnicalWorkflowModels.TaskStartResult(task.id(), task.caseId(), "STAINING", TechnicalWorkflowConstants.TASK_IN_PROGRESS);
    }

    @Transactional
    TechnicalWorkflowModels.SlideStainingResult completeSlideStaining(TechnicalWorkflowModels.SlideStainingCompleteCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTask(
            command.taskId(), TechnicalWorkflowConstants.NODE_STAINING, TechnicalWorkflowConstants.OBJECT_SLIDE);
        TechnicalWorkflowProcessingRecords.Slide slide = technicalWorkflowSupport.getSlide(command.slideId());
        technicalWorkflowSupport.validateTaskObject(task, slide.id());
        LocalDateTime now = LocalDateTime.now();
        technicalWorkflowRepository.insertSlideStaining(new TechnicalWorkflowProcessingRecords.CreateSlideStainingCommand(
            technicalWorkflowSupport.nextId("STN"),
            task.caseId(),
            slide.specimenId(),
            slide.id(),
            command.stainingType(),
            TechnicalWorkflowConstants.TASK_COMPLETED,
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.qualityIssue(),
            command.remarks()));
        technicalWorkflowRepository.updateSlideStatus(
            slide.id(), "STAINED", command.qualityIssue() == null ? "QUALIFIED" : "UNQUALIFIED");
        technicalWorkflowRepository.lockPathologyCase(task.caseId());
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TechnicalWorkflowConstants.TASK_COMPLETED, command.remarks(), now);
        String caseStatus = "STAINING";
        if (allStainingTasksCompleted(task.caseId())) {
            caseStatus = "DIAGNOSIS_PENDING";
            technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), caseStatus);
            diagnosticReportAppService.createPrimaryDiagnosticTaskIfAbsent(
                task.caseId(), "Auto created after staining completed");
            technicalWorkflowSupport.insertWorkflowEvent(task.applicationId(), slide.specimenId(), task.caseId(),
                "DIAGNOSIS_ASSIGN", "CREATE", "SUCCESS", command.operatorUserId(), command.operatorName(),
                command.terminalCode(), "Technical workflow handed off to diagnosis");
        }
        technicalWorkflowSupport.insertWorkflowEvent(task.applicationId(), slide.specimenId(), task.caseId(),
            TechnicalWorkflowConstants.NODE_STAINING, "COMPLETE", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), "Staining completed");
        return new TechnicalWorkflowModels.SlideStainingResult(task.id(), slide.id(), caseStatus);
    }

    private boolean allStainingTasksCompleted(String caseId) {
        return technicalWorkflowRepository.findActiveTechnicalTasksByCaseId(caseId).stream()
            .noneMatch(activeTask -> TechnicalWorkflowConstants.NODE_STAINING.equals(activeTask.taskType()));
    }
}
