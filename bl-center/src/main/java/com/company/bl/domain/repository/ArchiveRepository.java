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

    boolean existsArchiveCabinetByCodes(List<String> cabinetCodes);

    List<ArchiveCabinetNode> findArchiveCabinetNodes();

    Optional<ArchiveCabinetNode> findArchiveCabinetNodeById(String nodeId);

    Optional<ArchiveCabinetNode> findArchiveCabinetNodeByCabinetIdAndType(String cabinetId, String nodeType);

    Optional<ArchiveCabinetNode> findArchiveCabinetNodeByCabinetIdAndLayerNo(String cabinetId, int layerNo);

    void insertArchiveCabinet(CreateArchiveCabinetCommand command);

    void updateArchiveCabinet(UpdateArchiveCabinetCommand command);

    void updateArchiveCabinetCapacity(UpdateArchiveCabinetCapacityCommand command);

    void insertArchiveCabinetNode(CreateArchiveCabinetNodeCommand command);

    void updateArchiveCabinetNode(UpdateArchiveCabinetNodeCommand command);

    void updateArchiveCabinetNodeCapacity(UpdateArchiveCabinetNodeCapacityCommand command);

    boolean hasNonEmptyArchivePositions(String cabinetId);

    boolean hasArchivePositionReferences(String cabinetId);

    void deleteArchivePositionsByCabinetId(String cabinetId);

    void deleteArchiveCabinetNodesByCabinetId(String cabinetId);

    void deleteArchiveCabinet(String cabinetId);

    List<ArchivePosition> findAvailableArchivePositions(String cabinetType, String cabinetId);

    List<ArchivePosition> findArchivePositionsByCabinetId(String cabinetId);

    Optional<ArchivePosition> findArchivePositionById(String positionId);

    List<ArchivePosition> findAvailableArchivePositionsByCabinetId(String cabinetId, int limit);

    void insertArchivePosition(CreateArchivePositionCommand command);

    void occupyArchivePosition(String positionId, String objectType, String objectId, String remarks, LocalDateTime updatedAt);

    void releaseArchivePosition(String positionId, String remarks, LocalDateTime updatedAt);

    Optional<StorageRecord> findStorageRecord(String objectType, String objectId);

    void insertStorageRecord(CreateStorageRecordCommand command);

    void updateStorageRecord(UpdateStorageRecordCommand command);

    List<ArchiveRecordView> searchArchiveRecords(SearchArchiveRecordsQuery query);

    PagedArchiveObjects findArchiveObjects(SearchArchiveObjectsQuery query);

    Optional<MaterialLoan> findMaterialLoanById(String loanId);

    List<MaterialLoan> findMaterialLoans(String keyword, String materialType, String loanStatus);

    List<MaterialLoan> findPendingMaterialLoans(String keyword, String materialType);

    void insertMaterialLoan(CreateMaterialLoanCommand command);

    void updateMaterialLoanReturned(UpdateMaterialLoanReturnedCommand command);

    void insertMaterialLoanAbnormalRecord(CreateMaterialLoanAbnormalRecordCommand command);

    Optional<ApplicationArchiveSummary> findApplicationArchiveSummary(String caseId, String applicationId);

    List<ObjectArchiveSummary> findSpecimenArchiveSummaries(String caseId);

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

    record ArchiveCabinetNode(
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

    record UpdateArchiveCabinetCapacityCommand(
        String id,
        int layerCount,
        int slotCountPerLayer,
        int capacity,
        LocalDateTime updatedAt
    ) {
    }

    record CreateArchiveCabinetNodeCommand(
        String id,
        String parentId,
        String nodeCode,
        String nodeType,
        String cabinetType,
        String cabinetId,
        Integer layerNo,
        int capacity,
        String pathLocation,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateArchiveCabinetNodeCommand(
        String id,
        String nodeCode,
        String cabinetType,
        int capacity,
        String pathLocation,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }

    record UpdateArchiveCabinetNodeCapacityCommand(
        String id,
        int capacity,
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
        LocalDateTime archiveExpiresAt,
        Integer archiveReminderDays,
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
        LocalDateTime archiveExpiresAt,
        Integer archiveReminderDays,
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
        LocalDateTime archiveExpiresAt,
        Integer archiveReminderDays,
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
        String patientId,
        String patientIdDisplay,
        String patientName,
        String patientGender,
        String inpatientNo,
        String wardName,
        String applicantDoctorName,
        LocalDate applicationDate,
        String objectType,
        String objectId,
        String objectCode,
        String archiveStatus,
        String archiveLocation,
        String loanStatus,
        LocalDateTime archivedAt,
        String storedByName,
        String borrowedByName,
        LocalDateTime borrowedAt,
        String objectStatus,
        String sampledByName,
        LocalDateTime sampledAt,
        String slicedByName,
        LocalDateTime slicedAt,
        String contentDescribedByName,
        LocalDateTime archiveExpiresAt,
        Integer archiveReminderDays
    ) {
    }

    record SearchArchiveObjectsQuery(
        String keyword,
        String objectType,
        int page,
        int size
    ) {
    }

    record PagedArchiveObjects(
        List<ArchiveRecordView> items,
        long total
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
        String borrowerPhone,
        String borrowerUnit,
        String borrowPurpose,
        BigDecimal depositAmount,
        String approvedByUserId,
        String approvedByName,
        String returnedByUserId,
        String returnedByName,
        LocalDateTime returnedAt,
        String remarks,
        String objectCode,
        String pathologyNo,
        String applicationNo,
        String patientId,
        String patientIdDisplay,
        String patientName,
        String patientGender,
        String inpatientNo,
        String wardName
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
        String borrowerPhone,
        String borrowerUnit,
        String borrowPurpose,
        BigDecimal depositAmount,
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

    record CreateMaterialLoanAbnormalRecordCommand(
        String id,
        String caseId,
        String materialType,
        String materialId,
        String loanId,
        String abnormalReason,
        boolean contacted,
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
        String registeredByUserId,
        String registeredByName,
        LocalDateTime registeredAt,
        LocalDateTime createdAt,
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
