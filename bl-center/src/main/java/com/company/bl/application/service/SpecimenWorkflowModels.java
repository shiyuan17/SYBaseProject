package com.company.bl.application.service;

import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.model.Specimen;

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

    public record SpecimenBarcodeBindingCommand(
        String specimenId,
        String targetBarcode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record SpecimenBarcodeUnbindCommand(
        String specimenId,
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
}
