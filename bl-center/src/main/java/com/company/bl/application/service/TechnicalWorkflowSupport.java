package com.company.bl.application.service;

import com.company.bl.application.gateway.TechnicalMarkingGateway;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.masterdata.infrastructure.SamplingJdbcRepository;
import com.company.bl.support.application.NumberingService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class TechnicalWorkflowSupport {

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final NumberingService numberingService;
    private final SamplingJdbcRepository samplingJdbcRepository;
    private final TechnicalMarkingGateway technicalMarkingGateway;

    TechnicalWorkflowSupport(TechnicalWorkflowRepository technicalWorkflowRepository,
                             NumberingService numberingService,
                             SamplingJdbcRepository samplingJdbcRepository,
                             TechnicalMarkingGateway technicalMarkingGateway) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.numberingService = numberingService;
        this.samplingJdbcRepository = samplingJdbcRepository;
        this.technicalMarkingGateway = technicalMarkingGateway;
    }

    TechnicalWorkflowModels.TaskView toTaskView(TechnicalWorkflowRecords.TechnicalTask task,
                                                TechnicalTaskTimeoutPolicy.TimeoutEvaluation timeoutEvaluation) {
        return new TechnicalWorkflowModels.TaskView(
            task.id(),
            task.applicationId(),
            task.applicationNo(),
            task.patientName(),
            task.patientId(),
            task.caseId(),
            task.pathologyNo(),
            task.specimenId(),
            task.taskType(),
            task.taskStatus(),
            task.objectType(),
            task.objectId(),
            task.objectDisplayNo(),
            task.samplingBlockCode(),
            task.samplingBlockDescription(),
            task.sampledByName(),
            stringify(task.sampledAt()),
            task.payload(),
            task.priority(),
            task.currentNode(),
            task.stationCode(),
            task.stationName(),
            task.assignedToUserId(),
            task.assignedToName(),
            stringify(task.expectedCompletedAt()),
            task.productionRemarks(),
            stringify(task.receivedAt()),
            task.remarks(),
            stringify(task.createdAt()),
            stringify(task.startedAt()),
            stringify(task.completedAt()),
            stringify(timeoutEvaluation.deadlineAt()),
            timeoutEvaluation.timeoutRuleCode(),
            timeoutEvaluation.timedOut());
    }

    TechnicalWorkflowRecords.TechnicalTask startTask(TechnicalWorkflowModels.TaskStartCommand command,
                                                        String taskType,
                                                        String objectType) {
        TechnicalWorkflowRecords.TechnicalTask task = requireActiveTask(command.taskId(), taskType, objectType);
        LocalDateTime now = LocalDateTime.now();
        technicalWorkflowRepository.startTechnicalTask(task.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        return technicalWorkflowRepository.findTechnicalTaskById(task.id()).orElse(task);
    }

    TechnicalWorkflowRecords.TechnicalTask requireActiveTask(String taskId, String taskType, String objectType) {
        TechnicalWorkflowRecords.TechnicalTask task = technicalWorkflowRepository.findTechnicalTaskById(taskId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Technical task not found"));
        if (!taskType.equals(task.taskType()) || !objectType.equals(task.objectType())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Technical task type mismatch");
        }
        if (!TechnicalWorkflowConstants.TASK_PENDING.equals(task.taskStatus())
            && !TechnicalWorkflowConstants.TASK_IN_PROGRESS.equals(task.taskStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Technical task is not active");
        }
        return task;
    }

    TechnicalWorkflowRecords.TechnicalTask requireActiveTaskByObject(String taskType, String objectType, String objectId) {
        List<TechnicalWorkflowRecords.TechnicalTask> tasks =
            technicalWorkflowRepository.findActiveTechnicalTasksByObject(taskType, objectType, objectId);
        if (tasks.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Active technical task not found");
        }
        return tasks.get(0);
    }

    void createTechnicalTaskIfAbsent(String applicationId,
                                     String caseId,
                                     String specimenId,
                                     String taskType,
                                     String objectType,
                                     String objectId,
                                     String parentTaskId,
                                     String payload) {
        if (!technicalWorkflowRepository.findActiveTechnicalTasksByObject(taskType, objectType, objectId).isEmpty()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Active technical task already exists");
        }
        technicalWorkflowRepository.insertTechnicalTask(new TechnicalWorkflowRecords.CreateTechnicalTaskCommand(
            nextId("TT"),
            applicationId,
            caseId,
            specimenId,
            taskType,
            TechnicalWorkflowConstants.TASK_PENDING,
            objectType,
            objectId,
            parentTaskId,
            "NORMAL",
            taskType,
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now(),
            payload,
            null,
            LocalDateTime.now()));
    }

    String resolveSamplingTemplateId(String specimenType, String bodyPartId, String explicitTemplateId) {
        if (explicitTemplateId != null && !explicitTemplateId.isBlank()) {
            if (samplingJdbcRepository.findTemplateById(explicitTemplateId.trim()) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Sampling template not found");
            }
            return explicitTemplateId.trim();
        }
        if (bodyPartId == null || bodyPartId.isBlank() || specimenType == null || specimenType.isBlank()) {
            return null;
        }
        Map<String, List<SamplingJdbcRepository.TemplateSiteRow>> siteMap = samplingJdbcRepository.findTemplateSites().stream()
            .collect(Collectors.groupingBy(SamplingJdbcRepository.TemplateSiteRow::templateId));
        List<SamplingJdbcRepository.TemplateRow> matches = samplingJdbcRepository.findTemplates().stream()
            .filter(SamplingJdbcRepository.TemplateRow::enabled)
            .filter(template -> specimenType.trim().equalsIgnoreCase(nullToBlank(template.applicableSpecimenType())))
            .filter(template -> siteMap.getOrDefault(template.id(), List.of()).stream()
                .anyMatch(site -> bodyPartId.trim().equals(site.bodyPartId())))
            .toList();
        return matches.size() == 1 ? matches.get(0).id() : null;
    }

    void storeMediaAssets(PathologyCase pathologyCase,
                          String specimenId,
                          String objectType,
                          String mediaType,
                          String objectId,
                          List<TechnicalWorkflowModels.MediaAssetInput> mediaAssets,
                          TechnicalWorkflowModels.OperatorCarrier operatorCarrier,
                          LocalDateTime now) {
        for (TechnicalWorkflowModels.MediaAssetInput asset : mediaAssets) {
            technicalWorkflowRepository.insertCaseMediaAsset(new TechnicalWorkflowProcessingRecords.CreateCaseMediaAssetCommand(
                nextId("MED"),
                pathologyCase.id(),
                specimenId,
                objectType,
                objectId,
                mediaType,
                asset.fileUrl(),
                asset.fileName(),
                now,
                operatorCarrier.operatorUserId(),
                operatorCarrier.operatorName(),
                operatorCarrier.remarks()));
        }
    }

    TechnicalMarkingGateway.MarkingResult markObject(String caseId,
                                                     String objectType,
                                                     String objectId,
                                                     String deviceCode,
                                                     String label,
                                                     String operatorUserId,
                                                     String operatorName,
                                                     String terminalCode,
                                                     String nodeCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return new TechnicalMarkingGateway.MarkingResult(true, "Technical marking skipped");
        }
        TechnicalMarkingGateway.MarkingResult result = technicalMarkingGateway.mark(
            new TechnicalMarkingGateway.MarkingRequest(caseId, objectType, objectId, deviceCode, label));
        insertWorkflowEvent(getCase(caseId).applicationId(), null, caseId, nodeCode, "MARK",
            result.success() ? "SUCCESS" : "FAILED", operatorUserId, operatorName, terminalCode, result.message());
        return result;
    }

    void validateTaskObject(TechnicalWorkflowRecords.TechnicalTask task, String objectId) {
        if (!objectId.equals(task.objectId())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Task object mismatch");
        }
    }

    PathologyCase getCase(String caseId) {
        return technicalWorkflowRepository.findPathologyCaseById(caseId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology case not found"));
    }

    Specimen getSpecimen(String specimenId) {
        return technicalWorkflowRepository.findSpecimenById(specimenId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen not found"));
    }

    TechnicalWorkflowRecords.SamplingBlock getSamplingBlock(String samplingBlockId) {
        return technicalWorkflowRepository.findSamplingBlockById(samplingBlockId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Sampling block not found"));
    }

    TechnicalWorkflowRecords.EmbeddingBox getEmbeddingBox(String embeddingBoxId) {
        return technicalWorkflowRepository.findEmbeddingBoxById(embeddingBoxId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Embedding box not found"));
    }

    TechnicalWorkflowProcessingRecords.Slide getSlide(String slideId) {
        return technicalWorkflowRepository.findSlideById(slideId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Slide not found"));
    }

    TechnicalWorkflowRecords.DehydrationBatch getDehydrationBatch(String batchId) {
        return technicalWorkflowRepository.findDehydrationBatchById(batchId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Dehydration batch not found"));
    }

    void ensureSameCase(String caseId, String actualCaseId) {
        if (!caseId.equals(actualCaseId)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Object does not belong to case");
        }
    }

    void insertWorkflowEvent(TechnicalWorkflowRecords.TechnicalTask task,
                             String nodeCode,
                             String eventType,
                             String eventStatus,
                             String operatorUserId,
                             String operatorName,
                             String terminalCode,
                             String content) {
        insertWorkflowEvent(task.applicationId(), task.specimenId(), task.caseId(),
            resolveNodeCode(nodeCode, task), eventType, eventStatus,
            operatorUserId, operatorName, terminalCode, content);
    }

    void insertWorkflowEvent(String applicationId,
                             String specimenId,
                             String caseId,
                             String nodeCode,
                             String eventType,
                             String eventStatus,
                             String operatorUserId,
                             String operatorName,
                             String terminalCode,
                             String content) {
        technicalWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            nextId("EVT"),
            applicationId,
            specimenId,
            caseId,
            null,
            nodeCode,
            eventType,
            eventStatus,
            LocalDateTime.now(),
            operatorUserId,
            operatorName,
            terminalCode,
            content));
    }

    String generateBlockNo(String caseId) {
        return numberingService.generateBlockNo(caseId);
    }

    String generateSlideNo() {
        return numberingService.generateSlideNo();
    }

    String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value.trim();
    }

    private String resolveNodeCode(String explicitNodeCode, TechnicalWorkflowRecords.TechnicalTask task) {
        if (explicitNodeCode != null && !explicitNodeCode.isBlank()) {
            return explicitNodeCode.trim();
        }
        if (task.currentNode() != null && !task.currentNode().isBlank()) {
            return task.currentNode().trim();
        }
        return requireText(task.taskType(), "Technical workflow node code is required");
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
