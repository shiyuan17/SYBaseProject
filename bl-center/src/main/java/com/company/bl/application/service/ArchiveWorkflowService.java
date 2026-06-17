package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
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
    private static final String NODE_AREA = "AREA";
    private static final String NODE_CABINET = "CABINET";
    private static final String NODE_DRAWER = "DRAWER";
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
    public ArchiveModels.ArchiveCabinetNodeView createArchiveCabinetNode(ArchiveModels.CreateArchiveCabinetNodeCommand command) {
        String nodeType = normalizeNodeType(command.nodeType());
        return switch (nodeType) {
            case NODE_AREA -> createAreaNode(command);
            case NODE_CABINET -> createCabinetNode(command);
            case NODE_DRAWER -> createDrawerNode(command);
            default -> throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported archive cabinet node type");
        };
    }

    @Transactional
    public List<ArchiveModels.ArchiveCabinetView> batchCreateArchiveCabinets(ArchiveModels.BatchCreateArchiveCabinetCommand command) {
        ArchiveRepository.ArchiveCabinetNode parent = resolveOptionalParent(command.parentId());
        if (parent != null && !NODE_AREA.equals(parent.nodeType())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Cabinet node parent must be root or area");
        }
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
                command.remarks()),
                parent == null ? null : parent.id()));
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
        archiveRepository.deleteArchiveCabinetNodesByCabinetId(cabinet.id());
        archiveRepository.deleteArchivePositionsByCabinetId(cabinet.id());
        archiveRepository.deleteArchiveCabinet(cabinet.id());
    }

    private ArchiveModels.ArchiveCabinetView createArchiveCabinetWithoutDuplicateCheck(ArchiveModels.CreateArchiveCabinetCommand command) {
        return createArchiveCabinetWithoutDuplicateCheck(command, null);
    }

    @Transactional
    public ArchiveModels.ArchiveCabinetNodeView updateArchiveCabinetNode(ArchiveModels.UpdateArchiveCabinetNodeCommand command) {
        ArchiveRepository.ArchiveCabinetNode node = archiveRepository.findArchiveCabinetNodeById(command.nodeId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive cabinet node not found"));
        LocalDateTime now = LocalDateTime.now();
        String nodeCode = requireText(command.nodeCode(), "Archive cabinet node code is required");
        String cabinetType = command.cabinetType() == null || command.cabinetType().isBlank() ? node.cabinetType() : command.cabinetType().trim();
        int capacity = NODE_AREA.equals(node.nodeType()) ? node.capacity() : requirePositive(command.capacity(), "Archive cabinet node capacity must be greater than zero");
        archiveRepository.updateArchiveCabinetNode(new ArchiveRepository.UpdateArchiveCabinetNodeCommand(
            node.id(),
            nodeCode,
            cabinetType,
            capacity,
            optionalText(command.pathLocation()),
            optionalText(command.remarks()),
            now));

        if (NODE_CABINET.equals(node.nodeType()) && node.cabinetId() != null) {
            ArchiveRepository.ArchiveCabinet cabinet = archiveRepository.findArchiveCabinetById(node.cabinetId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive cabinet not found"));
            archiveRepository.updateArchiveCabinet(new ArchiveRepository.UpdateArchiveCabinetCommand(
                cabinet.id(),
                nodeCode,
                cabinet.cabinetStatus(),
                optionalText(command.pathLocation()),
                optionalText(command.remarks()),
                now));
        }

        return archiveRepository.findArchiveCabinetNodeById(node.id())
            .map(this::toArchiveCabinetNodeView)
            .orElseThrow();
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
        archiveRepository.findArchiveCabinetNodeByCabinetIdAndType(cabinet.id(), NODE_CABINET)
            .ifPresent(node -> archiveRepository.updateArchiveCabinetNode(new ArchiveRepository.UpdateArchiveCabinetNodeCommand(
                node.id(),
                command.cabinetName(),
                node.cabinetType(),
                node.capacity(),
                command.locationDescription(),
                command.remarks(),
                LocalDateTime.now())));
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

    private ArchiveModels.ArchiveCabinetNodeView createAreaNode(ArchiveModels.CreateArchiveCabinetNodeCommand command) {
        ArchiveRepository.ArchiveCabinetNode parent = resolveOptionalParent(command.parentId());
        if (parent != null && !NODE_AREA.equals(parent.nodeType())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Area node parent must be root or area");
        }
        LocalDateTime now = LocalDateTime.now();
        String nodeId = diagnosticReportSupport.nextId("ACN");
        archiveRepository.insertArchiveCabinetNode(new ArchiveRepository.CreateArchiveCabinetNodeCommand(
            nodeId,
            parent == null ? null : parent.id(),
            requireText(command.nodeCode(), "Archive cabinet node code is required"),
            NODE_AREA,
            optionalText(command.cabinetType()),
            null,
            null,
            0,
            optionalText(command.pathLocation()),
            optionalText(command.remarks()),
            now,
            now));
        return archiveRepository.findArchiveCabinetNodeById(nodeId).map(this::toArchiveCabinetNodeView).orElseThrow();
    }

    private ArchiveModels.ArchiveCabinetNodeView createCabinetNode(ArchiveModels.CreateArchiveCabinetNodeCommand command) {
        ArchiveRepository.ArchiveCabinetNode parent = resolveOptionalParent(command.parentId());
        if (parent != null && !NODE_AREA.equals(parent.nodeType())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Cabinet node parent must be root or area");
        }
        String cabinetCode = requireText(command.nodeCode(), "Archive cabinet code is required");
        if (archiveRepository.findArchiveCabinetByCode(cabinetCode).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archive cabinet code already exists");
        }
        String cabinetType = requireText(command.cabinetType(), "Archive cabinet type is required");
        int capacity = requirePositive(command.capacity(), "Archive cabinet capacity must be greater than zero");
        ArchiveModels.ArchiveCabinetView cabinet = createArchiveCabinetWithoutDuplicateCheck(
            new ArchiveModels.CreateArchiveCabinetCommand(
                cabinetCode,
                cabinetCode,
                cabinetType,
                1,
                capacity,
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                command.pathLocation(),
                command.remarks()),
            parent == null ? null : parent.id());
        return archiveRepository.findArchiveCabinetNodeByCabinetIdAndType(cabinet.id(), NODE_CABINET)
            .map(this::toArchiveCabinetNodeView)
            .orElseThrow();
    }

    private ArchiveModels.ArchiveCabinetNodeView createDrawerNode(ArchiveModels.CreateArchiveCabinetNodeCommand command) {
        ArchiveRepository.ArchiveCabinetNode parent = resolveRequiredParent(command.parentId());
        if (!NODE_CABINET.equals(parent.nodeType()) || parent.cabinetId() == null) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Drawer node parent must be cabinet");
        }
        ArchiveRepository.ArchiveCabinet cabinet = archiveRepository.findArchiveCabinetById(parent.cabinetId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive cabinet not found"));
        int capacity = requirePositive(command.capacity(), "Archive drawer capacity must be greater than zero");
        if (capacity != cabinet.slotCountPerLayer()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Archive drawer capacity must match cabinet slot count per layer");
        }
        LocalDateTime now = LocalDateTime.now();
        int nextLayerNo = cabinet.layerCount() + 1;
        for (int slot = 1; slot <= capacity; slot++) {
            archiveRepository.insertArchivePosition(new ArchiveRepository.CreateArchivePositionCommand(
                diagnosticReportSupport.nextId("AP"),
                cabinet.id(),
                "%s-L%d-S%d".formatted(cabinet.cabinetCode(), nextLayerNo, slot),
                nextLayerNo,
                slot,
                POSITION_AVAILABLE,
                null,
                now,
                now));
        }
        int nextCapacity = cabinet.capacity() + capacity;
        archiveRepository.updateArchiveCabinetCapacity(new ArchiveRepository.UpdateArchiveCabinetCapacityCommand(
            cabinet.id(),
            nextLayerNo,
            cabinet.slotCountPerLayer(),
            nextCapacity,
            now));
        archiveRepository.insertArchiveCabinetNode(new ArchiveRepository.CreateArchiveCabinetNodeCommand(
            diagnosticReportSupport.nextId("ACN"),
            parent.id(),
            "%d-%d".formatted((nextLayerNo - 1) * capacity + 1, nextLayerNo * capacity),
            NODE_DRAWER,
            cabinet.cabinetType(),
            cabinet.id(),
            nextLayerNo,
            capacity,
            optionalText(command.pathLocation()),
            optionalText(command.remarks()),
            now,
            now));
        archiveRepository.updateArchiveCabinetNodeCapacity(new ArchiveRepository.UpdateArchiveCabinetNodeCapacityCommand(
            parent.id(),
            nextCapacity,
            now));
        return archiveRepository.findArchiveCabinetNodeByCabinetIdAndLayerNo(cabinet.id(), nextLayerNo)
            .map(this::toArchiveCabinetNodeView)
            .orElseThrow();
    }

    private ArchiveModels.ArchiveCabinetView createArchiveCabinetWithoutDuplicateCheck(
        ArchiveModels.CreateArchiveCabinetCommand command,
        String parentNodeId
    ) {
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
        ensureCabinetNodes(command, cabinet, parentNodeId, now);
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

    private void ensureCabinetNodes(
        ArchiveModels.CreateArchiveCabinetCommand command,
        ArchiveRepository.ArchiveCabinet cabinet,
        String parentNodeId,
        LocalDateTime now
    ) {
        String resolvedParentNodeId = parentNodeId;
        if (resolvedParentNodeId == null) {
            resolvedParentNodeId = ensureTypeAreaNode(command.cabinetType(), now);
        }
        if (archiveRepository.findArchiveCabinetNodeByCabinetIdAndType(cabinet.id(), NODE_CABINET).isEmpty()) {
            archiveRepository.insertArchiveCabinetNode(new ArchiveRepository.CreateArchiveCabinetNodeCommand(
                diagnosticReportSupport.nextId("ACN"),
                resolvedParentNodeId,
                cabinet.cabinetCode(),
                NODE_CABINET,
                cabinet.cabinetType(),
                cabinet.id(),
                null,
                cabinet.capacity(),
                cabinet.locationDescription(),
                cabinet.remarks(),
                now,
                now));
        }
        ArchiveRepository.ArchiveCabinetNode cabinetNode = archiveRepository.findArchiveCabinetNodeByCabinetIdAndType(cabinet.id(), NODE_CABINET)
            .orElseThrow();
        for (int layer = 1; layer <= cabinet.layerCount(); layer++) {
            if (archiveRepository.findArchiveCabinetNodeByCabinetIdAndLayerNo(cabinet.id(), layer).isPresent()) {
                continue;
            }
            archiveRepository.insertArchiveCabinetNode(new ArchiveRepository.CreateArchiveCabinetNodeCommand(
                diagnosticReportSupport.nextId("ACN"),
                cabinetNode.id(),
                "%d-%d".formatted((layer - 1) * cabinet.slotCountPerLayer() + 1, layer * cabinet.slotCountPerLayer()),
                NODE_DRAWER,
                cabinet.cabinetType(),
                cabinet.id(),
                layer,
                cabinet.slotCountPerLayer(),
                cabinet.locationDescription(),
                null,
                now,
                now));
        }
    }

    private String ensureTypeAreaNode(String cabinetType, LocalDateTime now) {
        for (ArchiveRepository.ArchiveCabinetNode node : archiveRepository.findArchiveCabinetNodes()) {
            if (NODE_AREA.equals(node.nodeType()) && cabinetType.equals(node.cabinetType()) && node.parentId() == null) {
                return node.id();
            }
        }
        String nodeId = diagnosticReportSupport.nextId("ACN");
        archiveRepository.insertArchiveCabinetNode(new ArchiveRepository.CreateArchiveCabinetNodeCommand(
            nodeId,
            null,
            cabinetType,
            NODE_AREA,
            cabinetType,
            null,
            null,
            0,
            null,
            null,
            now,
            now));
        return nodeId;
    }

    private ArchiveRepository.ArchiveCabinetNode resolveOptionalParent(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        return archiveRepository.findArchiveCabinetNodeById(parentId.trim())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive cabinet parent node not found"));
    }

    private ArchiveRepository.ArchiveCabinetNode resolveRequiredParent(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Archive cabinet parent node is required");
        }
        return resolveOptionalParent(parentId);
    }

    private String normalizeNodeType(String nodeType) {
        String normalized = requireText(nodeType, "Archive cabinet node type is required").toUpperCase();
        if (!List.of(NODE_AREA, NODE_CABINET, NODE_DRAWER).contains(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported archive cabinet node type");
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value.trim();
    }

    private int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value;
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ArchiveModels.ArchiveCabinetNodeView toArchiveCabinetNodeView(ArchiveRepository.ArchiveCabinetNode node) {
        return new ArchiveModels.ArchiveCabinetNodeView(
            node.id(),
            node.parentId(),
            node.nodeCode(),
            node.nodeType(),
            node.cabinetType(),
            node.cabinetId(),
            node.layerNo(),
            node.capacity(),
            node.remainingCapacity(),
            node.pathLocation(),
            node.remarks());
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
    public ArchiveModels.ArchiveActionResult archiveSpecimen(ArchiveModels.ArchiveObjectCommand command) {
        Specimen specimen = technicalWorkflowRepository.findSpecimenById(command.objectId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen not found"));
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(specimen.caseId());
        return archiveObject(pathologyCase, specimen.id(), "SPECIMEN", specimen.id(), command, false);
    }

    @Transactional
    public List<ArchiveModels.ArchiveActionResult> batchArchiveEmbeddingBoxes(ArchiveModels.BatchArchiveObjectCommand command) {
        return batchArchiveObjects("EMBEDDING_BOX", command);
    }

    @Transactional
    public List<ArchiveModels.ArchiveActionResult> batchArchiveSlides(ArchiveModels.BatchArchiveObjectCommand command) {
        return batchArchiveObjects("SLIDE", command);
    }

    @Transactional
    public List<ArchiveModels.ArchiveActionResult> batchArchiveSpecimens(ArchiveModels.BatchArchiveObjectCommand command) {
        return batchArchiveObjects("SPECIMEN", command);
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
            command.borrowerPhone(),
            command.borrowerUnit(),
            command.borrowPurpose(),
            command.depositAmount(),
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
            storageRecord.archiveExpiresAt(),
            storageRecord.archiveReminderDays(),
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
            storageRecord.archiveExpiresAt(),
            storageRecord.archiveReminderDays(),
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

    @Transactional
    public ArchiveModels.MaterialLoanAbnormalRecordView createMaterialLoanAbnormalRecord(
        ArchiveModels.CreateMaterialLoanAbnormalRecordCommand command) {
        ArchiveRepository.StorageRecord storageRecord = archiveRepository.findStorageRecord(command.materialType(), command.materialId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive record not found"));
        ArchiveRepository.MaterialLoan loan = null;
        if (command.loanId() != null && !command.loanId().isBlank()) {
            loan = archiveRepository.findMaterialLoanById(command.loanId())
                .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Material loan not found"));
            if (!loan.materialType().equals(command.materialType()) || !loan.materialId().equals(command.materialId())) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Material loan does not match selected material");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String recordId = diagnosticReportSupport.nextId("MLAR");
        archiveRepository.insertMaterialLoanAbnormalRecord(new ArchiveRepository.CreateMaterialLoanAbnormalRecordCommand(
            recordId,
            storageRecord.caseId(),
            command.materialType(),
            command.materialId(),
            loan == null ? null : loan.id(),
            command.abnormalReason(),
            Boolean.TRUE.equals(command.contacted()),
            command.contactResult(),
            command.borrowedSlideNo(),
            command.borrowerName(),
            command.borrowerRelationship(),
            command.borrowerPhone(),
            command.borrowerUnit(),
            command.borrowerIdentityNo(),
            command.borrowedAt(),
            command.expectedReturnAt(),
            command.slideCount(),
            command.depositAmount(),
            command.borrowedContent(),
            command.returnAbnormalInfo(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            now,
            now));
        diagnosticReportSupport.insertWorkflowEvent(storageRecord.caseId(), "ARCHIVE", "LOAN_ABNORMAL", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.materialType() + ":" + command.materialId());
        return new ArchiveModels.MaterialLoanAbnormalRecordView(
            recordId,
            storageRecord.caseId(),
            command.materialType(),
            command.materialId(),
            loan == null ? null : loan.id(),
            command.abnormalReason(),
            Boolean.TRUE.equals(command.contacted()),
            command.contactResult(),
            command.operatorName(),
            now.toString());
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
                command.archiveExpiresAt(),
                command.archiveReminderDays(),
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
                command.archiveExpiresAt(),
                command.archiveReminderDays(),
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

    private List<ArchiveModels.ArchiveActionResult> batchArchiveObjects(
        String objectType,
        ArchiveModels.BatchArchiveObjectCommand command
    ) {
        List<String> objectIds = command.objectIds().stream()
            .filter(objectId -> objectId != null && !objectId.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
        if (objectIds.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Archive object ids are required");
        }

        ArchiveRepository.ArchiveCabinet cabinet = archiveRepository.findArchiveCabinetById(command.archiveCabinetId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Archive cabinet not found"));
        if (CABINET_DISABLED.equals(cabinet.cabinetStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Archive cabinet is disabled");
        }

        List<ArchiveRepository.ArchivePosition> positions =
            archiveRepository.findAvailableArchivePositionsByCabinetId(cabinet.id(), objectIds.size());
        if (positions.size() < objectIds.size()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Archive cabinet does not have enough available positions");
        }

        List<ArchiveModels.ArchiveActionResult> results = new ArrayList<>();
        for (int index = 0; index < objectIds.size(); index++) {
            ArchiveModels.ArchiveObjectCommand itemCommand = new ArchiveModels.ArchiveObjectCommand(
                objectIds.get(index),
                positions.get(index).id(),
                command.archiveCabinetId(),
                command.operatorUserId(),
                command.operatorName(),
                command.terminalCode(),
                null,
                null,
                "SPECIMEN".equals(objectType) ? command.archiveExpiresAt() : null,
                "SPECIMEN".equals(objectType) ? command.archiveReminderDays() : null,
                command.remarks());
            results.add(switch (objectType) {
                case "EMBEDDING_BOX" -> archiveEmbeddingBox(itemCommand);
                case "SLIDE" -> archiveSlide(itemCommand);
                case "SPECIMEN" -> archiveSpecimen(itemCommand);
                default -> throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported archive object type");
            });
        }
        return results;
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
            loan.patientId(),
            loan.patientName(),
            loan.patientGender(),
            loan.inpatientNo(),
            loan.wardName(),
            loan.materialType(),
            loan.materialId(),
            loan.objectCode(),
            loan.loanStatus(),
            loan.borrowedByName(),
            loan.borrowedAt() == null ? null : loan.borrowedAt().toString(),
            loan.borrowerPhone(),
            loan.borrowerUnit(),
            loan.borrowPurpose(),
            loan.depositAmount(),
            loan.approvedByName(),
            loan.returnedByName(),
            loan.returnedAt() == null ? null : loan.returnedAt().toString(),
            loan.remarks());
    }
}
