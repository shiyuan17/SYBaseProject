package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;

import java.util.List;
import java.util.Set;

final class MedicalOrderQcSupport {

    private static final Set<String> LIQUID_CYTOLOGY_ORDER_TYPES = Set.of(
        "LIQUID_CYTOLOGY",
        "GYNECOLOGY_LBC_CYTOLOGY",
        "NON_GYNECOLOGY_LBC_CYTOLOGY");

    private final TechnicalWorkflowRepository technicalWorkflowRepository;

    MedicalOrderQcSupport(TechnicalWorkflowRepository technicalWorkflowRepository) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
    }

    boolean canQc(MedicalOrderRepository.MedicalOrder order) {
        return canCreateTargetedQcEvaluation(order) || canCreateLiquidCytologyNoActionQcEvaluation(order);
    }

    boolean canCreateQcEvaluation(MedicalOrderRepository.MedicalOrder order,
                                  String processingAction,
                                  boolean hasSelectedSlide) {
        if ("NO_ACTION".equals(processingAction) && "ROUTINE".equalsIgnoreCase(order.orderType())) {
            return canCreateRoutineNoActionQcEvaluation(order);
        }
        if ("NO_ACTION".equals(processingAction) && isLiquidCytologyOrder(order)) {
            return canCreateLiquidCytologyNoActionQcEvaluation(order);
        }
        return canCreateTargetedQcEvaluation(order) && ("NO_ACTION".equals(processingAction) || hasSelectedSlide);
    }

    TechnicalWorkflowProcessingRecords.Slide resolveQcTargetSlide(MedicalOrderRepository.MedicalOrder order,
                                                                   String slideId) {
        if (slideId == null || slideId.isBlank()) {
            return null;
        }
        return resolveQcTargetSlides(order).stream()
            .filter(slide -> slide.id().equals(slideId))
            .findFirst()
            .orElse(null);
    }

    List<TechnicalWorkflowProcessingRecords.Slide> resolveQcTargetSlides(
        MedicalOrderRepository.MedicalOrder order
    ) {
        String targetType = resolveQcTargetType(order);
        return technicalWorkflowRepository.findSlidesByCaseId(order.caseId()).stream()
            .filter(slide -> switch (targetType) {
                case "SLIDE" -> order.targetSlideId() != null && order.targetSlideId().equals(slide.id());
                case "BLOCK" -> order.targetBlockId() != null && order.targetBlockId().equals(slide.samplingBlockId());
                case "SPECIMEN" -> order.targetSpecimenId() != null && order.targetSpecimenId().equals(slide.specimenId());
                default -> false;
            })
            .toList();
    }

    String resolveQcTargetType(MedicalOrderRepository.MedicalOrder order) {
        if (order.targetType() != null && !order.targetType().isBlank()) {
            return order.targetType().trim().toUpperCase();
        }
        if (order.targetSlideId() != null && !order.targetSlideId().isBlank()) {
            return "SLIDE";
        }
        if (order.targetBlockId() != null && !order.targetBlockId().isBlank()) {
            return "BLOCK";
        }
        if (order.targetSpecimenId() != null && !order.targetSpecimenId().isBlank()) {
            return "SPECIMEN";
        }
        return "UNLINKED";
    }

    BlBusinessException versionConflict() {
        return new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409,
            "Medical order QC evaluation was updated concurrently");
    }

    String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    String normalizeProcessingAction(String processingAction) {
        if (processingAction == null || processingAction.isBlank()) {
            return "NO_ACTION";
        }
        String normalized = processingAction.trim().toUpperCase();
        if (Set.of("NONE", "NO_NEED", "NO_ACTION", "NO").contains(normalized)) {
            return "NO_ACTION";
        }
        return normalized;
    }

    String resolveReworkType(String qcAspect, String processingAction) {
        if (processingAction == null || processingAction.isBlank() || "NO_ACTION".equals(processingAction)) {
            return null;
        }
        return "GROSSING".equalsIgnoreCase(qcAspect) ? "REGROSSING" : "RESLICE";
    }

    String appendUrgentFlag(String remarks) {
        if (remarks == null || remarks.isBlank()) {
            return "URGENT";
        }
        return remarks.contains("URGENT") ? remarks : remarks + " URGENT";
    }

    DiagnosticReportModels.MedicalOrderQcEvaluationResult toResult(
        MedicalOrderRepository.MedicalOrderQcEvaluation evaluation
    ) {
        return new DiagnosticReportModels.MedicalOrderQcEvaluationResult(
            evaluation.id(),
            evaluation.orderId(),
            evaluation.caseId(),
            evaluation.targetSlideId(),
            evaluation.targetSlideNo(),
            evaluation.version(),
            evaluation.qcAspect(),
            evaluation.totalScore(),
            evaluation.grade(),
            evaluation.evaluationReason(),
            evaluation.processingAction(),
            evaluation.reworkType(),
            evaluation.reworkOrderId(),
            evaluation.remarks(),
            evaluation.evaluatorName(),
            evaluation.evaluatedAt() == null ? null : evaluation.evaluatedAt().toString(),
            evaluation.detailPayload());
    }

    private boolean canCreateTargetedQcEvaluation(MedicalOrderRepository.MedicalOrder order) {
        return DiagnosticReportConstants.ORDER_IN_PROGRESS.equals(order.status())
            && order.printedAt() != null
            && !isTerminated(order)
            && hasTargetSnapshot(order);
    }

    private boolean canCreateRoutineNoActionQcEvaluation(MedicalOrderRepository.MedicalOrder order) {
        String normalizedStatus = order.status() == null ? null : order.status().trim().toUpperCase();
        return Set.of(
            DiagnosticReportConstants.ORDER_PENDING,
            DiagnosticReportConstants.ORDER_IN_PROGRESS,
            DiagnosticReportConstants.ORDER_COMPLETED).contains(normalizedStatus)
            && order.cancelledAt() == null
            && !isTerminated(order);
    }

    private boolean canCreateLiquidCytologyNoActionQcEvaluation(MedicalOrderRepository.MedicalOrder order) {
        return isLiquidCytologyOrder(order)
            && DiagnosticReportConstants.ORDER_IN_PROGRESS.equals(order.status())
            && order.cancelledAt() == null
            && !isTerminated(order);
    }

    private boolean isLiquidCytologyOrder(MedicalOrderRepository.MedicalOrder order) {
        if ("LIQUID_CYTOLOGY".equalsIgnoreCase(order.orderCategoryCode())) {
            return true;
        }
        String normalizedOrderType = order.orderType() == null ? null : order.orderType().trim().toUpperCase();
        return LIQUID_CYTOLOGY_ORDER_TYPES.contains(normalizedOrderType);
    }

    private boolean hasTargetSnapshot(MedicalOrderRepository.MedicalOrder order) {
        return order.targetType() != null && !order.targetType().isBlank()
            && order.targetSlideId() != null && !order.targetSlideId().isBlank();
    }

    private boolean isTerminated(MedicalOrderRepository.MedicalOrder order) {
        return "TERMINATED".equalsIgnoreCase(order.status()) || order.terminatedAt() != null;
    }
}
