package com.company.bl.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface SpecimenWorkflowRepository extends SpecimenWorkflowQueryRepository, SpecimenWorkflowCommandRepository {

    record PendingSpecimenQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo,
        String departmentId,
        String fixationStatus,
        String verificationStatus,
        LocalDateTime dateFrom,
        LocalDateTime dateTo
    ) {
    }

    record PendingSpecimenRow(
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

    record PagedPendingSpecimens(List<PendingSpecimenRow> items, long total) {
    }

    record SpecimenVerificationRecordRow(
        String applicationId,
        String specimenId,
        String barcode,
        String verificationType,
        String result,
        String operatorName,
        String terminalCode,
        String remarks,
        LocalDateTime verifiedAt
    ) {
    }

    record PendingTransportOrderQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo,
        String departmentId,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        String status
    ) {
    }

    record PendingTransportOrderRow(
        String id,
        String transportOrderNo,
        String applicationId,
        String applicationNo,
        String patientName,
        String handoverDepartmentName,
        String receiverDepartmentName,
        String status,
        LocalDateTime toBeTransportedAt,
        LocalDateTime handedOverAt,
        String outboundUserId,
        String outboundUserName
    ) {
    }

    record PagedPendingTransportOrders(List<PendingTransportOrderRow> items, long total) {
    }

    record SpecimenOutboundListQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo
    ) {
    }

    record SpecimenOutboundRow(
        String specimenId,
        String transportOrderId,
        String applicationId,
        String applicationNo,
        String barcode,
        String specimenNo,
        String patientName,
        String patientGender,
        String patientId,
        String inpatientNo,
        String surgeryName,
        String specimenName,
        String specimenStatus,
        String submittingDepartmentId,
        String submittingDepartmentName,
        LocalDateTime registeredAt,
        String registeredByName,
        LocalDateTime outboundAt,
        String outboundUserName
    ) {
    }

    record PagedSpecimenOutbounds(List<SpecimenOutboundRow> items, long total) {
    }

    record ApplicationListQuery(
        int page,
        int size,
        String applicationNo,
        String patientName,
        String submittingDepartmentId,
        String applicationType,
        String applicationFormStatus,
        java.time.LocalDate dateFrom,
        java.time.LocalDate dateTo
    ) {
    }

    record ApplicationListRow(
        String id,
        String applicationNo,
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
        java.time.LocalDate applicationDate,
        java.time.LocalDate submissionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record PagedApplications(List<ApplicationListRow> items, long total) {
    }

    record DuplicateApplicationQuery(
        String patientId,
        String patientName,
        String externalOrderNo,
        java.time.LocalDate applicationDate,
        String applicationType,
        String specimenSite
    ) {
    }

    record DuplicateApplicationRow(
        String id,
        String applicationNo,
        String patientName,
        String specimenSite,
        String status,
        String currentNode,
        java.time.LocalDate applicationDate,
        boolean externalOrderMatched,
        boolean sameDaySiteMatched
    ) {
    }

    record RegistrationSnapshotData(
        String collectionScene,
        String operatorUserId,
        String operatorName,
        String printerCode,
        String terminalCode,
        String remarks
    ) {
    }

    record SpecimenManagementListQuery(
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
        LocalDateTime dateFrom,
        LocalDateTime dateTo
    ) {
    }

    record SpecimenRemovalListQuery(
        int page,
        int size,
        String keyword,
        String applicationNo,
        String departmentId,
        String specimenStatus,
        Boolean abnormalFlag,
        LocalDateTime dateFrom,
        LocalDateTime dateTo
    ) {
    }

    record SpecimenManagementListRow(
        String specimenId,
        String specimenNo,
        String barcode,
        String applicationId,
        String applicationNo,
        String patientId,
        String patientName,
        String patientGender,
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

    record SpecimenRemovalListRow(
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

    record SpecimenRemovalSummary(
        long totalCount,
        long confirmedCount,
        long pendingCount,
        long abnormalCount
    ) {
    }

    record SpecimenManagementSummary(
        long totalCount,
        long labelPrintedCount,
        long pendingLabelCount,
        long abnormalCount,
        long unboundCount
    ) {
    }

    record PagedSpecimenManagementItems(
        List<SpecimenManagementListRow> items,
        long total,
        SpecimenManagementSummary summary
    ) {
    }

    record PagedSpecimenRemovalItems(
        List<SpecimenRemovalListRow> items,
        long total,
        SpecimenRemovalSummary summary
    ) {
    }
}
