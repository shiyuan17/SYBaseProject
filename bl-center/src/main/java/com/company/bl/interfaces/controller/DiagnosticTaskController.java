package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequireAnyPermission;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.AssignDiagnosticTaskRequest;
import com.company.bl.interfaces.dto.DiagnosticTaskActionRequest;
import com.company.bl.interfaces.vo.DiagnosticTaskOperationResponse;
import com.company.bl.interfaces.vo.PendingDiagnosticTaskPageResponse;
import com.company.bl.interfaces.vo.PendingDiagnosticTaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/diagnostic-tasks")
@Tag(name = "医生流程", description = "诊断任务待办与分派接口")
public class DiagnosticTaskController extends TechnicalControllerSupport {

    private final DiagnosticReportAppService diagnosticReportAppService;

    public DiagnosticTaskController(DiagnosticReportAppService diagnosticReportAppService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
    }

    @Operation(summary = "查询待处理诊断任务", description = "分页查询诊断流程待处理任务列表。")
    @RequireAnyPermission({M4PermissionCodes.WORKBENCH_QUERY, M4PermissionCodes.DIAG_TASK_QUERY})
    @GetMapping("/pending")
    public PendingDiagnosticTaskPageResponse listPending(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                         @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
                                                         @Parameter(description = "任务类型") @RequestParam(required = false) String taskType,
                                                         @Parameter(description = "任务状态") @RequestParam(required = false) String taskStatus,
                                                         @Parameter(description = "病理号") @RequestParam(required = false) String pathologyNo,
                                                         @Parameter(description = "创建开始日期，格式 YYYY-MM-DD") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                         @Parameter(description = "创建结束日期，格式 YYYY-MM-DD") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                                         HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PendingDiagnosticTaskPage result = diagnosticReportAppService.listPendingTasks(
            new DiagnosticReportModels.PendingDiagnosticTaskQuery(
                page,
                size,
                taskType,
                taskStatus,
                pathologyNo,
                dateFrom,
                dateTo,
                resolveUserId(httpServletRequest),
                resolveRoleCode(httpServletRequest)));
        return new PendingDiagnosticTaskPageResponse(
            result.items().stream().map(this::toResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "查询可分派诊断任务", description = "分页查询诊断分派列表，不按当前诊断医生过滤本人任务。")
    @RequirePermission(M4PermissionCodes.DIAG_TASK_QUERY)
    @GetMapping("/assignment")
    public PendingDiagnosticTaskPageResponse listAssignment(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                            @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
                                                            @Parameter(description = "任务类型") @RequestParam(required = false) String taskType,
                                                            @Parameter(description = "任务状态") @RequestParam(required = false) String taskStatus,
                                                            @Parameter(description = "病理号") @RequestParam(required = false) String pathologyNo,
                                                            @Parameter(description = "创建开始日期，格式 YYYY-MM-DD") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                            @Parameter(description = "创建结束日期，格式 YYYY-MM-DD") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                                            HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.PendingDiagnosticTaskPage result = diagnosticReportAppService.listPendingTasks(
            new DiagnosticReportModels.PendingDiagnosticTaskQuery(
                page,
                size,
                taskType,
                taskStatus,
                pathologyNo,
                dateFrom,
                dateTo,
                resolveUserId(httpServletRequest),
                null));
        return new PendingDiagnosticTaskPageResponse(
            result.items().stream().map(this::toResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "分派诊断任务", description = "将病例级诊断任务分派给责任医生、初诊医生和审核医生。")
    @RequirePermission(M4PermissionCodes.ASSIGN)
    @PostMapping("/{id}/assign")
    public DiagnosticTaskOperationResponse assign(@PathVariable("id") String taskId,
                                                  @Valid @RequestBody AssignDiagnosticTaskRequest request,
                                                  HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.DiagnosticTaskResult result = diagnosticReportAppService.assignTask(
            new DiagnosticReportModels.AssignDiagnosticTaskCommand(
                taskId,
                request.getDiagnosisDoctorUserId(),
                request.getDiagnosisDoctorName(),
                request.getPrimaryDoctorUserId(),
                request.getPrimaryDoctorName(),
                request.getReviewerUserId(),
                request.getReviewerName(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new DiagnosticTaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @Operation(summary = "接收诊断任务", description = "责任医生或初诊医生接收已分派诊断任务。")
    @RequirePermission(M4PermissionCodes.ACCEPT)
    @PostMapping("/{id}/accept")
    public DiagnosticTaskOperationResponse accept(@PathVariable("id") String taskId,
                                                  @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                  HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.DiagnosticTaskResult result = diagnosticReportAppService.acceptTask(
            new DiagnosticReportModels.TaskActionCommand(
                taskId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new DiagnosticTaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @Operation(summary = "开始诊断任务", description = "将诊断任务推进为进行中状态。")
    @RequirePermission(M4PermissionCodes.START)
    @PostMapping("/{id}/start")
    public DiagnosticTaskOperationResponse start(@PathVariable("id") String taskId,
                                                 @Valid @RequestBody DiagnosticTaskActionRequest request,
                                                 HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.DiagnosticTaskResult result = diagnosticReportAppService.startTask(
            new DiagnosticReportModels.TaskActionCommand(
                taskId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new DiagnosticTaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    private PendingDiagnosticTaskResponse toResponse(DiagnosticReportModels.TaskView item) {
        return new PendingDiagnosticTaskResponse(
            item.id(),
            item.applicationId(),
            item.applicationNo(),
            item.patientName(),
            item.patientId(),
            item.patientIdDisplay(),
            item.caseId(),
            item.pathologyNo(),
            item.applicationType(),
            item.checkItem(),
            item.blockCount(),
            item.submittingDepartmentName(),
            item.specimenName(),
            item.taskType(),
            item.taskStatus(),
            item.reportStatus(),
            item.diagnosisDoctorUserId(),
            item.diagnosisDoctorName(),
            item.primaryDoctorUserId(),
            item.primaryDoctorName(),
            item.reviewerUserId(),
            item.reviewerName(),
            item.assignedAt(),
            item.acceptedAt(),
            item.completedAt(),
            item.remarks());
    }
}
