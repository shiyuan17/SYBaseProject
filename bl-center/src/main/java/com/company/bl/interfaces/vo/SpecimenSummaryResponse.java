package com.company.bl.interfaces.vo;

public record SpecimenSummaryResponse(
    String id,
    String specimenNo,
    String barcode,
    String specimenName,
    String specimenType,
    String specimenSite,
    Integer specimenCount,
    String specimenStatus,
    String fixationStatus,
    String labelPrintStatus
) {
}
