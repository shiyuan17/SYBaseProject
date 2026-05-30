package com.company.bl.application.service;

import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.model.Specimen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SpecimenWorkflowModels {

    private SpecimenWorkflowModels() {
    }

    public record RegisterSpecimensCommand(
        String applicationId,
        String printerCode,
        String collectionScene,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        List<SpecimenRegistrationItem> items
    ) {
    }

    public record SpecimenRegistrationItem(
        String specimenNameStandardized,
        String specimenType,
        String specimenSite,
        String collectionMode,
        Integer specimenCount,
        String containerName,
        Integer containerCount,
        String barcode,
        String clinicalSymptom
    ) {
    }

    public record SpecimenRegistrationResult(
        List<Specimen> specimens,
        String labelPrintBatchNo,
        boolean labelPrintSuccess,
        String labelPrintMessage
    ) {
    }

    public record RegistrationSnapshot(
        String collectionScene,
        String operatorUserId,
        String operatorName,
        String printerCode,
        String terminalCode,
        String remarks
    ) {
    }

    public record LatestSpecimenRegistrationResult(
        String applicationId,
        List<Specimen> specimens,
        String labelPrintBatchNo,
        boolean labelPrintSuccess,
        String labelPrintMessage,
        RegistrationSnapshot registrationSnapshot
    ) {
    }

    public record FixationCommand(
        String specimenBarcode,
        String fixationLiquidType,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record FixationResult(
        String specimenId,
        String barcode,
        String fixationStatus,
        LocalDateTime fixationCompletedAt,
        String operatorUserId,
        String operatorName,
        String fixationLiquidType
    ) {
    }

    public record SpecimenVerificationCommand(
        String specimenBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ConfirmSpecimenCommand(
        String specimenBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record CheckInSpecimenCommand(
        String specimenBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record SpecimenVerificationResult(
        String id,
        String specimenNo,
        String barcode,
        String specimenName,
        String specimenType,
        String specimenSite,
        String collectionMode,
        String clinicalSymptom,
        Integer specimenCount,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        String verificationStatus,
        LocalDateTime verificationStartedAt,
        LocalDateTime verificationCompletedAt,
        String labelPrintStatus,
        String receiptStatus,
        String qualityCheckResult,
        String abnormalReason
    ) {
    }

    public record SpecimenVerificationRecord(
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

    public record CreateTransportOrderCommand(
        String applicationId,
        List<String> specimenBarcodes,
        String handoverUserId,
        String handoverUserName,
        String handoverDepartmentId,
        String handoverDepartmentName,
        String receiverDepartmentId,
        String receiverDepartmentName,
        String terminalCode,
        String remarks
    ) {
    }

    public record OperatorCommand(String operatorUserId, String operatorName, String terminalCode) {
    }

    public record HandoverTransportOrderCommand(
        String receiverUserId,
        String receiverUserName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ReceiveSpecimensCommand(
        String transportOrderId,
        String receivedByUserId,
        String receivedByName,
        String terminalCode,
        List<ReceiptItem> items
    ) {
    }

    public record DirectReceiveSpecimensCommand(
        String receivedByUserId,
        String receivedByName,
        String terminalCode,
        List<ReceiptItem> items
    ) {
    }

    public record ReceiptItem(
        String specimenBarcode,
        ReceiptStatus receiptStatus,
        Integer containerCount,
        String qualityCheckResult,
        List<String> qualityIssueCodes,
        String reason,
        String remarks
    ) {
    }

    public record ReceiptResult(
        String caseId,
        String pathologyNo,
        String receiptStatus,
        int unreceivedCount
    ) {
    }

    public record RetryLabelPrintCommand(
        String labelPrintBatchNo,
        String operatorUserId,
        String operatorName,
        String printerCode,
        String terminalCode,
        String remarks
    ) {
    }

    public record LabelPrintRetryResult(
        String labelPrintBatchNo,
        int retriedCount,
        int successCount,
        int failedCount,
        boolean allSuccessful,
        String message
    ) {
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

    public record PendingTransportOrderQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo,
        String departmentId,
        String dateFrom,
        String dateTo,
        String status
    ) {
    }

    public record PendingTransportOrderPage(
        List<PendingTransportOrderItem> items,
        int page,
        int size,
        long total
    ) {
    }

    public record PendingTransportOrderItem(
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
        List<String> specimenBarcodes
    ) {
    }

    public record ApplicationListQuery(
        int page,
        int size,
        String applicationNo,
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
        String patientName,
        String submittingDepartmentId,
        String submittingDepartmentName,
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
        String checkInStatus,
        LocalDateTime checkedInAt,
        String checkedInByName,
        String labelPrintStatus,
        String labelPrintBatchNo,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    public record SpecimenManagementSummary(
        long totalCount,
        long labelPrintedCount,
        long pendingLabelCount,
        long abnormalCount
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

    public record SpecimenRemovalCommand(
        String specimenBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record SpecimenRemovalQuickConfirmCommand(
        String identifierType,
        String identifier,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record SpecimenRemovalResult(
        String specimenId,
        String barcode,
        LocalDateTime specimenRemovalAt,
        String operatorName
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
