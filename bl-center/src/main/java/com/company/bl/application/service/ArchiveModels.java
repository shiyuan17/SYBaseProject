package com.company.bl.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public record ArchiveObjectCommand(
        String objectId,
        String archivePositionId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String fileUrl,
        String fileName,
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
        String patientName,
        String objectType,
        String objectId,
        String objectCode,
        String archiveStatus,
        String archiveLocation,
        String loanStatus,
        String archivedAt,
        String storedByName,
        String borrowedByName,
        String borrowedAt
    ) {
    }

    public record CreateMaterialLoanCommand(
        String materialType,
        String materialId,
        String borrowedByUserId,
        String borrowedByName,
        String borrowPurpose,
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
        String patientName,
        String materialType,
        String materialId,
        String objectCode,
        String loanStatus,
        String borrowedByName,
        String borrowedAt,
        String borrowPurpose,
        String approvedByName,
        String returnedByName,
        String returnedAt,
        String remarks
    ) {
    }
}
