package com.company.bl.interfaces.controller;

import com.company.bl.application.service.OperatorVerificationService;
import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.application.service.SpecimenWorkflowTransportModels;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateTransportOrderRequest;
import com.company.bl.interfaces.dto.HandoverTransportOrderRequest;
import com.company.bl.interfaces.dto.OutboundTransportOrderRequest;
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
@Tag(name = "Clinical Submission", description = "Transport order workflow APIs")
public class TransportOrderController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;
    private final OperatorVerificationService operatorVerificationService;

    @Operation(summary = "List pending transport orders", description = "Query pending transport orders with paging.")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @GetMapping("/pending")
    public PendingTransportOrderPageResponse listPending(
        @Parameter(description = "Page number, starting from 1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "Page size, default 20")
        @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "Application ID")
        @RequestParam(required = false) String applicationId,
        @Parameter(description = "Specimen serial number")
        @RequestParam(required = false) String specimenNo,
        @Parameter(description = "Submitting department ID")
        @RequestParam(required = false) String departmentId,
        @Parameter(description = "Start date")
        @RequestParam(required = false) String dateFrom,
        @Parameter(description = "End date")
        @RequestParam(required = false) String dateTo,
        @Parameter(description = "Transport order status")
        @RequestParam(required = false) String status
    ) {
        SpecimenWorkflowTransportModels.PendingTransportOrderPage result =
            specimenWorkflowAppService.listPendingTransportOrders(
                new SpecimenWorkflowTransportModels.PendingTransportOrderQuery(
                    page,
                    size,
                    applicationId,
                    specimenNo,
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

    @Operation(summary = "Create transport order", description = "Create transport order for selected specimen barcodes.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Created successfully", useReturnTypeSchema = true))
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @PostMapping
    public ResponseEntity<TransportOrderResponse> create(@Valid @RequestBody CreateTransportOrderRequest request,
                                                         HttpServletRequest httpServletRequest) {
        OperatorVerificationService.VerifiedOperator operator = resolveVerifiedOrCurrentOperator(
            request.getOperatorVerificationToken(),
            request.getHandoverUserId(),
            request.getHandoverUserName(),
            httpServletRequest);
        return ResponseEntity.status(201).body(toResponse(specimenWorkflowAppService.createTransportOrder(
            new SpecimenWorkflowTransportModels.CreateTransportOrderCommand(
                request.getApplicationId(),
                request.getSpecimenBarcodes(),
                operator.operatorUserId(),
                operator.operatorName(),
                request.getHandoverDepartmentId(),
                request.getHandoverDepartmentName(),
                request.getReceiverDepartmentId(),
                request.getReceiverDepartmentName(),
                request.getTerminalCode(),
                request.getRemarks()))));
    }

    @Operation(summary = "Print transport order", description = "Print the specified transport order.")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @PostMapping("/{id}/print")
    public TransportOrderResponse print(@Parameter(description = "Transport order ID") @PathVariable("id") String id,
                                        @Valid @RequestBody TransportOrderOperatorRequest request,
                                        HttpServletRequest httpServletRequest) {
        return toResponse(specimenWorkflowAppService.printTransportOrder(
            id,
            new SpecimenWorkflowTransportModels.OperatorCommand(
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getTerminalCode())));
    }

    @Operation(summary = "Handover transport order", description = "Confirm handover for the specified transport order.")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @PostMapping("/{id}/handover")
    public TransportOrderResponse handover(@Parameter(description = "Transport order ID") @PathVariable("id") String id,
                                           @Valid @RequestBody HandoverTransportOrderRequest request,
                                           HttpServletRequest httpServletRequest) {
        return toResponse(specimenWorkflowAppService.handoverTransportOrder(
            id,
            new SpecimenWorkflowTransportModels.HandoverTransportOrderCommand(
                resolveUserId(httpServletRequest),
                resolveOperatorName(httpServletRequest),
                request.getReceiverUserId(),
                request.getReceiverUserName(),
                request.getTerminalCode(),
                request.getRemarks())));
    }

    @Operation(summary = "Outbound transport order", description = "Confirm specimen outbound for the specified transport order.")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @PostMapping("/{id}/outbound")
    public TransportOrderResponse outbound(@Parameter(description = "Transport order ID") @PathVariable("id") String id,
                                           @Valid @RequestBody OutboundTransportOrderRequest request,
                                           HttpServletRequest httpServletRequest) {
        OperatorVerificationService.VerifiedOperator operator = resolveVerifiedOrCurrentOperator(
            request.getOperatorVerificationToken(),
            request.getOutboundUserId(),
            request.getOutboundUserName(),
            httpServletRequest);
        return toResponse(specimenWorkflowAppService.outboundTransportOrder(
            id,
            new SpecimenWorkflowTransportModels.OutboundTransportOrderCommand(
                operator.operatorUserId(),
                operator.operatorName(),
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
            order.outboundUserId(),
            order.outboundUserName(),
            stringify(order.toBeTransportedAt()),
            stringify(order.handedOverAt()));
    }

    private PendingTransportOrderResponse toPendingResponse(
        SpecimenWorkflowTransportModels.PendingTransportOrderItem item) {
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
            item.outboundUserId(),
            item.outboundUserName(),
            item.specimenBarcodes());
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(HttpServletRequest request) {
        return RequestOperatorContext.currentUserId(request);
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        if (bodyUserId != null && !bodyUserId.isBlank()) {
            return bodyUserId;
        }
        return resolveUserId(request);
    }

    private String resolveOperatorName(HttpServletRequest request) {
        return RequestOperatorContext.currentOperatorName(request);
    }

    private String resolveOperatorName(String bodyOperatorName, HttpServletRequest request) {
        if (bodyOperatorName != null && !bodyOperatorName.isBlank()) {
            return bodyOperatorName;
        }
        return resolveOperatorName(request);
    }

    private OperatorVerificationService.VerifiedOperator resolveVerifiedOperator(
        String operatorVerificationToken,
        HttpServletRequest request
    ) {
        return operatorVerificationService.resolveVerifiedOperator(
            operatorVerificationToken,
            RequestOperatorContext.currentUserId(request));
    }

    private OperatorVerificationService.VerifiedOperator resolveVerifiedOrCurrentOperator(
        String operatorVerificationToken,
        String bodyUserId,
        String bodyOperatorName,
        HttpServletRequest request
    ) {
        if (operatorVerificationToken == null || operatorVerificationToken.isBlank()) {
            String currentUserId = resolveUserId(request);
            String operatorName = currentUserId != null
                && bodyUserId != null
                && currentUserId.equals(bodyUserId.trim())
                ? resolveOperatorName(bodyOperatorName, request)
                : resolveOperatorName(request);
            return new OperatorVerificationService.VerifiedOperator(
                currentUserId,
                null,
                operatorName);
        }
        return resolveVerifiedOperator(operatorVerificationToken, request);
    }
}
