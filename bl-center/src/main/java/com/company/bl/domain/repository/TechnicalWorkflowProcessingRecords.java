package com.company.bl.domain.repository;

import java.time.LocalDateTime;

public final class TechnicalWorkflowProcessingRecords {

    private TechnicalWorkflowProcessingRecords() {
    }

    public record CreateSlicingCommand(
        String id,
        String caseId,
        String specimenId,
        String embeddingId,
        String embeddingBoxId,
        String slicingBatchNo,
        String slicingStatus,
        int slideCount,
        Integer sliceCountPerSlide,
        String sliceThickness,
        String slicedByUserId,
        String slicedByName,
        LocalDateTime slicedAt,
        String qualityIssue,
        String remarks
    ) {
    }

    public record Slicing(
        String id,
        String caseId,
        String specimenId,
        String embeddingId,
        String embeddingBoxId,
        String slicingBatchNo,
        String slicingStatus,
        int slideCount
    ) {
    }

    public record CreateSlideCommand(
        String id,
        String caseId,
        String specimenId,
        String slicingId,
        String embeddingBoxId,
        String samplingBlockId,
        String slideNo,
        String slideLabel,
        boolean combinedSlideFlag,
        String qualityStatus,
        String slideStatus,
        Integer sliceCount
    ) {
    }

    public record Slide(
        String id,
        String caseId,
        String specimenId,
        String slicingId,
        String embeddingBoxId,
        String samplingBlockId,
        String slideNo,
        String qualityStatus,
        String slideStatus,
        Integer sliceCount
    ) {
    }

    public record CreateSlideStainingCommand(
        String id,
        String caseId,
        String specimenId,
        String slideId,
        String stainingType,
        String stainingStatus,
        String stainedByUserId,
        String stainedByName,
        LocalDateTime stainedAt,
        String qualityIssue,
        String remarks
    ) {
    }

    public record SlideStaining(
        String id,
        String caseId,
        String specimenId,
        String slideId,
        String stainingType,
        String stainingStatus,
        LocalDateTime stainedAt,
        String qualityIssue,
        String remarks
    ) {
    }

    public record CreateReworkOrderCommand(
        String id,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingBoxId,
        String slideId,
        String reworkType,
        String status,
        String reason,
        String requestedByUserId,
        String requestedByName,
        LocalDateTime requestedAt,
        String remarks
    ) {
    }

    public record ReworkOrder(
        String id,
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingBoxId,
        String slideId,
        String reworkType,
        String status,
        String reason
    ) {
    }

    public record CreateSlideQcEvaluationCommand(
        String id,
        String caseId,
        String specimenId,
        String slideId,
        String qcType,
        String evaluationResult,
        String issueDescription,
        String improvementSuggestion,
        String evaluatorUserId,
        String evaluatorName,
        LocalDateTime evaluatedAt,
        String remarks
    ) {
    }

    public record CreateCaseMediaAssetCommand(
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
}
