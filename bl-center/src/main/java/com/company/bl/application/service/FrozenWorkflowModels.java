package com.company.bl.application.service;

import java.util.List;

public final class FrozenWorkflowModels {

    private FrozenWorkflowModels() {
    }

    public record FrozenTechnicalWorkbenchView(
        FrozenReminderSummary reminders,
        List<FrozenSession> sessions
    ) {
    }

    public record FrozenSessionListQuery(
        int page,
        int size,
        String keyword,
        String sessionStatus,
        String timeoutLevel
    ) {
    }

    public record FrozenSessionListPage(
        List<FrozenSession> items,
        int page,
        int size,
        long total
    ) {
    }

    public record FrozenReminderSummary(
        List<FrozenReminderItem> items,
        int total,
        int orangeCount,
        int redCount
    ) {
    }

    public record FrozenReminderItem(
        String id,
        String sessionId,
        String caseId,
        String sessionNo,
        String frozenPathologyNo,
        String patientName,
        String requestedAt,
        String currentTaskType,
        String nextAction,
        String timeoutLevel,
        String title
    ) {
    }

    public record FrozenSession(
        String id,
        String applicationId,
        String applicationNo,
        boolean autoPrintSlides,
        String caseId,
        String compareStatus,
        String compareSummary,
        String currentTaskType,
        String finalConfirmedAt,
        String finalDiagnosis,
        String frozenPathologyNo,
        String grossingCompletedAt,
        String grossingDescription,
        String grossingStartedAt,
        String handoverComment,
        boolean hasRegularCaseLinked,
        boolean intraoperativePhoneBack,
        String nextAction,
        String patientName,
        String phoneBackAt,
        String preliminaryResult,
        String receivedAt,
        String remainingTissueStatus,
        String reportConfirmedAt,
        String requestedAt,
        String requestDoctorName,
        String sessionNo,
        String sessionStatus,
        String slicingCompletedAt,
        String slicingStartedAt,
        String timeoutLevel
    ) {
    }

    public record FrozenSessionDetail(
        String id,
        String applicationId,
        String applicationNo,
        boolean autoPrintSlides,
        String caseId,
        String compareStatus,
        String compareSummary,
        String currentTaskType,
        String finalConfirmedAt,
        String finalDiagnosis,
        String frozenPathologyNo,
        String grossingCompletedAt,
        String grossingDescription,
        String grossingStartedAt,
        String handoverComment,
        boolean hasRegularCaseLinked,
        boolean intraoperativePhoneBack,
        String nextAction,
        String patientName,
        String phoneBackAt,
        String preliminaryResult,
        String receivedAt,
        String remainingTissueStatus,
        String reportConfirmedAt,
        String requestedAt,
        String requestDoctorName,
        String sessionNo,
        String sessionStatus,
        String slicingCompletedAt,
        String slicingStartedAt,
        String timeoutLevel,
        List<String> reminders,
        List<FrozenSessionTask> tasks,
        List<FrozenTimelineEvent> timeline
    ) {
    }

    public record FrozenSessionTask(
        String id,
        String taskType,
        String status,
        String timeoutLevel,
        String startedAt,
        String completedAt,
        String operatorName,
        String remarks
    ) {
    }

    public record FrozenTimelineEvent(
        String id,
        String nodeCode,
        String eventType,
        String eventTime,
        String eventContent,
        String operatorName
    ) {
    }

    public record FrozenActionCommand(
        String sessionId,
        String operatorUserId,
        String operatorName,
        boolean workbenchOverrideAllowed,
        String terminalCode,
        String remarks
    ) {
    }

    public record FrozenPhoneBackCommand(
        String sessionId,
        String operatorUserId,
        String operatorName,
        boolean workbenchOverrideAllowed,
        String terminalCode,
        String remarks,
        String preliminaryResult
    ) {
    }

    public record FrozenParaffinCompareCommand(
        String sessionId,
        String operatorUserId,
        String operatorName,
        boolean workbenchOverrideAllowed,
        String terminalCode,
        String remarks,
        String compareStatus,
        String compareSummary
    ) {
    }

    public record FrozenRemainingTissueCommand(
        String sessionId,
        String operatorUserId,
        String operatorName,
        boolean workbenchOverrideAllowed,
        String terminalCode,
        String remarks,
        String remainingTissueStatus
    ) {
    }

    public record FrozenTaskActionResult(
        String sessionId,
        String caseId,
        String frozenPathologyNo,
        String pathologyNo,
        String taskType,
        String taskStatus,
        String sessionStatus,
        String nextTaskType
    ) {
    }
}
