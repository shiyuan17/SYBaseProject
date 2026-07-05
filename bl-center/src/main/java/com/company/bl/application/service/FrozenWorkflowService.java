package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
class FrozenWorkflowService {

    private static final String APPLICATION_TYPE_FROZEN = "FROZEN";
    private static final String NODE_FROZEN_RECEIVE = "RECEIVE";
    private static final String NODE_FROZEN_GROSSING = "GROSSING";
    private static final String NODE_FROZEN_SLICING = "SLICING";
    private static final String NODE_FROZEN_REPORT = "REPORT";
    private static final String NODE_FROZEN_COMPARE = "COMPARE";
    private static final String EVENT_FROZEN_RECEIVE_COMPLETED = "FROZEN_RECEIVE_COMPLETED";
    private static final String EVENT_FROZEN_GROSSING_COMPLETED = "FROZEN_GROSSING_COMPLETED";
    private static final String EVENT_FROZEN_SLICING_COMPLETED = "FROZEN_SLICING_COMPLETED";
    private static final String EVENT_FROZEN_PRELIMINARY_SAVED = "FROZEN_PRELIMINARY_SAVED";
    private static final String EVENT_FROZEN_PHONE_BACK_COMPLETED = "FROZEN_PHONE_BACK_COMPLETED";
    private static final String EVENT_FROZEN_REPORT_CONFIRMED = "FROZEN_REPORT_CONFIRMED";
    private static final String EVENT_FROZEN_COMPARE_COMPLETED = "FROZEN_COMPARE_COMPLETED";
    private static final String EVENT_FROZEN_CLOSED = "FROZEN_CLOSED";

    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final TechnicalWorkflowSupport technicalWorkflowSupport;
    private final TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService;
    private final DiagnosticReportRepository diagnosticReportRepository;
    private final DiagnosticReportAppService diagnosticReportAppService;
    private final FrozenWorkflowSessionSupport frozenWorkflowSessionSupport;

