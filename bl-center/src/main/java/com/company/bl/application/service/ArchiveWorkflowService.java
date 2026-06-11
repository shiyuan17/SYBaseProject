package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.repository.ArchiveRepository;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ArchiveWorkflowService {

    private static final String CABINET_ACTIVE = "ACTIVE";
    private static final String CABINET_DISABLED = "DISABLED";
    private static final String POSITION_AVAILABLE = "AVAILABLE";
    private static final String POSITION_DISABLED = "DISABLED";
    private static final String STORAGE_IN_STORAGE = "IN_STORAGE";
    private static final String STORAGE_BORROWED = "BORROWED";
    private static final String LOAN_BORROWED = "BORROWED";
    private static final String LOAN_RETURNED = "RETURNED";

    private final ArchiveRepository archiveRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;

    public ArchiveWorkflowService(ArchiveRepository archiveRepository,
                                  TechnicalWorkflowRepository technicalWorkflowRepository,
                                  DiagnosticReportSupport diagnosticReportSupport) {
        this.archiveRepository = archiveRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
    }

    @Transactional
    public ArchiveModels.ArchiveCabinetView createArchiveCabinet(ArchiveModels.CreateArchiveCabinetCommand command) {
        if (archiveRepository.findArchiveCabinetByCode(command.cabinetCode()).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archive cabinet code already exists");
        }
        return createArchiveCabinetWithoutDuplicateCheck(command);
    }

    @Transactional
    public List<ArchiveModels.ArchiveCabinetView> batchCreateArchiveCabinets(ArchiveModels.BatchCreateArchiveCabinetCommand command) {
        List<String> cabinetCodes = new ArrayList<>();
        for (int index = 0; index < command.count(); index++) {
            int serialNo = command.startNo() + index;
            cabinetCodes.add("%s%s".formatted(command.cabinetCodePrefix(), formatSerialNo(serialNo, command.numberWidth())));
        }

        if (archiveRepository.existsArchiveCabinetByCodes(cabinetCodes)) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archive cabinet code already exists");
        }

        List<ArchiveModels.ArchiveCabinetView> cabinets = new ArrayList<>();
        for (int index = 0; index < cabinetCodes.size(); index++) {
            String cabinetCode = cabinetCodes.get(index);
            String serialText = formatSerialNo(command.startNo() + index, command.numberWidth());
            String cabinetName = buildBatchCabinetName(command.cabinetNamePrefix(), cabinetCode, serialText);
            cabinets.add(createArchiveCabinetWithoutDuplicateCheck(new ArchiveModels.CreateArchiveCabinetCommand(
                cabinetCode,
                cabinetName,
                command.cabinetType(),
                command.layerCount(),
                command.slotCountPerLayer(),
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                command.locationDescription(),
                command.remarks())));
        }
        return cabinets;
    }

    @Transactional
    public void deleteArchiveCabinet(String cabinetId) {
        ArchiveRepository.ArchiveCabinet cabinet = archiveRepository.findArchiveCabinetById(cabinetId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive cabinet not found"));
        if (archiveRepository.hasNonEmptyArchivePositions(cabinet.id())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archive cabinet is not empty");
        }
        if (archiveRepository.hasArchivePositionReferences(cabinet.id())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archive cabinet has archive references");
        }
        archiveRepository.deleteArchivePositionsByCabinetId(cabinet.id());
        archiveRepository.deleteArchiveCabinet(cabinet.id());
    }

    private ArchiveModels.ArchiveCabinetView createArchiveCabinetWithoutDuplicateCheck(ArchiveModels.CreateArchiveCabinetCommand command) {
        LocalDateTime now = LocalDateTime.now();
        String cabinetId = diagnosticReportSupport.nextId("AC");
        int capacity = command.layerCount() * command.slotCountPerLayer();
        archiveRepository.insertArchiveCabinet(new ArchiveRepository.CreateArchiveCabinetCommand(
            cabinetId,
            command.cabinetCode(),
            command.cabinetName(),
            command.cabinetType(),
            command.layerCount(),
            command.slotCountPerLayer(),
            capacity,
            CABINET_ACTIVE,
            command.locationDescription(),
            command.remarks(),
            now,
            now));
        for (int layer = 1; layer <= command.layerCount(); layer++) {
            for (int slot = 1; slot <= command.slotCountPerLayer(); slot++) {
                String positionCode = "%s-L%d-S%d".formatted(command.cabinetCode(), layer, slot);
                archiveRepository.insertArchivePosition(new ArchiveRepository.CreateArchivePositionCommand(
                    diagnosticReportSupport.nextId("AP"),
                    cabinetId,
                    positionCode,
                    layer,
                    slot,
                    POSITION_AVAILABLE,
                    null,
                    now,
                    now));
            }
        }
        ArchiveRepository.ArchiveCabinet cabinet = archiveRepository.findArchiveCabinetById(cabinetId).orElseThrow();
        return new ArchiveModels.ArchiveCabinetView(
            cabinet.id(),
            cabinet.cabinetCode(),
            cabinet.cabinetName(),
            cabinet.cabinetType(),
            cabinet.layerCount(),
            cabinet.slotCountPerLayer(),
            cabinet.capacity(),
            cabinet.cabinetStatus(),
            cabinet.locationDescription(),
            cabinet.remarks());
    }

    private String formatSerialNo(int serialNo, int numberWidth) {
        return String.format("%0" + numberWidth + "d", serialNo);
    }

    private String buildBatchCabinetName(String cabinetNamePrefix, String cabinetCode, String serialText) {
        if (cabinetNamePrefix == null || cabinetNamePrefix.isBlank()) {
            return cabinetCode;
        }
        return cabinetNamePrefix.trim() + serialText;
    }

    @Transactional
    public ArchiveModels.ArchiveCabinetView updateArchiveCabinet(ArchiveModels.UpdateArchiveCabinetCommand command) {
        ArchiveRepository.ArchiveCabinet cabinet = archiveRepository.findArchiveCabinetById(command.cabinetId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive cabinet not found"));
        archiveRepository.updateArchiveCabinet(new ArchiveRepository.UpdateArchiveCabinetCommand(
            cabinet.id(),
            command.cabinetName(),
            command.cabinetStatus(),
            command.locationDescription(),
            command.remarks(),
            LocalDateTime.now()));
        ArchiveRepository.ArchiveCabinet updated = archiveRepository.findArchiveCabinetById(command.cabinetId()).orElseThrow();
        return new ArchiveModels.ArchiveCabinetView(
            updated.id(),
            updated.cabinetCode(),
            updated.cabinetName(),
            updated.cabinetType(),
            updated.layerCount(),
            updated.slotCountPerLayer(),
            updated.capacity(),
            updated.cabinetStatus(),
            updated.locationDescription(),
            updated.remarks());
    }

    @Transactional
    public ArchiveModels.ArchiveActionResult archiveApplicationForm(ArchiveModels.ArchiveObjectCommand command) {
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(command.objectId());
        Application application = diagnosticReportSupport.getApplication(pathologyCase.applicationId());
        return archiveObject(
            pathologyCase,
            null,
            "APPLICATION_FORM",
            application.getId().value(),
            command,
            true);
    }

    @Transactional
    public ArchiveModels.ArchiveActionResult archiveEmbeddingBox(ArchiveModels.ArchiveObjectCommand command) {
        TechnicalWorkflowRecords.EmbeddingBox embeddingBox = technicalWorkflowRepository.findEmbeddingBoxById(command.objectId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Embedding box not found"));
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(embeddingBox.caseId());
        return archiveObject(pathologyCase, embeddingBox.specimenId(), "EMBEDDING_BOX", embeddingBox.id(), command, false);
    }

    @Transactional
    public ArchiveModels.ArchiveActionResult archiveSlide(ArchiveModels.ArchiveObjectCommand command) {
        TechnicalWorkflowProcessingRecords.Slide slide = technicalWorkflowRepository.findSlideById(command.objectId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Slide not found"));
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(slide.caseId());
        return archiveObject(pathologyCase, slide.specimenId(), "SLIDE", slide.id(), command, false);
    }

    @Transactional
    public ArchiveModels.MaterialLoanView createMaterialLoan(ArchiveModels.CreateMaterialLoanCommand command) {
        ArchiveRepository.StorageRecord storageRecord = archiveRepository.findStorageRecord(command.materialType(), command.materialId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive record not found"));
        if (STORAGE_BORROWED.equals(storageRecord.storageStatus())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archived material is already borrowed");
        }
        ArchiveRepository.ArchivePosition originPosition = storageRecord.archivePositionId() == null ? null
            : archiveRepository.findArchivePositionById(storageRecord.archivePositionId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        String loanId = diagnosticReportSupport.nextId("LOAN");
        archiveRepository.insertMaterialLoan(new ArchiveRepository.CreateMaterialLoanCommand(
            loanId,
            storageRecord.caseId(),
            storageRecord.specimenId(),
            command.materialType(),
            command.materialId(),
            storageRecord.archivePositionId(),
            LOAN_BORROWED,
            command.borrowedByUserId(),
            command.borrowedByName(),
            now,
            command.borrowPurpose(),
            command.operatorUserId(),
            command.operatorName(),
            command.remarks(),
            now,
            now));
        archiveRepository.updateStorageRecord(new ArchiveRepository.UpdateStorageRecordCommand(
            storageRecord.id(),
            STORAGE_BORROWED,
            storageRecord.storageLocation(),
            storageRecord.archivePositionId(),
            storageRecord.cabinetNo(),
            storageRecord.layerNo(),
            storageRecord.slotNo(),
            storageRecord.storedByUserId(),
            storageRecord.storedByName(),
            storageRecord.storedAt(),
            storageRecord.remarks(),
            now));
        if (originPosition != null) {
            archiveRepository.releaseArchivePosition(originPosition.id(), originPosition.remarks(), now);
        }
        diagnosticReportSupport.insertWorkflowEvent(storageRecord.caseId(), "ARCHIVE", "BORROW", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.materialType() + ":" + command.materialId());
        ArchiveRepository.MaterialLoan loan = archiveRepository.findMaterialLoanById(loanId).orElseThrow();
        return toMaterialLoanView(loan);
    }

    @Transactional
    public ArchiveModels.MaterialLoanView returnMaterialLoan(ArchiveModels.ReturnMaterialLoanCommand command) {
        ArchiveRepository.MaterialLoan loan = archiveRepository.findMaterialLoanById(command.loanId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Material loan not found"));
        if (!LOAN_BORROWED.equals(loan.loanStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Material loan is not pending return");
        }
        ArchiveRepository.StorageRecord storageRecord = archiveRepository.findStorageRecord(loan.materialType(), loan.materialId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive record not found"));
        ArchiveRepository.ArchivePosition originalPosition = loan.archivePositionId() == null ? null
            : archiveRepository.findArchivePositionById(loan.archivePositionId()).orElse(null);
        ArchiveRepository.ArchivePosition targetPosition = resolveReturnPosition(command.archivePositionId(), originalPosition);
        LocalDateTime now = LocalDateTime.now();
        archiveRepository.occupyArchivePosition(targetPosition.id(), loan.materialType(), loan.materialId(), targetPosition.remarks(), now);
        archiveRepository.updateStorageRecord(new ArchiveRepository.UpdateStorageRecordCommand(
            storageRecord.id(),
            STORAGE_IN_STORAGE,
            targetPosition.positionCode(),
            targetPosition.id(),
            archiveRepository.findArchiveCabinetById(targetPosition.cabinetId()).map(ArchiveRepository.ArchiveCabinet::cabinetCode).orElse(null),
            String.valueOf(targetPosition.layerNo()),
            String.valueOf(targetPosition.slotNo()),
            command.operatorUserId(),
            command.operatorName(),
            now,
            storageRecord.remarks(),
            now));
        archiveRepository.updateMaterialLoanReturned(new ArchiveRepository.UpdateMaterialLoanReturnedCommand(
            loan.id(),
            LOAN_RETURNED,
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.remarks(),
            now));
        diagnosticReportSupport.insertWorkflowEvent(storageRecord.caseId(), "ARCHIVE", "RETURN", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), loan.materialType() + ":" + loan.materialId());
        return toMaterialLoanView(archiveRepository.findMaterialLoanById(loan.id()).orElseThrow());
    }

    private ArchiveRepository.ArchivePosition resolveReturnPosition(String overridePositionId, ArchiveRepository.ArchivePosition originalPosition) {
        if (originalPosition != null
            && POSITION_AVAILABLE.equals(originalPosition.positionStatus())
            && !POSITION_DISABLED.equals(originalPosition.positionStatus())) {
            return originalPosition;
        }
        if (overridePositionId == null || overridePositionId.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "A replacement archive position is required for return");
        }
        ArchiveRepository.ArchivePosition position = archiveRepository.findArchivePositionById(overridePositionId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive position not found"));
        ensureAvailablePosition(position);
        return position;
    }

    private ArchiveModels.ArchiveActionResult archiveObject(PathologyCase pathologyCase,
                                                            String specimenId,
                                                            String objectType,
                                                            String objectId,
                                                            ArchiveModels.ArchiveObjectCommand command,
                                                            boolean persistImage) {
        ArchiveRepository.ArchivePosition position = archiveRepository.findArchivePositionById(command.archivePositionId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive position not found"));
        ensureAvailablePosition(position);
        LocalDateTime now = LocalDateTime.now();
        ArchiveRepository.ArchiveCabinet cabinet = archiveRepository.findArchiveCabinetById(position.cabinetId()).orElseThrow();
        String location = position.positionCode();
        ArchiveRepository.StorageRecord existing = archiveRepository.findStorageRecord(objectType, objectId).orElse(null);
        if (existing != null && STORAGE_BORROWED.equals(existing.storageStatus())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archived material is currently borrowed");
        }
        if (existing != null && existing.archivePositionId() != null && !existing.archivePositionId().equals(position.id())) {
            archiveRepository.releaseArchivePosition(existing.archivePositionId(), existing.remarks(), now);
        }
        if (existing == null) {
            archiveRepository.insertStorageRecord(new ArchiveRepository.CreateStorageRecordCommand(
                diagnosticReportSupport.nextId("SSR"),
                pathologyCase.id(),
                specimenId,
                objectType,
                objectId,
                STORAGE_IN_STORAGE,
                location,
                position.id(),
                cabinet.cabinetCode(),
                String.valueOf(position.layerNo()),
                String.valueOf(position.slotNo()),
                command.operatorUserId(),
                command.operatorName(),
                now,
                command.remarks(),
                now,
                now));
        } else {
            archiveRepository.updateStorageRecord(new ArchiveRepository.UpdateStorageRecordCommand(
                existing.id(),
                STORAGE_IN_STORAGE,
                location,
                position.id(),
                cabinet.cabinetCode(),
                String.valueOf(position.layerNo()),
                String.valueOf(position.slotNo()),
                command.operatorUserId(),
                command.operatorName(),
                now,
                command.remarks(),
                now));
        }
        archiveRepository.occupyArchivePosition(position.id(), objectType, objectId, position.remarks(), now);
        if (persistImage && command.fileUrl() != null && !command.fileUrl().isBlank()) {
            technicalWorkflowRepository.insertCaseMediaAsset(new TechnicalWorkflowProcessingRecords.CreateCaseMediaAssetCommand(
                diagnosticReportSupport.nextId("MEDIA"),
                pathologyCase.id(),
                specimenId,
                "APPLICATION_FORM",
                objectId,
                "ARCHIVE_IMAGE",
                command.fileUrl(),
                command.fileName(),
                now,
                command.operatorUserId(),
                command.operatorName(),
                command.remarks()));
        }
        diagnosticReportSupport.insertWorkflowEvent(pathologyCase.id(), "ARCHIVE", "ARCHIVE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), objectType + ":" + objectId);
        return new ArchiveModels.ArchiveActionResult(pathologyCase.id(), objectType, objectId, STORAGE_IN_STORAGE, location);
    }

    private void ensureAvailablePosition(ArchiveRepository.ArchivePosition position) {
        if (!POSITION_AVAILABLE.equals(position.positionStatus())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archive position is not available");
        }
        ArchiveRepository.ArchiveCabinet cabinet = archiveRepository.findArchiveCabinetById(position.cabinetId()).orElseThrow();
        if (CABINET_DISABLED.equals(cabinet.cabinetStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Archive cabinet is disabled");
        }
    }

    private ArchiveModels.MaterialLoanView toMaterialLoanView(ArchiveRepository.MaterialLoan loan) {
        return new ArchiveModels.MaterialLoanView(
            loan.id(),
            loan.caseId(),
            loan.pathologyNo(),
            loan.applicationNo(),
            loan.patientName(),
            loan.materialType(),
            loan.materialId(),
            loan.objectCode(),
            loan.loanStatus(),
            loan.borrowedByName(),
            loan.borrowedAt() == null ? null : loan.borrowedAt().toString(),
            loan.borrowPurpose(),
            loan.approvedByName(),
            loan.returnedByName(),
            loan.returnedAt() == null ? null : loan.returnedAt().toString(),
            loan.remarks());
    }
}
