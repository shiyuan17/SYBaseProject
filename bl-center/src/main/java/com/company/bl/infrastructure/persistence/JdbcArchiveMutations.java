package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ArchiveRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

final class JdbcArchiveMutations {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcArchiveMutations(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insertArchiveCabinet(ArchiveRepository.CreateArchiveCabinetCommand command) {
        jdbcTemplate.update("""
            insert into archive_cabinets
                (id, cabinet_code, cabinet_name, cabinet_type, layer_count, slot_count_per_layer, capacity,
                 cabinet_status, location_description, remarks, created_at, updated_at)
            values
                (:id, :cabinetCode, :cabinetName, :cabinetType, :layerCount, :slotCountPerLayer, :capacity,
                 :cabinetStatus, :locationDescription, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("cabinetCode", command.cabinetCode())
            .addValue("cabinetName", command.cabinetName())
            .addValue("cabinetType", command.cabinetType())
            .addValue("layerCount", command.layerCount())
            .addValue("slotCountPerLayer", command.slotCountPerLayer())
            .addValue("capacity", command.capacity())
            .addValue("cabinetStatus", command.cabinetStatus())
            .addValue("locationDescription", command.locationDescription())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    void updateArchiveCabinet(ArchiveRepository.UpdateArchiveCabinetCommand command) {
        jdbcTemplate.update("""
            update archive_cabinets
            set cabinet_name = :cabinetName,
                cabinet_status = :cabinetStatus,
                location_description = :locationDescription,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("cabinetName", command.cabinetName())
            .addValue("cabinetStatus", command.cabinetStatus())
            .addValue("locationDescription", command.locationDescription())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }

    void insertArchivePosition(ArchiveRepository.CreateArchivePositionCommand command) {
        jdbcTemplate.update("""
            insert into archive_positions
                (id, cabinet_id, position_code, layer_no, slot_no, position_status, remarks, created_at, updated_at)
            values
                (:id, :cabinetId, :positionCode, :layerNo, :slotNo, :positionStatus, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("cabinetId", command.cabinetId())
            .addValue("positionCode", command.positionCode())
            .addValue("layerNo", command.layerNo())
            .addValue("slotNo", command.slotNo())
            .addValue("positionStatus", command.positionStatus())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    void occupyArchivePosition(String positionId, String objectType, String objectId, String remarks, java.time.LocalDateTime updatedAt) {
        jdbcTemplate.update("""
            update archive_positions
            set position_status = 'OCCUPIED',
                current_object_type = :objectType,
                current_object_id = :objectId,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", positionId)
            .addValue("objectType", objectType)
            .addValue("objectId", objectId)
            .addValue("remarks", remarks)
            .addValue("updatedAt", updatedAt));
    }

    void releaseArchivePosition(String positionId, String remarks, java.time.LocalDateTime updatedAt) {
        jdbcTemplate.update("""
            update archive_positions
            set position_status = 'AVAILABLE',
                current_object_type = null,
                current_object_id = null,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", positionId)
            .addValue("remarks", remarks)
            .addValue("updatedAt", updatedAt));
    }

    void insertStorageRecord(ArchiveRepository.CreateStorageRecordCommand command) {
        jdbcTemplate.update("""
            insert into specimen_storage_records
                (id, case_id, specimen_id, object_type, object_id, storage_status, storage_location, archive_position_id,
                 cabinet_no, layer_no, slot_no, stored_by_user_id, stored_by_name, stored_at, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :objectType, :objectId, :storageStatus, :storageLocation, :archivePositionId,
                 :cabinetNo, :layerNo, :slotNo, :storedByUserId, :storedByName, :storedAt, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("objectType", command.objectType())
            .addValue("objectId", command.objectId())
            .addValue("storageStatus", command.storageStatus())
            .addValue("storageLocation", command.storageLocation())
            .addValue("archivePositionId", command.archivePositionId())
            .addValue("cabinetNo", command.cabinetNo())
            .addValue("layerNo", command.layerNo())
            .addValue("slotNo", command.slotNo())
            .addValue("storedByUserId", command.storedByUserId())
            .addValue("storedByName", command.storedByName())
            .addValue("storedAt", command.storedAt())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    void updateStorageRecord(ArchiveRepository.UpdateStorageRecordCommand command) {
        jdbcTemplate.update("""
            update specimen_storage_records
            set storage_status = :storageStatus,
                storage_location = :storageLocation,
                archive_position_id = :archivePositionId,
                cabinet_no = :cabinetNo,
                layer_no = :layerNo,
                slot_no = :slotNo,
                stored_by_user_id = :storedByUserId,
                stored_by_name = :storedByName,
                stored_at = :storedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("storageStatus", command.storageStatus())
            .addValue("storageLocation", command.storageLocation())
            .addValue("archivePositionId", command.archivePositionId())
            .addValue("cabinetNo", command.cabinetNo())
            .addValue("layerNo", command.layerNo())
            .addValue("slotNo", command.slotNo())
            .addValue("storedByUserId", command.storedByUserId())
            .addValue("storedByName", command.storedByName())
            .addValue("storedAt", command.storedAt())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }

    void insertMaterialLoan(ArchiveRepository.CreateMaterialLoanCommand command) {
        jdbcTemplate.update("""
            insert into material_loans
                (id, case_id, specimen_id, material_type, material_id, archive_position_id, loan_status,
                 borrowed_by_user_id, borrowed_by_name, borrowed_at, borrow_purpose, approved_by_user_id,
                 approved_by_name, remarks, created_at, updated_at)
            values
                (:id, :caseId, :specimenId, :materialType, :materialId, :archivePositionId, :loanStatus,
                 :borrowedByUserId, :borrowedByName, :borrowedAt, :borrowPurpose, :approvedByUserId,
                 :approvedByName, :remarks, :createdAt, :updatedAt)
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("caseId", command.caseId())
            .addValue("specimenId", command.specimenId())
            .addValue("materialType", command.materialType())
            .addValue("materialId", command.materialId())
            .addValue("archivePositionId", command.archivePositionId())
            .addValue("loanStatus", command.loanStatus())
            .addValue("borrowedByUserId", command.borrowedByUserId())
            .addValue("borrowedByName", command.borrowedByName())
            .addValue("borrowedAt", command.borrowedAt())
            .addValue("borrowPurpose", command.borrowPurpose())
            .addValue("approvedByUserId", command.approvedByUserId())
            .addValue("approvedByName", command.approvedByName())
            .addValue("remarks", command.remarks())
            .addValue("createdAt", command.createdAt())
            .addValue("updatedAt", command.updatedAt()));
    }

    void updateMaterialLoanReturned(ArchiveRepository.UpdateMaterialLoanReturnedCommand command) {
        jdbcTemplate.update("""
            update material_loans
            set loan_status = :loanStatus,
                returned_by_user_id = :returnedByUserId,
                returned_by_name = :returnedByName,
                returned_at = :returnedAt,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :id
            """, new MapSqlParameterSource()
            .addValue("id", command.id())
            .addValue("loanStatus", command.loanStatus())
            .addValue("returnedByUserId", command.returnedByUserId())
            .addValue("returnedByName", command.returnedByName())
            .addValue("returnedAt", command.returnedAt())
            .addValue("remarks", command.remarks())
            .addValue("updatedAt", command.updatedAt()));
    }
}
