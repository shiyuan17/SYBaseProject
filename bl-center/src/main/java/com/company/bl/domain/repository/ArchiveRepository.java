package com.company.bl.domain.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArchiveRepository {

    List<ArchiveCabinet> findArchiveCabinets();

    Optional<ArchiveCabinet> findArchiveCabinetById(String cabinetId);

    Optional<ArchiveCabinet> findArchiveCabinetByCode(String cabinetCode);

    void insertArchiveCabinet(CreateArchiveCabinetCommand command);

    void updateArchiveCabinet(UpdateArchiveCabinetCommand command);

    List<ArchivePosition> findAvailableArchivePositions(String cabinetType, String cabinetId);

    List<ArchivePosition> findArchivePositionsByCabinetId(String cabinetId);

    Optional<ArchivePosition> findArchivePositionById(String positionId);

    void insertArchivePosition(CreateArchivePositionCommand command);

    void occupyArchivePosition(String positionId, String objectType, String objectId, String remarks, LocalDateTime updatedAt);

    void releaseArchivePosition(String positionId, String remarks, LocalDateTime updatedAt);

    Optional<StorageRecord> findStorageRecord(String objectType, String objectId);

    void insertStorageRecord(CreateStorageRecordCommand command);

    void updateStorageRecord(UpdateStorageRecordCommand command);

    List<ArchiveRecordView> searchArchiveRecords(SearchArchiveRecordsQuery query);

    Optional<MaterialLoan> findMaterialLoanById(String loanId);

    List<MaterialLoan> findPendingMaterialLoans(String keyword, String materialType);

    void insertMaterialLoan(CreateMaterialLoanCommand command);

    void updateMaterialLoanReturned(UpdateMaterialLoanReturnedCommand command);

    Optional<ApplicationArchiveSummary> findApplicationArchiveSummary(String caseId, String applicationId);

    List<ObjectArchiveSummary> findEmbeddingBoxArchiveSummaries(String caseId);

    List<ObjectArchiveSummary> findSlideArchiveSummaries(String caseId);

    record ArchiveCabinet(
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

    record CreateArchiveCabinetCommand(
        String id,
        String cabinetCode,
        String cabinetName,
        String cabinetType,
        int layerCount,
        int slotCountPerLayer,
        int capacity,
        String cabinetStatus,
        String locationDescription,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateArchiveCabinetCommand(
        String id,
        String cabinetName,
        String cabinetStatus,
        String locationDescription,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }

    record ArchivePosition(
        String id,
        String cabinetId,
        String positionCode,
        int layerNo,
        int slotNo,
        String positionStatus,
        String currentObjectType,
        String currentObjectId,
        String remarks
    ) {
    }

    record CreateArchivePositionCommand(
        String id,
        String cabinetId,
        String positionCode,
        int layerNo,
        int slotNo,
        String positionStatus,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record StorageRecord(
        String id,
        String caseId,
        String specimenId,
        String objectType,
        String objectId,
        String storageStatus,
        String storageLocation,
        String archivePositionId,
        String cabinetNo,
        String layerNo,
        String slotNo,
        String storedByUserId,
        String storedByName,
        LocalDateTime storedAt,
        String remarks
    ) {
    }

    record CreateStorageRecordCommand(
        String id,
        String caseId,
        String specimenId,
        String objectType,
        String objectId,
        String storageStatus,
        String storageLocation,
        String archivePositionId,
        String cabinetNo,
        String layerNo,
        String slotNo,
        String storedByUserId,
        String storedByName,
        LocalDateTime storedAt,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateStorageRecordCommand(
        String id,
        String storageStatus,
        String storageLocation,
        String archivePositionId,
        String cabinetNo,
        String layerNo,
        String slotNo,
        String storedByUserId,
        String storedByName,
        LocalDateTime storedAt,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }

    record SearchArchiveRecordsQuery(
        String keyword,
        String objectType,
        String caseId
    ) {
    }

    record ArchiveRecordView(
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
        LocalDateTime archivedAt,
        String storedByName,
        String borrowedByName,
        LocalDateTime borrowedAt
    ) {
    }

    record MaterialLoan(
        String id,
        String caseId,
        String specimenId,
        String materialType,
        String materialId,
        String archivePositionId,
        String loanStatus,
        String borrowedByUserId,
        String borrowedByName,
        LocalDateTime borrowedAt,
        String borrowPurpose,
        String approvedByUserId,
        String approvedByName,
        String returnedByUserId,
        String returnedByName,
        LocalDateTime returnedAt,
        String remarks,
        String objectCode,
        String pathologyNo,
        String applicationNo,
        String patientName
    ) {
    }

    record CreateMaterialLoanCommand(
        String id,
        String caseId,
        String specimenId,
        String materialType,
        String materialId,
        String archivePositionId,
        String loanStatus,
        String borrowedByUserId,
        String borrowedByName,
        LocalDateTime borrowedAt,
        String borrowPurpose,
        String approvedByUserId,
        String approvedByName,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateMaterialLoanReturnedCommand(
        String id,
        String loanStatus,
        String returnedByUserId,
        String returnedByName,
        LocalDateTime returnedAt,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }

    record ApplicationArchiveSummary(
        String archiveStatus,
        String archiveLocation,
        String imageUrl
    ) {
    }

    record ObjectArchiveSummary(
        String objectId,
        String archiveStatus,
        String archiveLocation,
        String loanStatus
    ) {
    }
}
