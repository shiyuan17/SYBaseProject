package com.company.bl.interfaces.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class FrozenWorkflowVo {

    private FrozenWorkflowVo() {
    }

    @Schema(name = "FrozenTechnicalWorkbenchResponse", description = "冰冻工作台响应")
    public record FrozenTechnicalWorkbenchResponse(
        FrozenReminderSummaryResponse reminders,
        List<FrozenSessionResponse> sessions
    ) {
    }

    @Schema(name = "FrozenSessionPageResponse", description = "冰冻会话分页响应")
    public record FrozenSessionPageResponse(
        List<FrozenSessionResponse> items,
        int page,
        int size,
        long total
    ) {
    }

    @Schema(name = "FrozenReminderSummaryResponse", description = "冰冻提醒汇总")
    public record FrozenReminderSummaryResponse(
        List<FrozenReminderItemResponse> items,
        int total,
        int orangeCount,
        int redCount
    ) {
    }

    @Schema(name = "FrozenReminderItemResponse", description = "冰冻提醒项")
    public record FrozenReminderItemResponse(
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

    @Schema(name = "FrozenSessionResponse", description = "冰冻会话摘要")
    public record FrozenSessionResponse(
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

    @Schema(name = "FrozenSessionDetailResponse", description = "冰冻会话详情")
    public record FrozenSessionDetailResponse(
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
        List<FrozenSessionTaskResponse> tasks,
        List<FrozenTimelineEventResponse> timeline
    ) {
    }

    @Schema(name = "FrozenSessionTaskResponse", description = "冰冻会话任务")
    public record FrozenSessionTaskResponse(
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

    @Schema(name = "FrozenTimelineEventResponse", description = "冰冻时间线事件")
    public record FrozenTimelineEventResponse(
        String id,
        String nodeCode,
        String eventType,
        String eventTime,
        String eventContent,
        String operatorName
    ) {
    }

    @Schema(name = "FrozenTaskActionResponse", description = "冰冻流程动作结果")
    public record FrozenTaskActionResponse(
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
