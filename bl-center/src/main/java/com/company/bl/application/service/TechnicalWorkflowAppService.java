package com.company.bl.application.service;

import com.company.bl.application.gateway.TechnicalMarkingGateway;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.masterdata.infrastructure.SamplingJdbcRepository;
import com.company.bl.support.application.NumberingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TechnicalWorkflowAppService {

    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    private static final String TASK_COMPLETED = "COMPLETED";
    private static final String TASK_RETURNED = "RETURNED";
    private static final String OBJECT_CASE = "CASE";
    private static final String OBJECT_SAMPLING_BLOCK = "SAMPLING_BLOCK";
    private static final String OBJECT_EMBEDDING_BOX = "EMBEDDING_BOX";
    private static final String OBJECT_SLIDE = "SLIDE";
    private static final String NODE_GROSSING = "GROSSING";
    private static final String NODE_DEHYDRATION = "DEHYDRATION";
    private static final String NODE_EMBEDDING = "EMBEDDING";
    private static final String NODE_SLICING = "SLICING";
    private static final String NODE_STAINING = "STAINING";
    private static final String NODE_REWORK = "REWORK";
    private static final String NODE_QC = "QC";

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final NumberingService numberingService;
    private final SamplingJdbcRepository samplingJdbcRepository;
    private final TechnicalMarkingGateway technicalMarkingGateway;
    private final DiagnosticReportAppService diagnosticReportAppService;

    @Transactional(readOnly = true)
    public PendingTechnicalTaskPage listPendingTasks(PendingTechnicalTaskQuery query) {
        TechnicalWorkflowRepository.PagedTechnicalTasks paged = technicalWorkflowRepository.findTechnicalTasks(
            new TechnicalWorkflowRepository.PendingTechnicalTaskQuery(
                query.page(),
                query.size(),
                query.taskType(),
                query.taskStatus(),
                query.applicationNo(),
                query.pathologyNo(),
                query.objectType()));
        return new PendingTechnicalTaskPage(
            paged.items().stream().map(this::toTaskView).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional
    public TaskStartResult startGrossing(TaskStartCommand command) {
        TechnicalWorkflowRepository.TechnicalTask task = startTask(command, NODE_GROSSING, OBJECT_CASE);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "SAMPLING");
        insertWorkflowEvent(task, NODE_GROSSING, "START", "SUCCESS", command.operatorUserId(), command.operatorName(),
            command.terminalCode(), "Grossing started");
        return new TaskStartResult(task.id(), task.caseId(), "SAMPLING", TASK_IN_PROGRESS);
    }

    @Transactional
    public GrossingResult completeGrossing(GrossingCompleteCommand command) {
        TechnicalWorkflowRepository.TechnicalTask task = requireActiveTask(command.taskId(), NODE_GROSSING, OBJECT_CASE);
        PathologyCase pathologyCase = getCase(task.caseId());
        LocalDateTime now = LocalDateTime.now();
        int nextTaskCount = 0;
        for (GrossingSpecimenItem item : command.specimens()) {
            Specimen specimen = getSpecimen(item.specimenId());
            ensureSameCase(pathologyCase.id(), specimen.caseId());
            String templateId = resolveSamplingTemplateId(item.specimenType(), item.bodyPartId(), item.samplingTemplateId());
            String samplingId = nextId("SMP");
            technicalWorkflowRepository.insertSampling(new TechnicalWorkflowRepository.CreateSamplingCommand(
                samplingId,
                pathologyCase.id(),
                specimen.id(),
                TASK_COMPLETED,
                item.blocks().size(),
                item.mediaAssets().size(),
                templateId,
                item.grossDescription(),
                command.operatorUserId(),
                command.operatorName(),
                now,
                command.remarks()));
            int sequenceNo = 0;
            for (GrossingBlockItem block : item.blocks()) {
                String blockId = nextId("SBK");
                String blockCode = numberingService.generateBlockNo(pathologyCase.id());
                String embeddingBoxNo = "BX-" + blockCode;
                technicalWorkflowRepository.insertSamplingBlock(new TechnicalWorkflowRepository.CreateSamplingBlockCommand(
                    blockId,
                    pathologyCase.id(),
                    specimen.id(),
                    samplingId,
                    ++sequenceNo,
                    blockCode,
                    block.blockSite(),
                    block.blockDescription(),
                    embeddingBoxNo,
                    block.specialRequirement()));
                createTechnicalTaskIfAbsent(task.applicationId(), pathologyCase.id(), specimen.id(), NODE_DEHYDRATION,
                    OBJECT_SAMPLING_BLOCK, blockId, task.id(), "blockCode=" + blockCode + ";embeddingBoxNo=" + embeddingBoxNo);
                nextTaskCount++;
            }
            storeMediaAssets(pathologyCase, specimen.id(), "SAMPLING", "GROSS_IMAGE", samplingId, item.mediaAssets(), command, now);
            insertWorkflowEvent(task.applicationId(), specimen.id(), pathologyCase.id(), NODE_GROSSING, "COMPLETE", "SUCCESS",
                command.operatorUserId(), command.operatorName(), command.terminalCode(),
                "Grossing completed for specimen " + specimen.specimenNo());
        }
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TASK_COMPLETED, command.remarks(), now);
        return new GrossingResult(task.id(), pathologyCase.id(), "SAMPLING", nextTaskCount);
    }

    @Transactional
    public DehydrationBatchResult createDehydrationBatch(CreateDehydrationBatchCommand command) {
        PathologyCase pathologyCase = getCase(command.caseId());
        LocalDateTime now = LocalDateTime.now();
        List<TechnicalWorkflowRepository.SamplingBlock> blocks = technicalWorkflowRepository.findSamplingBlocksByIds(command.samplingBlockIds());
        if (blocks.size() != command.samplingBlockIds().size()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Sampling block not found");
        }
        String batchId = nextId("DB");
        String batchNo = "DB-" + UUID.randomUUID().toString().substring(0, 8);
        technicalWorkflowRepository.insertDehydrationBatch(new TechnicalWorkflowRepository.CreateDehydrationBatchCommand(
            batchId,
            command.caseId(),
            batchNo,
            TASK_PENDING,
            command.basketNo(),
            command.deviceNo(),
            command.operatorUserId(),
            command.operatorName(),
            command.remarks(),
            now));
        for (TechnicalWorkflowRepository.SamplingBlock block : blocks) {
            requireActiveTaskByObject(NODE_DEHYDRATION, OBJECT_SAMPLING_BLOCK, block.id());
            technicalWorkflowRepository.insertDehydrationBatchItem(new TechnicalWorkflowRepository.CreateDehydrationBatchItemCommand(
                nextId("DBI"),
                batchId,
                block.caseId(),
                block.specimenId(),
                block.id(),
                "LOADED",
                now,
                command.remarks()));
        }
        insertWorkflowEvent(pathologyCase.applicationId(), null, pathologyCase.id(), NODE_DEHYDRATION, "CREATE_BATCH", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Dehydration batch created");
        return new DehydrationBatchResult(batchId, batchNo, TASK_PENDING, blocks.size());
    }

    @Transactional
    public DehydrationBatchResult startDehydrationBatch(BatchOperatorCommand command) {
        TechnicalWorkflowRepository.DehydrationBatch batch = getDehydrationBatch(command.batchId());
        LocalDateTime now = LocalDateTime.now();
        for (TechnicalWorkflowRepository.DehydrationBatchItem item : technicalWorkflowRepository.findDehydrationBatchItems(batch.id())) {
            TechnicalWorkflowRepository.TechnicalTask task = requireActiveTaskByObject(NODE_DEHYDRATION, OBJECT_SAMPLING_BLOCK, item.samplingBlockId());
            technicalWorkflowRepository.startTechnicalTask(task.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
            technicalWorkflowRepository.updateDehydrationBatchItemStatus(batch.id(), item.samplingBlockId(), TASK_IN_PROGRESS, command.remarks());
        }
        technicalWorkflowRepository.updateDehydrationBatchStatus(batch.id(), TASK_IN_PROGRESS,
            command.operatorUserId(), command.operatorName(), now, null, command.remarks());
        technicalWorkflowRepository.updatePathologyCaseStatus(batch.caseId(), "DEHYDRATION");
        insertWorkflowEvent(getCase(batch.caseId()).applicationId(), null, batch.caseId(), NODE_DEHYDRATION, "START", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Dehydration started");
        return new DehydrationBatchResult(batch.id(), batch.batchNo(), TASK_IN_PROGRESS,
            technicalWorkflowRepository.findDehydrationBatchItems(batch.id()).size());
    }

    @Transactional
    public DehydrationBatchResult completeDehydrationBatch(CompleteDehydrationBatchCommand command) {
        TechnicalWorkflowRepository.DehydrationBatch batch = getDehydrationBatch(command.batchId());
        LocalDateTime now = LocalDateTime.now();
        List<TechnicalWorkflowRepository.DehydrationBatchItem> items = technicalWorkflowRepository.findDehydrationBatchItems(batch.id());
        for (TechnicalWorkflowRepository.DehydrationBatchItem item : items) {
            TechnicalWorkflowRepository.TechnicalTask dehydrationTask = requireActiveTaskByObject(NODE_DEHYDRATION, OBJECT_SAMPLING_BLOCK, item.samplingBlockId());
            technicalWorkflowRepository.completeTechnicalTask(dehydrationTask.id(), TASK_COMPLETED, command.remarks(), now);
            technicalWorkflowRepository.updateDehydrationBatchItemStatus(batch.id(), item.samplingBlockId(), TASK_COMPLETED, command.remarks());
            createTechnicalTaskIfAbsent(dehydrationTask.applicationId(), dehydrationTask.caseId(), dehydrationTask.specimenId(), NODE_EMBEDDING,
                OBJECT_SAMPLING_BLOCK, item.samplingBlockId(), dehydrationTask.id(), null);
        }
        storeMediaAssets(getCase(batch.caseId()), null, "DEHYDRATION", "DEHYDRATION_IMAGE", batch.id(), command.mediaAssets(), command, now);
        technicalWorkflowRepository.updateDehydrationBatchStatus(batch.id(), TASK_COMPLETED,
            command.operatorUserId(), command.operatorName(), null, now, command.remarks());
        insertWorkflowEvent(getCase(batch.caseId()).applicationId(), null, batch.caseId(), NODE_DEHYDRATION, "COMPLETE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Dehydration completed");
        return new DehydrationBatchResult(batch.id(), batch.batchNo(), TASK_COMPLETED, items.size());
    }

    @Transactional
    public TaskStartResult startEmbedding(TaskStartCommand command) {
        TechnicalWorkflowRepository.TechnicalTask task = startTask(command, NODE_EMBEDDING, OBJECT_SAMPLING_BLOCK);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "EMBEDDING");
        insertWorkflowEvent(task, NODE_EMBEDDING, "START", "SUCCESS", command.operatorUserId(), command.operatorName(),
            command.terminalCode(), "Embedding started");
        return new TaskStartResult(task.id(), task.caseId(), "EMBEDDING", TASK_IN_PROGRESS);
    }

    @Transactional
    public EmbeddingResult completeEmbedding(EmbeddingCompleteCommand command) {
        TechnicalWorkflowRepository.TechnicalTask task = requireActiveTask(command.taskId(), NODE_EMBEDDING, OBJECT_SAMPLING_BLOCK);
        TechnicalWorkflowRepository.SamplingBlock block = getSamplingBlock(command.samplingBlockId());
        validateTaskObject(task, block.id());
        LocalDateTime now = LocalDateTime.now();
        String embeddingId = nextId("EMB");
        technicalWorkflowRepository.insertEmbedding(new TechnicalWorkflowRepository.CreateEmbeddingCommand(
            embeddingId,
            task.caseId(),
            block.specimenId(),
            block.samplingId(),
            block.id(),
            TASK_COMPLETED,
            command.evaluationLevel(),
            command.samplingEvaluation(),
            task.startedAt() == null ? now : task.startedAt(),
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.remarks()));
        String embeddingBoxNo = command.embeddingBoxNo() == null || command.embeddingBoxNo().isBlank()
            ? block.embeddingBoxNo()
            : command.embeddingBoxNo().trim();
        String embeddingBoxId = nextId("BOX");
        technicalWorkflowRepository.insertEmbeddingBox(new TechnicalWorkflowRepository.CreateEmbeddingBoxCommand(
            embeddingBoxId,
            task.caseId(),
            block.specimenId(),
            block.id(),
            embeddingId,
            embeddingBoxNo,
            command.blockCount(),
            false,
            command.sliceNotice(),
            "ACTIVE"));
        TechnicalMarkingGateway.MarkingResult markingResult = markObject(task.caseId(), OBJECT_EMBEDDING_BOX, embeddingBoxId,
            command.deviceCode(), embeddingBoxNo, command.operatorUserId(), command.operatorName(), command.terminalCode(), NODE_EMBEDDING);
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TASK_COMPLETED, command.remarks(), now);
        createTechnicalTaskIfAbsent(task.applicationId(), task.caseId(), block.specimenId(), NODE_SLICING,
            OBJECT_EMBEDDING_BOX, embeddingBoxId, task.id(), "embeddingBoxNo=" + embeddingBoxNo);
        return new EmbeddingResult(task.id(), embeddingId, embeddingBoxId, "EMBEDDING", markingResult.success(), markingResult.message());
    }

    @Transactional
    public TaskStartResult startSlicing(TaskStartCommand command) {
        TechnicalWorkflowRepository.TechnicalTask task = startTask(command, NODE_SLICING, OBJECT_EMBEDDING_BOX);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "SLICING");
        insertWorkflowEvent(task, NODE_SLICING, "START", "SUCCESS", command.operatorUserId(), command.operatorName(),
            command.terminalCode(), "Slicing started");
        return new TaskStartResult(task.id(), task.caseId(), "SLICING", TASK_IN_PROGRESS);
    }

    @Transactional
    public SlicingResult completeSlicing(SlicingCompleteCommand command) {
        TechnicalWorkflowRepository.TechnicalTask task = requireActiveTask(command.taskId(), NODE_SLICING, OBJECT_EMBEDDING_BOX);
        TechnicalWorkflowRepository.EmbeddingBox box = getEmbeddingBox(command.embeddingBoxId());
        validateTaskObject(task, box.id());
        LocalDateTime now = LocalDateTime.now();
        String slicingId = nextId("SLC");
        String slicingBatchNo = "SLC-" + UUID.randomUUID().toString().substring(0, 8);
        technicalWorkflowRepository.insertSlicing(new TechnicalWorkflowRepository.CreateSlicingCommand(
            slicingId,
            task.caseId(),
            box.specimenId(),
            box.embeddingId(),
            box.id(),
            slicingBatchNo,
            TASK_COMPLETED,
            command.slideCount(),
            command.sliceCountPerSlide(),
            command.sliceThickness(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.qualityIssue(),
            command.remarks()));
        List<String> slideIds = new ArrayList<>();
        for (int i = 0; i < command.slideCount(); i++) {
            String slideId = nextId("SLD");
            String slideNo = numberingService.generateSlideNo();
            technicalWorkflowRepository.insertSlide(new TechnicalWorkflowRepository.CreateSlideCommand(
                slideId,
                task.caseId(),
                box.specimenId(),
                slicingId,
                box.id(),
                box.samplingBlockId(),
                slideNo,
                slideNo,
                false,
                "PENDING",
                "CREATED",
                command.sliceCountPerSlide()));
            TechnicalMarkingGateway.MarkingResult result = markObject(task.caseId(), OBJECT_SLIDE, slideId,
                command.deviceCode(), slideNo, command.operatorUserId(), command.operatorName(), command.terminalCode(), NODE_SLICING);
            if (!result.success()) {
                insertWorkflowEvent(task.applicationId(), box.specimenId(), task.caseId(), NODE_SLICING, "MARK", "FAILED",
                    command.operatorUserId(), command.operatorName(), command.terminalCode(), result.message());
            }
            createTechnicalTaskIfAbsent(task.applicationId(), task.caseId(), box.specimenId(), NODE_STAINING,
                OBJECT_SLIDE, slideId, task.id(), "slideNo=" + slideNo);
            slideIds.add(slideId);
        }
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TASK_COMPLETED, command.remarks(), now);
        insertWorkflowEvent(task.applicationId(), box.specimenId(), task.caseId(), NODE_SLICING, "COMPLETE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Slicing completed");
        return new SlicingResult(task.id(), slicingId, slideIds, "SLICING");
    }

    @Transactional
    public TaskStartResult startSlideStaining(TaskStartCommand command) {
        TechnicalWorkflowRepository.TechnicalTask task = startTask(command, NODE_STAINING, OBJECT_SLIDE);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "STAINING");
        insertWorkflowEvent(task, NODE_STAINING, "START", "SUCCESS", command.operatorUserId(), command.operatorName(),
            command.terminalCode(), "Staining started");
        return new TaskStartResult(task.id(), task.caseId(), "STAINING", TASK_IN_PROGRESS);
    }

    @Transactional
    public SlideStainingResult completeSlideStaining(SlideStainingCompleteCommand command) {
        TechnicalWorkflowRepository.TechnicalTask task = requireActiveTask(command.taskId(), NODE_STAINING, OBJECT_SLIDE);
        TechnicalWorkflowRepository.Slide slide = getSlide(command.slideId());
        validateTaskObject(task, slide.id());
        LocalDateTime now = LocalDateTime.now();
        technicalWorkflowRepository.insertSlideStaining(new TechnicalWorkflowRepository.CreateSlideStainingCommand(
            nextId("STN"),
            task.caseId(),
            slide.specimenId(),
            slide.id(),
            command.stainingType(),
            TASK_COMPLETED,
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.qualityIssue(),
            command.remarks()));
        technicalWorkflowRepository.updateSlideStatus(slide.id(), "STAINED", command.qualityIssue() == null ? "QUALIFIED" : "UNQUALIFIED");
        technicalWorkflowRepository.completeTechnicalTask(task.id(), TASK_COMPLETED, command.remarks(), now);
        boolean hasRemainingStainingTasks = technicalWorkflowRepository.findActiveTechnicalTasksByCaseId(task.caseId()).stream()
            .anyMatch(activeTask -> NODE_STAINING.equals(activeTask.taskType()));
        String caseStatus = "STAINING";
        if (!hasRemainingStainingTasks) {
            technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "DIAGNOSIS_PENDING");
            diagnosticReportAppService.createPrimaryDiagnosticTaskIfAbsent(task.caseId(), "Auto created after staining completed");
            insertWorkflowEvent(task.applicationId(), slide.specimenId(), task.caseId(), "DIAGNOSIS_ASSIGN", "CREATE", "SUCCESS",
                command.operatorUserId(), command.operatorName(), command.terminalCode(), "Technical workflow handed off to diagnosis");
            caseStatus = "DIAGNOSIS_PENDING";
        }
        insertWorkflowEvent(task.applicationId(), slide.specimenId(), task.caseId(), NODE_STAINING, "COMPLETE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Staining completed");
        return new SlideStainingResult(task.id(), slide.id(), caseStatus);
    }

    @Transactional
    public ReworkOrderResult createReworkOrder(CreateReworkOrderCommand command) {
        PathologyCase pathologyCase = getCase(command.caseId());
        LocalDateTime now = LocalDateTime.now();
        technicalWorkflowRepository.insertReworkOrder(new TechnicalWorkflowRepository.CreateReworkOrderCommand(
            nextId("RW"),
            command.caseId(),
            command.specimenId(),
            command.samplingBlockId(),
            command.embeddingBoxId(),
            command.slideId(),
            command.reworkType(),
            TASK_PENDING,
            command.reason(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.remarks()));
        if (command.slideId() != null && !command.slideId().isBlank()) {
            technicalWorkflowRepository.insertSlideQcEvaluation(new TechnicalWorkflowRepository.CreateSlideQcEvaluationCommand(
                nextId("QC"),
                command.caseId(),
                command.specimenId(),
                command.slideId(),
                command.qcType() == null || command.qcType().isBlank() ? "HE" : command.qcType(),
                "REWORK_REQUIRED",
                command.reason(),
                command.remarks(),
                command.operatorUserId(),
                command.operatorName(),
                now,
                command.remarks()));
            insertWorkflowEvent(pathologyCase.applicationId(), command.specimenId(), command.caseId(), NODE_QC, "EVALUATE", "REWORK_REQUIRED",
                command.operatorUserId(), command.operatorName(), command.terminalCode(), command.reason());
        }
        insertWorkflowEvent(pathologyCase.applicationId(), command.specimenId(), command.caseId(), NODE_REWORK, "CREATE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.reason());
        return new ReworkOrderResult(command.caseId(), command.reworkType(), TASK_PENDING);
    }

    @Transactional
    public ReworkOrderResult executeReworkOrder(ExecuteReworkOrderCommand command) {
        TechnicalWorkflowRepository.ReworkOrder order = technicalWorkflowRepository.findReworkOrderById(command.reworkOrderId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Rework order not found"));
        PathologyCase pathologyCase = getCase(order.caseId());
        LocalDateTime now = LocalDateTime.now();
        String taskType = switch (order.reworkType()) {
            case "REEMBED" -> NODE_EMBEDDING;
            case "RECUT", "DEEPER_CUT", "ADD_SLIDE" -> NODE_SLICING;
            case "RESTAIN" -> NODE_STAINING;
            default -> NODE_GROSSING;
        };
        String objectType;
        String objectId;
        String specimenId = order.specimenId();
        if ("REEMBED".equals(order.reworkType())) {
            objectType = OBJECT_SAMPLING_BLOCK;
            objectId = requireText(order.samplingBlockId(), "Sampling block id is required for re-embed");
        } else if ("RECUT".equals(order.reworkType()) || "DEEPER_CUT".equals(order.reworkType()) || "ADD_SLIDE".equals(order.reworkType())) {
            TechnicalWorkflowRepository.Slide slide = getSlide(requireText(order.slideId(), "Slide id is required for re-cut"));
            objectType = OBJECT_EMBEDDING_BOX;
            objectId = slide.embeddingBoxId();
            specimenId = slide.specimenId();
        } else if ("RESTAIN".equals(order.reworkType())) {
            objectType = OBJECT_SLIDE;
            objectId = requireText(order.slideId(), "Slide id is required for re-stain");
        } else {
            objectType = OBJECT_CASE;
            objectId = pathologyCase.id();
        }
        List<TechnicalWorkflowRepository.TechnicalTask> activeTasks =
            technicalWorkflowRepository.findActiveTechnicalTasksByObject(taskType, objectType, objectId);
        for (TechnicalWorkflowRepository.TechnicalTask activeTask : activeTasks) {
            technicalWorkflowRepository.completeTechnicalTask(activeTask.id(), TASK_RETURNED, command.remarks(), now);
        }
        String parentTaskId = activeTasks.isEmpty() ? null : activeTasks.get(0).id();
        createTechnicalTaskIfAbsent(pathologyCase.applicationId(), pathologyCase.id(), specimenId, taskType, objectType, objectId,
            parentTaskId, "reworkOrderId=" + order.id() + ";reworkType=" + order.reworkType());
        technicalWorkflowRepository.updateReworkOrderStatus(order.id(), TASK_COMPLETED,
            command.operatorUserId(), command.operatorName(), now, command.remarks());
        insertWorkflowEvent(pathologyCase.applicationId(), specimenId, pathologyCase.id(), NODE_REWORK, "EXECUTE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), order.reason());
        return new ReworkOrderResult(order.caseId(), order.reworkType(), TASK_COMPLETED);
    }

    @Transactional(readOnly = true)
    public TechnicalTrackingView getTechnicalTracking(String caseId) {
        PathologyCase pathologyCase = getCase(caseId);
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        List<TechnicalWorkflowRepository.TechnicalTask> tasks = technicalWorkflowRepository.findActiveTechnicalTasksByCaseId(caseId);
        List<TechnicalWorkflowRepository.SamplingBlock> blocks = technicalWorkflowRepository.findSamplingBlocksByCaseId(caseId);
        List<TechnicalWorkflowRepository.EmbeddingBox> boxes = technicalWorkflowRepository.findEmbeddingBoxesByCaseId(caseId);
        List<TechnicalWorkflowRepository.Slide> slides = technicalWorkflowRepository.findSlidesByCaseId(caseId);
        List<TechnicalWorkflowRepository.ReworkOrder> reworkOrders = technicalWorkflowRepository.findReworkOrdersByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(caseId);
        Map<String, List<TechnicalWorkflowRepository.Slide>> slidesByBox = slides.stream()
            .collect(Collectors.groupingBy(TechnicalWorkflowRepository.Slide::embeddingBoxId));
        return new TechnicalTrackingView(
            pathologyCase.id(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            tasks.stream().map(this::toTaskView).toList(),
            specimens.stream().map(specimen -> new TechnicalSpecimenSummary(
                specimen.id(), specimen.specimenNo(), specimen.barcode(), specimen.specimenNameStandardized(), specimen.specimenStatus().name()))
                .toList(),
            blocks.stream().map(block -> new TechnicalBlockSummary(
                block.id(), block.specimenId(), block.blockCode(), block.embeddingBoxNo(), block.blockDescription()))
                .toList(),
            boxes.stream().map(box -> new TechnicalEmbeddingBoxSummary(
                box.id(), box.specimenId(), box.embeddingBoxNo(), box.sliceNotice(),
                slidesByBox.getOrDefault(box.id(), List.of()).size()))
                .toList(),
            slides.stream().map(slide -> new TechnicalSlideSummary(
                slide.id(), slide.specimenId(), slide.embeddingBoxId(), slide.slideNo(), slide.slideStatus(), slide.qualityStatus()))
                .toList(),
            reworkOrders.stream().map(order -> new ReworkSummary(
                order.id(), order.reworkType(), order.status(), order.reason()))
                .toList(),
            events.stream()
                .sorted(Comparator.comparing(TrackingEvent::eventTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(event -> new TechnicalTrackingEvent(
                    event.nodeCode(), event.eventType(), event.eventStatus(),
                    event.eventTime() == null ? null : event.eventTime().toString(), event.operatorName(), event.eventContent()))
                .toList());
    }

    private TaskView toTaskView(TechnicalWorkflowRepository.TechnicalTask task) {
        return new TaskView(
            task.id(),
            task.applicationId(),
            task.applicationNo(),
            task.caseId(),
            task.pathologyNo(),
            task.specimenId(),
            task.taskType(),
            task.taskStatus(),
            task.objectType(),
            task.objectId(),
            task.payload(),
            task.remarks(),
            stringify(task.createdAt()),
            stringify(task.startedAt()),
            stringify(task.completedAt()));
    }

    private TechnicalWorkflowRepository.TechnicalTask startTask(TaskStartCommand command, String taskType, String objectType) {
        TechnicalWorkflowRepository.TechnicalTask task = requireActiveTask(command.taskId(), taskType, objectType);
        LocalDateTime now = LocalDateTime.now();
        technicalWorkflowRepository.startTechnicalTask(task.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        return technicalWorkflowRepository.findTechnicalTaskById(task.id()).orElse(task);
    }

    private TechnicalWorkflowRepository.TechnicalTask requireActiveTask(String taskId, String taskType, String objectType) {
        TechnicalWorkflowRepository.TechnicalTask task = technicalWorkflowRepository.findTechnicalTaskById(taskId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Technical task not found"));
        if (!taskType.equals(task.taskType()) || !objectType.equals(task.objectType())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Technical task type mismatch");
        }
        if (!TASK_PENDING.equals(task.taskStatus()) && !TASK_IN_PROGRESS.equals(task.taskStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Technical task is not active");
        }
        return task;
    }

    private TechnicalWorkflowRepository.TechnicalTask requireActiveTaskByObject(String taskType, String objectType, String objectId) {
        List<TechnicalWorkflowRepository.TechnicalTask> tasks = technicalWorkflowRepository.findActiveTechnicalTasksByObject(taskType, objectType, objectId);
        if (tasks.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Active technical task not found");
        }
        return tasks.get(0);
    }

    private void createTechnicalTaskIfAbsent(String applicationId,
                                             String caseId,
                                             String specimenId,
                                             String taskType,
                                             String objectType,
                                             String objectId,
                                             String parentTaskId,
                                             String payload) {
        if (!technicalWorkflowRepository.findActiveTechnicalTasksByObject(taskType, objectType, objectId).isEmpty()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Active technical task already exists");
        }
        technicalWorkflowRepository.insertTechnicalTask(new TechnicalWorkflowRepository.CreateTechnicalTaskCommand(
            nextId("TT"),
            applicationId,
            caseId,
            specimenId,
            taskType,
            TASK_PENDING,
            objectType,
            objectId,
            parentTaskId,
            payload,
            null,
            LocalDateTime.now()));
    }

    private String resolveSamplingTemplateId(String specimenType, String bodyPartId, String explicitTemplateId) {
        if (explicitTemplateId != null && !explicitTemplateId.isBlank()) {
            if (samplingJdbcRepository.findTemplateById(explicitTemplateId.trim()) == null) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Sampling template not found");
            }
            return explicitTemplateId.trim();
        }
        if (bodyPartId == null || bodyPartId.isBlank() || specimenType == null || specimenType.isBlank()) {
            return null;
        }
        Map<String, List<SamplingJdbcRepository.TemplateSiteRow>> siteMap = samplingJdbcRepository.findTemplateSites().stream()
            .collect(Collectors.groupingBy(SamplingJdbcRepository.TemplateSiteRow::templateId));
        List<SamplingJdbcRepository.TemplateRow> matches = samplingJdbcRepository.findTemplates().stream()
            .filter(SamplingJdbcRepository.TemplateRow::enabled)
            .filter(template -> specimenType.trim().equalsIgnoreCase(nullToBlank(template.applicableSpecimenType())))
            .filter(template -> siteMap.getOrDefault(template.id(), List.of()).stream()
                .anyMatch(site -> bodyPartId.trim().equals(site.bodyPartId())))
            .toList();
        return matches.size() == 1 ? matches.get(0).id() : null;
    }

    private void storeMediaAssets(PathologyCase pathologyCase,
                                  String specimenId,
                                  String objectType,
                                  String mediaType,
                                  String objectId,
                                  List<MediaAssetInput> mediaAssets,
                                  OperatorCarrier operatorCarrier,
                                  LocalDateTime now) {
        for (MediaAssetInput asset : mediaAssets) {
            technicalWorkflowRepository.insertCaseMediaAsset(new TechnicalWorkflowRepository.CreateCaseMediaAssetCommand(
                nextId("MED"),
                pathologyCase.id(),
                specimenId,
                objectType,
                objectId,
                mediaType,
                asset.fileUrl(),
                asset.fileName(),
                now,
                operatorCarrier.operatorUserId(),
                operatorCarrier.operatorName(),
                operatorCarrier.remarks()));
        }
    }

    private TechnicalMarkingGateway.MarkingResult markObject(String caseId,
                                                             String objectType,
                                                             String objectId,
                                                             String deviceCode,
                                                             String label,
                                                             String operatorUserId,
                                                             String operatorName,
                                                             String terminalCode,
                                                             String nodeCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return new TechnicalMarkingGateway.MarkingResult(true, "Technical marking skipped");
        }
        TechnicalMarkingGateway.MarkingResult result = technicalMarkingGateway.mark(
            new TechnicalMarkingGateway.MarkingRequest(caseId, objectType, objectId, deviceCode, label));
        insertWorkflowEvent(getCase(caseId).applicationId(), null, caseId, nodeCode, "MARK",
            result.success() ? "SUCCESS" : "FAILED", operatorUserId, operatorName, terminalCode, result.message());
        return result;
    }

    private void validateTaskObject(TechnicalWorkflowRepository.TechnicalTask task, String objectId) {
        if (!objectId.equals(task.objectId())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Task object mismatch");
        }
    }

    private PathologyCase getCase(String caseId) {
        return technicalWorkflowRepository.findPathologyCaseById(caseId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology case not found"));
    }

    private Specimen getSpecimen(String specimenId) {
        return technicalWorkflowRepository.findSpecimenById(specimenId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen not found"));
    }

    private TechnicalWorkflowRepository.SamplingBlock getSamplingBlock(String samplingBlockId) {
        return technicalWorkflowRepository.findSamplingBlockById(samplingBlockId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Sampling block not found"));
    }

    private TechnicalWorkflowRepository.EmbeddingBox getEmbeddingBox(String embeddingBoxId) {
        return technicalWorkflowRepository.findEmbeddingBoxById(embeddingBoxId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Embedding box not found"));
    }

    private TechnicalWorkflowRepository.Slide getSlide(String slideId) {
        return technicalWorkflowRepository.findSlideById(slideId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Slide not found"));
    }

    private TechnicalWorkflowRepository.DehydrationBatch getDehydrationBatch(String batchId) {
        return technicalWorkflowRepository.findDehydrationBatchById(batchId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Dehydration batch not found"));
    }

    private void ensureSameCase(String caseId, String actualCaseId) {
        if (!caseId.equals(actualCaseId)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Object does not belong to case");
        }
    }

    private void insertWorkflowEvent(TechnicalWorkflowRepository.TechnicalTask task,
                                     String nodeCode,
                                     String eventType,
                                     String eventStatus,
                                     String operatorUserId,
                                     String operatorName,
                                     String terminalCode,
                                     String content) {
        insertWorkflowEvent(task.applicationId(), task.specimenId(), task.caseId(), nodeCode, eventType, eventStatus,
            operatorUserId, operatorName, terminalCode, content);
    }

    private void insertWorkflowEvent(String applicationId,
                                     String specimenId,
                                     String caseId,
                                     String nodeCode,
                                     String eventType,
                                     String eventStatus,
                                     String operatorUserId,
                                     String operatorName,
                                     String terminalCode,
                                     String content) {
        technicalWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            nextId("EVT"),
            applicationId,
            specimenId,
            caseId,
            null,
            nodeCode,
            eventType,
            eventStatus,
            LocalDateTime.now(),
            operatorUserId,
            operatorName,
            terminalCode,
            content));
    }

    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value.trim();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    public interface OperatorCarrier {
        String operatorUserId();
        String operatorName();
        String remarks();
    }

    public record PendingTechnicalTaskQuery(
        int page,
        int size,
        String taskType,
        String taskStatus,
        String applicationNo,
        String pathologyNo,
        String objectType
    ) {
    }

    public record PendingTechnicalTaskPage(List<TaskView> items, int page, int size, long total) {
    }

    public record TaskView(
        String id,
        String applicationId,
        String applicationNo,
        String caseId,
        String pathologyNo,
        String specimenId,
        String taskType,
        String taskStatus,
        String objectType,
        String objectId,
        String payload,
        String remarks,
        String createdAt,
        String startedAt,
        String completedAt
    ) {
    }

    public record TaskStartCommand(
        String taskId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record BatchOperatorCommand(
        String batchId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record TaskStartResult(String taskId, String caseId, String caseStatus, String taskStatus) {
    }

    public record MediaAssetInput(String fileUrl, String fileName) {
    }

    public record GrossingBlockItem(String blockSite, String blockDescription, String specialRequirement) {
    }

    public record GrossingSpecimenItem(
        String specimenId,
        String specimenType,
        String bodyPartId,
        String samplingTemplateId,
        String grossDescription,
        List<GrossingBlockItem> blocks,
        List<MediaAssetInput> mediaAssets
    ) {
    }

    public record GrossingCompleteCommand(
        String taskId,
        String caseId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        List<GrossingSpecimenItem> specimens
    ) implements OperatorCarrier {
    }

    public record GrossingResult(String taskId, String caseId, String caseStatus, int createdDehydrationTaskCount) {
    }

    public record CreateDehydrationBatchCommand(
        String caseId,
        String basketNo,
        String deviceNo,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        List<String> samplingBlockIds
    ) implements OperatorCarrier {
    }

    public record CompleteDehydrationBatchCommand(
        String batchId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks,
        List<MediaAssetInput> mediaAssets
    ) implements OperatorCarrier {
    }

    public record DehydrationBatchResult(String batchId, String batchNo, String batchStatus, int taskCount) {
    }

    public record EmbeddingCompleteCommand(
        String taskId,
        String samplingBlockId,
        String embeddingBoxNo,
        int blockCount,
        String sliceNotice,
        String evaluationLevel,
        String samplingEvaluation,
        String deviceCode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record EmbeddingResult(
        String taskId,
        String embeddingId,
        String embeddingBoxId,
        String caseStatus,
        boolean markingSuccess,
        String markingMessage
    ) {
    }

    public record SlicingCompleteCommand(
        String taskId,
        String embeddingBoxId,
        int slideCount,
        Integer sliceCountPerSlide,
        String sliceThickness,
        String qualityIssue,
        String deviceCode,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlicingResult(String taskId, String slicingId, List<String> slideIds, String caseStatus) {
    }

    public record SlideStainingCompleteCommand(
        String taskId,
        String slideId,
        String stainingType,
        String qualityIssue,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record SlideStainingResult(String taskId, String slideId, String caseStatus) {
    }

    public record CreateReworkOrderCommand(
        String caseId,
        String specimenId,
        String samplingBlockId,
        String embeddingBoxId,
        String slideId,
        String reworkType,
        String reason,
        String qcType,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record ExecuteReworkOrderCommand(
        String reworkOrderId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) implements OperatorCarrier {
    }

    public record ReworkOrderResult(String caseId, String reworkType, String status) {
    }

    public record TechnicalTrackingView(
        String caseId,
        String pathologyNo,
        String caseStatus,
        List<TaskView> technicalTasks,
        List<TechnicalSpecimenSummary> specimens,
        List<TechnicalBlockSummary> blocks,
        List<TechnicalEmbeddingBoxSummary> embeddingBoxes,
        List<TechnicalSlideSummary> slides,
        List<ReworkSummary> reworks,
        List<TechnicalTrackingEvent> events
    ) {
    }

    public record TechnicalSpecimenSummary(
        String specimenId,
        String specimenNo,
        String barcode,
        String specimenName,
        String specimenStatus
    ) {
    }

    public record TechnicalBlockSummary(
        String blockId,
        String specimenId,
        String blockCode,
        String embeddingBoxNo,
        String description
    ) {
    }

    public record TechnicalEmbeddingBoxSummary(
        String embeddingBoxId,
        String specimenId,
        String embeddingBoxNo,
        String sliceNotice,
        int slideCount
    ) {
    }

    public record TechnicalSlideSummary(
        String slideId,
        String specimenId,
        String embeddingBoxId,
        String slideNo,
        String slideStatus,
        String qualityStatus
    ) {
    }

    public record ReworkSummary(String reworkOrderId, String reworkType, String status, String reason) {
    }

    public record TechnicalTrackingEvent(
        String nodeCode,
        String eventType,
        String eventStatus,
        String eventTime,
        String operatorName,
        String eventContent
    ) {
    }
}
