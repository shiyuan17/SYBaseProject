package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.application.service.DiagnosticReportViews;
import com.company.bl.application.service.MedicalOrderWorkflowService;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.ChangeMedicalOrderBlockRequest;
import com.company.bl.interfaces.dto.CreateMedicalOrderRequest;
import com.company.bl.interfaces.dto.CreateMedicalOrderQcEvaluationRequest;
import com.company.bl.interfaces.dto.MedicalOrderActionRequest;
import com.company.bl.interfaces.dto.MedicalOrderBillingRequest;
import com.company.bl.interfaces.dto.RoutineMedicalOrderMergeRequest;
import com.company.bl.interfaces.dto.RoutineMedicalOrderUnmergeRequest;
import com.company.bl.interfaces.dto.TerminateMedicalOrderRequest;
import com.company.bl.interfaces.vo.MedicalOrderBillingResponse;
import com.company.bl.interfaces.vo.MedicalOrderOperationResponse;
import com.company.bl.interfaces.vo.MedicalOrderQcEvaluationResponse;
import com.company.bl.interfaces.vo.MedicalOrderQcContextResponse;
import com.company.bl.interfaces.vo.MedicalOrderSlidePrintResponse;
import com.company.bl.interfaces.vo.MedicalOrderTargetSnapshotResponse;
import com.company.bl.interfaces.vo.PendingMedicalOrderPageResponse;
import com.company.bl.interfaces.vo.PendingMedicalOrderResponse;
import com.company.bl.interfaces.vo.RoutineMedicalOrderMergeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medical-orders")
@Tag(name = "医生流程", description = "病理医嘱闭环接口")
public class PathologyMedicalOrderController extends TechnicalControllerSupport {

    private final DiagnosticReportAppService diagnosticReportAppService;
    private final MedicalOrderWorkflowService medicalOrderWorkflowService;

    public PathologyMedicalOrderController(DiagnosticReportAppService diagnosticReportAppService,
                                           MedicalOrderWorkflowService medicalOrderWorkflowService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
        this.medicalOrderWorkflowService = medicalOrderWorkflowService;
    }

