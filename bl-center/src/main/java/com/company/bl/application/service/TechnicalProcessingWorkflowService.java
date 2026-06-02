package com.company.bl.application.service;

import com.company.bl.application.gateway.TechnicalMarkingGateway;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
class TechnicalProcessingWorkflowService {

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
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.startTask(
            command, TechnicalWorkflowConstants.NODE_EMBEDDING, TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "EMBEDDING");
        technicalWorkflowSupport.insertWorkflowEvent(task, TechnicalWorkflowConstants.NODE_EMBEDDING, "START", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Embedding started");
        return new TechnicalWorkflowModels.TaskStartResult(task.id(), task.caseId(), "EMBEDDING", TechnicalWorkflowConstants.TASK_IN_PROGRESS);
    }

    @Transactional
    TechnicalWorkflowModels.EmbeddingResult completeEmbedding(TechnicalWorkflowModels.EmbeddingCompleteCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTask(
            command.taskId(), TechnicalWorkflowConstants.NODE_EMBEDDING, TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK);
        TechnicalWorkflowRecords.SamplingBlock block = technicalWorkflowSupport.getSamplingBlock(command.samplingBlockId());
        technicalWorkflowSupport.validateTaskObject(task, block.id());
        LocalDateTime now = LocalDateTime.now();
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
        String embeddingBoxNo = command.embeddingBoxNo() == null || command.embeddingBoxNo().isBlank()
            ? block.embeddingBoxNo()
            : command.embeddingBoxNo().trim();
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
        LocalDateTime now = LocalDateTime.now();
        String slicingId = technicalWorkflowSupport.nextId("SLC");
        String slicingBatchNo = "SLC-" + UUID.randomUUID().toString().substring(0, 8);
        technicalWorkflowRepository.insertSlicing(new TechnicalWorkflowProcessingRecords.CreateSlicingCommand(
            slicingId,
            task.caseId(),
            box.specimenId(),
            box.embeddingId(),
            box.id(),
            slicingBatchNo,
            TechnicalWorkflowConstants.TASK_COMPLETED,
            command.slideCount(),
            command.sliceCountPerSlide(),
            command.sliceThickness(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.qualityIssue(),
            command.remarks()));
        List<String> slideIds = new ArrayList<>();
        for (int i = 0; i < command.slideCount(); i++) {
            String slideId = technicalWorkflowSupport.nextId("SLD");
            String slideNo = technicalWorkflowSupport.generateSlideNo();
            technicalWorkflowRepository.insertSlide(new TechnicalWorkflowProcessingRecords.CreateSlideCommand(
                slideId,
                task.caseId(),
                box.specimenId(),
                slicingId,
                box.id(),
                box.samplingBlockId(),
                slideNo,
                slideNo,
                false,
                "PENDING",
                "CREATED",
                command.sliceCountPerSlide()));
            TechnicalMarkingGateway.MarkingResult result = technicalWorkflowSupport.markObject(
                task.caseId(),
                TechnicalWorkflowConstants.OBJECT_SLIDE,
                slideId,
                command.deviceCode(),
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
                    "MARK",
                    "FAILED",
                    command.operatorUserId(),
                    command.operatorName(),
                    command.terminalCode(),
                    result.message());
            }
            technicalWorkflowSupport.createTechnicalTaskIfAbsent(
                task.applicationId(),
                task.caseId(),
                box.specimenId(),
                TechnicalWorkflowConstants.NODE_STAINING,
                TechnicalWorkflowConstants.OBJECT_SLIDE,
                slideId,
                task.id(),
                "slideNo=" + slideNo);
            slideIds.add(slideId);
        }
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TechnicalWorkflowConstants.TASK_COMPLETED, command.remarks(), now);
        technicalWorkflowSupport.insertWorkflowEvent(task.applicationId(), box.specimenId(), task.caseId(),
            TechnicalWorkflowConstants.NODE_SLICING, "COMPLETE", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), "Slicing completed");
        return new TechnicalWorkflowModels.SlicingResult(task.id(), slicingId, slideIds, "SLICING");
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
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TechnicalWorkflowConstants.TASK_COMPLETED, command.remarks(), now);
        boolean hasRemainingStainingTasks = technicalWorkflowRepository.findActiveTechnicalTasksByCaseId(task.caseId()).stream()
            .anyMatch(activeTask -> TechnicalWorkflowConstants.NODE_STAINING.equals(activeTask.taskType()));
        String caseStatus = "STAINING";
        if (!hasRemainingStainingTasks) {
            technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "DIAGNOSIS_PENDING");
            diagnosticReportAppService.createPrimaryDiagnosticTaskIfAbsent(task.caseId(), "Auto created after staining completed");
            technicalWorkflowSupport.insertWorkflowEvent(task.applicationId(), slide.specimenId(), task.caseId(),
                "DIAGNOSIS_ASSIGN", "CREATE", "SUCCESS", command.operatorUserId(), command.operatorName(),
                command.terminalCode(), "Technical workflow handed off to diagnosis");
            caseStatus = "DIAGNOSIS_PENDING";
        }
        technicalWorkflowSupport.insertWorkflowEvent(task.applicationId(), slide.specimenId(), task.caseId(),
            TechnicalWorkflowConstants.NODE_STAINING, "COMPLETE", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), "Staining completed");
        return new TechnicalWorkflowModels.SlideStainingResult(task.id(), slide.id(), caseStatus);
    }
}
