package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SlicingCompleteRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.SlicingResponse;
import com.company.bl.interfaces.vo.SlicingWorkbenchResponse;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slicings")
@Tag(name = "技术流程", description = "切片开始与完成接口")
public class SlicingController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public SlicingController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "查询切片工作站", description = "返回旧站式切片工作站所需的顶部统计、待切列表和今日已完成列表。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @GetMapping("/workbench")
    public SlicingWorkbenchResponse getWorkbench(
        @Parameter(description = "病人 ID / 病理号 / 患者姓名 / 标本名称关键词")
        @RequestParam(required = false) String keyword,
        @Parameter(description = "是否仅查看今天待切")
        @RequestParam(defaultValue = "false") boolean pendingTodayOnly,
        @Parameter(description = "是否仅查看过期任务")
        @RequestParam(defaultValue = "false") boolean overdueOnly,
        @Parameter(description = "待切页码")
        @RequestParam(defaultValue = "1") int pendingPage,
        @Parameter(description = "待切分页大小")
        @RequestParam(defaultValue = "20") int pendingSize,
        @Parameter(description = "已完成页码")
        @RequestParam(defaultValue = "1") int completedPage,
        @Parameter(description = "已完成分页大小")
        @RequestParam(defaultValue = "20") int completedSize,
        HttpServletRequest httpServletRequest
    ) {
        TechnicalWorkflowModels.SlicingWorkbenchView result =
            technicalWorkflowAppService.getSlicingWorkbench(
                new TechnicalWorkflowModels.SlicingWorkbenchQuery(
                    keyword,
                    pendingTodayOnly,
                    overdueOnly,
                    pendingPage,
                    pendingSize,
                    completedPage,
                    completedSize,
                    resolveUserId(httpServletRequest)));
        return new SlicingWorkbenchResponse(
            new SlicingWorkbenchResponse.Stats(
                result.stats().pendingTodayCount(),
                result.stats().pendingTomorrowCount(),
                result.stats().completedMineTodayCount(),
                result.stats().completedDeptTodayCount(),
                result.stats().overdueCount(),
                result.stats().pendingPrintCount()),
            result.pendingList().stream().map(item -> new SlicingWorkbenchResponse.Row(
                item.taskId(),
                item.caseId(),
                item.pathologyNo(),
                item.patientName(),
                item.patientId(),
                item.specimenId(),
                item.specimenName(),
                item.embeddingBoxId(),
                item.slideId(),
                item.slideNo(),
                item.slicingOperatorName(),
                item.slicingRemark(),
                item.completedAt(),
                item.grossingEvaluation(),
                item.embeddingEvaluation(),
                item.embeddingOperatorName(),
                item.embeddingClearRemark(),
                item.shiftRemark(),
                item.sliceNotice(),
                item.taskStatus(),
                item.timedOut(),
                item.selectable())).toList(),
            result.pendingPage(),
            result.pendingSize(),
            result.pendingTotal(),
            result.completedTodayList().stream().map(item -> new SlicingWorkbenchResponse.Row(
                item.taskId(),
                item.caseId(),
                item.pathologyNo(),
                item.patientName(),
                item.patientId(),
                item.specimenId(),
                item.specimenName(),
                item.embeddingBoxId(),
                item.slideId(),
                item.slideNo(),
                item.slicingOperatorName(),
                item.slicingRemark(),
                item.completedAt(),
                item.grossingEvaluation(),
                item.embeddingEvaluation(),
                item.embeddingOperatorName(),
                item.embeddingClearRemark(),
                item.shiftRemark(),
                item.sliceNotice(),
                item.taskStatus(),
                item.timedOut(),
                item.selectable())).toList(),
            result.completedPage(),
            result.completedSize(),
            result.completedTotal());
    }

    @Operation(summary = "开始切片", description = "将技术任务推进到切片中状态。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/start")
    public TaskOperationResponse start(@Valid @RequestBody TechnicalTaskStartRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TaskStartResult result = technicalWorkflowAppService.startSlicing(
            new TechnicalWorkflowModels.TaskStartCommand(
                request.getTaskId(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new TaskOperationResponse(result.taskId(), result.caseId(), result.caseStatus(), result.taskStatus());
    }

    @Operation(summary = "完成切片", description = "完成切片并返回生成的切片信息。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/complete")
    public SlicingResponse complete(@Valid @RequestBody SlicingCompleteRequest request,
                                    HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.SlicingResult result = technicalWorkflowAppService.completeSlicing(
            new TechnicalWorkflowModels.SlicingCompleteCommand(
                request.getTaskId(),
                request.getEmbeddingBoxId(),
                request.getSlideCount(),
                request.getSliceCountPerSlide(),
                request.getSliceThickness(),
                request.getQualityIssue(),
                request.getDeviceCode(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new SlicingResponse(result.taskId(), result.slicingId(), result.slideIds(), result.caseStatus());
    }
}
