package com.company.bl.application.service;

import com.company.bl.domain.repository.ArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArchiveQueryService {

    private final ArchiveRepository archiveRepository;

    public ArchiveQueryService(ArchiveRepository archiveRepository) {
        this.archiveRepository = archiveRepository;
    }

    @Transactional(readOnly = true)
    public List<ArchiveModels.ArchiveCabinetView> listArchiveCabinets() {
        return archiveRepository.findArchiveCabinets().stream().map(item -> new ArchiveModels.ArchiveCabinetView(
            item.id(),
            item.cabinetCode(),
            item.cabinetName(),
            item.cabinetType(),
            item.layerCount(),
            item.slotCountPerLayer(),
            item.capacity(),
            item.cabinetStatus(),
            item.locationDescription(),
            item.remarks())).toList();
    }

    @Transactional(readOnly = true)
    public List<ArchiveModels.ArchivePositionView> listAvailablePositions(String cabinetType, String cabinetId) {
        return archiveRepository.findAvailableArchivePositions(cabinetType, cabinetId).stream().map(item -> new ArchiveModels.ArchivePositionView(
            item.id(), item.cabinetId(), item.positionCode(), item.layerNo(), item.slotNo(), item.positionStatus())).toList();
    }

    @Transactional(readOnly = true)
    public List<ArchiveModels.ArchiveRecordView> searchArchiveRecords(ArchiveModels.SearchArchiveRecordsQuery query) {
        return archiveRepository.searchArchiveRecords(new ArchiveRepository.SearchArchiveRecordsQuery(
            query.keyword(), query.objectType(), query.caseId())).stream().map(item -> new ArchiveModels.ArchiveRecordView(
            item.caseId(),
            item.pathologyNo(),
            item.applicationNo(),
            item.patientName(),
            item.objectType(),
            item.objectId(),
            item.objectCode(),
            item.archiveStatus(),
            item.archiveLocation(),
            item.loanStatus(),
            stringify(item.archivedAt()),
            item.storedByName(),
            item.borrowedByName(),
            stringify(item.borrowedAt()))).toList();
    }

    @Transactional(readOnly = true)
    public List<ArchiveModels.MaterialLoanView> listPendingMaterialLoans(String keyword, String materialType) {
        return archiveRepository.findPendingMaterialLoans(keyword, materialType).stream().map(item -> new ArchiveModels.MaterialLoanView(
            item.id(),
            item.caseId(),
            item.pathologyNo(),
            item.applicationNo(),
            item.patientName(),
            item.materialType(),
            item.materialId(),
            item.objectCode(),
            item.loanStatus(),
            item.borrowedByName(),
            stringify(item.borrowedAt()),
            item.borrowPurpose(),
            item.approvedByName(),
            item.returnedByName(),
            stringify(item.returnedAt()),
            item.remarks())).toList();
    }

    private String stringify(LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
