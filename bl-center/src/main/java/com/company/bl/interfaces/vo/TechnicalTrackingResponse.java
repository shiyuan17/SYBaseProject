package com.company.bl.interfaces.vo;

import java.util.List;

public record TechnicalTrackingResponse(
    String caseId,
    String pathologyNo,
    String caseStatus,
    List<PendingTechnicalTaskResponse> technicalTasks,
    List<SpecimenSummary> specimens,
    List<BlockSummary> blocks,
    List<EmbeddingBoxSummary> embeddingBoxes,
    List<SlideSummary> slides,
    List<ReworkSummary> reworks,
    List<EventSummary> events
) {
    public record SpecimenSummary(
        String specimenId,
        String specimenNo,
        String barcode,
        String specimenName,
        String specimenStatus
    ) {
    }

    public record BlockSummary(
        String blockId,
        String specimenId,
        String blockCode,
        String embeddingBoxNo,
        String description
    ) {
    }

    public record EmbeddingBoxSummary(
        String embeddingBoxId,
        String specimenId,
        String embeddingBoxNo,
        String sliceNotice,
        int slideCount
    ) {
    }

    public record SlideSummary(
        String slideId,
        String specimenId,
        String embeddingBoxId,
        String slideNo,
        String slideStatus,
        String qualityStatus
    ) {
    }

    public record ReworkSummary(
        String reworkOrderId,
        String reworkType,
        String status,
        String reason
    ) {
    }

    public record EventSummary(
        String nodeCode,
        String eventType,
        String eventStatus,
        String eventTime,
        String operatorName,
        String eventContent
    ) {
    }
}
