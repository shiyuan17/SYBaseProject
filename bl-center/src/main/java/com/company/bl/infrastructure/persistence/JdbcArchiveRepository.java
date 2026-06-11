package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ArchiveRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcArchiveRepository implements ArchiveRepository {

    private final JdbcArchiveQueries queries;
    private final JdbcArchiveMutations mutations;

    public JdbcArchiveRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.queries = new JdbcArchiveQueries(jdbcTemplate);
        this.mutations = new JdbcArchiveMutations(jdbcTemplate);
    }

    @Override
    public List<ArchiveCabinet> findArchiveCabinets() {
        return queries.findArchiveCabinets();
    }

    @Override
    public Optional<ArchiveCabinet> findArchiveCabinetById(String cabinetId) {
        return queries.findArchiveCabinetById(cabinetId);
    }

    @Override
    public Optional<ArchiveCabinet> findArchiveCabinetByCode(String cabinetCode) {
        return queries.findArchiveCabinetByCode(cabinetCode);
    }

    @Override
    public boolean existsArchiveCabinetByCodes(List<String> cabinetCodes) {
        return queries.existsArchiveCabinetByCodes(cabinetCodes);
    }

    @Override
    public void insertArchiveCabinet(CreateArchiveCabinetCommand command) {
        mutations.insertArchiveCabinet(command);
    }

    @Override
    public void updateArchiveCabinet(UpdateArchiveCabinetCommand command) {
        mutations.updateArchiveCabinet(command);
    }

    @Override
    public boolean hasNonEmptyArchivePositions(String cabinetId) {
        return queries.hasNonEmptyArchivePositions(cabinetId);
    }

    @Override
    public boolean hasArchivePositionReferences(String cabinetId) {
        return queries.hasArchivePositionReferences(cabinetId);
    }

    @Override
    public void deleteArchivePositionsByCabinetId(String cabinetId) {
        mutations.deleteArchivePositionsByCabinetId(cabinetId);
    }

    @Override
    public void deleteArchiveCabinet(String cabinetId) {
        mutations.deleteArchiveCabinet(cabinetId);
    }

    @Override
    public List<ArchivePosition> findAvailableArchivePositions(String cabinetType, String cabinetId) {
        return queries.findAvailableArchivePositions(cabinetType, cabinetId);
    }

    @Override
    public List<ArchivePosition> findArchivePositionsByCabinetId(String cabinetId) {
        return queries.findArchivePositionsByCabinetId(cabinetId);
    }

    @Override
    public Optional<ArchivePosition> findArchivePositionById(String positionId) {
        return queries.findArchivePositionById(positionId);
    }

    @Override
    public void insertArchivePosition(CreateArchivePositionCommand command) {
        mutations.insertArchivePosition(command);
    }

    @Override
    public void occupyArchivePosition(String positionId, String objectType, String objectId, String remarks, LocalDateTime updatedAt) {
        mutations.occupyArchivePosition(positionId, objectType, objectId, remarks, updatedAt);
    }

    @Override
    public void releaseArchivePosition(String positionId, String remarks, LocalDateTime updatedAt) {
        mutations.releaseArchivePosition(positionId, remarks, updatedAt);
    }

    @Override
    public Optional<StorageRecord> findStorageRecord(String objectType, String objectId) {
        return queries.findStorageRecord(objectType, objectId);
    }

    @Override
    public void insertStorageRecord(CreateStorageRecordCommand command) {
        mutations.insertStorageRecord(command);
    }

    @Override
    public void updateStorageRecord(UpdateStorageRecordCommand command) {
        mutations.updateStorageRecord(command);
    }

    @Override
    public List<ArchiveRecordView> searchArchiveRecords(SearchArchiveRecordsQuery query) {
        return queries.searchArchiveRecords(query);
    }

    @Override
    public Optional<MaterialLoan> findMaterialLoanById(String loanId) {
        return queries.findMaterialLoanById(loanId);
    }

    @Override
    public List<MaterialLoan> findPendingMaterialLoans(String keyword, String materialType) {
        return queries.findPendingMaterialLoans(keyword, materialType);
    }

    @Override
    public void insertMaterialLoan(CreateMaterialLoanCommand command) {
        mutations.insertMaterialLoan(command);
    }

    @Override
    public void updateMaterialLoanReturned(UpdateMaterialLoanReturnedCommand command) {
        mutations.updateMaterialLoanReturned(command);
    }

    @Override
    public Optional<ApplicationArchiveSummary> findApplicationArchiveSummary(String caseId, String applicationId) {
        return queries.findApplicationArchiveSummary(caseId, applicationId);
    }

    @Override
    public List<ObjectArchiveSummary> findEmbeddingBoxArchiveSummaries(String caseId) {
        return queries.findEmbeddingBoxArchiveSummaries(caseId);
    }

    @Override
    public List<ObjectArchiveSummary> findSlideArchiveSummaries(String caseId) {
        return queries.findSlideArchiveSummaries(caseId);
    }
}
