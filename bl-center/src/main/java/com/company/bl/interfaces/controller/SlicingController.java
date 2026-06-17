package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SlicingCompleteRequest;
import com.company.bl.interfaces.dto.SlicingSlidePrintMergeGroupCancelRequest;
import com.company.bl.interfaces.dto.SlicingSlidePrintMergeGroupPrintRequest;
import com.company.bl.interfaces.dto.SlicingSlidePrintMergeGroupRequest;
import com.company.bl.interfaces.dto.SlicingSlidePrintRequest;
import com.company.bl.interfaces.dto.TechnicalTaskStartRequest;
import com.company.bl.interfaces.vo.SlicingResponse;
import com.company.bl.interfaces.vo.SlicingSlidePrintMergeGroupResponse;
import com.company.bl.interfaces.vo.SlicingSlidePrintResponse;
import com.company.bl.interfaces.vo.SlicingWorkbenchResponse;
import com.company.bl.interfaces.vo.TaskOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
        @Parameter(description = "申请类型：ROUTINE / FROZEN")
        @RequestParam(required = false) String applicationType,
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
        @Parameter(description = "开始日期，格式 YYYY-MM-DD")
        @RequestParam(required = false) LocalDate dateFrom,
        @Parameter(description = "结束日期，格式 YYYY-MM-DD")
        @RequestParam(required = false) LocalDate dateTo,
        @Parameter(description = "工作日期，格式 YYYY-MM-DD")
        @RequestParam(required = false) LocalDate workDate,
        HttpServletRequest httpServletRequest
    ) {
        TechnicalWorkflowModels.SlicingWorkbenchView result =
            technicalWorkflowAppService.getSlicingWorkbench(
                new TechnicalWorkflowModels.SlicingWorkbenchQuery(
                    keyword,
                    applicationType,
                    pendingTodayOnly,
                    overdueOnly,
                    pendingPage,
                    pendingSize,
                    completedPage,
                    completedSize,
                    resolveUserId(httpServletRequest),
                    dateFrom,
                    dateTo,
                    workDate));
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
                item.applicationType(),
                item.pathologyNo(),
                item.patientName(),
                item.patientId(),
                item.specimenId(),
                item.specimenName(),
                item.embeddingBoxId(),
                item.embeddingBoxNo(),
                item.slideId(),
                item.slideNo(),
                item.slicingOperatorName(),
                item.slicingRemark(),
                item.completedAt(),
                item.grossingEvaluation(),
                item.embeddingEvaluation(),
                item.embeddingOperatorName(),
                item.embeddingClearRemark(),
                item.embeddingRemarks(),
                item.shiftRemark(),
                item.sliceNotice(),
                item.submittingDepartmentName(),
                item.taskStatus(),
                item.slidePrintStatus(),
                item.printedSlideCount(),
                item.combinedSlide(),
                item.timedOut(),
                item.selectable(),
                item.printGroupId(),
                item.mergedPrintGroup(),
                item.taskIds(),
                item.embeddingBoxIds())).toList(),
            result.pendingPrintList().stream().map(item -> new SlicingWorkbenchResponse.Row(
                item.taskId(),
                item.caseId(),
                item.applicationType(),
                item.pathologyNo(),
                item.patientName(),
                item.patientId(),
                item.specimenId(),
                item.specimenName(),
                item.embeddingBoxId(),
                item.embeddingBoxNo(),
                item.slideId(),
                item.slideNo(),
                item.slicingOperatorName(),
                item.slicingRemark(),
                item.completedAt(),
                item.grossingEvaluation(),
                item.embeddingEvaluation(),
                item.embeddingOperatorName(),
                item.embeddingClearRemark(),
                item.embeddingRemarks(),
                item.shiftRemark(),
                item.sliceNotice(),
                item.submittingDepartmentName(),
                item.taskStatus(),
                item.slidePrintStatus(),
                item.printedSlideCount(),
                item.combinedSlide(),
                item.timedOut(),
                item.selectable(),
                item.printGroupId(),
                item.mergedPrintGroup(),
                item.taskIds(),
                item.embeddingBoxIds())).toList(),
            result.pendingSliceList().stream().map(item -> new SlicingWorkbenchResponse.Row(
                item.taskId(),
                item.caseId(),
                item.applicationType(),
                item.pathologyNo(),
                item.patientName(),
                item.patientId(),
                item.specimenId(),
                item.specimenName(),
                item.embeddingBoxId(),
                item.embeddingBoxNo(),
                item.slideId(),
                item.slideNo(),
                item.slicingOperatorName(),
                item.slicingRemark(),
                item.completedAt(),
                item.grossingEvaluation(),
                item.embeddingEvaluation(),
                item.embeddingOperatorName(),
                item.embeddingClearRemark(),
                item.embeddingRemarks(),
                item.shiftRemark(),
                item.sliceNotice(),
                item.submittingDepartmentName(),
                item.taskStatus(),
                item.slidePrintStatus(),
                item.printedSlideCount(),
                item.combinedSlide(),
                item.timedOut(),
                item.selectable(),
                item.printGroupId(),
                item.mergedPrintGroup(),
                item.taskIds(),
                item.embeddingBoxIds())).toList(),
            result.pendingPage(),
            result.pendingSize(),
            result.pendingTotal(),
            result.pendingPrintTotal(),
            result.pendingSliceTotal(),
            result.completedTodayList().stream().map(item -> new SlicingWorkbenchResponse.Row(
                item.taskId(),
                item.caseId(),
                item.applicationType(),
                item.pathologyNo(),
                item.patientName(),
                item.patientId(),
                item.specimenId(),
                item.specimenName(),
                item.embeddingBoxId(),
                item.embeddingBoxNo(),
                item.slideId(),
                item.slideNo(),
                item.slicingOperatorName(),
                item.slicingRemark(),
                item.completedAt(),
                item.grossingEvaluation(),
                item.embeddingEvaluation(),
                item.embeddingOperatorName(),
                item.embeddingClearRemark(),
                item.embeddingRemarks(),
                item.shiftRemark(),
                item.sliceNotice(),
                item.submittingDepartmentName(),
                item.taskStatus(),
                item.slidePrintStatus(),
                item.printedSlideCount(),
                item.combinedSlide(),
                item.timedOut(),
                item.selectable(),
                item.printGroupId(),
                item.mergedPrintGroup(),
                item.taskIds(),
                item.embeddingBoxIds())).toList(),
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

    @Operation(summary = "完成玻片打印", description = "为切片任务预生成玻片并确认打印，打印后任务进入切片处理列表。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/slide-print")
    public SlicingSlidePrintResponse printSlides(@Valid @RequestBody SlicingSlidePrintRequest request,
                                                 HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.SlicingSlidePrintResult result = technicalWorkflowAppService.printSlicingSlides(
            new TechnicalWorkflowModels.SlicingSlidePrintCommand(
                request.getTaskId(),
                request.getEmbeddingBoxId(),
                request.getSourceSlideCount(),
                request.isMergeAdjacent(),
                request.getPrinterCode(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new SlicingSlidePrintResponse(
            result.taskId(),
            result.slicingId(),
            result.slideIds(),
            result.slideNos(),
            result.merged(),
            result.printedSlideCount());
    }

    @Operation(summary = "两两合片", description = "将未打印切片任务按同患者、同病例、同蜡块前缀两两合并显示。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/slide-print-merge-groups")
    public SlicingSlidePrintMergeGroupResponse createPrintMergeGroups(
        @Valid @RequestBody SlicingSlidePrintMergeGroupRequest request,
        HttpServletRequest httpServletRequest
    ) {
        TechnicalWorkflowModels.SlicingSlidePrintMergeGroupResult result =
            technicalWorkflowAppService.createSlicingSlidePrintMergeGroups(
                new TechnicalWorkflowModels.SlicingSlidePrintMergeGroupCommand(
                    request.getTaskIds(),
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    request.getTerminalCode(),
                    request.getRemarks()));
        return new SlicingSlidePrintMergeGroupResponse(result.printGroupIds());
    }

    @Operation(summary = "取消合片", description = "取消未打印合片组，恢复组内蜡块为普通待打印记录。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/slide-print-merge-groups/cancel")
    public SlicingSlidePrintMergeGroupResponse cancelPrintMergeGroups(
        @Valid @RequestBody SlicingSlidePrintMergeGroupCancelRequest request,
        HttpServletRequest httpServletRequest
    ) {
        TechnicalWorkflowModels.SlicingSlidePrintMergeGroupResult result =
            technicalWorkflowAppService.cancelSlicingSlidePrintMergeGroups(
                new TechnicalWorkflowModels.SlicingSlidePrintMergeGroupCancelCommand(
                    request.getPrintGroupIds(),
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    request.getTerminalCode(),
                    request.getRemarks()));
        return new SlicingSlidePrintMergeGroupResponse(result.printGroupIds());
    }

    @Operation(summary = "打印合片组玻片", description = "打印未打印合片组，打印标签的蜡块号按合片组展示。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping("/slide-print-merge-groups/print")
    public SlicingSlidePrintResponse printMergeGroup(
        @Valid @RequestBody SlicingSlidePrintMergeGroupPrintRequest request,
        HttpServletRequest httpServletRequest
    ) {
        TechnicalWorkflowModels.SlicingSlidePrintResult result =
            technicalWorkflowAppService.printSlicingSlideMergeGroup(
                new TechnicalWorkflowModels.SlicingSlidePrintMergeGroupPrintCommand(
                    request.getPrintGroupId(),
                    request.getPrinterCode(),
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    request.getTerminalCode(),
                    request.getRemarks()));
        return new SlicingSlidePrintResponse(
            result.taskId(),
            result.slicingId(),
            result.slideIds(),
            result.slideNos(),
            result.merged(),
            result.printedSlideCount());
    }
}