    FrozenWorkflowService(TechnicalWorkflowRepository technicalWorkflowRepository,
                          TechnicalWorkflowSupport technicalWorkflowSupport,
                          TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService,
                          DiagnosticReportRepository diagnosticReportRepository,
                          DiagnosticReportAppService diagnosticReportAppService,
                          FrozenWorkflowSessionSupport frozenWorkflowSessionSupport) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.technicalSpecimenRegistrationService = technicalSpecimenRegistrationService;
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.diagnosticReportAppService = diagnosticReportAppService;
        this.frozenWorkflowSessionSupport = frozenWorkflowSessionSupport;
    }

    @Transactional(readOnly = true)
    FrozenWorkflowModels.FrozenTechnicalWorkbenchView getWorkbench() {
        return frozenWorkflowSessionSupport.getWorkbench();
    }

    @Transactional(readOnly = true)
    FrozenWorkflowModels.FrozenReminderSummary getReminderSummary() {
        return frozenWorkflowSessionSupport.getReminderSummary();
    }

    @Transactional(readOnly = true)
    FrozenWorkflowModels.FrozenSessionListPage listSessions(FrozenWorkflowModels.FrozenSessionListQuery query) {
        return frozenWorkflowSessionSupport.listSessions(query);
    }

    @Transactional(readOnly = true)
    FrozenWorkflowModels.FrozenSessionDetail getSessionDetail(String sessionId) {
        return frozenWorkflowSessionSupport.getSessionDetail(sessionId);
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeReceive(FrozenWorkflowModels.FrozenActionCommand command) {
        FrozenSessionContext context = frozenWorkflowSessionSupport.loadSessionContext(command.sessionId());
        FrozenSessionStage stage = frozenWorkflowSessionSupport.resolveStage(context);
        if (!"REQUESTED".equals(stage.sessionStatus())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen receive can only be completed from requested status");
        }

        TechnicalWorkflowModels.TechnicalSpecimenRegistrationCompleteResult result =
            technicalSpecimenRegistrationService.completeRegistration(
                new TechnicalWorkflowModels.CompleteTechnicalSpecimenRegistrationCommand(
                    context.caseId(),
                    command.operatorUserId(),
                    command.operatorName(),
                    APPLICATION_TYPE_FROZEN,
                    null,
                    command.terminalCode(),
                    command.remarks()));
        technicalWorkflowRepository.updatePathologyCaseStatus(context.caseId(), "RECEIVED");
        insertWorkflowEvent(
            context,
            NODE_FROZEN_RECEIVE,
            EVENT_FROZEN_RECEIVE_COMPLETED,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Frozen receive completed");

        return actionResult(context, result.pathologyNo(), NODE_FROZEN_RECEIVE, TechnicalWorkflowConstants.TASK_COMPLETED, "RECEIVED", "GROSSING");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeGrossing(FrozenWorkflowModels.FrozenActionCommand command) {
        FrozenSessionContext context = frozenWorkflowSessionSupport.loadSessionContext(command.sessionId());
        TechnicalWorkflowRecords.TechnicalTask grossingTask = frozenWorkflowSessionSupport
            .findPendingTask(context.tasks(), TechnicalWorkflowConstants.NODE_GROSSING)
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen grossing task is not pending"));
        ensurePendingOrInProgress(grossingTask, "Frozen grossing task is not pending");

        LocalDateTime now = LocalDateTime.now();
        if (TechnicalWorkflowConstants.TASK_PENDING.equals(grossingTask.taskStatus())) {
            technicalWorkflowRepository.startTechnicalTask(
                grossingTask.id(),
                command.operatorUserId(),
                command.operatorName(),
                TechnicalWorkflowConstants.TASK_IN_PROGRESS,
                command.remarks(),
                now);
        }
        technicalWorkflowRepository.completeTechnicalTask(
            grossingTask.id(),
            TechnicalWorkflowConstants.TASK_COMPLETED,
            command.remarks(),
            now);
        technicalWorkflowSupport.createTechnicalTaskIfAbsent(
            grossingTask.applicationId(),
            grossingTask.caseId(),
            grossingTask.specimenId(),
            TechnicalWorkflowConstants.NODE_SLICING,
            TechnicalWorkflowConstants.OBJECT_CASE,
            grossingTask.caseId(),
            grossingTask.id(),
            "frozen=true",
            null);
        technicalWorkflowRepository.updatePathologyCaseStatus(grossingTask.caseId(), "GROSSING");
        insertWorkflowEvent(
            context,
            NODE_FROZEN_GROSSING,
            EVENT_FROZEN_GROSSING_COMPLETED,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Frozen grossing completed");

        return actionResult(context, context.pathologyCase().pathologyNo(), NODE_FROZEN_GROSSING, TechnicalWorkflowConstants.TASK_COMPLETED, "GROSSING", "SLICING");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeSlicing(FrozenWorkflowModels.FrozenActionCommand command) {
        FrozenSessionContext context = frozenWorkflowSessionSupport.loadSessionContext(command.sessionId());
        TechnicalWorkflowRecords.TechnicalTask slicingTask = frozenWorkflowSessionSupport
            .findPendingTask(context.tasks(), TechnicalWorkflowConstants.NODE_SLICING)
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen slicing task is not pending"));
        ensurePendingOrInProgress(slicingTask, "Frozen slicing task is not pending");

        LocalDateTime now = LocalDateTime.now();
        if (TechnicalWorkflowConstants.TASK_PENDING.equals(slicingTask.taskStatus())) {
            technicalWorkflowRepository.startTechnicalTask(
                slicingTask.id(),
                command.operatorUserId(),
                command.operatorName(),
                TechnicalWorkflowConstants.TASK_IN_PROGRESS,
                command.remarks(),
                now);
        }
        technicalWorkflowRepository.completeTechnicalTask(
            slicingTask.id(),
            TechnicalWorkflowConstants.TASK_COMPLETED,
            command.remarks(),
            now);
        technicalWorkflowRepository.updatePathologyCaseStatus(slicingTask.caseId(), "DIAGNOSIS_PENDING");
        diagnosticReportAppService.createFrozenDiagnosticTaskIfAbsent(
            slicingTask.caseId(),
            "Auto created after frozen slicing completed");
        insertWorkflowEvent(
            context,
            NODE_FROZEN_SLICING,
            EVENT_FROZEN_SLICING_COMPLETED,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Frozen slicing completed");

        return actionResult(context, context.pathologyCase().pathologyNo(), NODE_FROZEN_SLICING, TechnicalWorkflowConstants.TASK_COMPLETED, "DIAGNOSING", "REPORT");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult savePreliminaryReport(FrozenWorkflowModels.FrozenPhoneBackCommand command) {
        FrozenSessionContext context = frozenWorkflowSessionSupport.loadSessionContext(command.sessionId());
        FrozenSessionStage stage = frozenWorkflowSessionSupport.resolveStage(context);
        if (!NODE_FROZEN_REPORT.equals(stage.currentTaskType())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen preliminary report can only be saved during report stage");
        }
        DiagnosticReportRepository.DiagnosticTask diagnosticTask =
            frozenWorkflowSessionSupport.latestDiagnosticTaskAssignedDoctor(context, command.operatorUserId());
        if (frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen preliminary report cannot be saved after phone back is completed");
        }
        frozenWorkflowSessionSupport.ensureDiagnosticTaskStarted(diagnosticTask);
        LocalDateTime now = LocalDateTime.now();
        String preliminaryResult = frozenWorkflowSessionSupport.requireText(
            command.preliminaryResult(),
            "Frozen preliminary result is required");
        diagnosticReportRepository.updateFrozenDiagnosisResult(
            diagnosticTask.id(),
            preliminaryResult,
            command.remarks(),
            now);
        insertWorkflowEvent(
            context,
            NODE_FROZEN_REPORT,
            EVENT_FROZEN_PRELIMINARY_SAVED,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            frozenWorkflowSessionSupport.formatPreliminaryEventContent(preliminaryResult));

        return actionResult(context, context.pathologyCase().pathologyNo(), NODE_FROZEN_REPORT, TechnicalWorkflowConstants.TASK_IN_PROGRESS, "DIAGNOSING", "PHONE_BACK");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completePhoneBack(FrozenWorkflowModels.FrozenPhoneBackCommand command) {
        FrozenSessionContext context = frozenWorkflowSessionSupport.loadSessionContext(command.sessionId());
        FrozenSessionStage stage = frozenWorkflowSessionSupport.resolveStage(context);
        if (!NODE_FROZEN_REPORT.equals(stage.currentTaskType())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen phone back can only be completed during report stage");
        }
        DiagnosticReportRepository.DiagnosticTask diagnosticTask =
            frozenWorkflowSessionSupport.latestDiagnosticTaskAssignedDoctor(context, command.operatorUserId());
        if (frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen phone back is already completed");
        }
        if (!frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_PRELIMINARY_SAVED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen preliminary result must be saved before phone back");
        }
        frozenWorkflowSessionSupport.ensureDiagnosticTaskStarted(diagnosticTask);
        LocalDateTime now = LocalDateTime.now();
        String preliminaryResult = frozenWorkflowSessionSupport.requireText(
            command.preliminaryResult(),
            "Frozen preliminary result is required");
        diagnosticReportRepository.updateFrozenDiagnosisResult(
            diagnosticTask.id(),
            preliminaryResult,
            command.remarks(),
            now);
        diagnosticReportRepository.completeDiagnosticTask(diagnosticTask.id(), command.remarks(), now);
        insertWorkflowEvent(
            context,
            NODE_FROZEN_REPORT,
            EVENT_FROZEN_PHONE_BACK_COMPLETED,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            frozenWorkflowSessionSupport.formatPhoneBackEventContent(preliminaryResult));

        return actionResult(context, context.pathologyCase().pathologyNo(), "PHONE_BACK", TechnicalWorkflowConstants.TASK_COMPLETED, "REPORTED", "REPORT");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult confirmReport(FrozenWorkflowModels.FrozenActionCommand command) {
        FrozenSessionContext context = frozenWorkflowSessionSupport.loadSessionContext(command.sessionId());
        frozenWorkflowSessionSupport.latestDiagnosticTaskAssignedDoctor(context, command.operatorUserId());
        if (!frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen phone back must be completed before report confirmation");
        }
        if (frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_REPORT_CONFIRMED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen report is already confirmed");
        }
        technicalWorkflowRepository.updatePathologyCaseStatus(context.caseId(), "REPORT_PUBLISHED");
        insertWorkflowEvent(
            context,
            NODE_FROZEN_REPORT,
            EVENT_FROZEN_REPORT_CONFIRMED,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "冰冻快速报告最终确认");

        return actionResult(context, context.pathologyCase().pathologyNo(), NODE_FROZEN_REPORT, TechnicalWorkflowConstants.TASK_COMPLETED, "CONFIRMED", "COMPARE");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeParaffinCompare(FrozenWorkflowModels.FrozenParaffinCompareCommand command) {
        FrozenSessionContext context = frozenWorkflowSessionSupport.loadSessionContext(command.sessionId());
        frozenWorkflowSessionSupport.latestDiagnosticTaskReviewer(context, command.operatorUserId());
        if (!frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_REPORT_CONFIRMED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen report must be confirmed before paraffin compare");
        }
        if (frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_COMPARE_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen paraffin compare is already completed");
        }
        String compareStatus = frozenWorkflowSessionSupport.normalizeCompareStatus(command.compareStatus());
        String compareSummary = frozenWorkflowSessionSupport.requireText(
            command.compareSummary(),
            "Frozen compare summary is required");
        insertWorkflowEvent(
            context,
            NODE_FROZEN_COMPARE,
            EVENT_FROZEN_COMPARE_COMPLETED,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            frozenWorkflowSessionSupport.formatCompareEventContent(compareStatus, compareSummary));

        return actionResult(
            context,
            context.pathologyCase().pathologyNo(),
            NODE_FROZEN_COMPARE,
            TechnicalWorkflowConstants.TASK_COMPLETED,
            "PARAFFIN_REVIEWED",
            "REMAINING_TISSUE");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeRemainingTissue(FrozenWorkflowModels.FrozenRemainingTissueCommand command) {
        FrozenSessionContext context = frozenWorkflowSessionSupport.loadSessionContext(command.sessionId());
        if (frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_CLOSED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen session is already closed");
        }
        if (!frozenWorkflowSessionSupport.hasCompletedEvent(context.events(), EVENT_FROZEN_COMPARE_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen paraffin compare must be completed before remaining tissue handling");
        }
        String remainingTissueStatus = frozenWorkflowSessionSupport.normalizeRemainingTissueStatus(command.remainingTissueStatus());
        technicalWorkflowRepository.updatePathologyCaseStatus(context.caseId(), "CLOSED");
        insertWorkflowEvent(
            context,
            "REMAINING_TISSUE",
            EVENT_FROZEN_CLOSED,
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            frozenWorkflowSessionSupport.formatRemainingEventContent(remainingTissueStatus, command.remarks()));

        return actionResult(context, context.pathologyCase().pathologyNo(), "REMAINING_TISSUE", TechnicalWorkflowConstants.TASK_COMPLETED, "CLOSED", null);
    }

    private void ensurePendingOrInProgress(TechnicalWorkflowRecords.TechnicalTask task, String message) {
        if (!TechnicalWorkflowConstants.TASK_PENDING.equals(task.taskStatus())
            && !TechnicalWorkflowConstants.TASK_IN_PROGRESS.equals(task.taskStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, message);
        }
    }

    private void insertWorkflowEvent(FrozenSessionContext context,
                                     String nodeCode,
                                     String eventType,
                                     String operatorUserId,
                                     String operatorName,
                                     String terminalCode,
                                     String content) {
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            frozenWorkflowSessionSupport.firstSpecimenId(context.specimens()),
            context.caseId(),
            nodeCode,
            eventType,
            "SUCCESS",
            operatorUserId,
            operatorName,
            terminalCode,
            content);
    }

    private FrozenWorkflowModels.FrozenTaskActionResult actionResult(FrozenSessionContext context,
                                                                     String pathologyNo,
                                                                     String taskType,
                                                                     String taskStatus,
                                                                     String sessionStatus,
                                                                     String nextTaskType) {
        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            pathologyNo,
            pathologyNo,
            taskType,
            taskStatus,
            sessionStatus,
            nextTaskType);
    }
}
