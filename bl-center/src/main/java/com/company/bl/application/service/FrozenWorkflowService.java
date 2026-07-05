package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
class FrozenWorkflowService {

    private static final int FROZEN_REGISTRATION_FETCH_SIZE = 200;
    private static final String APPLICATION_TYPE_FROZEN = "FROZEN";
    private static final String NODE_FROZEN_RECEIVE = "RECEIVE";
    private static final String NODE_FROZEN_GROSSING = "GROSSING";
    private static final String NODE_FROZEN_SLICING = "SLICING";
    private static final String NODE_FROZEN_REPORT = "REPORT";
    private static final String NODE_FROZEN_COMPARE = "COMPARE";
    private static final String NODE_FROZEN_APPOINTMENT = "APPOINTMENT";
    private static final String NODE_DIAGNOSIS_ASSIGN = "DIAGNOSIS_ASSIGN";
    private static final String NODE_DIAGNOSIS_ACCEPT = "DIAGNOSIS_ACCEPT";
    private static final String NODE_DIAGNOSIS_START = "DIAGNOSIS_START";
    private static final String EVENT_FROZEN_REQUESTED = "FROZEN_REQUESTED";
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
    private final ApplicationRegistrationWorkbenchRepository applicationRegistrationWorkbenchRepository;
    private final ApplicationRepository applicationRepository;
    private final DiagnosticReportRepository diagnosticReportRepository;
    private final DiagnosticReportAppService diagnosticReportAppService;
    private final DiagnosticReportSupport diagnosticReportSupport;

    FrozenWorkflowService(TechnicalWorkflowRepository technicalWorkflowRepository,
                          TechnicalWorkflowSupport technicalWorkflowSupport,
                          TechnicalSpecimenRegistrationService technicalSpecimenRegistrationService,
                          ApplicationRegistrationWorkbenchRepository applicationRegistrationWorkbenchRepository,
                          ApplicationRepository applicationRepository,
                          DiagnosticReportRepository diagnosticReportRepository,
                          DiagnosticReportAppService diagnosticReportAppService,
                          DiagnosticReportSupport diagnosticReportSupport) {
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.technicalWorkflowSupport = technicalWorkflowSupport;
        this.technicalSpecimenRegistrationService = technicalSpecimenRegistrationService;
        this.applicationRegistrationWorkbenchRepository = applicationRegistrationWorkbenchRepository;
        this.applicationRepository = applicationRepository;
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.diagnosticReportAppService = diagnosticReportAppService;
        this.diagnosticReportSupport = diagnosticReportSupport;
    }

