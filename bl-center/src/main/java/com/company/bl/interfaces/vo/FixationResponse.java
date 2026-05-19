package com.company.bl.interfaces.vo;

public record FixationResponse(
    String specimenId,
    String barcode,
    String fixationStatus
) {
}
