package com.company.bl.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SpecimenWorkflowQueryModels {

    private SpecimenWorkflowQueryModels() {
    }

    public record PendingSpecimenQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo,
        String departmentId,
        String fixationStatus,
        String verificationStatus,
        String dateFrom,
        String dateTo
    ) {
    }

    public record PendingSpecimenPage(
        List<PendingSpecimenItem> items,
        int page,
        int size,
        long total
    ) {
    }

    public record PendingSpecimenItem(
        String applicationId,
        String applicationNo,
        String patientName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String transportOrderId,
        String specimenId,
        String specimenNo,
        String barcode,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        LocalDateTime fixationStartedAt,
        LocalDateTime fixationCompletedAt,
        String fixationLiquidType,
        String fixationOperatorUserId,
        String fixationOperatorName,
        String verificationStatus,
        LocalDateTime verificationStartedAt,
        LocalDateTime verificationCompletedAt,
        LocalDateTime specimenConfirmedAt,
        String checkInStatus,
        LocalDateTime checkedInAt,
        String checkedInByName,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    public record ApplicationListQuery(
        int page,
        int size,
        String applicationNo,
        String pathologyNo,
        String patientName,
        String submittingDepartmentId,
        String applicationType,
        String applicationFormStatus,
        String dateFrom,
        String dateTo
    ) {
    }

    public record ApplicationPage(
        List<ApplicationListItem> items,
        int page,
        int size,
        long total
    ) {
    }

    public record DuplicateCheckCommand(
        String patientId,
        String patientName,
        String externalOrderNo,
        String applicationDate,
        String applicationType,
        String specimenSite
    ) {
    }

    public record DuplicateCheckItem(
        String id,
        String applicationNo,
        String patientName,
        LocalDate applicationDate,
        String specimenSite,
        String status,
        String currentNode,
        List<String> matchedBy
    ) {
    }

    public record DuplicateCheckResult(
        List<DuplicateCheckItem> items,
        String suggestedAction
    ) {
    }

    public record ApplicationListItem(
        String id,
        String applicationNo,
        String pathologyNo,
        String patientName,
        String patientGender,
        String patientAge,
        String status,
        String submittingDepartmentName,
        String submittingDoctorName,
        String applicationType,
        String applicationFormStatus,
        String currentNode,
        boolean abnormalFlag,
        int registeredSpecimenCount,
        String latestLabelPrintStatus,
        boolean editable,
        boolean deletable,
        boolean voided,
        String operationDisabledReason,
        LocalDate applicationDate,
        LocalDate submissionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record ApplicationOperationState(
        boolean editable,
        boolean deletable,
        boolean voided,
        String disabledReason
    ) {
    }

    public record SpecimenManagementListQuery(
        int page,
        int size,
        String keyword,
        String applicationNo,
        String departmentId,
        String buildingId,
        String roomId,
        String barcodeBindingStatus,
        String specimenStatus,
        String labelPrintStatus,
        Boolean abnormalFlag,
        String dateFrom,
        String dateTo
    ) {
    }

    public record SpecimenManagementListPage(
        List<SpecimenManagementListItem> items,
        int page,
        int size,
        long total,
        SpecimenManagementSummary summary
    ) {
    }

    public record SpecimenManagementListItem(
        String specimenId,
        String specimenNo,
        String barcode,
        String applicationId,
        String applicationNo,
        String patientId,
        String patientIdDisplay,
        String patientName,
        String patientGender,
        String inpatientNo,
        String wardName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String buildingId,
        String roomId,
        String surgeryName,
        String specimenName,
        String specimenType,
        String specimenSite,
        Integer specimenCount,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        LocalDateTime fixationStartedAt,
        LocalDateTime fixationCompletedAt,
        String fixationLiquidType,
        String fixationOperatorUserId,
        String fixationOperatorName,
        String verificationStatus,
        LocalDateTime specimenConfirmedAt,
        String specimenConfirmedByUserId,
        String specimenConfirmedByName,
        LocalDateTime specimenRemovalAt,
        String specimenRemovalOperatorName,
        String checkInStatus,
        LocalDateTime checkedInAt,
        String checkedInByName,
        String labelPrintStatus,
        String labelPrintBatchNo,
        String registrationOperatorName,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    public record SpecimenManagementSummary(
        long totalCount,
        long labelPrintedCount,
        long pendingLabelCount,
        long abnormalCount,
        long unboundCount
    ) {
    }

    public record SpecimenRemovalQuery(
        int page,
        int size,
        String keyword,
        String applicationNo,
        String departmentId,
        String specimenStatus,
        Boolean abnormalFlag,
        String dateFrom,
        String dateTo
    ) {
    }

    public record SpecimenRemovalListPage(
        List<SpecimenRemovalListItem> items,
        int page,
        int size,
        long total,
        SpecimenRemovalSummary summary
    ) {
    }

    public record SpecimenRemovalListItem(
        String specimenId,
        String specimenNo,
        String barcode,
        String applicationId,
        String applicationNo,
        String patientName,
        String patientGender,
        String inpatientNo,
        String surgeryName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String specimenName,
        String specimenType,
        Integer specimenCount,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        String verificationStatus,
        LocalDateTime specimenRemovalAt,
        String specimenRemovalOperatorName,
        LocalDateTime registeredAt,
        String labelPrintBatchNo,
        String registeredByName,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    public record SpecimenRemovalSummary(
        long totalCount,
        long confirmedCount,
        long pendingCount,
        long abnormalCount
    ) {
    }
}
