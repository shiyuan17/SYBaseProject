package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateTransportOrderRequest;
import com.company.bl.interfaces.dto.HandoverTransportOrderRequest;
import com.company.bl.interfaces.dto.TransportOrderOperatorRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transport-orders")
@RequiredArgsConstructor
@Tag(name = "临床送检", description = "标本转运单创建、打印与交接接口")
public class TransportOrderController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

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

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        if (bodyUserId != null && !bodyUserId.isBlank()) {
            return bodyUserId.trim();
        }
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }
}
