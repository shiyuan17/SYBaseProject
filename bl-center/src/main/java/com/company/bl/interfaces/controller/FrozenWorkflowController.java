package com.company.bl.interfaces.controller;

import com.company.bl.application.service.FrozenWorkflowModels;
import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RbacPermissionRepository;
import com.company.bl.interfaces.auth.RequireAnyPermission;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.FrozenActionRequest;
import com.company.bl.interfaces.dto.FrozenParaffinCompareRequest;
import com.company.bl.interfaces.dto.FrozenPhoneBackRequest;
import com.company.bl.interfaces.dto.FrozenRemainingTissueRequest;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenReminderItemResponse;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenReminderSummaryResponse;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenSessionDetailResponse;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenSessionPageResponse;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenSessionResponse;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenSessionTaskResponse;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenTaskActionResponse;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenTechnicalWorkbenchResponse;
import com.company.bl.interfaces.vo.FrozenWorkflowVo.FrozenTimelineEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "冰冻流程", description = "冰冻流程工作台与动作接口")
public class FrozenWorkflowController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;
    private final RbacPermissionRepository permissionRepository;

    public FrozenWorkflowController(TechnicalWorkflowAppService technicalWorkflowAppService,
                                    RbacPermissionRepository permissionRepository) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
        this.permissionRepository = permissionRepository;
    }

    @Operation(summary = "查询冰冻工作台", description = "返回冰冻工作台会话列表与提醒汇总。")
    @RequireAnyPermission({
        M2PermissionCodes.SPECIMEN_RECEIVE,
        M3PermissionCodes.TECHNICAL_TASK_QUERY
    })
    @GetMapping({"/frozen-workflow/workbench", "/frozen-sessions/workbench"})
    public FrozenTechnicalWorkbenchResponse getWorkbench() {
        FrozenWorkflowModels.FrozenTechnicalWorkbenchView result =
            technicalWorkflowAppService.getFrozenTechnicalWorkbench();
        return new FrozenTechnicalWorkbenchResponse(
            toReminderSummaryResponse(result.reminders()),
            result.sessions().stream().map(this::toSessionResponse).toList());
    }

    @Operation(summary = "查询冰冻提醒汇总", description = "返回冰冻提醒摘要，用于提醒轮询和摘要卡片。")
    @RequireAnyPermission({
        M2PermissionCodes.SPECIMEN_RECEIVE,
        M2PermissionCodes.SPECIMEN_REGISTER
    })
    @GetMapping({"/frozen-workflow/reminders", "/frozen-sessions/reminders"})
    public FrozenReminderSummaryResponse getReminderSummary() {
        return toReminderSummaryResponse(technicalWorkflowAppService.getFrozenReminderSummary());
    }

    @Operation(summary = "分页查询冰冻会话", description = "按关键字、会话状态、超时等级查询冰冻会话分页结果。")
    @RequireAnyPermission({
        M2PermissionCodes.SPECIMEN_RECEIVE,
        M2PermissionCodes.SPECIMEN_REGISTER
    })
    @GetMapping({"/frozen-workflow/sessions", "/frozen-sessions"})
    public FrozenSessionPageResponse listSessions(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sessionStatus,
        @RequestParam(required = false) String timeoutLevel
    ) {
        FrozenWorkflowModels.FrozenSessionListPage result = technicalWorkflowAppService.listFrozenSessions(
            new FrozenWorkflowModels.FrozenSessionListQuery(
                page,
                size,
                keyword,
                sessionStatus,
                timeoutLevel));
        return new FrozenSessionPageResponse(
            result.items().stream().map(this::toSessionResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "查询冰冻会话详情", description = "返回冰冻会话详情、任务和时间线。")
    @RequireAnyPermission({
        M2PermissionCodes.SPECIMEN_RECEIVE,
        M2PermissionCodes.SPECIMEN_REGISTER,
        M3PermissionCodes.TECHNICAL_TASK_QUERY,
        M4PermissionCodes.WORKBENCH_QUERY
    })
    @GetMapping({
        "/frozen-workflow/sessions/{sessionId}",
        "/frozen-sessions/{sessionId}"
    })
    public FrozenSessionDetailResponse getSessionDetail(@PathVariable String sessionId) {
        FrozenWorkflowModels.FrozenSessionDetail result =
            technicalWorkflowAppService.getFrozenSessionDetail(sessionId);
        return new FrozenSessionDetailResponse(
            result.id(),
            result.applicationId(),
            result.applicationNo(),
            result.autoPrintSlides(),
            result.caseId(),
            result.compareStatus(),
            result.compareSummary(),
            result.currentTaskType(),
            result.finalConfirmedAt(),
            result.finalDiagnosis(),
            result.frozenPathologyNo(),
            result.grossingCompletedAt(),
            result.grossingDescription(),
            result.grossingStartedAt(),
            result.handoverComment(),
            result.hasRegularCaseLinked(),
            result.intraoperativePhoneBack(),
            result.nextAction(),
            result.patientName(),
            result.phoneBackAt(),
            result.preliminaryResult(),
            result.receivedAt(),
            result.remainingTissueStatus(),
            result.reportConfirmedAt(),
            result.requestedAt(),
            result.requestDoctorName(),
            result.sessionNo(),
            result.sessionStatus(),
            result.slicingCompletedAt(),
            result.slicingStartedAt(),
            result.timeoutLevel(),
            result.reminders(),
            result.tasks().stream().map(task -> new FrozenSessionTaskResponse(
                task.id(),
                task.taskType(),
                task.status(),
                task.timeoutLevel(),
                task.startedAt(),
                task.completedAt(),
                task.operatorName(),
                task.remarks())).toList(),
            result.timeline().stream().map(event -> new FrozenTimelineEventResponse(
                event.id(),
                event.nodeCode(),
                event.eventType(),
                event.eventTime(),
                event.eventContent(),
                event.operatorName())).toList());
    }

    @Operation(summary = "完成冰冻接收", description = "完成冰冻接收并推进到冰冻取材。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PostMapping({
        "/frozen-workflow/sessions/{sessionId}/receive/complete",
        "/frozen-sessions/{sessionId}/receive"
    })
    public FrozenTaskActionResponse completeReceive(@PathVariable String sessionId,
                                                    @Valid @RequestBody FrozenActionRequest request,
                                                    HttpServletRequest httpServletRequest) {
        return toActionResponse(technicalWorkflowAppService.completeFrozenReceive(
            new FrozenWorkflowModels.FrozenActionCommand(
                sessionId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                false,
                request.getTerminalCode(),
                request.getRemarks())));
    }

    @Operation(summary = "完成冰冻取材", description = "完成冰冻取材并推进到冰冻切片。")
    @RequirePermission(M3PermissionCodes.GROSSING)
    @PostMapping({
        "/frozen-workflow/sessions/{sessionId}/grossing/complete",
        "/frozen-sessions/{sessionId}/grossing/complete"
    })
    public FrozenTaskActionResponse completeGrossing(@PathVariable String sessionId,
                                                     @Valid @RequestBody FrozenActionRequest request,
                                                     HttpServletRequest httpServletRequest) {
        return toActionResponse(technicalWorkflowAppService.completeFrozenGrossing(
            new FrozenWorkflowModels.FrozenActionCommand(
                sessionId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                false,
                request.getTerminalCode(),
                request.getRemarks())));
    }

    @Operation(summary = "完成冰冻切片", description = "完成冰冻切片并推进到诊断。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping({
        "/frozen-workflow/sessions/{sessionId}/slicing/complete",
        "/frozen-sessions/{sessionId}/slicing/complete"
    })
    public FrozenTaskActionResponse completeSlicing(@PathVariable String sessionId,
                                                    @Valid @RequestBody FrozenActionRequest request,
                                                    HttpServletRequest httpServletRequest) {
        return toActionResponse(technicalWorkflowAppService.completeFrozenSlicing(
            new FrozenWorkflowModels.FrozenActionCommand(
                sessionId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                false,
                request.getTerminalCode(),
                request.getRemarks())));
    }

    @Operation(summary = "保存冰冻初步结果", description = "保存冰冻初步结果并推进到电话回报。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_CREATE})
    @PostMapping({
        "/frozen-workflow/sessions/{sessionId}/preliminary-report/save",
        "/frozen-sessions/{sessionId}/preliminary-report/save"
    })
    public FrozenTaskActionResponse savePreliminaryReport(@PathVariable String sessionId,
                                                          @Valid @RequestBody FrozenPhoneBackRequest request,
                                                          HttpServletRequest httpServletRequest) {
        return toActionResponse(technicalWorkflowAppService.saveFrozenPreliminaryReport(
            new FrozenWorkflowModels.FrozenPhoneBackCommand(
                sessionId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getPreliminaryResult())));
    }

    @Operation(summary = "完成术中电话回报", description = "完成术中电话回报并推进到冰石对比。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_CREATE})
    @PostMapping({
        "/frozen-workflow/sessions/{sessionId}/phone-back/complete",
        "/frozen-sessions/{sessionId}/phone-back/complete"
    })
    public FrozenTaskActionResponse completePhoneBack(@PathVariable String sessionId,
                                                      @Valid @RequestBody FrozenPhoneBackRequest request,
                                                      HttpServletRequest httpServletRequest) {
        return toActionResponse(technicalWorkflowAppService.completeFrozenPhoneBack(
            new FrozenWorkflowModels.FrozenPhoneBackCommand(
                sessionId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getPreliminaryResult())));
    }

    @Operation(summary = "确认冰冻报告", description = "确认术中快速冰冻结果。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_CREATE})
    @PostMapping({
        "/frozen-workflow/sessions/{sessionId}/report/confirm",
        "/frozen-sessions/{sessionId}/report/confirm"
    })
    public FrozenTaskActionResponse confirmReport(@PathVariable String sessionId,
                                                  @Valid @RequestBody FrozenActionRequest request,
                                                  HttpServletRequest httpServletRequest) {
        return toActionResponse(technicalWorkflowAppService.confirmFrozenReport(
            new FrozenWorkflowModels.FrozenActionCommand(
                sessionId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks())));
    }

    @Operation(summary = "完成冰石对比", description = "完成冰冻与石蜡结果对比。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.REPORT_REVIEW})
    @PostMapping({
        "/frozen-workflow/sessions/{sessionId}/paraffin-compare/complete",
        "/frozen-sessions/{sessionId}/paraffin-compare/complete"
    })
    public FrozenTaskActionResponse completeParaffinCompare(@PathVariable String sessionId,
                                                            @Valid @RequestBody FrozenParaffinCompareRequest request,
                                                            HttpServletRequest httpServletRequest) {
        return toActionResponse(technicalWorkflowAppService.completeFrozenParaffinCompare(
            new FrozenWorkflowModels.FrozenParaffinCompareCommand(
                sessionId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getCompareStatus(),
                request.getCompareSummary())));
    }

    @Operation(summary = "完成剩余组织处理", description = "完成剩余组织处理并关闭冰冻会话。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M3PermissionCodes.GROSSING})
    @PostMapping({
        "/frozen-workflow/sessions/{sessionId}/remaining-tissue/complete",
        "/frozen-sessions/{sessionId}/remaining-tissue/complete"
    })
    public FrozenTaskActionResponse completeRemainingTissue(@PathVariable String sessionId,
                                                            @Valid @RequestBody FrozenRemainingTissueRequest request,
                                                            HttpServletRequest httpServletRequest) {
        return toActionResponse(technicalWorkflowAppService.completeFrozenRemainingTissue(
            new FrozenWorkflowModels.FrozenRemainingTissueCommand(
                sessionId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                workbenchOverrideAllowed(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks(),
                request.getRemainingTissueStatus())));
    }

    private FrozenSessionResponse toSessionResponse(FrozenWorkflowModels.FrozenSession session) {
        return new FrozenSessionResponse(
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
            session.timeoutLevel());
    }

    private boolean workbenchOverrideAllowed(HttpServletRequest request) {
        return permissionRepository.isWorkbenchOverrideAllowed(resolveUserId(request));
    }

    private FrozenTaskActionResponse toActionResponse(FrozenWorkflowModels.FrozenTaskActionResult result) {
        return new FrozenTaskActionResponse(
            result.sessionId(),
            result.caseId(),
            result.frozenPathologyNo(),
            result.pathologyNo(),
            result.taskType(),
            result.taskStatus(),
            result.sessionStatus(),
            result.nextTaskType());
    }

    private FrozenReminderSummaryResponse toReminderSummaryResponse(FrozenWorkflowModels.FrozenReminderSummary summary) {
        return new FrozenReminderSummaryResponse(
            summary.items().stream().map(item -> new FrozenReminderItemResponse(
                item.id(),
                item.sessionId(),
                item.caseId(),
                item.sessionNo(),
                item.frozenPathologyNo(),
                item.patientName(),
                item.requestedAt(),
                item.currentTaskType(),
                item.nextAction(),
                item.timeoutLevel(),
                item.title())).toList(),
            summary.total(),
            summary.orangeCount(),
            summary.redCount());
    }
}
