package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SpecimenManagementItemResponse", description = "Specimen management list item")
public record SpecimenManagementItemResponse(
    @Schema(description = "Specimen id")
    String specimenId,
    @Schema(description = "Specimen number")
    String specimenNo,
    @Schema(description = "Specimen barcode")
    String barcode,
    @Schema(description = "Application id")
    String applicationId,
    @Schema(description = "Application number")
    String applicationNo,
    @Schema(description = "Patient name")
    String patientName,
    @Schema(description = "Submitting department id")
    String submittingDepartmentId,
    @Schema(description = "Submitting department name")
    String submittingDepartmentName,
    @Schema(description = "Specimen name")
    String specimenName,
    @Schema(description = "Specimen type")
    String specimenType,
    @Schema(description = "Specimen site")
    String specimenSite,
    @Schema(description = "Specimen count")
    Integer specimenCount,
    @Schema(description = "Container name")
    String containerName,
    @Schema(description = "Container count")
    Integer containerCount,
    @Schema(description = "Specimen status")
    String specimenStatus,
    @Schema(description = "Fixation status")
    String fixationStatus,
    @Schema(description = "Verification status")
    String verificationStatus,
    @Schema(description = "Barcode binding status")
    String barcodeBindingStatus,
    @Schema(description = "Label print status")
    String labelPrintStatus,
    @Schema(description = "Label print batch number")
    String labelPrintBatchNo,
    @Schema(description = "Abnormal type")
    String abnormalType,
    @Schema(description = "Recent node")
    String recentNode,
    @Schema(description = "Registration time")
    String registeredAt,
    @Schema(description = "Latest tracking time")
    String latestTrackingAt,
    @Schema(description = "Whether the specimen is abnormal")
    boolean abnormalFlag
) {
}
