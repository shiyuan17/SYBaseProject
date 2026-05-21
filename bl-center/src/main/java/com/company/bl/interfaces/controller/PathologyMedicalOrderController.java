package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.application.service.DiagnosticReportViews;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateMedicalOrderRequest;
import com.company.bl.interfaces.dto.MedicalOrderActionRequest;
import com.company.bl.interfaces.vo.MedicalOrderOperationResponse;
import com.company.bl.interfaces.vo.PendingMedicalOrderPageResponse;
import com.company.bl.interfaces.vo.PendingMedicalOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/medical-orders")
@Tag(name = "医生流程", description = "病理医嘱闭环接口")
public class PathologyMedicalOrderController extends TechnicalControllerSupport {

    private final DiagnosticReportAppService diagnosticReportAppService;

    public PathologyMedicalOrderController(DiagnosticReportAppService diagnosticReportAppService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
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
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderOperationResponse(result.orderId(), result.caseId(), result.orderNumber(), result.status());
    }

    @Operation(summary = "查询待处理病理医嘱", description = "分页查询技术执行域待处理病理医嘱。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_QUERY)
    @GetMapping("/pending")
    public PendingMedicalOrderPageResponse listPending(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                       @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
                                                       @Parameter(description = "病理号") @RequestParam(required = false) String pathologyNo,
                                                       @Parameter(description = "医嘱状态") @RequestParam(required = false) String status) {
        DiagnosticReportModels.PendingMedicalOrderPage result = diagnosticReportAppService.listPendingMedicalOrders(
            new DiagnosticReportModels.PendingMedicalOrderQuery(page, size, pathologyNo, status));
        return new PendingMedicalOrderPageResponse(
            result.items().stream().map(this::toResponse).toList(),
            result.page(),
            result.size(),
            result.total());
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
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderOperationResponse(result.orderId(), result.caseId(), result.orderNumber(), result.status());
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
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new MedicalOrderOperationResponse(result.orderId(), result.caseId(), result.orderNumber(), result.status());
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
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
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
            item.patientName(),
            item.orderNumber(),
            item.orderType(),
            item.orderContent(),
            item.executionScope(),
            item.billingStatus(),
            item.status(),
            item.doctorName(),
            item.executorName(),
            item.orderDate(),
            item.acceptedAt(),
            item.completedAt(),
            item.cancelledAt(),
            item.remarks());
    }
}
