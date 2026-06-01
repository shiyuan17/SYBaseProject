package com.company.bl.application.service;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
class TechnicalGrossingWorkflowService {

    private static final String GROSSING_MEDIA_OBJECT_TYPE = "SAMPLING";
    private static final String GROSSING_MEDIA_TYPE = "GROSS_IMAGE";

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final TechnicalWorkflowQueryService technicalWorkflowQueryService;
    private final TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService;

    TechnicalGrossingWorkflowService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                     TechnicalWorkflowSupport technicalWorkflowSupport,
                                     TechnicalWorkflowQueryService technicalWorkflowQueryService,
                                     TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.technicalWorkflowQueryService = technicalWorkflowQueryService;
        this.technicalSpecimenRegistrationService = technicalSpecimenRegistrationService;
    }

    @Transactional(readOnly = true)
    TechnicalWorkflowModels.GrossingWorkbenchContext getGrossingWorkbenchContext(String taskId) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTask(
            taskId, TechnicalWorkflowConstants.NODE_GROSSING, TechnicalWorkflowConstants.OBJECT_CASE);
        TechnicalWorkflowModels.TechnicalTrackingView tracking =
            technicalWorkflowQueryService.getTechnicalTracking(task.caseId());
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationWorkspace workspace =
            technicalSpecimenRegistrationService.getRegistrationWorkspace(task.caseId());
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail detail =
            technicalSpecimenRegistrationService.getRegistrationDetail(task.caseId());
        List<TechnicalWorkflowModels.GrossingWorkbenchMediaAsset> mediaAssets =
            technicalWorkflowRepository.findCaseMediaAssets(
                    task.caseId(),
                    GROSSING_MEDIA_OBJECT_TYPE,
                    GROSSING_MEDIA_TYPE)
                .stream()
                .map(this::toWorkbenchMediaAsset)
                .toList();
        return new TechnicalWorkflowModels.GrossingWorkbenchContext(
            new TechnicalWorkflowModels.GrossingWorkbenchTaskSummary(
                task.id(),
                task.taskStatus(),
                task.objectType(),
                task.objectId()),
            new TechnicalWorkflowModels.GrossingWorkbenchCaseSummary(
                workspace.pendingSummary().caseId(),
                workspace.pendingSummary().applicationId(),
                workspace.pendingSummary().applicationNo(),
                workspace.basicInfo().pathologyNo(),
                tracking.caseStatus(),
                workspace.basicInfo().patientName(),
                workspace.basicInfo().patientId(),
                workspace.basicInfo().inpatientNo(),
                workspace.basicInfo().applicationType(),
                workspace.basicInfo().submittingDepartmentName()),
            tracking,
            detail.clinicalDiagnosis(),
            workspace.detailSections().historySummary(),
            workspace.detailSections().labAndImagingExaminations(),
            joinSections(
                workspace.detailSections().clinicalExaminationAndSurgeryFindings(),
                workspace.detailSections().clinicalSubmissionRequirements(),
                workspace.detailSections().infectiousAndPastHistorySummary()),
            detail.checkItems(),
            mediaAssets);
    }

    @Transactional
    TechnicalWorkflowModels.TaskStartResult startGrossing(TechnicalWorkflowModels.TaskStartCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.startTask(
            command, TechnicalWorkflowConstants.NODE_GROSSING, TechnicalWorkflowConstants.OBJECT_CASE);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "SAMPLING");
        technicalWorkflowSupport.insertWorkflowEvent(task, TechnicalWorkflowConstants.NODE_GROSSING, "START", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Grossing started");
        return new TechnicalWorkflowModels.TaskStartResult(task.id(), task.caseId(), "SAMPLING", TechnicalWorkflowConstants.TASK_IN_PROGRESS);
    }

    @Transactional
    TechnicalWorkflowModels.GrossingResult completeGrossing(TechnicalWorkflowModels.GrossingCompleteCommand command) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTask(
            command.taskId(), TechnicalWorkflowConstants.NODE_GROSSING, TechnicalWorkflowConstants.OBJECT_CASE);
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(task.caseId());
        LocalDateTime now = LocalDateTime.now();
        int nextTaskCount = 0;
        for (TechnicalWorkflowModels.GrossingSpecimenItem item : command.specimens()) {
            Specimen specimen = technicalWorkflowSupport.getSpecimen(item.specimenId());
            technicalWorkflowSupport.ensureSameCase(pathologyCase.id(), specimen.caseId());
            String templateId = technicalWorkflowSupport.resolveSamplingTemplateId(
                item.specimenType(), item.bodyPartId(), item.samplingTemplateId());
            String samplingId = technicalWorkflowSupport.nextId("SMP");
            technicalWorkflowRepository.insertSampling(new TechnicalWorkflowRecords.CreateSamplingCommand(
                samplingId,
                pathologyCase.id(),
                specimen.id(),
                TechnicalWorkflowConstants.TASK_COMPLETED,
                item.blockCount() == null ? item.blocks().size() : item.blockCount(),
                item.mediaAssets().size(),
                templateId,
                item.sizeText(),
                item.cutSurfaceFeature(),
                item.marginMarking(),
                item.grossDescription(),
                command.operatorUserId(),
                command.operatorName(),
                now,
                command.remarks()));
            int sequenceNo = 0;
            for (TechnicalWorkflowModels.GrossingBlockItem block : item.blocks()) {
                String blockId = technicalWorkflowSupport.nextId("SBK");
                String blockCode = technicalWorkflowSupport.generateBlockNo(pathologyCase.id());
                String embeddingBoxNo = "BX-" + blockCode;
                technicalWorkflowRepository.insertSamplingBlock(new TechnicalWorkflowRecords.CreateSamplingBlockCommand(
                    blockId,
                    pathologyCase.id(),
                    specimen.id(),
                    samplingId,
                    ++sequenceNo,
                    blockCode,
                    block.blockSite(),
                    block.blockDescription(),
                    embeddingBoxNo,
                    block.specialRequirement()));
                technicalWorkflowSupport.createTechnicalTaskIfAbsent(
                    task.applicationId(),
                    pathologyCase.id(),
                    specimen.id(),
                    TechnicalWorkflowConstants.NODE_DEHYDRATION,
                    TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK,
                    blockId,
                    task.id(),
                    "blockCode=" + blockCode + ";embeddingBoxNo=" + embeddingBoxNo);
                nextTaskCount++;
            }
            technicalWorkflowSupport.storeMediaAssets(
                pathologyCase,
                specimen.id(),
                "SAMPLING",
                "GROSS_IMAGE",
                samplingId,
                item.mediaAssets(),
                command,
                now);
            technicalWorkflowSupport.insertWorkflowEvent(task.applicationId(), specimen.id(), pathologyCase.id(),
                TechnicalWorkflowConstants.NODE_GROSSING, "COMPLETE", "SUCCESS", command.operatorUserId(),
                command.operatorName(), command.terminalCode(), "Grossing completed for specimen " + specimen.specimenNo());
        }
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TechnicalWorkflowConstants.TASK_COMPLETED, command.remarks(), now);
        return new TechnicalWorkflowModels.GrossingResult(task.id(), pathologyCase.id(), "SAMPLING", nextTaskCount);
    }

    @Transactional
    TechnicalWorkflowModels.DehydrationBatchResult createDehydrationBatch(TechnicalWorkflowModels.CreateDehydrationBatchCommand command) {
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(command.caseId());
        LocalDateTime now = LocalDateTime.now();
        List<TechnicalWorkflowRecords.SamplingBlock> blocks =
            technicalWorkflowRepository.findSamplingBlocksByIds(command.samplingBlockIds());
        if (blocks.size() != command.samplingBlockIds().size()) {
            throw new com.company.bl.domain.exception.BlBusinessException(
                com.company.bl.domain.enums.BlErrorCode.RESOURCE_NOT_FOUND, 404, "Sampling block not found");
        }
        String batchId = technicalWorkflowSupport.nextId("DB");
        String batchNo = "DB-" + UUID.randomUUID().toString().substring(0, 8);
        technicalWorkflowRepository.insertDehydrationBatch(new TechnicalWorkflowRecords.CreateDehydrationBatchCommand(
            batchId,
            command.caseId(),
            batchNo,
            TechnicalWorkflowConstants.TASK_PENDING,
            command.basketNo(),
            command.deviceNo(),
            command.operatorUserId(),
            command.operatorName(),
            command.remarks(),
            now));
        for (TechnicalWorkflowRecords.SamplingBlock block : blocks) {
            technicalWorkflowSupport.requireActiveTaskByObject(
                TechnicalWorkflowConstants.NODE_DEHYDRATION,
                TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK,
                block.id());
            technicalWorkflowRepository.insertDehydrationBatchItem(new TechnicalWorkflowRecords.CreateDehydrationBatchItemCommand(
                technicalWorkflowSupport.nextId("DBI"),
                batchId,
                block.caseId(),
                block.specimenId(),
                block.id(),
                "LOADED",
                now,
                command.remarks()));
        }
        technicalWorkflowSupport.insertWorkflowEvent(pathologyCase.applicationId(), null, pathologyCase.id(),
            TechnicalWorkflowConstants.NODE_DEHYDRATION, "CREATE_BATCH", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), "Dehydration batch created");
        return new TechnicalWorkflowModels.DehydrationBatchResult(batchId, batchNo, TechnicalWorkflowConstants.TASK_PENDING, blocks.size());
    }

    @Transactional
    TechnicalWorkflowModels.DehydrationBatchResult startDehydrationBatch(TechnicalWorkflowModels.BatchOperatorCommand command) {
        TechnicalWorkflowRecords.DehydrationBatch batch = technicalWorkflowSupport.getDehydrationBatch(command.batchId());
        LocalDateTime now = LocalDateTime.now();
        List<TechnicalWorkflowRecords.DehydrationBatchItem> items = technicalWorkflowRepository.findDehydrationBatchItems(batch.id());
        for (TechnicalWorkflowRecords.DehydrationBatchItem item : items) {
            TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowSupport.requireActiveTaskByObject(
                TechnicalWorkflowConstants.NODE_DEHYDRATION,
                TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK,
                item.samplingBlockId());
            technicalWorkflowRepository.startTechnicalTask(task.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
            technicalWorkflowRepository.updateDehydrationBatchItemStatus(batch.id(), item.samplingBlockId(),
                TechnicalWorkflowConstants.TASK_IN_PROGRESS, command.remarks());
        }
        technicalWorkflowRepository.updateDehydrationBatchStatus(
            batch.id(),
            TechnicalWorkflowConstants.TASK_IN_PROGRESS,
            command.operatorUserId(),
            command.operatorName(),
            now,
            null,
            command.remarks());
        technicalWorkflowRepository.updatePathologyCaseStatus(batch.caseId(), "DEHYDRATION");
        technicalWorkflowSupport.insertWorkflowEvent(technicalWorkflowSupport.getCase(batch.caseId()).applicationId(), null, batch.caseId(),
            TechnicalWorkflowConstants.NODE_DEHYDRATION, "START", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), "Dehydration started");
        return new TechnicalWorkflowModels.DehydrationBatchResult(
            batch.id(), batch.batchNo(), TechnicalWorkflowConstants.TASK_IN_PROGRESS, items.size());
    }

    @Transactional
    TechnicalWorkflowModels.DehydrationBatchResult completeDehydrationBatch(TechnicalWorkflowModels.CompleteDehydrationBatchCommand command) {
        TechnicalWorkflowRecords.DehydrationBatch batch = technicalWorkflowSupport.getDehydrationBatch(command.batchId());
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(batch.caseId());
        LocalDateTime now = LocalDateTime.now();
        List<TechnicalWorkflowRecords.DehydrationBatchItem> items = technicalWorkflowRepository.findDehydrationBatchItems(batch.id());
        for (TechnicalWorkflowRecords.DehydrationBatchItem item : items) {
            TechnicalWorkflowRecords.TechnicalTask dehydrationTask = technicalWorkflowSupport.requireActiveTaskByObject(
                TechnicalWorkflowConstants.NODE_DEHYDRATION,
                TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK,
                item.samplingBlockId());
            technicalWorkflowRepository.completeTechnicalTask(
                dehydrationTask.id(), TechnicalWorkflowConstants.TASK_COMPLETED, command.remarks(), now);
            technicalWorkflowRepository.updateDehydrationBatchItemStatus(
                batch.id(), item.samplingBlockId(), TechnicalWorkflowConstants.TASK_COMPLETED, command.remarks());
            technicalWorkflowSupport.createTechnicalTaskIfAbsent(
                dehydrationTask.applicationId(),
                dehydrationTask.caseId(),
                dehydrationTask.specimenId(),
                TechnicalWorkflowConstants.NODE_EMBEDDING,
                TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK,
                item.samplingBlockId(),
                dehydrationTask.id(),
                null);
        }
        technicalWorkflowSupport.storeMediaAssets(pathologyCase, null, "DEHYDRATION", "DEHYDRATION_IMAGE",
            batch.id(), command.mediaAssets(), command, now);
        technicalWorkflowRepository.updateDehydrationBatchStatus(
            batch.id(),
            TechnicalWorkflowConstants.TASK_COMPLETED,
            command.operatorUserId(),
            command.operatorName(),
            null,
            now,
            command.remarks());
        technicalWorkflowSupport.insertWorkflowEvent(pathologyCase.applicationId(), null, batch.caseId(),
            TechnicalWorkflowConstants.NODE_DEHYDRATION, "COMPLETE", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), "Dehydration completed");
        return new TechnicalWorkflowModels.DehydrationBatchResult(
            batch.id(), batch.batchNo(), TechnicalWorkflowConstants.TASK_COMPLETED, items.size());
    }

    private TechnicalWorkflowModels.GrossingWorkbenchMediaAsset toWorkbenchMediaAsset(
        TechnicalWorkflowRecords.CaseMediaAsset asset
    ) {
        return new TechnicalWorkflowModels.GrossingWorkbenchMediaAsset(
            asset.id(),
            asset.specimenId(),
            asset.fileName(),
            asset.fileUrl(),
            asset.capturedAt() == null ? null : asset.capturedAt().toString(),
            asset.capturedByName());
    }

    private String joinSections(String... values) {
        return java.util.Arrays.stream(values)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .reduce((left, right) -> left + "\n\n" + right)
            .orElse(null);
    }
}