    @Operation(summary = "创建病理医嘱", description = "由诊断医生创建技术域病理医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_CREATE)
    @PostMapping
    public MedicalOrderOperationResponse create(@Valid @RequestBody CreateMedicalOrderRequest request,
                                                HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderResult result = diagnosticReportAppService.createMedicalOrder(
            new DiagnosticReportModels.CreateMedicalOrderCommand(
                request.getCaseId(),
                request.getOrderType(),
                request.getOrderContent(),
                request.getOrderItemId(),
                request.getTargetType(),
                request.getTargetSpecimenId(),
                request.getTargetSpecimenNo(),
                request.getTargetBlockId(),
                request.getTargetBlockNo(),
                request.getTargetSlideId(),
                request.getTargetSlideNo(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderOperationResponse(result.orderId(), result.caseId(), result.orderNumber(), result.status());
    }

    @Operation(summary = "改绑病理医嘱蜡块", description = "必要时创建医嘱专用蜡块，并把待处理病理医嘱的目标切换到指定蜡块。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_CREATE)
    @PostMapping("/{id}/change-block")
    public MedicalOrderTargetSnapshotResponse changeBlock(@PathVariable("id") String orderId,
                                                          @Valid @RequestBody ChangeMedicalOrderBlockRequest request,
                                                          HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderTargetSnapshotResult result = diagnosticReportAppService.changeMedicalOrderBlock(
            new DiagnosticReportModels.ChangeMedicalOrderBlockCommand(
                orderId,
                request.getBlockNo(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderTargetSnapshotResponse(
            result.orderId(),
            result.caseId(),
            result.orderNumber(),
            result.status(),
            result.targetType(),
            result.targetSpecimenId(),
            result.targetSpecimenNo(),
            result.targetBlockId(),
            result.targetBlockNo(),
            result.targetSlideId(),
            result.targetSlideNo(),
            result.medicalOrderBlockId());
    }

    @Operation(summary = "查询待处理病理医嘱", description = "分页查询技术执行域待处理病理医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_QUERY)
    @GetMapping("/pending")
    public PendingMedicalOrderPageResponse listPending(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                       @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
                                                       @Parameter(description = "病理号") @RequestParam(required = false) String pathologyNo,
                                                       @Parameter(description = "医嘱状态") @RequestParam(required = false) String status,
                                                       @Parameter(description = "医嘱分类码，多个分类用英文逗号分隔") @RequestParam(required = false) String orderCategoryCode,
                                                       @Parameter(description = "开始日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate dateFrom,
                                                       @Parameter(description = "结束日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate dateTo,
                                                       @Parameter(description = "工作日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate workDate) {
        DiagnosticReportModels.PendingMedicalOrderPage result = diagnosticReportAppService.listPendingMedicalOrders(
            new DiagnosticReportModels.PendingMedicalOrderQuery(
                page,
                size,
                pathologyNo,
                status,
                orderCategoryCode,
                dateFrom,
                dateTo,
                workDate));
        return new PendingMedicalOrderPageResponse(
            result.items().stream().map(this::toResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "常规医嘱相同项目合片", description = "代理切片待打印合片能力，按所选常规医嘱创建未打印合片组。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_PRINT)
    @PostMapping("/merge-slides")
    public RoutineMedicalOrderMergeResponse mergeSlides(@Valid @RequestBody RoutineMedicalOrderMergeRequest request,
                                                        HttpServletRequest httpServletRequest) {
        return new RoutineMedicalOrderMergeResponse(
            medicalOrderWorkflowService.mergeRoutineMedicalOrderSlides(
                request.getOrderIds(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
    }

    @Operation(summary = "常规医嘱取消合片", description = "代理切片未打印合片组取消能力，仅允许取消未打印合片组。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_PRINT)
    @PostMapping("/unmerge-slides")
    public RoutineMedicalOrderMergeResponse unmergeSlides(@Valid @RequestBody RoutineMedicalOrderUnmergeRequest request,
                                                          HttpServletRequest httpServletRequest) {
        return new RoutineMedicalOrderMergeResponse(
            medicalOrderWorkflowService.unmergeRoutineMedicalOrderSlides(
                request.getPrintGroupIds(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
    }

    @Operation(summary = "导出待处理病理医嘱", description = "按当前待处理病理医嘱查询条件导出 UTF-8 CSV。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_QUERY)
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPendingMedicalOrders(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                             @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
                                                             @Parameter(description = "病理号") @RequestParam(required = false) String pathologyNo,
                                                             @Parameter(description = "医嘱状态") @RequestParam(required = false) String status,
                                                             @Parameter(description = "医嘱分类码，多个分类用英文逗号分隔") @RequestParam(required = false) String orderCategoryCode,
                                                             @Parameter(description = "开始日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate dateFrom,
                                                             @Parameter(description = "结束日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate dateTo,
                                                             @Parameter(description = "工作日期，格式 YYYY-MM-DD") @RequestParam(required = false) LocalDate workDate) {
        return medicalOrderWorkflowService.exportPendingMedicalOrders(
            new DiagnosticReportModels.PendingMedicalOrderQuery(
                page,
                size,
                pathologyNo,
                status,
                orderCategoryCode,
                dateFrom,
                dateTo,
                workDate));
    }

    @Operation(summary = "执行医嘱收费", description = "为诊断工作站医嘱触发真实收费；未指定医嘱时处理当前病例全部未收费医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_CREATE)
    @PostMapping("/billing/execute")
    public MedicalOrderBillingResponse executeBilling(@Valid @RequestBody MedicalOrderBillingRequest request,
                                                      HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderBillingResult result = diagnosticReportAppService.executeMedicalOrderBilling(
            new DiagnosticReportModels.MedicalOrderBillingCommand(
                request.getCaseId(),
                request.getOrderIds(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return toBillingResponse(result);
    }

    @Operation(summary = "确认医嘱收费完成", description = "为诊断工作站医嘱登记收费完成回执；未指定医嘱时处理当前病例全部未收费医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_CREATE)
    @PostMapping("/billing/confirm")
    public MedicalOrderBillingResponse confirmBilling(@Valid @RequestBody MedicalOrderBillingRequest request,
                                                      HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderBillingResult result = diagnosticReportAppService.confirmMedicalOrderBilling(
            new DiagnosticReportModels.MedicalOrderBillingCommand(
                request.getCaseId(),
                request.getOrderIds(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return toBillingResponse(result);
    }

    @Operation(summary = "接收病理医嘱", description = "由技术执行角色接收待处理病理医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_ACCEPT)
    @PostMapping("/{id}/accept")
    public MedicalOrderOperationResponse accept(@PathVariable("id") String orderId,
                                                @Valid @RequestBody MedicalOrderActionRequest request,
                                                HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderResult result = diagnosticReportAppService.acceptMedicalOrder(
            new DiagnosticReportModels.MedicalOrderActionCommand(
                orderId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderOperationResponse(result.orderId(), result.caseId(), result.orderNumber(), result.status());
    }

    @Operation(summary = "打印玻片标签", description = "由技术执行角色打印病理医嘱关联玻片标签。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_PRINT)
    @PostMapping("/{id}/print-slide")
    public MedicalOrderSlidePrintResponse printSlide(@PathVariable("id") String orderId,
                                                     @Valid @RequestBody MedicalOrderActionRequest request,
                                                     HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderSlidePrintResult result = diagnosticReportAppService.printMedicalOrderSlide(
            new DiagnosticReportModels.MedicalOrderActionCommand(
                orderId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderSlidePrintResponse(
            result.orderId(),
            result.caseId(),
            result.orderNumber(),
            result.status(),
            result.printedAt(),
            result.printedByName(),
            result.labels().stream()
                .map(label -> new MedicalOrderSlidePrintResponse.MedicalOrderSlidePrintLabelResponse(
                    label.slideId(),
                    label.slideNo(),
                    label.pathologyNo(),
                    label.patientName(),
                    label.patientId(),
                    label.specimenNo(),
                    label.blockNo()))
                .toList());
    }

    @Operation(summary = "完成病理医嘱", description = "由技术执行角色完成进行中的病理医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_COMPLETE)
    @PostMapping("/{id}/complete")
    public MedicalOrderOperationResponse complete(@PathVariable("id") String orderId,
                                                  @Valid @RequestBody MedicalOrderActionRequest request,
                                                  HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderResult result = diagnosticReportAppService.completeMedicalOrder(
            new DiagnosticReportModels.MedicalOrderActionCommand(
                orderId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderOperationResponse(result.orderId(), result.caseId(), result.orderNumber(), result.status());
    }

    @Operation(summary = "终止病理医嘱", description = "由技术执行角色终止进行中的病理医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_TERMINATE)
    @PostMapping("/{id}/terminate")
    public MedicalOrderOperationResponse terminate(@PathVariable("id") String orderId,
                                                   @Valid @RequestBody TerminateMedicalOrderRequest request,
                                                   HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderResult result = diagnosticReportAppService.terminateMedicalOrder(
            new DiagnosticReportModels.TerminateMedicalOrderCommand(
                orderId,
                request.getTerminationReasonCode(),
                request.getTerminationReasonLabel(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderOperationResponse(result.orderId(), result.caseId(), result.orderNumber(), result.status());
    }

    @Operation(summary = "创建医嘱质控评价", description = "记录病理医嘱质控评价并按需生成返工单。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_QC)
    @PostMapping("/{id}/qc-evaluations")
    public MedicalOrderQcEvaluationResponse createQcEvaluation(@PathVariable("id") String orderId,
                                                               @Valid @RequestBody CreateMedicalOrderQcEvaluationRequest request,
                                                               HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderQcEvaluationResult result = diagnosticReportAppService.createMedicalOrderQcEvaluation(
            new DiagnosticReportModels.MedicalOrderQcEvaluationCommand(
                orderId,
                request.getSlideId(),
                request.getExpectedVersion(),
                request.getQcAspect(),
                request.getTotalScore(),
                request.getGrade(),
                request.getEvaluationReason(),
                request.getProcessingAction(),
                request.getDetailPayload(),
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return toQcEvaluationResponse(result);
    }

    @Operation(summary = "查询最新医嘱质控评价", description = "返回病理医嘱最新一次质控评价；医嘱存在但尚无历史评价时，成功响应的 data 为 null。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_QC)
    @GetMapping("/{id}/qc-evaluations/latest")
    @Schema(nullable = true)
    public MedicalOrderQcEvaluationResponse getLatestQcEvaluation(@PathVariable("id") String orderId,
                                                                  @RequestParam(required = false) String qcAspect,
                                                                  @RequestParam(required = false) String slideId) {
        DiagnosticReportModels.MedicalOrderQcEvaluationResult result =
            diagnosticReportAppService.getLatestMedicalOrderQcEvaluation(orderId, qcAspect, slideId);
        return result == null ? null : toQcEvaluationResponse(result);
    }

    @Operation(summary = "查询医嘱质控上下文", description = "返回医嘱目标范围内的全部切片及每片当前评价。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_QC)
    @GetMapping("/{id}/qc-evaluations/context")
    public MedicalOrderQcContextResponse getQcContext(@PathVariable("id") String orderId) {
        DiagnosticReportModels.MedicalOrderQcContextResult context =
            diagnosticReportAppService.getMedicalOrderQcContext(orderId);
        return new MedicalOrderQcContextResponse(
            context.orderId(), context.caseId(), context.targetType(), context.targetResolved(), context.unlinkedReason(),
            context.slides().stream().map(slide -> new MedicalOrderQcContextResponse.SlideItem(
                slide.slideId(), slide.slideNo(), slide.specimenId(), slide.specimenNo(), slide.blockId(), slide.blockNo(),
                slide.projectName(), slide.slideStatus(), slide.qualityStatus(),
                slide.evaluations().stream().map(this::toQcEvaluationResponse).toList())).toList());
    }

    @Operation(summary = "取消病理医嘱", description = "由诊断医生取消待处理病理医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_CANCEL)
    @PostMapping("/{id}/cancel")
    public MedicalOrderOperationResponse cancel(@PathVariable("id") String orderId,
                                                @Valid @RequestBody MedicalOrderActionRequest request,
                                                HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.MedicalOrderResult result = diagnosticReportAppService.cancelMedicalOrder(
            new DiagnosticReportModels.MedicalOrderActionCommand(
                orderId,
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderOperationResponse(result.orderId(), result.caseId(), result.orderNumber(), result.status());
    }

    private PendingMedicalOrderResponse toResponse(DiagnosticReportViews.MedicalOrderView item) {
        return new PendingMedicalOrderResponse(
            item.orderId(),
            item.caseId(),
            item.pathologyNo(),
            item.applicationNo(),
            item.inpatientNo(),
            item.slicingTaskId(),
            item.slicingPrintGroupId(),
            item.slicingMergedPrintGroup(),
            item.slicingTaskIds(),
            item.patientName(),
            item.patientId(),
            item.patientIdDisplay(),
            item.submittingDepartmentName(),
            item.orderNumber(),
            item.orderType(),
            item.orderContent(),
            item.orderItemId(),
            item.orderItemCode(),
            item.orderItemName(),
            item.orderCategoryId(),
            item.orderCategoryCode(),
            item.orderCategoryName(),
            item.executionScope(),
            item.billingStatus(),
            item.status(),
            item.doctorName(),
            item.executorName(),
            item.orderDate(),
            item.acceptedAt(),
            item.printedAt(),
            item.printedByName(),
            item.releasedAt(),
            item.releasedByName(),
            item.completedAt(),
            item.cancelledAt(),
            item.terminatedAt(),
            item.terminatedByName(),
            item.terminationReasonCode(),
            item.terminationReasonLabel(),
            item.terminationRemarks(),
            item.remarks(),
            item.targetType(),
            item.targetSpecimenId(),
            item.targetSpecimenNo(),
            item.targetBlockId(),
            item.targetBlockNo(),
            item.targetSlideId(),
            item.targetSlideNo(),
            item.specimenNo(),
            item.blockNo(),
            item.slideNo(),
            item.canConfirm(),
            item.canPrint(),
            item.canRelease(),
            item.canTerminate(),
            item.canQc());
    }

    private MedicalOrderBillingResponse toBillingResponse(DiagnosticReportModels.MedicalOrderBillingResult result) {
        return new MedicalOrderBillingResponse(
            result.totalCount(),
            result.successCount(),
            result.failureCount(),
            result.items().stream()
                .map(item -> new MedicalOrderBillingResponse.MedicalOrderBillingItemResponse(
                    item.orderId(),
                    item.billingStatus(),
                    item.billingRecordId(),
                    item.message()))
                .toList());
    }

    private MedicalOrderQcEvaluationResponse toQcEvaluationResponse(DiagnosticReportModels.MedicalOrderQcEvaluationResult result) {
        return new MedicalOrderQcEvaluationResponse(
            result.qcEvaluationId(),
            result.orderId(),
            result.caseId(),
            result.slideId(),
            result.slideNo(),
            result.version(),
            result.qcAspect(),
            result.totalScore(),
            result.grade(),
            result.evaluationReason(),
            result.processingAction(),
            result.reworkType(),
            result.reworkOrderId(),
            result.remarks(),
            result.evaluatorName(),
            result.evaluatedAt(),
            result.detailPayload());
    }
}
