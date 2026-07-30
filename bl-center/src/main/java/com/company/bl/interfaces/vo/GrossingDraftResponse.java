package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "GrossingDraftResponse", description = "取材整单草稿")
public record GrossingDraftResponse(
    String taskId,
    String caseId,
    String terminalCode,
    String remarks,
    String savedAt,
    List<SpecimenItem> specimens
) {
    public record SpecimenItem(
        String specimenId,
        String specimenType,
        String bodyPartId,
        String samplingTemplateId,
        String sizeText,
        String cutSurfaceFeature,
        String marginMarking,
        Integer blockCount,
        String grossDescription,
        List<BlockItem> blocks,
        List<MediaAssetItem> mediaAssets,
        List<EmbeddingBoxItem> embeddingBoxes
    ) {
    }

    public record BlockItem(String blockSite, String blockDescription, String specialRequirement) {
    }

    public record EmbeddingBoxItem(
        Integer sequenceNo,
        String boxName,
        String embeddingBoxNo,
        String status,
        String embeddingRemarks
    ) {
    }

    public record MediaAssetItem(String fileUrl, String fileName) {
    }
}
