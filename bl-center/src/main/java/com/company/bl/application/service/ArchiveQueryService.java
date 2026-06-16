package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.ArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArchiveQueryService {

    private static final List<String> ARCHIVE_OBJECT_TYPES = List.of("APPLICATION_FORM", "EMBEDDING_BOX", "SLIDE", "SPECIMEN");
    private static final List<String> MATERIAL_LOAN_STATUSES = List.of("BORROWED", "RETURNED");

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
    public List<ArchiveModels.ArchiveCabinetNodeView> listArchiveCabinetNodes() {
        return archiveRepository.findArchiveCabinetNodes().stream()
            .map(this::toArchiveCabinetNodeView)
            .toList();
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
            item.applicantDoctorName(),
            stringify(item.applicationDate()),
            item.objectType(),
            item.objectId(),
            item.objectCode(),
            item.archiveStatus(),
            item.archiveLocation(),
            item.loanStatus(),
            stringify(item.archivedAt()),
            item.storedByName(),
            item.borrowedByName(),
            stringify(item.borrowedAt()),
            item.objectStatus(),
            item.sampledByName(),
            stringify(item.sampledAt()),
            item.slicedByName(),
            stringify(item.slicedAt()),
            item.contentDescribedByName(),
            stringify(item.archiveExpiresAt()),
            item.archiveReminderDays())).toList();
    }

    @Transactional(readOnly = true)
    public ArchiveModels.ArchiveObjectPage findArchiveObjects(ArchiveModels.SearchArchiveObjectsQuery query) {
        String objectType = normalizeObjectType(query.objectType());
        int page = normalizePage(query.page());
        int size = normalizeSize(query.size());
        ArchiveRepository.PagedArchiveObjects result = archiveRepository.findArchiveObjects(
            new ArchiveRepository.SearchArchiveObjectsQuery(query.keyword(), objectType, page, size));
        return new ArchiveModels.ArchiveObjectPage(
            result.items().stream().map(item -> new ArchiveModels.ArchiveRecordView(
                item.caseId(),
                item.pathologyNo(),
                item.applicationNo(),
                item.patientName(),
                item.applicantDoctorName(),
                stringify(item.applicationDate()),
                item.objectType(),
                item.objectId(),
                item.objectCode(),
                item.archiveStatus(),
                item.archiveLocation(),
                item.loanStatus(),
                stringify(item.archivedAt()),
                item.storedByName(),
                item.borrowedByName(),
                stringify(item.borrowedAt()),
                item.objectStatus(),
                item.sampledByName(),
                stringify(item.sampledAt()),
                item.slicedByName(),
                stringify(item.slicedAt()),
                item.contentDescribedByName(),
                stringify(item.archiveExpiresAt()),
                item.archiveReminderDays())).toList(),
            page,
            size,
            result.total());
    }

    @Transactional(readOnly = true)
    public List<ArchiveModels.MaterialLoanView> listMaterialLoans(String keyword, String materialType, String loanStatus) {
        return archiveRepository.findMaterialLoans(keyword, materialType, normalizeLoanStatus(loanStatus)).stream()
            .map(this::toMaterialLoanView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ArchiveModels.MaterialLoanView> listPendingMaterialLoans(String keyword, String materialType) {
        return archiveRepository.findPendingMaterialLoans(keyword, materialType).stream()
            .map(this::toMaterialLoanView)
            .toList();
    }

    private String stringify(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    private ArchiveModels.ArchiveCabinetNodeView toArchiveCabinetNodeView(ArchiveRepository.ArchiveCabinetNode item) {
        return new ArchiveModels.ArchiveCabinetNodeView(
            item.id(),
            item.parentId(),
            item.nodeCode(),
            item.nodeType(),
            item.cabinetType(),
            item.cabinetId(),
            item.layerNo(),
            item.capacity(),
            item.remainingCapacity(),
            item.pathLocation(),
            item.remarks());
    }

    private String stringify(java.time.LocalDate date) {
        return date == null ? null : date.toString();
    }

    private String normalizeObjectType(String objectType) {
        if (objectType == null || objectType.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Archive object type is required");
        }
        String normalized = objectType.trim().toUpperCase();
        if (!ARCHIVE_OBJECT_TYPES.contains(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported archive object type");
        }
        return normalized;
    }

    private String normalizeLoanStatus(String loanStatus) {
        if (loanStatus == null || loanStatus.isBlank()) {
            return "BORROWED";
        }
        String normalized = loanStatus.trim().toUpperCase();
        if (!MATERIAL_LOAN_STATUSES.contains(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported material loan status");
        }
        return normalized;
    }

    private ArchiveModels.MaterialLoanView toMaterialLoanView(ArchiveRepository.MaterialLoan item) {
        return new ArchiveModels.MaterialLoanView(
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
            item.borrowerPhone(),
            item.borrowerUnit(),
            item.borrowPurpose(),
            item.depositAmount(),
            item.approvedByName(),
            item.returnedByName(),
            stringify(item.returnedAt()),
            item.remarks());
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 200);
    }
}