    @Transactional(readOnly = true)
    FrozenWorkflowModels.FrozenTechnicalWorkbenchView getWorkbench() {
        List<FrozenWorkflowModels.FrozenSession> allSessions = loadAllFrozenSessions();
        List<FrozenWorkflowModels.FrozenSession> sessions = allSessions.stream()
            .filter(this::isTechnicalWorkbenchSession)
            .sorted(Comparator.comparing(FrozenWorkflowModels.FrozenSession::requestedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
        FrozenWorkflowModels.FrozenReminderSummary reminderSummary = buildReminderSummary(allSessions);

        return new FrozenWorkflowModels.FrozenTechnicalWorkbenchView(reminderSummary, sessions);
    }

    @Transactional(readOnly = true)
    FrozenWorkflowModels.FrozenReminderSummary getReminderSummary() {
        return buildReminderSummary(loadAllFrozenSessions());
    }

    private FrozenWorkflowModels.FrozenReminderSummary buildReminderSummary(List<FrozenWorkflowModels.FrozenSession> sessions) {
        List<FrozenWorkflowModels.FrozenReminderItem> reminderItems = sessions.stream()
            .filter(session -> !"NONE".equals(session.timeoutLevel()))
            .map(session -> new FrozenWorkflowModels.FrozenReminderItem(
                session.caseId(),
                session.id(),
                session.caseId(),
                session.sessionNo(),
                session.frozenPathologyNo(),
                session.patientName(),
                session.requestedAt(),
                session.currentTaskType(),
                session.nextAction(),
                session.timeoutLevel(),
                formatReminderTitle(session)))
            .toList();

        int orangeCount = (int) sessions.stream().filter(session -> "ORANGE".equals(session.timeoutLevel())).count();
        int redCount = (int) sessions.stream().filter(session -> "RED".equals(session.timeoutLevel())).count();

        return new FrozenWorkflowModels.FrozenReminderSummary(reminderItems, reminderItems.size(), orangeCount, redCount);
    }

    @Transactional(readOnly = true)
    FrozenWorkflowModels.FrozenSessionListPage listSessions(FrozenWorkflowModels.FrozenSessionListQuery query) {
        int page = Math.max(query.page(), 1);
        int size = Math.max(query.size(), 1);
        String keyword = normalize(query.keyword());
        String sessionStatus = normalize(query.sessionStatus());
        String timeoutLevel = normalize(query.timeoutLevel());

        List<FrozenWorkflowModels.FrozenSession> filtered = loadAllFrozenSessions().stream()
            .filter(session -> sessionStatus.isEmpty() || sessionStatus.equals(normalize(session.sessionStatus())))
            .filter(session -> timeoutLevel.isEmpty() || timeoutLevel.equals(normalize(session.timeoutLevel())))
            .filter(session -> keyword.isEmpty() || matchesKeyword(session, keyword))
            .sorted(Comparator.comparing(FrozenWorkflowModels.FrozenSession::requestedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        int fromIndex = Math.min((page - 1) * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        return new FrozenWorkflowModels.FrozenSessionListPage(
            filtered.subList(fromIndex, toIndex),
            page,
            size,
            filtered.size());
    }

    @Transactional(readOnly = true)
    FrozenWorkflowModels.FrozenSessionDetail getSessionDetail(String sessionId) {
        SessionContext context = loadSessionContext(sessionId);
        FrozenWorkflowModels.FrozenSession session = buildSession(context);
        List<FrozenWorkflowModels.FrozenSessionTask> tasks = buildTasks(context, session);
        List<FrozenWorkflowModels.FrozenTimelineEvent> timeline = Stream.concat(
                syntheticRequestedTimelineEvent(context),
                context.events().stream())
            .filter(this::isFrozenTimelineEvent)
            .sorted(Comparator.comparing(TrackingEvent::eventTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(event -> new FrozenWorkflowModels.FrozenTimelineEvent(
                event.id(),
                event.nodeCode(),
                event.eventType(),
                stringify(event.eventTime()),
                event.eventContent(),
                event.operatorName()))
            .toList();
        List<String> reminders = session.timeoutLevel().equals("NONE")
            ? List.of()
            : List.of(session.nextAction());
        return new FrozenWorkflowModels.FrozenSessionDetail(
            session.id(),
            session.applicationId(),
            session.applicationNo(),
            session.autoPrintSlides(),
            session.caseId(),
            session.compareStatus(),
            session.compareSummary(),
            session.currentTaskType(),
            session.finalConfirmedAt(),
            session.finalDiagnosis(),
            session.frozenPathologyNo(),
            session.grossingCompletedAt(),
            session.grossingDescription(),
            session.grossingStartedAt(),
            session.handoverComment(),
            session.hasRegularCaseLinked(),
            session.intraoperativePhoneBack(),
            session.nextAction(),
            session.patientName(),
            session.phoneBackAt(),
            session.preliminaryResult(),
            session.receivedAt(),
            session.remainingTissueStatus(),
            session.reportConfirmedAt(),
            session.requestedAt(),
            session.requestDoctorName(),
            session.sessionNo(),
            session.sessionStatus(),
            session.slicingCompletedAt(),
            session.slicingStartedAt(),
            session.timeoutLevel(),
            reminders,
            tasks,
            timeline);
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeReceive(FrozenWorkflowModels.FrozenActionCommand command) {
        SessionContext context = loadSessionContext(command.sessionId());
        FrozenWorkflowModels.FrozenSession session = buildSession(context);
        if (!"REQUESTED".equals(session.sessionStatus())) {
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

        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            firstSpecimenId(context.specimens()),
            context.caseId(),
            NODE_FROZEN_RECEIVE,
            EVENT_FROZEN_RECEIVE_COMPLETED,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Frozen receive completed");

        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            result.pathologyNo(),
            result.pathologyNo(),
            NODE_FROZEN_RECEIVE,
            TechnicalWorkflowConstants.TASK_COMPLETED,
            "RECEIVED",
            "GROSSING");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeGrossing(FrozenWorkflowModels.FrozenActionCommand command) {
        SessionContext context = loadSessionContext(command.sessionId());
        TechnicalWorkflowRecords.TechnicalTask grossingTask = findPendingTask(context.tasks(), TechnicalWorkflowConstants.NODE_GROSSING)
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen grossing task is not pending"));
        if (!TechnicalWorkflowConstants.TASK_PENDING.equals(grossingTask.taskStatus())
            && !TechnicalWorkflowConstants.TASK_IN_PROGRESS.equals(grossingTask.taskStatus())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen grossing task is not pending");
        }

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
        technicalWorkflowSupport.insertWorkflowEvent(
            grossingTask.applicationId(),
            grossingTask.specimenId(),
            grossingTask.caseId(),
            NODE_FROZEN_GROSSING,
            EVENT_FROZEN_GROSSING_COMPLETED,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Frozen grossing completed");

        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            context.pathologyCase().pathologyNo(),
            context.pathologyCase().pathologyNo(),
            NODE_FROZEN_GROSSING,
            TechnicalWorkflowConstants.TASK_COMPLETED,
            "GROSSING",
            "SLICING");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeSlicing(FrozenWorkflowModels.FrozenActionCommand command) {
        SessionContext context = loadSessionContext(command.sessionId());
        TechnicalWorkflowRecords.TechnicalTask slicingTask = findPendingTask(context.tasks(), TechnicalWorkflowConstants.NODE_SLICING)
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen slicing task is not pending"));
        if (!TechnicalWorkflowConstants.TASK_PENDING.equals(slicingTask.taskStatus())
            && !TechnicalWorkflowConstants.TASK_IN_PROGRESS.equals(slicingTask.taskStatus())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen slicing task is not pending");
        }

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
        technicalWorkflowSupport.insertWorkflowEvent(
            slicingTask.applicationId(),
            slicingTask.specimenId(),
            slicingTask.caseId(),
            NODE_FROZEN_SLICING,
            EVENT_FROZEN_SLICING_COMPLETED,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "Frozen slicing completed");

        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            context.pathologyCase().pathologyNo(),
            context.pathologyCase().pathologyNo(),
            NODE_FROZEN_SLICING,
            TechnicalWorkflowConstants.TASK_COMPLETED,
            "DIAGNOSING",
            "REPORT");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult savePreliminaryReport(FrozenWorkflowModels.FrozenPhoneBackCommand command) {
        SessionContext context = loadSessionContext(command.sessionId());
        FrozenStage stage = resolveStage(context);
        if (!NODE_FROZEN_REPORT.equals(stage.currentTaskType())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen preliminary report can only be saved during report stage");
        }
        DiagnosticReportRepository.DiagnosticTask diagnosticTask =
            latestDiagnosticTaskAssignedDoctor(context, command.operatorUserId());
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen preliminary report cannot be saved after phone back is completed");
        }
        ensureDiagnosticTaskStarted(diagnosticTask);
        LocalDateTime now = LocalDateTime.now();
        String preliminaryResult = requireText(command.preliminaryResult(), "Frozen preliminary result is required");
        diagnosticReportRepository.updateFrozenDiagnosisResult(
            diagnosticTask.id(),
            preliminaryResult,
            command.remarks(),
            now);
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            firstSpecimenId(context.specimens()),
            context.caseId(),
            NODE_FROZEN_REPORT,
            EVENT_FROZEN_PRELIMINARY_SAVED,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            formatPreliminaryEventContent(preliminaryResult));

        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            context.pathologyCase().pathologyNo(),
            context.pathologyCase().pathologyNo(),
            NODE_FROZEN_REPORT,
            TechnicalWorkflowConstants.TASK_IN_PROGRESS,
            "DIAGNOSING",
            "PHONE_BACK");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completePhoneBack(FrozenWorkflowModels.FrozenPhoneBackCommand command) {
        SessionContext context = loadSessionContext(command.sessionId());
        FrozenStage stage = resolveStage(context);
        if (!NODE_FROZEN_REPORT.equals(stage.currentTaskType())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen phone back can only be completed during report stage");
        }
        DiagnosticReportRepository.DiagnosticTask diagnosticTask =
            latestDiagnosticTaskAssignedDoctor(context, command.operatorUserId());
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen phone back is already completed");
        }
        if (!hasCompletedEvent(context.events(), EVENT_FROZEN_PRELIMINARY_SAVED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen preliminary result must be saved before phone back");
        }
        ensureDiagnosticTaskStarted(diagnosticTask);
        LocalDateTime now = LocalDateTime.now();
        String preliminaryResult = requireText(command.preliminaryResult(), "Frozen preliminary result is required");
        diagnosticReportRepository.updateFrozenDiagnosisResult(
            diagnosticTask.id(),
            preliminaryResult,
            command.remarks(),
            now);
        diagnosticReportRepository.completeDiagnosticTask(diagnosticTask.id(), command.remarks(), now);
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            firstSpecimenId(context.specimens()),
            context.caseId(),
            NODE_FROZEN_REPORT,
            EVENT_FROZEN_PHONE_BACK_COMPLETED,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            formatPhoneBackEventContent(preliminaryResult));

        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            context.pathologyCase().pathologyNo(),
            context.pathologyCase().pathologyNo(),
            "PHONE_BACK",
            TechnicalWorkflowConstants.TASK_COMPLETED,
            "REPORTED",
            "REPORT");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult confirmReport(FrozenWorkflowModels.FrozenActionCommand command) {
        SessionContext context = loadSessionContext(command.sessionId());
        latestDiagnosticTaskAssignedDoctor(context, command.operatorUserId());
        if (!hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen phone back must be completed before report confirmation");
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_REPORT_CONFIRMED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen report is already confirmed");
        }
        technicalWorkflowRepository.updatePathologyCaseStatus(context.caseId(), "REPORT_PUBLISHED");
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            firstSpecimenId(context.specimens()),
            context.caseId(),
            NODE_FROZEN_REPORT,
            EVENT_FROZEN_REPORT_CONFIRMED,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            "冰冻快速报告最终确认");

        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            context.pathologyCase().pathologyNo(),
            context.pathologyCase().pathologyNo(),
            NODE_FROZEN_REPORT,
            TechnicalWorkflowConstants.TASK_COMPLETED,
            "CONFIRMED",
            "COMPARE");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeParaffinCompare(FrozenWorkflowModels.FrozenParaffinCompareCommand command) {
        SessionContext context = loadSessionContext(command.sessionId());
        diagnosticReportSupport.ensureReviewer(latestDiagnosticTask(context), command.operatorUserId());
        if (!hasCompletedEvent(context.events(), EVENT_FROZEN_REPORT_CONFIRMED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen report must be confirmed before paraffin compare");
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_COMPARE_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen paraffin compare is already completed");
        }
        String compareStatus = normalizeCompareStatus(command.compareStatus());
        String compareSummary = requireText(command.compareSummary(), "Frozen compare summary is required");
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            firstSpecimenId(context.specimens()),
            context.caseId(),
            NODE_FROZEN_COMPARE,
            EVENT_FROZEN_COMPARE_COMPLETED,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            formatCompareEventContent(compareStatus, compareSummary));

        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            context.pathologyCase().pathologyNo(),
            context.pathologyCase().pathologyNo(),
            NODE_FROZEN_COMPARE,
            TechnicalWorkflowConstants.TASK_COMPLETED,
            "PARAFFIN_REVIEWED",
            "REMAINING_TISSUE");
    }

    @Transactional
    FrozenWorkflowModels.FrozenTaskActionResult completeRemainingTissue(FrozenWorkflowModels.FrozenRemainingTissueCommand command) {
        SessionContext context = loadSessionContext(command.sessionId());
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_CLOSED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen session is already closed");
        }
        if (!hasCompletedEvent(context.events(), EVENT_FROZEN_COMPARE_COMPLETED)) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen paraffin compare must be completed before remaining tissue handling");
        }
        String remainingTissueStatus = normalizeRemainingTissueStatus(command.remainingTissueStatus());
        technicalWorkflowRepository.updatePathologyCaseStatus(context.caseId(), "CLOSED");
        technicalWorkflowSupport.insertWorkflowEvent(
            context.application().getId().value(),
            firstSpecimenId(context.specimens()),
            context.caseId(),
            "REMAINING_TISSUE",
            EVENT_FROZEN_CLOSED,
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            formatRemainingEventContent(remainingTissueStatus, command.remarks()));

        return new FrozenWorkflowModels.FrozenTaskActionResult(
            context.caseId(),
            context.caseId(),
            context.pathologyCase().pathologyNo(),
            context.pathologyCase().pathologyNo(),
            "REMAINING_TISSUE",
            TechnicalWorkflowConstants.TASK_COMPLETED,
            "CLOSED",
            null);
    }

    private FrozenWorkflowModels.FrozenSession buildSession(String caseId) {
        return buildSession(loadSessionContext(caseId));
    }

    private List<FrozenWorkflowModels.FrozenSession> loadAllFrozenSessions() {
        return Stream.concat(
                loadRegistrationItems("PENDING").stream().map(item -> buildSession(item.caseId())),
                loadRegistrationItems("COMPLETED").stream().map(item -> buildSession(item.caseId())))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private List<TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationItem> loadRegistrationItems(
        String registrationStatus
    ) {
        List<TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationItem> items = new ArrayList<>();
        int page = 1;
        while (true) {
            TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage currentPage =
                technicalSpecimenRegistrationService.listRegistrations(
                    new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery(
                        page,
                        FROZEN_REGISTRATION_FETCH_SIZE,
                        null,
                        APPLICATION_TYPE_FROZEN,
                        registrationStatus,
                        null,
                        null));
            items.addAll(currentPage.items());
            if (currentPage.items().size() < FROZEN_REGISTRATION_FETCH_SIZE || items.size() >= currentPage.total()) {
                return items;
            }
            page++;
        }
    }

    private FrozenWorkflowModels.FrozenSession buildSession(SessionContext context) {
        FrozenStage stage = resolveStage(context);
        return new FrozenWorkflowModels.FrozenSession(
            context.caseId(),
            context.application().getId().value(),
            context.application().getApplicationNo(),
            true,
            context.caseId(),
            resolveCompareStatus(context, stage),
            latestCompareSummary(context.events()),
            stage.currentTaskType(),
            completedEventTime(context.events(), EVENT_FROZEN_REPORT_CONFIRMED),
            resolveFinalDiagnosis(context),
            context.pathologyCase().pathologyNo(),
            completedEventTime(context.events(), EVENT_FROZEN_GROSSING_COMPLETED),
            summarizeGrossingDescription(context.tasks()),
            startedAt(context.tasks(), TechnicalWorkflowConstants.NODE_GROSSING),
            latestHandoverComment(context.events()),
            hasRegularCaseLinked(context),
            hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED),
            stage.nextAction(),
            context.application().getPatientName(),
            completedEventTime(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED),
            resolvePreliminaryResult(context),
            completedEventTime(context.events(), EVENT_FROZEN_RECEIVE_COMPLETED),
            latestRemainingTissueStatus(context.events()),
            completedEventTime(context.events(), EVENT_FROZEN_REPORT_CONFIRMED),
            requestedAt(context.application()),
            context.application().getSubmittingDoctorName(),
            buildSessionNo(context),
            stage.sessionStatus(),
            completedEventTime(context.events(), EVENT_FROZEN_SLICING_COMPLETED),
            startedAt(context.tasks(), TechnicalWorkflowConstants.NODE_SLICING),
            timeoutLevel(stage.sessionStatus()));
    }

    private List<FrozenWorkflowModels.FrozenSessionTask> buildTasks(
        SessionContext context,
        FrozenWorkflowModels.FrozenSession session
    ) {
        List<FrozenWorkflowModels.FrozenSessionTask> tasks = new ArrayList<>();
        tasks.add(new FrozenWorkflowModels.FrozenSessionTask(
            context.caseId() + "-APPOINTMENT",
            NODE_FROZEN_APPOINTMENT,
            "COMPLETED",
            "NONE",
            requestedAt(context.application()),
            requestedAt(context.application()),
            context.application().getSubmittingDoctorName(),
            "术中冰冻申请"));
        tasks.add(new FrozenWorkflowModels.FrozenSessionTask(
            context.caseId() + "-RECEIVE",
            "RECEIVE",
            taskStatus(session.sessionStatus(), "REQUESTED", "RECEIVED"),
            "REQUESTED".equals(session.sessionStatus()) ? "ORANGE" : "NONE",
            null,
            session.receivedAt(),
            receivedOperator(context.events()),
            null));
        tasks.add(new FrozenWorkflowModels.FrozenSessionTask(
            context.caseId() + "-GROSSING",
            "GROSSING",
            taskStatus(session.sessionStatus(), "RECEIVED", "GROSSING"),
            "RECEIVED".equals(session.sessionStatus()) ? "ORANGE" : "NONE",
            startedAt(context.tasks(), TechnicalWorkflowConstants.NODE_GROSSING),
            session.grossingCompletedAt(),
            operatorByEvent(context.events(), EVENT_FROZEN_GROSSING_COMPLETED),
            null));
        tasks.add(new FrozenWorkflowModels.FrozenSessionTask(
            context.caseId() + "-SLICING",
            "SLICING",
            taskStatus(session.sessionStatus(), "GROSSING", "DIAGNOSING"),
            "GROSSING".equals(session.sessionStatus()) ? "RED" : "NONE",
            startedAt(context.tasks(), TechnicalWorkflowConstants.NODE_SLICING),
            session.slicingCompletedAt(),
            operatorByEvent(context.events(), EVENT_FROZEN_SLICING_COMPLETED),
            null));
        tasks.add(new FrozenWorkflowModels.FrozenSessionTask(
            context.caseId() + "-REPORT",
            "REPORT",
            reportTaskStatus(context, session),
            "NONE",
            reportTaskStartedAt(context),
            completedEventTime(context.events(), EVENT_FROZEN_REPORT_CONFIRMED),
            operatorByEvent(context.events(), EVENT_FROZEN_REPORT_CONFIRMED),
            null));
        tasks.add(new FrozenWorkflowModels.FrozenSessionTask(
            context.caseId() + "-PHONE_BACK",
            "PHONE_BACK",
            phoneBackTaskStatus(context),
            "NONE",
            completedEventTime(context.events(), EVENT_FROZEN_PRELIMINARY_SAVED),
            completedEventTime(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED),
            operatorByEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED),
            null));
        tasks.add(new FrozenWorkflowModels.FrozenSessionTask(
            context.caseId() + "-COMPARE",
            "COMPARE",
            compareTaskStatus(context, session),
            "NONE",
            completedEventTime(context.events(), EVENT_FROZEN_REPORT_CONFIRMED),
            completedEventTime(context.events(), EVENT_FROZEN_COMPARE_COMPLETED),
            operatorByEvent(context.events(), EVENT_FROZEN_COMPARE_COMPLETED),
            null));
        tasks.add(new FrozenWorkflowModels.FrozenSessionTask(
            context.caseId() + "-REMAINING_TISSUE",
            "REMAINING_TISSUE",
            remainingTissueTaskStatus(context, session),
            "NONE",
            completedEventTime(context.events(), EVENT_FROZEN_COMPARE_COMPLETED),
            completedEventTime(context.events(), EVENT_FROZEN_CLOSED),
            operatorByEvent(context.events(), EVENT_FROZEN_CLOSED),
            latestHandoverComment(context.events())));
        return tasks;
    }

    private SessionContext loadSessionContext(String sessionId) {
        PathologyCase pathologyCase = technicalWorkflowRepository.findPathologyCaseById(sessionId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Frozen session not found"));
        Application application = applicationRepository.findById(new ApplicationId(pathologyCase.applicationId()))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
        if (!APPLICATION_TYPE_FROZEN.equalsIgnoreCase(application.getApplicationType())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Frozen session not found");
        }
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(pathologyCase.id());
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration =
            technicalWorkflowRepository.findTechnicalSpecimenRegistrationByCaseId(pathologyCase.id()).orElse(null);
        List<TechnicalWorkflowRecords.TechnicalTask> tasks = technicalWorkflowRepository.findTechnicalTasksByCaseId(pathologyCase.id());
        List<DiagnosticReportRepository.DiagnosticTask> diagnosticTasks = diagnosticReportRepository.findDiagnosticTasksByCaseId(pathologyCase.id());
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(pathologyCase.id());
        ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData extension =
            applicationRegistrationWorkbenchRepository.findExtensionByApplicationId(application.getId().value()).orElse(null);
        return new SessionContext(pathologyCase.id(), pathologyCase, application, registration, extension, specimens, tasks, diagnosticTasks, events);
    }

    private Optional<TechnicalWorkflowRecords.TechnicalTask> findPendingTask(
        List<TechnicalWorkflowRecords.TechnicalTask> tasks,
        String taskType
    ) {
        return tasks.stream()
            .filter(task -> taskType.equalsIgnoreCase(task.taskType()))
            .filter(task -> TechnicalWorkflowConstants.TASK_PENDING.equals(task.taskStatus())
                || TechnicalWorkflowConstants.TASK_IN_PROGRESS.equals(task.taskStatus()))
            .findFirst();
    }

    private FrozenStage resolveStage(SessionContext context) {
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_CLOSED)) {
            return new FrozenStage("CLOSED", "REMAINING_TISSUE", "冰冻流程已关闭");
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_COMPARE_COMPLETED)) {
            return new FrozenStage("PARAFFIN_REVIEWED", "REMAINING_TISSUE", "处理剩余组织");
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_REPORT_CONFIRMED)) {
            return new FrozenStage("CONFIRMED", "COMPARE", "完成冰石对比");
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED)) {
            return new FrozenStage("REPORTED", "REPORT", "确认冰冻报告");
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_PRELIMINARY_SAVED)) {
            return new FrozenStage("DIAGNOSING", "REPORT", "完成术中电话回报");
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_SLICING_COMPLETED)) {
            return new FrozenStage("DIAGNOSING", "REPORT", "完成快速报告");
        }
        if (findPendingTask(context.tasks(), TechnicalWorkflowConstants.NODE_SLICING).isPresent()
            || hasCompletedEvent(context.events(), EVENT_FROZEN_GROSSING_COMPLETED)) {
            return new FrozenStage("GROSSING", "SLICING", "完成冰冻切片");
        }
        if (findPendingTask(context.tasks(), TechnicalWorkflowConstants.NODE_GROSSING).isPresent()
            || hasCompletedEvent(context.events(), EVENT_FROZEN_RECEIVE_COMPLETED)
            || registrationCompleted(context.registration())) {
            return new FrozenStage("RECEIVED", "GROSSING", "完成冰冻取材");
        }
        return new FrozenStage("REQUESTED", "RECEIVE", "完成冰冻接收");
    }

    private String taskStatus(String currentStatus, String pendingStatus, String completedBoundary) {
        if (pendingStatus.equals(currentStatus)) {
            return "PENDING";
        }
        if (statusReachedOrPassed(currentStatus, completedBoundary)) {
            return "COMPLETED";
        }
        return "PENDING";
    }

    private String timeoutLevel(String sessionStatus) {
        return switch (sessionStatus) {
            case "REQUESTED", "RECEIVED", "DIAGNOSING", "REPORTED", "CONFIRMED" -> "ORANGE";
            case "GROSSING" -> "RED";
            default -> "NONE";
        };
    }

    private boolean isTechnicalWorkbenchSession(FrozenWorkflowModels.FrozenSession session) {
        return List.of(NODE_FROZEN_RECEIVE, NODE_FROZEN_GROSSING, NODE_FROZEN_SLICING)
            .contains(session.currentTaskType());
    }

    private String buildSessionNo(SessionContext context) {
        return "FS-" + context.caseId();
    }

    private String completedEventTime(List<TrackingEvent> events, String eventType) {
        return events.stream()
            .filter(event -> eventType.equalsIgnoreCase(event.eventType()))
            .map(TrackingEvent::eventTime)
            .filter(value -> value != null)
            .max(LocalDateTime::compareTo)
            .map(this::stringify)
            .orElse(null);
    }

    private boolean hasCompletedEvent(List<TrackingEvent> events, String eventType) {
        return events.stream().anyMatch(event -> eventType.equalsIgnoreCase(event.eventType()));
    }

    private boolean registrationCompleted(TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration) {
        return registration != null && "COMPLETED".equalsIgnoreCase(registration.registrationStatus());
    }

    private String operatorByEvent(List<TrackingEvent> events, String eventType) {
        return events.stream()
            .filter(event -> eventType.equalsIgnoreCase(event.eventType()))
            .max(Comparator.comparing(TrackingEvent::eventTime, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(TrackingEvent::operatorName)
            .orElse(null);
    }

    private String receivedOperator(List<TrackingEvent> events) {
        return operatorByEvent(events, EVENT_FROZEN_RECEIVE_COMPLETED);
    }

    private String startedAt(List<TechnicalWorkflowRecords.TechnicalTask> tasks, String taskType) {
        return tasks.stream()
            .filter(task -> taskType.equalsIgnoreCase(task.taskType()))
            .map(TechnicalWorkflowRecords.TechnicalTask::startedAt)
            .filter(value -> value != null)
            .max(LocalDateTime::compareTo)
            .map(this::stringify)
            .orElse(null);
    }

    private Stream<TrackingEvent> syntheticRequestedTimelineEvent(SessionContext context) {
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_REQUESTED)) {
            return Stream.empty();
        }
        LocalDateTime requestedAt = context.application().getCreatedAt();
        if (requestedAt == null) {
            return Stream.empty();
        }
        return Stream.of(new TrackingEvent(
            context.caseId() + "-FROZEN_REQUESTED",
            context.application().getId().value(),
            firstSpecimenId(context.specimens()),
            context.caseId(),
            null,
            NODE_FROZEN_APPOINTMENT,
            EVENT_FROZEN_REQUESTED,
            null,
            requestedAt,
            null,
            context.application().getSubmittingDoctorName(),
            null,
            "创建冰冻术中申请",
            null));
    }

    private boolean isFrozenTimelineEvent(TrackingEvent event) {
        return event != null
            && event.eventType() != null
            && event.eventType().toUpperCase(Locale.ROOT).startsWith("FROZEN_");
    }

    private String reportTaskStartedAt(SessionContext context) {
        return latestEventTimeByNodeCode(context.events(), NODE_DIAGNOSIS_START)
            .or(() -> latestEventTimeByNodeCode(context.events(), NODE_DIAGNOSIS_ACCEPT))
            .or(() -> latestEventTimeByNodeCode(context.events(), NODE_DIAGNOSIS_ASSIGN))
            .orElse(null);
    }

    private Optional<String> latestEventTimeByNodeCode(List<TrackingEvent> events, String nodeCode) {
        return events.stream()
            .filter(event -> nodeCode.equalsIgnoreCase(event.nodeCode()))
            .map(TrackingEvent::eventTime)
            .filter(value -> value != null)
            .max(LocalDateTime::compareTo)
            .map(this::stringify);
    }

    private String summarizeGrossingDescription(List<TechnicalWorkflowRecords.TechnicalTask> tasks) {
        return tasks.stream()
            .map(TechnicalWorkflowRecords.TechnicalTask::grossDescription)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(null);
    }

    private boolean hasRegularCaseLinked(SessionContext context) {
        String checkItem = context.extension() == null ? null : context.extension().checkItem();
        return checkItem != null && checkItem.contains("常规病理");
    }

    private String firstSpecimenId(List<Specimen> specimens) {
        return specimens.stream().findFirst().map(Specimen::id).orElse(null);
    }

    private DiagnosticReportRepository.DiagnosticTask latestDiagnosticTask(SessionContext context) {
        return context.diagnosticTasks().stream()
            .max(Comparator.comparing(DiagnosticReportRepository.DiagnosticTask::createdAt))
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen diagnostic task is not available"));
    }

    private DiagnosticReportRepository.DiagnosticTask latestDiagnosticTaskAssignedDoctor(
        SessionContext context,
        String operatorUserId
    ) {
        DiagnosticReportRepository.DiagnosticTask diagnosticTask = latestDiagnosticTask(context);
        diagnosticReportSupport.ensureAssignedDoctor(diagnosticTask, operatorUserId);
        return diagnosticTask;
    }

    private DiagnosticReportRepository.DiagnosticTask latestStartedDiagnosticTaskAssignedDoctor(
        SessionContext context,
        String operatorUserId
    ) {
        DiagnosticReportRepository.DiagnosticTask diagnosticTask =
            latestDiagnosticTaskAssignedDoctor(context, operatorUserId);
        ensureDiagnosticTaskStarted(diagnosticTask);
        return diagnosticTask;
    }

    private void ensureDiagnosticTaskStarted(DiagnosticReportRepository.DiagnosticTask diagnosticTask) {
        if (!DiagnosticReportConstants.TASK_IN_PROGRESS.equals(diagnosticTask.status())) {
            throw new BlBusinessException(
                BlErrorCode.OPERATION_NOT_ALLOWED,
                409,
                "Frozen diagnostic task must be started before report actions");
        }
    }

    private String resolvePreliminaryResult(SessionContext context) {
        return context.diagnosticTasks().stream()
            .map(DiagnosticReportRepository.DiagnosticTask::frozenDiagnosisResult)
            .filter(value -> value != null && !value.isBlank())
            .reduce((first, second) -> second)
            .orElse(null);
    }

    private String resolveFinalDiagnosis(SessionContext context) {
        return resolvePreliminaryResult(context);
    }

    private String resolveCompareStatus(SessionContext context, FrozenStage stage) {
        String completedStatus = latestEventContent(context.events(), EVENT_FROZEN_COMPARE_COMPLETED)
            .map(this::extractBracketCode)
            .orElse(null);
        if (completedStatus != null) {
            return completedStatus;
        }
        if (NODE_FROZEN_COMPARE.equals(stage.currentTaskType())) {
            return "PENDING";
        }
        return null;
    }

    private String latestCompareSummary(List<TrackingEvent> events) {
        return latestEventContent(events, EVENT_FROZEN_COMPARE_COMPLETED)
            .map(this::extractEventValue)
            .orElse(null);
    }

    private String latestRemainingTissueStatus(List<TrackingEvent> events) {
        return latestEventContent(events, EVENT_FROZEN_CLOSED)
            .map(this::extractBracketCode)
            .orElse("PENDING");
    }

    private String requestedAt(Application application) {
        if (application.getSubmissionDate() != null) {
            return stringify(application.getSubmissionDate().atStartOfDay());
        }
        return stringify(application.getCreatedAt());
    }

    private String reportTaskStatus(SessionContext context, FrozenWorkflowModels.FrozenSession session) {
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_REPORT_CONFIRMED)) {
            return "COMPLETED";
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_PRELIMINARY_SAVED)) {
            return "IN_PROGRESS";
        }
        boolean reportStarted = context.diagnosticTasks().stream()
            .max(Comparator.comparing(DiagnosticReportRepository.DiagnosticTask::createdAt))
            .map(DiagnosticReportRepository.DiagnosticTask::status)
            .filter(DiagnosticReportConstants.TASK_IN_PROGRESS::equals)
            .isPresent();
        if (reportStarted && "REPORT".equals(session.currentTaskType())) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }

    private String compareTaskStatus(SessionContext context, FrozenWorkflowModels.FrozenSession session) {
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_COMPARE_COMPLETED)) {
            return "COMPLETED";
        }
        if ("COMPARE".equals(session.currentTaskType())) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }

    private String remainingTissueTaskStatus(SessionContext context, FrozenWorkflowModels.FrozenSession session) {
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_CLOSED)) {
            return "COMPLETED";
        }
        if ("REMAINING_TISSUE".equals(session.currentTaskType())) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }

    private String phoneBackTaskStatus(SessionContext context) {
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_PHONE_BACK_COMPLETED)) {
            return "COMPLETED";
        }
        if (hasCompletedEvent(context.events(), EVENT_FROZEN_PRELIMINARY_SAVED)) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }

    private boolean statusReachedOrPassed(String currentStatus, String boundaryStatus) {
        List<String> orderedStatuses = List.of(
            "REQUESTED",
            "RECEIVED",
            "GROSSING",
            "DIAGNOSING",
            "REPORTED",
            "CONFIRMED",
            "PARAFFIN_REVIEWED",
            "CLOSED");
        int currentIndex = orderedStatuses.indexOf(currentStatus);
        int boundaryIndex = orderedStatuses.indexOf(boundaryStatus);
        if (currentIndex < 0 || boundaryIndex < 0) {
            return false;
        }
        return currentIndex >= boundaryIndex;
    }

    private String latestHandoverComment(List<TrackingEvent> events) {
        return latestEventContent(events, EVENT_FROZEN_CLOSED)
            .map(this::extractEventValue)
            .map(value -> "-".equals(value) ? null : value)
            .orElse(null);
    }

    private Optional<String> latestEventContent(List<TrackingEvent> events, String eventType) {
        return events.stream()
            .filter(event -> eventType.equalsIgnoreCase(event.eventType()))
            .max(Comparator.comparing(TrackingEvent::eventTime, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(TrackingEvent::eventContent);
    }

    private String extractEventValue(String content) {
        if (content == null) {
            return null;
        }
        int index = content.indexOf('：');
        if (index < 0 || index + 1 >= content.length()) {
            return null;
        }
        return content.substring(index + 1).trim();
    }

    private String extractBracketCode(String content) {
        if (content == null) {
            return null;
        }
        int start = content.indexOf('[');
        int end = content.indexOf(']');
        if (start < 0 || end <= start + 1) {
            return null;
        }
        return content.substring(start + 1, end).trim();
    }

    private String formatPreliminaryEventContent(String preliminaryResult) {
        return "保存冰冻初步结果：" + preliminaryResult;
    }

    private String formatPhoneBackEventContent(String preliminaryResult) {
        return "完成术中电话回报：" + preliminaryResult;
    }

    private String formatCompareEventContent(String compareStatus, String compareSummary) {
        return "完成冰石对比[" + compareStatus + "]：" + compareSummary;
    }

    private String formatRemainingEventContent(String remainingTissueStatus, String remarks) {
        String handoverComment = remarks == null || remarks.isBlank() ? "-" : remarks.trim();
        return "完成剩余组织处理[" + remainingTissueStatus + "]：" + handoverComment;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, message);
        }
        return value.trim();
    }

    private String normalizeCompareStatus(String compareStatus) {
        String normalized = requireText(compareStatus, "Frozen compare status is required").toUpperCase(Locale.ROOT);
        if (!List.of("SIGNED_OFF", "MISMATCH").contains(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Frozen compare status is invalid");
        }
        return normalized;
    }

    private String normalizeRemainingTissueStatus(String remainingTissueStatus) {
        String normalized = requireText(remainingTissueStatus, "Frozen remaining tissue status is required")
            .toUpperCase(Locale.ROOT);
        if (!List.of("DISPOSED", "RETAINED").contains(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Frozen remaining tissue status is invalid");
        }
        return normalized;
    }

    private boolean matchesKeyword(FrozenWorkflowModels.FrozenSession session, String keyword) {
        return Stream.of(
                session.applicationNo(),
                session.caseId(),
                session.frozenPathologyNo(),
                session.patientName(),
                session.sessionNo())
            .filter(value -> value != null && !value.isBlank())
            .map(this::normalize)
            .anyMatch(value -> value.contains(keyword));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String formatReminderTitle(FrozenWorkflowModels.FrozenSession session) {
        return session.patientName() + " / " + session.frozenPathologyNo();
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private record SessionContext(
        String caseId,
        PathologyCase pathologyCase,
        Application application,
        TechnicalWorkflowRecords.TechnicalSpecimenRegistration registration,
        ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData extension,
        List<Specimen> specimens,
        List<TechnicalWorkflowRecords.TechnicalTask> tasks,
        List<DiagnosticReportRepository.DiagnosticTask> diagnosticTasks,
        List<TrackingEvent> events
    ) {
    }

    private record FrozenStage(
        String sessionStatus,
        String currentTaskType,
        String nextAction
    ) {
    }
}
