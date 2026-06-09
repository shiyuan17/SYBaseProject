package com.company.bl.interfaces.controller;

import com.company.bl.application.service.OperatorVerificationService;
import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.application.service.SpecimenWorkflowTransportModels;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.QuickOutboundTransportOrderRequest;
import com.company.bl.interfaces.vo.SpecimenOutboundItemResponse;
import com.company.bl.interfaces.vo.SpecimenOutboundPageResponse;
import com.company.bl.interfaces.vo.TransportOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/specimen-outbounds")
@RequiredArgsConstructor
@Tag(name = "Specimen Outbound Workbench", description = "标本出库工作台查询接口")
public class SpecimenOutboundController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;
    private final OperatorVerificationService operatorVerificationService;

    @Operation(summary = "List specimen outbounds", description = "Query mixed pending/outbounded specimen outbound records with paging.")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @GetMapping
    public SpecimenOutboundPageResponse list(
        @Parameter(description = "Page number, starting from 1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "Page size, default 20")
        @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "Application ID")
        @RequestParam(required = false) String applicationId,
        @Parameter(description = "Specimen serial number")
        @RequestParam(required = false) String specimenNo
    ) {
        SpecimenWorkflowTransportModels.SpecimenOutboundPage result =
            specimenWorkflowAppService.listSpecimenOutbounds(
                new SpecimenWorkflowTransportModels.SpecimenOutboundListQuery(
                    page,
                    size,
                    applicationId,
                    specimenNo));
        return new SpecimenOutboundPageResponse(
            result.items().stream().map(this::toItem).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "Quick outbound specimen", description = "Auto-create transport order when needed, then complete outbound by specimen identifier.")
    @RequirePermission(M2PermissionCodes.TRANSPORT_HANDOVER)
    @PostMapping("/quick-outbound")
    public TransportOrderResponse quickOutbound(
        @Valid @RequestBody QuickOutboundTransportOrderRequest request,
        HttpServletRequest httpServletRequest
    ) {
        OperatorVerificationService.VerifiedOperator operator = resolveVerifiedOrCurrentOperator(
            request.getOperatorVerificationToken(),
            request.getOutboundUserId(),
            request.getOutboundUserName(),
            httpServletRequest);
        return toResponse(specimenWorkflowAppService.quickOutboundTransportOrder(
            new SpecimenWorkflowTransportModels.QuickOutboundTransportOrderCommand(
                request.getIdentifierType(),
                request.getIdentifier(),
                operator.operatorUserId(),
                operator.operatorName(),
                request.getTerminalCode(),
                request.getRemarks())));
    }

    private SpecimenOutboundItemResponse toItem(SpecimenWorkflowTransportModels.SpecimenOutboundItem item) {
        return new SpecimenOutboundItemResponse(
            item.specimenId(),
            item.transportOrderId(),
            item.applicationId(),
            item.applicationNo(),
            item.barcode(),
            item.specimenNo(),
            item.patientName(),
            item.patientGender(),
            item.patientId(),
            item.inpatientNo(),
            item.surgeryName(),
            item.specimenName(),
            item.specimenStatus(),
            item.fixationStatus(),
            item.checkInStatus(),
            stringify(item.specimenConfirmedAt()),
            item.submittingDepartmentId(),
            item.submittingDepartmentName(),
            stringify(item.registeredAt()),
            item.registeredByName(),
            stringify(item.outboundAt()),
            item.outboundUserName());
    }

    private TransportOrderResponse toResponse(com.company.bl.domain.model.TransportOrder order) {
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

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        if (bodyUserId != null && !bodyUserId.isBlank()) {
            return bodyUserId;
        }
        return RequestOperatorContext.currentUserId(request);
    }

    private String resolveOperatorName(String bodyOperatorName, HttpServletRequest request) {
        if (bodyOperatorName != null && !bodyOperatorName.isBlank()) {
            return bodyOperatorName;
        }
        return RequestOperatorContext.currentOperatorName(request);
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
            String currentUserId = resolveUserId(null, request);
            String operatorName = currentUserId != null
                && bodyUserId != null
                && currentUserId.equals(bodyUserId.trim())
                ? resolveOperatorName(bodyOperatorName, request)
                : resolveOperatorName(null, request);
            return new OperatorVerificationService.VerifiedOperator(
                currentUserId,
                null,
                operatorName);
        }
        return resolveVerifiedOperator(operatorVerificationToken, request);
    }
}
