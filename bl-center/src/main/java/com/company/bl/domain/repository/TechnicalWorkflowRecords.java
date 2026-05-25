package com.company.bl.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

public final class TechnicalWorkflowRecords {

    private TechnicalWorkflowRecords() {
    }

    public record PendingTechnicalTaskQuery(
        int page,
        int size,
        String taskType,
        String taskStatus,
        String priority,
        String assignedToUserId,
        String currentNode,
        String applicationNo,
        String pathologyNo,
        String objectType,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        boolean timedOutOnly,
        LocalDateTime grossingTimedOutBefore,
        LocalDateTime dehydrationTimedOutBefore,
        LocalDateTime stainingTimedOutBefore
    ) {
    }

    public record PagedTechnicalTasks(List<TechnicalTask> items, long total) {
    }

    public record TechnicalTask(
        String id,
        String applicationId,
        String applicationNo,
        String caseId,
        String pathologyNo,
        String specimenId,
        String taskType,
        String taskStatus,
        String objectType,
        String objectId,
        String parentTaskId,
        String priority,
        String currentNode,
        String stationCode,
        String stationName,
        String assignedToUserId,
        String assignedToName,
        LocalDateTime expectedCompletedAt,
        String productionRemarks,
        LocalDateTime receivedAt,
        String payload,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
    ) {
    }

    public record CreateTechnicalTaskCommand(
        String id,
        String applicationId,
        String caseId,
        String specimenId,
        String taskType,
        String taskStatus,
        String objectType,
        String objectId,
        String parentTaskId,
        String priority,
        String currentNode,
        String stationCode,
        String stationName,
        String assignedToUserId,
        String assignedToName,
        LocalDateTime expectedCompletedAt,
        String productionRemarks,
        LocalDateTime receivedAt,
        String payload,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    public record CreateSamplingCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingStatus,
        int blockCount,
        int grossImageCount,
        String samplingTemplateId,
        String sizeText,
        String cutSurfaceFeature,
        String marginMarking,
        String grossDescription,
        String sampledByUserId,
        String sampledByName,
        LocalDateTime sampledAt,
        String remarks
    ) {
    }

    public record CreateSamplingBlockCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingId,
        int sequenceNo,
        String blockCode,
        String blockSite,
        String blockDescription,
        String embeddingBoxNo,
        String specialRequirement
    ) {
    }

    public record SamplingBlock(
        String id,
        String caseId,
        String specimenId,
        String samplingId,
        int sequenceNo,
        String blockCode,
        String blockSite,
        String blockDescription,
        String embeddingBoxNo,
        String specialRequirement
    ) {
    }

    public record CreateDehydrationBatchCommand(
        String id,
        String caseId,
        String batchNo,
        String batchStatus,
        String basketNo,
        String deviceNo,
        String operatorUserId,
        String operatorName,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    public record CreateDehydrationBatchItemCommand(
        String id,
        String batchId,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String itemStatus,
        LocalDateTime loadedAt,
        String remarks
    ) {
    }

    public record DehydrationBatch(
        String id,
        String caseId,
        String batchNo,
        String batchStatus,
        String basketNo,
        String deviceNo,
        String operatorUserId,
        String operatorName,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String remarks
    ) {
    }

    public record DehydrationBatchItem(
        String id,
        String batchId,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String itemStatus,
        LocalDateTime loadedAt,
        String remarks
    ) {
    }

    public record CreateEmbeddingCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingId,
        String samplingBlockId,
        String embeddingStatus,
        String evaluationLevel,
        String samplingEvaluation,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String embeddedByUserId,
        String embeddedByName,
        String remarks
    ) {
    }

    public record Embedding(
        String id,
        String caseId,
        String specimenId,
        String samplingId,
        String samplingBlockId,
        String embeddingStatus
    ) {
    }

    public record CreateEmbeddingBoxCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingId,
        String embeddingBoxNo,
        int blockCount,
        boolean reEmbeddingFlag,
        String sliceNotice,
        String storageStatus
    ) {
    }

    public record EmbeddingBox(
        String id,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingId,
        String embeddingBoxNo,
        int blockCount,
        boolean reEmbeddingFlag,
        String sliceNotice,
        String storageStatus
    ) {
    }

}
