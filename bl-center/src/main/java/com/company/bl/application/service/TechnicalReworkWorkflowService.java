package com.company.bl.application.service;

import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
class TechnicalReworkWorkflowService {

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;

    TechnicalReworkWorkflowService(TechnicalWorkflowRepository technicalWorkflowRepository,
                                   TechnicalWorkflowSupport technicalWorkflowSupport) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
    }

    @Transactional
    TechnicalWorkflowModels.ReworkOrderResult createReworkOrder(TechnicalWorkflowModels.CreateReworkOrderCommand command) {
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(command.caseId());
        LocalDateTime now = LocalDateTime.now();
        technicalWorkflowRepository.insertReworkOrder(new TechnicalWorkflowProcessingRecords.CreateReworkOrderCommand(
            technicalWorkflowSupport.nextId("RW"),
            command.caseId(),
            command.specimenId(),
            command.samplingBlockId(),
            command.embeddingBoxId(),
            command.slideId(),
            command.reworkType(),
            TechnicalWorkflowConstants.TASK_PENDING,
            command.reason(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.remarks()));
        if (command.slideId() != null && !command.slideId().isBlank()) {
            technicalWorkflowRepository.insertSlideQcEvaluation(new TechnicalWorkflowProcessingRecords.CreateSlideQcEvaluationCommand(
                technicalWorkflowSupport.nextId("QC"),
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
            technicalWorkflowSupport.insertWorkflowEvent(pathologyCase.applicationId(), command.specimenId(), command.caseId(),
                TechnicalWorkflowConstants.NODE_QC, "EVALUATE", "REWORK_REQUIRED", command.operatorUserId(),
                command.operatorName(), command.terminalCode(), command.reason());
        }
        technicalWorkflowSupport.insertWorkflowEvent(pathologyCase.applicationId(), command.specimenId(), command.caseId(),
            TechnicalWorkflowConstants.NODE_REWORK, "CREATE", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), command.reason());
        return new TechnicalWorkflowModels.ReworkOrderResult(command.caseId(), command.reworkType(), TechnicalWorkflowConstants.TASK_PENDING);
    }

    @Transactional
    TechnicalWorkflowModels.ReworkOrderResult executeReworkOrder(TechnicalWorkflowModels.ExecuteReworkOrderCommand command) {
        TechnicalWorkflowProcessingRecords.ReworkOrder order = technicalWorkflowRepository.findReworkOrderById(command.reworkOrderId())
            .orElseThrow(() -> new com.company.bl.domain.exception.BlBusinessException(
                com.company.bl.domain.enums.BlErrorCode.RESOURCE_NOT_FOUND, 404, "Rework order not found"));
        PathologyCase pathologyCase = technicalWorkflowSupport.getCase(order.caseId());
        LocalDateTime now = LocalDateTime.now();
        String taskType = switch (order.reworkType()) {
            case "REEMBED" -> TechnicalWorkflowConstants.NODE_EMBEDDING;
            case "RECUT", "DEEPER_CUT", "ADD_SLIDE" -> TechnicalWorkflowConstants.NODE_SLICING;
            case "RESTAIN" -> TechnicalWorkflowConstants.NODE_STAINING;
            default -> TechnicalWorkflowConstants.NODE_GROSSING;
        };
        String objectType;
        String objectId;
        String specimenId = order.specimenId();
        if ("REEMBED".equals(order.reworkType())) {
            objectType = TechnicalWorkflowConstants.OBJECT_SAMPLING_BLOCK;
            objectId = technicalWorkflowSupport.requireText(order.samplingBlockId(), "Sampling block id is required for re-embed");
        } else if ("RECUT".equals(order.reworkType()) || "DEEPER_CUT".equals(order.reworkType()) || "ADD_SLIDE".equals(order.reworkType())) {
            TechnicalWorkflowProcessingRecords.Slide slide =
                technicalWorkflowSupport.getSlide(technicalWorkflowSupport.requireText(order.slideId(), "Slide id is required for re-cut"));
            objectType = TechnicalWorkflowConstants.OBJECT_EMBEDDING_BOX;
            objectId = slide.embeddingBoxId();
            specimenId = slide.specimenId();
        } else if ("RESTAIN".equals(order.reworkType())) {
            objectType = TechnicalWorkflowConstants.OBJECT_SLIDE;
            objectId = technicalWorkflowSupport.requireText(order.slideId(), "Slide id is required for re-stain");
        } else {
            objectType = TechnicalWorkflowConstants.OBJECT_CASE;
            objectId = pathologyCase.id();
        }
        List<TechnicalWorkflowRecords.TechnicalTask> activeTasks =
            technicalWorkflowRepository.findActiveTechnicalTasksByObject(taskType, objectType, objectId);
        for (TechnicalWorkflowRecords.TechnicalTask activeTask : activeTasks) {
            technicalWorkflowRepository.completeTechnicalTask(activeTask.id(), TechnicalWorkflowConstants.TASK_RETURNED, command.remarks(), now);
        }
        String parentTaskId = activeTasks.isEmpty() ? null : activeTasks.get(0).id();
        technicalWorkflowSupport.createTechnicalTaskIfAbsent(
            pathologyCase.applicationId(),
            pathologyCase.id(),
            specimenId,
            taskType,
            objectType,
            objectId,
            parentTaskId,
            "reworkOrderId=" + order.id() + ";reworkType=" + order.reworkType());
        technicalWorkflowRepository.updateReworkOrderStatus(
            order.id(), TechnicalWorkflowConstants.TASK_COMPLETED, command.operatorUserId(), command.operatorName(), now, command.remarks());
        technicalWorkflowSupport.insertWorkflowEvent(pathologyCase.applicationId(), specimenId, pathologyCase.id(),
            TechnicalWorkflowConstants.NODE_REWORK, "EXECUTE", "SUCCESS", command.operatorUserId(),
            command.operatorName(), command.terminalCode(), order.reason());
        return new TechnicalWorkflowModels.ReworkOrderResult(order.caseId(), order.reworkType(), TechnicalWorkflowConstants.TASK_COMPLETED);
    }
}
