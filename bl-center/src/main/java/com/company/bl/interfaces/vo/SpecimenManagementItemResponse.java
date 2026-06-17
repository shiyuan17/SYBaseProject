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
    @Schema(description = "Patient id")
    String patientId,
    @Schema(description = "Patient id display value from registration workbench id_no")
    String patientIdDisplay,
    @Schema(description = "Patient name")
    String patientName,
    @Schema(description = "Patient gender")
    String patientGender,
    @Schema(description = "Inpatient number")
    String inpatientNo,
    @Schema(description = "Ward name")
    String wardName,
    @Schema(description = "Submitting department id")
    String submittingDepartmentId,
    @Schema(description = "Submitting department name")
    String submittingDepartmentName,
    @Schema(description = "Operating building id")
    String buildingId,
    @Schema(description = "Operating room id")
    String roomId,
    @Schema(description = "Surgery display name")
    String surgeryName,
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
    @Schema(description = "Fixation start at")
    String fixationStartedAt,
    @Schema(description = "Fixation completed at")
    String fixationCompletedAt,
    @Schema(description = "Fixation liquid type")
    String fixationLiquidType,
    @Schema(description = "Fixation operator user id")
    String fixationOperatorUserId,
    @Schema(description = "Fixation operator name")
    String fixationOperatorName,
    @Schema(description = "Verification status")
    String verificationStatus,
    @Schema(description = "Specimen confirmed at")
    String specimenConfirmedAt,
    @Schema(description = "Specimen confirmed by user id")
    String specimenConfirmedByUserId,
    @Schema(description = "Specimen confirmed by name")
    String specimenConfirmedByName,
    @Schema(description = "Specimen removal confirmed at")
    String specimenRemovalAt,
    @Schema(description = "Specimen removal operator name")
    String specimenRemovalOperatorName,
    @Schema(description = "Check-in status")
    String checkInStatus,
    @Schema(description = "Checked-in at")
    String checkedInAt,
    @Schema(description = "Checked-in by name")
    String checkedInByName,
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
    @Schema(description = "Registration operator name")
    String registrationOperatorName,
    @Schema(description = "Registration time")
    String registeredAt,
    @Schema(description = "Latest tracking time")
    String latestTrackingAt,
    @Schema(description = "Whether the specimen is abnormal")
    boolean abnormalFlag
) {
}
