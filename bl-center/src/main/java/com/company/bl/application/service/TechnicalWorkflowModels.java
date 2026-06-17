package com.company.bl.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        boolean includeAllStatuses
    ) {
    }

    public record PendingTechnicalTaskPage(List<TaskView> items, int page, int size, long total) {
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
        LocalDate dateFrom,
        LocalDate dateTo,
        LocalDate workDate
    ) {
    }

    public record LocalDateRange(
        LocalDate dateFrom,
        LocalDate dateTo
    ) {
    }

    public record SlicingWorkbenchView(
        SlicingWorkbenchStats stats,
        List<SlicingWorkbenchRow> pendingList,
        List<SlicingWorkbenchRow> pendingPrintList,
        List<SlicingWorkbenchRow> pendingSliceList,
        int pendingPage,
        int pendingSize,
        long pendingTotal,
        long pendingPrintTotal,
        long pendingSliceTotal,
        List<SlicingWorkbenchRow> completedTodayList,
        int completedPage,
        int completedSize,
        long completedTotal
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
        String specimenId,
        String specimenName,
        String embeddingBoxId,
        String embeddingBoxNo,
        String slideId,
        String slideNo,
        String slicingOperatorName,
        String slicingRemark,
        String completedAt,
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

    public record PendingTechnicalSpecimenRegistrationPage(
        List<PendingTechnicalSpecimenRegistrationItem> items,
        int page,
        int size,
        long total
    ) {
    }

    public record PendingTechnicalSpecimenRegistrationItem(
        String caseId,
        String applicationId,
        String applicationNo,
        String pathologyNo,
        String patientName,
        String patientGender,
        String patientAge,
        String patientId,
        String inpatientNo,
        String applicationType,
        String submittingDepartmentName,
        String checkItem,
        String registeredByName,
        String registrationStatus,
        String receivedAt,
        String registeredAt
    ) {
    }

    public record TechnicalSpecimenRegistrationMaterial(
        String specimenId,
        String specimenBarcode,
        int sequenceNo,
        String specimenType,
        String specimenName,
        String sourcePart,
        int tissueCount,
        String specimenSize,
        boolean frozen,
        List<String> evaluationItems,
        String verificationStatus,
        String verificationCompletedAt,
        String verifiedByName
    ) {
    }

    public record TechnicalSpecimenRegistrationBasicInfo(
        String patientName,
        String patientGender,
        String patientAge,
        String patientId,
        String inpatientNo,
        String applicationNo,
        String submittingDepartmentName,
        String submittingDoctorName,
        String submissionDate,
        String specimenRemovalTime,
        String fixationTime,
        String applicationType,
        String pathologyNo,
        String registrationStatus
    ) {
    }

    public record TechnicalSpecimenRegistrationDetailSections(
        String historySummary,
        String clinicalExaminationAndSurgeryFindings,
        String labAndImagingExaminations,
        String clinicalSubmissionRequirements,
        String infectiousAndPastHistorySummary,
        String externalPathologyDiagnosis
    ) {
    }

    public record TechnicalSpecimenRegistrationMediaAsset(
        String assetId,
        String fileName,
        String fileUrl,
        String capturedAt
    ) {
    }

    public record TechnicalSpecimenRegistrationActionFlags(
        boolean canCompleteRegistration,
        boolean canSaveDetailSections,
        boolean canSaveMaterials,
        boolean canUploadMediaAssets,
        boolean canDeleteMediaAssets
    ) {
    }

    public record TechnicalSpecimenRegistrationWorkspace(
        PendingTechnicalSpecimenRegistrationItem pendingSummary,
        TechnicalSpecimenRegistrationBasicInfo basicInfo,
        TechnicalSpecimenRegistrationDetailSections detailSections,
        List<TechnicalSpecimenRegistrationMaterial> materials,
        List<TechnicalSpecimenRegistrationCheckItem> checkItems,
        List<TechnicalSpecimenRegistrationMediaAsset> mediaAssets,
        TechnicalSpecimenRegistrationActionFlags actionFlags
    ) {
    }

    public record TechnicalSpecimenRegistrationCheckItem(
        int sequenceNo,
        String name
    ) {
    }

    public record TechnicalSpecimenRegistrationDetail(
        String caseId,
        String applicationId,
        String applicationNo,
        String pathologyNo,
        String patientName,
        String patientId,
        String inpatientNo,
        String applicationType,
        String submittingDepartmentName,
        String clinicalDiagnosis,
        String registrationStatus,
        String registeredByName,
        String registeredAt,
        String registrationRemarks,
        String receivedAt,
        List<TechnicalSpecimenRegistrationMaterial> materials,
        List<TechnicalSpecimenRegistrationCheckItem> checkItems
    ) {
    }

    public record TaskView(
        String id,
        String applicationId,
        String applicationNo,
        String patientName,
        String patientId,
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
        String sampledByName,
        String sampledAt,
        String payload,
        String priority,
        String currentNode,
        String stationCode,
        String stationName,
        String assignedToUserId,
        String assignedToName,
        String expectedCompletedAt,
        String productionRemarks,
        String receivedAt,
        String remarks,
        String createdAt,
        String startedAt,
        String completedAt,
        String deadlineAt,
        String timeoutRuleCode,
        boolean timedOut
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

    public record TechnicalTaskAssignCommand(
        String taskId,
        String priority,
        String stationCode,
        String stationName,
        String assignedToUserId,
        String assignedToName,
        LocalDateTime expectedCompletedAt,
        String productionRemarks,
        String operatorUserId,
        String operatorName,
        String terminalCode
    ) implements OperatorCarrier {
        @Override
        public String remarks() {
            return productionRemarks;
        }
    }

    public record TechnicalTaskClaimCommand(
        String taskId,
        String assignedToUserId,
        String assignedToName,
        String stationCode,
        String stationName,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record TechnicalTaskReleaseCommand(
        String taskId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record TechnicalTaskPriorityCommand(
        String taskId,
        String priority,
        String productionRemarks,
        String operatorUserId,
        String operatorName,
        String terminalCode
    ) implements OperatorCarrier {
        @Override
        public String remarks() {
            return productionRemarks;
        }
    }

    public record TechnicalTaskRemarksCommand(
        String taskId,
        String remarks,
        String productionRemarks,
        String operatorUserId,
        String operatorName,
        String terminalCode
    ) implements OperatorCarrier {
    }

    public record CompleteTechnicalSpecimenRegistrationCommand(
        String caseId,
        String operatorUserId,
        String operatorName,
        String applicationType,
        String pathologyNo,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record TechnicalSpecimenRegistrationCompleteResult(
        String caseId,
        String pathologyNo,
        String registrationStatus,
        boolean grossingTaskCreated
    ) {
    }

    public record SaveTechnicalSpecimenRegistrationMaterialsCommand(
        String caseId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        List<TechnicalSpecimenRegistrationMaterialInput> materials
    ) implements OperatorCarrier {
        @Override
        public String remarks() {
            return null;
        }
    }

    public record TechnicalSpecimenRegistrationMaterialInput(
        String specimenId,
        String specimenType,
        String specimenName,
        String sourcePart,
        Integer tissueCount,
        String specimenSize,
        Boolean frozen,
        List<String> evaluationItems
    ) {
    }

    public record TechnicalSpecimenRegistrationMaterialVerificationCommand(
        String caseId,
        String specimenId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SaveTechnicalSpecimenRegistrationDetailSectionsCommand(
        String caseId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        TechnicalSpecimenRegistrationDetailSections detailSections
    ) implements OperatorCarrier {
        @Override
        public String remarks() {
            return null;
        }
    }

    public record UploadTechnicalSpecimenRegistrationMediaAssetCommand(
        String caseId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String fileName,
        String fileUrl
    ) implements OperatorCarrier {
        @Override
        public String remarks() {
            return null;
        }
    }

    public record DeleteTechnicalSpecimenRegistrationMediaAssetCommand(
        String caseId,
        String assetId,
        String operatorUserId,
        String operatorName,
        String terminalCode
    ) implements OperatorCarrier {
        @Override
        public String remarks() {
            return null;
        }
    }

    public record MediaAssetInput(String fileUrl, String fileName) {
    }

    public record GrossingBlockItem(String blockSite, String blockDescription, String specialRequirement) {
    }

    public record GrossingEmbeddingBoxItem(
        Integer sequenceNo,
        String boxName,
        String embeddingBoxNo,
        String status,
        String embeddingRemarks
    ) {
    }

    public record GrossingSpecimenItem(
        String specimenId,
        String specimenType,
        String bodyPartId,
        String samplingTemplateId,
        String sizeText,
        String cutSurfaceFeature,
        String marginMarking,
        Integer blockCount,
        String grossDescription,
        List<GrossingBlockItem> blocks,
        List<MediaAssetInput> mediaAssets,
        List<GrossingEmbeddingBoxItem> embeddingBoxes
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

    public record GrossingWorkbenchTaskSummary(
        String taskId,
        String taskStatus,
        String objectType,
        String objectId
    ) {
    }

    public record GrossingWorkbenchCaseSummary(
        String caseId,
        String applicationId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        String patientId,
        String inpatientNo,
        String applicationType,
        String submittingDepartmentName
    ) {
    }

    public record GrossingWorkbenchMediaAsset(
        String assetId,
        String specimenId,
        String fileName,
        String fileUrl,
        String capturedAt,
        String capturedByName
    ) {
    }

    public record GrossingWorkbenchContext(
        GrossingWorkbenchTaskSummary task,
        GrossingWorkbenchCaseSummary caseSummary,
        TechnicalTrackingView tracking,
        String clinicalDiagnosis,
        String clinicalHistory,
        String relatedExaminations,
        String contextSummary,
        String clinicalSubmissionRequirements,
        String infectiousAndPastHistorySummary,
        String externalPathologyDiagnosis,
        List<TechnicalSpecimenRegistrationCheckItem> checkItems,
        List<GrossingWorkbenchMediaAsset> mediaAssets
    ) {
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

    public record EmbeddingQualityReviewCommand(
        String embeddingId,
        String sliceNotice,
        String evaluationLevel,
        String samplingEvaluation,
        List<String> unqualifiedReasons,
        String treatmentAction,
        String treatmentRemark,
        boolean notifiedGrossingOperator,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record EmbeddingQualityReviewResult(
        TechnicalEmbeddingRecord record,
        String reworkType,
        String reworkStatus
    ) {
    }

    public record EmbeddingWorkstationSummary(
        LocalDate workDate,
        int pendingCount,
        int completedCount,
        List<TaskView> pendingTasks,
        List<TechnicalEmbeddingRecord> completedRecords
    ) {
    }

    public record SlicingCompleteCommand(
        String taskId,
        String embeddingBoxId,
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

    public record SlicingSlidePrintCommand(
        String taskId,
        String embeddingBoxId,
        int sourceSlideCount,
        boolean mergeAdjacent,
        String printerCode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlicingSlidePrintResult(
        String taskId,
        String slicingId,
        List<String> slideIds,
        List<String> slideNos,
        boolean merged,
        int printedSlideCount
    ) {
    }

    public record SlicingSlidePrintMergeGroupCommand(
        List<String> taskIds,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlicingSlidePrintMergeGroupCancelCommand(
        List<String> printGroupIds,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlicingSlidePrintMergeGroupPrintCommand(
        String printGroupId,
        String printerCode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlicingSlidePrintMergeGroupResult(List<String> printGroupIds) {
    }

    public record CreateSlideQcEvaluationCommand(
        String caseId,
        String specimenId,
        String slideId,
        String qcType,
        String evaluationResult,
        String issueDescription,
        String improvementSuggestion,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlideQcEvaluationResult(
        String qcEvaluationId,
        String slideId,
        String evaluationResult,
        String qualityStatus
    ) {
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
        List<TechnicalEmbeddingRecord> embeddingRecords,
        List<TechnicalEmbeddingEvaluationRecord> embeddingEvaluationRecords,
        List<TechnicalSlideSummary> slides,
        List<SlideQcEvaluationSummary> qcEvaluations,
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
        String description,
        String specimenName,
        String grossDescription
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

    public record TechnicalEmbeddingRecord(
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
        String sampledAt,
        String embeddedByName,
        String startedAt,
        String endedAt,
        String taskStatus
    ) {
    }

    public record TechnicalEmbeddingEvaluationRecord(
        String embeddingId,
        String caseId,
        String pathologyNo,
        String specimenId,
        String specimenName,
        String samplingBlockId,
        String samplingBlockCode,
        String embeddingBoxNo,
        String evaluationLevel,
        String samplingEvaluation,
        String embeddingRemarks,
        String embeddedByName,
        String endedAt
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

    public record SlideQcEvaluationSummary(
        String qcEvaluationId,
        String specimenId,
        String slideId,
        String slideNo,
        String qcType,
        String evaluationResult,
        String issueDescription,
        String improvementSuggestion,
        String evaluatorName,
        String evaluatedAt,
        String remarks
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
