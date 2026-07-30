package com.company.bl.domain.repository;

import java.time.LocalDate;
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
        String taskId,
        String applicationNo,
        String pathologyNo,
        String keyword,
        String objectType,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        boolean timedOutOnly,
        boolean includeAllStatuses,
        LocalDateTime grossingTimedOutBefore,
        LocalDateTime dehydrationTimedOutBefore,
        LocalDateTime slicingTimedOutBefore,
        LocalDateTime stainingTimedOutBefore
    ) {
    }

    public record PagedTechnicalTasks(List<TechnicalTask> items, long total) {
    }

    public record TechnicalTrackingCaseListQuery(
        int page,
        int size,
        LocalDateTime dateFrom,
        LocalDateTime dateToExclusive
    ) {
    }

    public record PagedTechnicalTrackingCases(
        List<TechnicalTrackingCaseListItem> items,
        long total
    ) {
    }

    public record TechnicalTrackingCaseListItem(
        String caseId,
        String pathologyNo,
        String patientName,
        String patientIdDisplay,
        String applicationNo,
        String applicationType,
        String submittingDepartmentName,
        String caseStatus,
        LocalDateTime latestActivityAt,
        List<String> matchedActivityTypes
    ) {
    }

    public record SlicingWorkbenchQuery(
        String keyword,
        String applicationType,
        boolean pendingTodayOnly,
        boolean overdueOnly,
        int pendingPage,
        int pendingSize,
        int completedPage,
        int completedSize,
        String currentUserId,
        LocalDateTime dateFrom,
        LocalDateTime dateToExclusive,
        LocalDateTime todayStart,
        LocalDateTime tomorrowStart,
        LocalDateTime dayAfterTomorrowStart,
        LocalDateTime slicingTimedOutBefore
    ) {
    }

    public record SlicingWorkbenchStats(
        long pendingTodayCount,
        long pendingTomorrowCount,
        long completedMineTodayCount,
        long completedDeptTodayCount,
        long overdueCount,
        long pendingPrintCount
    ) {
    }

    public record SlicingWorkbenchRow(
        String taskId,
        String caseId,
        String applicationType,
        String pathologyNo,
        String patientName,
        String patientId,
        String patientIdDisplay,
        String specimenId,
        String specimenName,
        String embeddingBoxId,
        String embeddingBoxNo,
        String slideId,
        String slideNo,
        String slicingOperatorName,
        String slicingRemark,
        LocalDateTime completedAt,
        String grossingEvaluation,
        String embeddingEvaluation,
        String embeddingOperatorName,
        String embeddingClearRemark,
        String embeddingRemarks,
        String shiftRemark,
        String sliceNotice,
        String submittingDepartmentName,
        String taskStatus,
        String slidePrintStatus,
        int printedSlideCount,
        boolean combinedSlide,
        boolean timedOut,
        boolean selectable,
        String printGroupId,
        boolean mergedPrintGroup,
        List<String> taskIds,
        List<String> embeddingBoxIds
    ) {
    }

    public record SlicingSlidePrintMergeGroupItem(
        String groupId,
        String taskId,
        String caseId,
        String pathologyNo,
        String patientId,
        String patientIdDisplay,
        String embeddingBoxId,
        String embeddingBoxNo,
        int sequenceNo
    ) {
    }

    public record PagedSlicingWorkbenchRows(List<SlicingWorkbenchRow> items, long total) {
    }

    public record PendingTechnicalSpecimenRegistrationQuery(
        int page,
        int size,
        String keyword,
        String applicationType,
        String registrationStatus,
        LocalDateTime receivedFrom,
        LocalDateTime receivedTo
    ) {
    }

    public record PagedTechnicalSpecimenRegistrations(
        List<TechnicalSpecimenRegistration> items,
        long total
    ) {
    }

    public record TechnicalSpecimenRegistration(
        String caseId,
        String applicationId,
        String pathologyNo,
        String applicationNo,
        String patientName,
        String patientGender,
        String patientAge,
        String patientId,
        String patientIdDisplay,
        String inpatientNo,
        String applicationType,
        String submittingDepartmentName,
        String checkItem,
        String registrationStatus,
        String registeredByUserId,
        String registeredByName,
        LocalDateTime registeredAt,
        String registrationRemarks,
        LocalDateTime receivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record CaseMediaAsset(
        String id,
        String caseId,
        String specimenId,
        String objectType,
        String objectId,
        String mediaType,
        String fileUrl,
        String fileName,
        LocalDateTime capturedAt,
        String capturedByUserId,
        String capturedByName,
        String remarks
    ) {
    }

    public record TechnicalTask(
        String id,
        String applicationId,
        String applicationNo,
        String patientName,
        String patientId,
        String patientIdDisplay,
        String caseId,
        String pathologyNo,
        String specimenId,
        String taskType,
        String taskStatus,
        String objectType,
        String objectId,
        String objectDisplayNo,
        String samplingBlockCode,
        String samplingBlockDescription,
        String embeddingRemarks,
        String specimenName,
        String grossDescription,
        String sampledByName,
        LocalDateTime sampledAt,
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

    public record GrossingDraft(
        String taskId,
        String caseId,
        String draftPayload,
        String savedByUserId,
        String savedByName,
        LocalDateTime savedAt,
        LocalDateTime updatedAt
    ) {
    }

    public record SaveGrossingDraftCommand(
        String taskId,
        String caseId,
        String draftPayload,
        String savedByUserId,
        String savedByName,
        LocalDateTime savedAt
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
        String specialRequirement,
        String embeddingBoxName,
        String embeddingBoxStatus,
        String embeddingRemarks
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
        String embeddingBoxName,
        String specialRequirement,
        String embeddingRemarks,
        String specimenName,
        String grossDescription
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

    public record WorkstationDailyClearRecord(
        String id,
        String workstationType,
        LocalDate workDate,
        String operatorUserId,
        String operatorName,
        LocalDateTime clearedAt,
        String clearStatus,
        String operatorIp
    ) {
    }

    public record CreateWorkstationDailyClearCommand(
        String id,
        String workstationType,
        LocalDate workDate,
        String operatorUserId,
        String operatorName,
        LocalDateTime clearedAt,
        String clearStatus,
        String operatorIp
    ) {
    }

    public record EmbeddingWorkstationRecord(
        String taskId,
        String caseId,
        String pathologyNo,
        String specimenId,
        String specimenName,
        String samplingBlockId,
        String samplingBlockCode,
        String samplingBlockDescription,
        String grossDescription,
        String embeddingId,
        String embeddingBoxId,
        String embeddingBoxNo,
        String sliceNotice,
        String evaluationLevel,
        String samplingEvaluation,
        String embeddingRemarks,
        String sampledByName,
        LocalDateTime sampledAt,
        String embeddedByName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String taskStatus
    ) {
    }

}
