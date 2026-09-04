package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "MedicalOrderQcContextResponse", description = "Medical order slide QC context")
public record MedicalOrderQcContextResponse(
    String orderId,
    String caseId,
    String targetType,
    boolean targetResolved,
    String unlinkedReason,
    List<SlideItem> slides
) {
    public record SlideItem(
        String slideId,
        String slideNo,
        String specimenId,
        String specimenNo,
        String blockId,
        String blockNo,
        String projectName,
        String slideStatus,
        String qualityStatus,
        List<MedicalOrderQcEvaluationResponse> evaluations
    ) {
    }
}
