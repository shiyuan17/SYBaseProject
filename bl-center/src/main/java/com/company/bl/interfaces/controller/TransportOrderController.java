package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateTransportOrderRequest;
import com.company.bl.interfaces.dto.HandoverTransportOrderRequest;
import com.company.bl.interfaces.dto.TransportOrderOperatorRequest;
import com.company.bl.interfaces.vo.PendingTransportOrderPageResponse;
import com.company.bl.interfaces.vo.PendingTransportOrderResponse;
import com.company.bl.interfaces.vo.TransportOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transport-orders")
@RequiredArgsConstructor
@Tag(name = "临床送检", description = "标本转运单创建、打印与交接接口")
public class TransportOrderController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @Operation(summary = "查询待处理转运单", description = "分页查询当前待处理的转运单工作台列表。")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @GetMapping("/pending")
    public PendingTransportOrderPageResponse listPending(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                         @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
                                                         @Parameter(description = "申请单 ID") @RequestParam(required = false) String applicationId,
                                                         @Parameter(description = "送检科室 ID") @RequestParam(required = false) String departmentId,
                                                         @Parameter(description = "开始日期") @RequestParam(required = false) String dateFrom,
                                                         @Parameter(description = "结束日期") @RequestParam(required = false) String dateTo,
                                                         @Parameter(description = "转运状态") @RequestParam(required = false) String status) {
        SpecimenWorkflowAppService.PendingTransportOrderPage result =
            specimenWorkflowAppService.listPendingTransportOrders(
                new SpecimenWorkflowAppService.PendingTransportOrderQuery(
                    page,
                    size,
                    applicationId,
                    departmentId,
                    dateFrom,
                    dateTo,
                    status));
        return new PendingTransportOrderPageResponse(
            result.items().stream().map(this::toPendingResponse).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "创建转运单", description = "为申请单下指定标本创建转运单。")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "创建成功", useReturnTypeSchema = true))
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @PostMapping
    public ResponseEntity<TransportOrderResponse> create(@Valid @RequestBody CreateTransportOrderRequest request,
                                                         HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(201).body(toResponse(specimenWorkflowAppService.createTransportOrder(
            new SpecimenWorkflowAppService.CreateTransportOrderCommand(
                request.getApplicationId(),
                request.getSpecimenBarcodes(),
                resolveUserId(request.getHandoverUserId(), httpServletRequest),
                request.getHandoverUserName(),
                request.getHandoverDepartmentId(),
                request.getHandoverDepartmentName(),
                request.getReceiverDepartmentId(),
                request.getReceiverDepartmentName(),
                request.getTerminalCode(),
                request.getRemarks()))));
    }

    @Operation(summary = "打印转运单", description = "对指定转运单执行打印操作。")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @PostMapping("/{id}/print")
    public TransportOrderResponse print(@Parameter(description = "转运单 ID") @PathVariable("id") String id,
                                        @Valid @RequestBody TransportOrderOperatorRequest request,
                                        HttpServletRequest httpServletRequest) {
        return toResponse(specimenWorkflowAppService.printTransportOrder(
            id,
            new SpecimenWorkflowAppService.OperatorCommand(
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode())));
    }

    @Operation(summary = "交接转运单", description = "对指定转运单执行交接确认。")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @PostMapping("/{id}/handover")
    public TransportOrderResponse handover(@Parameter(description = "转运单 ID") @PathVariable("id") String id,
                                           @Valid @RequestBody HandoverTransportOrderRequest request,
                                           HttpServletRequest httpServletRequest) {
        return toResponse(specimenWorkflowAppService.handoverTransportOrder(
            id,
            new SpecimenWorkflowAppService.HandoverTransportOrderCommand(
                resolveUserId(request.getReceiverUserId(), httpServletRequest),
                request.getReceiverUserName(),
                request.getTerminalCode(),
                request.getRemarks())));
    }

    private TransportOrderResponse toResponse(TransportOrder order) {
        return new TransportOrderResponse(
            order.id(),
            order.transportOrderNo(),
            order.applicationId(),
            order.status().name(),
            order.handoverUserName(),
            order.receiverUserName(),
            stringify(order.toBeTransportedAt()),
            stringify(order.handedOverAt()));
    }

    private PendingTransportOrderResponse toPendingResponse(
        SpecimenWorkflowAppService.PendingTransportOrderItem item) {
        return new PendingTransportOrderResponse(
            item.id(),
            item.transportOrderNo(),
            item.applicationId(),
            item.applicationNo(),
            item.patientName(),
            item.handoverDepartmentName(),
            item.receiverDepartmentName(),
            item.status(),
            item.specimenBarcodes().size(),
            item.specimenBarcodes().size(),
            "PARTIALLY_RECEIVED".equals(item.status()),
            stringify(item.toBeTransportedAt()),
            stringify(item.handedOverAt()),
            item.specimenBarcodes());
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }
}
