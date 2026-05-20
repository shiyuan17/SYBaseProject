package com.company.bl.application.service;

import java.util.List;

public final class TechnicalWorkflowModels {

    private TechnicalWorkflowModels() {
    }

    public interface OperatorCarrier {
        String operatorUserId();
        String operatorName();
        String remarks();
    }

    public record PendingTechnicalTaskQuery(
        int page,
        int size,
        String taskType,
        String taskStatus,
        String applicationNo,
        String pathologyNo,
        String objectType
    ) {
    }

    public record PendingTechnicalTaskPage(List<TaskView> items, int page, int size, long total) {
    }

    public record TaskView(
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
        String payload,
        String remarks,
        String createdAt,
        String startedAt,
        String completedAt
    ) {
    }

    public record TaskStartCommand(
        String taskId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record BatchOperatorCommand(
        String batchId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record TaskStartResult(String taskId, String caseId, String caseStatus, String taskStatus) {
    }

    public record MediaAssetInput(String fileUrl, String fileName) {
    }

    public record GrossingBlockItem(String blockSite, String blockDescription, String specialRequirement) {
    }

    public record GrossingSpecimenItem(
        String specimenId,
        String specimenType,
        String bodyPartId,
        String samplingTemplateId,
        String grossDescription,
        List<GrossingBlockItem> blocks,
        List<MediaAssetInput> mediaAssets
    ) {
    }

    public record GrossingCompleteCommand(
        String taskId,
        String caseId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        List<GrossingSpecimenItem> specimens
    ) implements OperatorCarrier {
    }

    public record GrossingResult(String taskId, String caseId, String caseStatus, int createdDehydrationTaskCount) {
    }

    public record CreateDehydrationBatchCommand(
        String caseId,
        String basketNo,
        String deviceNo,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        List<String> samplingBlockIds
    ) implements OperatorCarrier {
    }

    public record CompleteDehydrationBatchCommand(
        String batchId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        List<MediaAssetInput> mediaAssets
    ) implements OperatorCarrier {
    }

    public record DehydrationBatchResult(String batchId, String batchNo, String batchStatus, int taskCount) {
    }

    public record EmbeddingCompleteCommand(
        String taskId,
        String samplingBlockId,
        String embeddingBoxNo,
        int blockCount,
        String sliceNotice,
        String evaluationLevel,
        String samplingEvaluation,
        String deviceCode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record EmbeddingResult(
        String taskId,
        String embeddingId,
        String embeddingBoxId,
        String caseStatus,
        boolean markingSuccess,
        String markingMessage
    ) {
    }

    public record SlicingCompleteCommand(
        String taskId,
        String embeddingBoxId,
        int slideCount,
        Integer sliceCountPerSlide,
        String sliceThickness,
        String qualityIssue,
        String deviceCode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlicingResult(String taskId, String slicingId, List<String> slideIds, String caseStatus) {
    }

    public record SlideStainingCompleteCommand(
        String taskId,
        String slideId,
        String stainingType,
        String qualityIssue,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlideStainingResult(String taskId, String slideId, String caseStatus) {
    }

    public record CreateReworkOrderCommand(
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingBoxId,
        String slideId,
        String reworkType,
        String reason,
        String qcType,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record ExecuteReworkOrderCommand(
        String reworkOrderId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record ReworkOrderResult(String caseId, String reworkType, String status) {
    }

    public record TechnicalTrackingView(
        String caseId,
        String pathologyNo,
        String caseStatus,
        List<TaskView> technicalTasks,
        List<TechnicalSpecimenSummary> specimens,
        List<TechnicalBlockSummary> blocks,
        List<TechnicalEmbeddingBoxSummary> embeddingBoxes,
        List<TechnicalSlideSummary> slides,
        List<ReworkSummary> reworks,
        List<TechnicalTrackingEvent> events
    ) {
    }

    public record TechnicalSpecimenSummary(
        String specimenId,
        String specimenNo,
        String barcode,
        String specimenName,
        String specimenStatus
    ) {
    }

    public record TechnicalBlockSummary(
        String blockId,
        String specimenId,
        String blockCode,
        String embeddingBoxNo,
        String description
    ) {
    }

    public record TechnicalEmbeddingBoxSummary(
        String embeddingBoxId,
        String specimenId,
        String embeddingBoxNo,
        String sliceNotice,
        int slideCount
    ) {
    }

    public record TechnicalSlideSummary(
        String slideId,
        String specimenId,
        String embeddingBoxId,
        String slideNo,
        String slideStatus,
        String qualityStatus
    ) {
    }

    public record ReworkSummary(String reworkOrderId, String reworkType, String status, String reason) {
    }

    public record TechnicalTrackingEvent(
        String nodeCode,
        String eventType,
        String eventStatus,
        String eventTime,
        String operatorName,
        String eventContent
    ) {
    }
}
