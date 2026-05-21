package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.DirectReceiveSpecimensRequest;
import com.company.bl.interfaces.dto.ReceiveSpecimensRequest;
import com.company.bl.interfaces.vo.PendingSpecimenItemResponse;
import com.company.bl.interfaces.vo.PendingSpecimenPageResponse;
import com.company.bl.interfaces.vo.SpecimenReceiptResponse;
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
@RequestMapping("/api/v1/specimen-receipts")
@RequiredArgsConstructor
@Tag(name = "临床送检", description = "标本接收与待接收查询接口")
public class SpecimenReceiptController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @Operation(summary = "按转运单接收标本", description = "基于转运单和接收明细批量接收标本。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PostMapping
    public SpecimenReceiptResponse receive(@Valid @RequestBody ReceiveSpecimensRequest request,
                                          HttpServletRequest httpServletRequest) {
        SpecimenWorkflowAppService.ReceiptResult result = specimenWorkflowAppService.receiveSpecimens(
            new SpecimenWorkflowAppService.ReceiveSpecimensCommand(
                request.getTransportOrderId(),
                resolveUserId(request.getReceivedByUserId(), httpServletRequest),
                request.getReceivedByName(),
                request.getTerminalCode(),
                request.getItems().stream().map(item -> new SpecimenWorkflowAppService.ReceiptItem(
                    item.getSpecimenBarcode(),
                    ReceiptStatus.from(item.getReceiptStatus()),
                    item.getContainerCount(),
                    item.getReason(),
                    item.getRemarks()))
                    .toList()));
        return new SpecimenReceiptResponse(result.caseId(), result.pathologyNo(), result.receiptStatus(), result.unreceivedCount());
    }

    @Operation(summary = "按条码直接接收标本", description = "不依赖转运单，直接根据标本条码完成接收。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PostMapping("/by-barcodes")
    public SpecimenReceiptResponse receiveByBarcodes(@Valid @RequestBody DirectReceiveSpecimensRequest request,
                                                     HttpServletRequest httpServletRequest) {
        SpecimenWorkflowAppService.ReceiptResult result = specimenWorkflowAppService.receiveSpecimensByBarcodes(
            new SpecimenWorkflowAppService.DirectReceiveSpecimensCommand(
                resolveUserId(request.getReceivedByUserId(), httpServletRequest),
                request.getReceivedByName(),
                request.getTerminalCode(),
                request.getItems().stream().map(item -> new SpecimenWorkflowAppService.ReceiptItem(
                    item.getSpecimenBarcode(),
                    ReceiptStatus.from(item.getReceiptStatus()),
                    item.getContainerCount(),
                    item.getReason(),
                    item.getRemarks()))
                    .toList()));
        return new SpecimenReceiptResponse(result.caseId(), result.pathologyNo(), result.receiptStatus(), result.unreceivedCount());
    }

    @Operation(summary = "查询待接收标本", description = "分页查询当前待接收的标本列表。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @GetMapping("/pending")
    public PendingSpecimenPageResponse listPending(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                                   @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
                                                   @Parameter(description = "申请单 ID") @RequestParam(required = false) String applicationId,
                                                   @Parameter(description = "送检科室 ID") @RequestParam(required = false) String departmentId,
                                                   @Parameter(description = "开始日期") @RequestParam(required = false) String dateFrom,
                                                   @Parameter(description = "结束日期") @RequestParam(required = false) String dateTo) {
        SpecimenWorkflowAppService.PendingSpecimenPage result = specimenWorkflowAppService.listPendingReceipts(
            new SpecimenWorkflowAppService.PendingSpecimenQuery(page, size, applicationId, departmentId, dateFrom, dateTo));
        return new PendingSpecimenPageResponse(
            result.items().stream().map(this::toPendingItem).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    private PendingSpecimenItemResponse toPendingItem(SpecimenWorkflowAppService.PendingSpecimenItem item) {
        return new PendingSpecimenItemResponse(
            item.applicationId(),
            item.applicationNo(),
            item.patientName(),
            item.submittingDepartmentId(),
            item.submittingDepartmentName(),
            item.transportOrderId(),
            item.specimenId(),
            item.specimenNo(),
            item.barcode(),
            item.specimenStatus(),
            item.fixationStatus(),
            stringify(item.registeredAt()),
            stringify(item.latestTrackingAt()),
            item.abnormalFlag());
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }
}
