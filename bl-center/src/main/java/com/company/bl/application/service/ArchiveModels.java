package com.company.bl.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ArchiveModels {

    private ArchiveModels() {
    }

    public record ArchiveCabinetView(
        String id,
        String cabinetCode,
        String cabinetName,
        String cabinetType,
        int layerCount,
        int slotCountPerLayer,
        int capacity,
        String cabinetStatus,
        String locationDescription,
        String remarks
    ) {
    }

    public record ArchivePositionView(
        String id,
        String cabinetId,
        String positionCode,
        int layerNo,
        int slotNo,
        String positionStatus
    ) {
    }

    public record ArchiveCabinetNodeView(
        String id,
        String parentId,
        String nodeCode,
        String nodeType,
        String cabinetType,
        String cabinetId,
        Integer layerNo,
        int capacity,
        int remainingCapacity,
        String pathLocation,
        String remarks
    ) {
    }

    public record CreateArchiveCabinetCommand(
        String cabinetCode,
        String cabinetName,
        String cabinetType,
        int layerCount,
        int slotCountPerLayer,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String locationDescription,
        String remarks
    ) {
    }

    public record BatchCreateArchiveCabinetCommand(
        String parentId,
        String cabinetType,
        String cabinetCodePrefix,
        int startNo,
        int count,
        int numberWidth,
        String cabinetNamePrefix,
        int layerCount,
        int slotCountPerLayer,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String locationDescription,
        String remarks
    ) {
    }

    public record CreateArchiveCabinetNodeCommand(
        String parentId,
        String nodeCode,
        String nodeType,
        String cabinetType,
        int capacity,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String pathLocation,
        String remarks
    ) {
    }

    public record UpdateArchiveCabinetCommand(
        String cabinetId,
        String cabinetName,
        String cabinetStatus,
        String locationDescription,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record UpdateArchiveCabinetNodeCommand(
        String nodeId,
        String nodeCode,
        String cabinetType,
        int capacity,
        String pathLocation,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ArchiveObjectCommand(
        String objectId,
        String archivePositionId,
        String archiveCabinetId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String fileUrl,
        String fileName,
        LocalDateTime archiveExpiresAt,
        Integer archiveReminderDays,
        String remarks
    ) {
    }

    public record BatchArchiveObjectCommand(
        String archiveCabinetId,
        List<String> objectIds,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        LocalDateTime archiveExpiresAt,
        Integer archiveReminderDays,
        String remarks
    ) {
    }

    public record ArchiveActionResult(
        String caseId,
        String objectType,
        String objectId,
        String archiveStatus,
        String archiveLocation
    ) {
    }

    public record SearchArchiveRecordsQuery(
        String keyword,
        String objectType,
        String caseId
    ) {
    }

    public record ArchiveRecordView(
        String caseId,
        String pathologyNo,
        String applicationNo,
        String patientId,
        String patientIdDisplay,
        String patientName,
        String patientGender,
        String inpatientNo,
        String wardName,
        String applicantDoctorName,
        String applicationDate,
        String objectType,
        String objectId,
        String objectCode,
        String archiveStatus,
        String archiveLocation,
        String loanStatus,
        String archivedAt,
        String storedByName,
        String borrowedByName,
        String borrowedAt,
        String objectStatus,
        String sampledByName,
        String sampledAt,
        String slicedByName,
        String slicedAt,
        String contentDescribedByName,
        String archiveExpiresAt,
        Integer archiveReminderDays
    ) {
    }

    public record SearchArchiveObjectsQuery(
        String keyword,
        String objectType,
        int page,
        int size
    ) {
    }

    public record ArchiveObjectPage(
        List<ArchiveRecordView> items,
        int page,
        int size,
        long total
    ) {
    }

    public record CreateMaterialLoanCommand(
        String materialType,
        String materialId,
        String borrowedByUserId,
        String borrowedByName,
        String borrowerPhone,
        String borrowerUnit,
        String borrowPurpose,
        BigDecimal depositAmount,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ReturnMaterialLoanCommand(
        String loanId,
        String archivePositionId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record MaterialLoanView(
        String loanId,
        String caseId,
        String pathologyNo,
        String applicationNo,
        String patientId,
        String patientIdDisplay,
        String patientName,
        String patientGender,
        String inpatientNo,
        String wardName,
        String materialType,
        String materialId,
        String objectCode,
        String loanStatus,
        String borrowedByName,
        String borrowedAt,
        String borrowerPhone,
        String borrowerUnit,
        String borrowPurpose,
        BigDecimal depositAmount,
        String approvedByName,
        String returnedByName,
        String returnedAt,
        String remarks
    ) {
    }

    public record CreateMaterialLoanAbnormalRecordCommand(
        String materialType,
        String materialId,
        String loanId,
        String abnormalReason,
        Boolean contacted,
        String contactResult,
        String borrowedSlideNo,
        String borrowerName,
        String borrowerRelationship,
        String borrowerPhone,
        String borrowerUnit,
        String borrowerIdentityNo,
        LocalDateTime borrowedAt,
        LocalDateTime expectedReturnAt,
        Integer slideCount,
        BigDecimal depositAmount,
        String borrowedContent,
        String returnAbnormalInfo,
        String operatorUserId,
        String operatorName,
        String terminalCode
    ) {
    }

    public record MaterialLoanAbnormalRecordView(
        String id,
        String caseId,
        String materialType,
        String materialId,
        String loanId,
        String abnormalReason,
        Boolean contacted,
        String contactResult,
        String registeredByName,
        String registeredAt
    ) {
    }
}
