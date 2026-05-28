package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.interfaces.auth.ApiPermissionContext;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SpecimenRemovalConfirmRequest;
import com.company.bl.interfaces.vo.SpecimenRemovalConfirmResponse;
import com.company.bl.interfaces.vo.SpecimenRemovalItemResponse;
import com.company.bl.interfaces.vo.SpecimenRemovalPageResponse;
import com.company.bl.interfaces.vo.SpecimenRemovalSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
@RestController
@RequestMapping("/api/v1/specimen-removals")
@RequiredArgsConstructor
@Tag(name = "Specimen Removal Workbench", description = "标本离体时间设置工作台")
public class SpecimenRemovalController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;

    @Operation(summary = "List pending removals", description = "Query specimens pending removal time settings with paging.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @GetMapping("/pending")
    public SpecimenRemovalPageResponse listPending(
        @Parameter(description = "Page number, starting from 1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "Page size, default 20")
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String applicationNo,
        @RequestParam(required = false) String departmentId,
        @RequestParam(required = false) String specimenStatus,
        @RequestParam(required = false) Boolean abnormalFlag,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo
    ) {
        SpecimenWorkflowAppService.SpecimenRemovalListPage result =
            specimenWorkflowAppService.listSpecimenRemovalItems(
                new SpecimenWorkflowAppService.SpecimenRemovalQuery(
                    page,
                    size,
                    keyword,
                    applicationNo,
                    departmentId,
                    specimenStatus,
                    abnormalFlag,
                    dateFrom,
                    dateTo));
        return new SpecimenRemovalPageResponse(
            result.items().stream().map(this::toRow).toList(),
            result.page(),
            result.size(),
            result.total(),
            new SpecimenRemovalSummaryResponse(
                result.summary().totalCount(),
                result.summary().confirmedCount(),
                result.summary().pendingCount(),
                result.summary().abnormalCount()));
    }

    @Operation(summary = "Confirm specimen removal", description = "Persist removal time and operator for a specimen.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/confirm")
    public SpecimenRemovalConfirmResponse confirm(
        @Valid @RequestBody SpecimenRemovalConfirmRequest request,
        HttpServletRequest httpServletRequest
    ) {
        SpecimenWorkflowAppService.SpecimenRemovalResult result = specimenWorkflowAppService.confirmSpecimenRemoval(
            new SpecimenWorkflowAppService.SpecimenRemovalCommand(
                request.getSpecimenBarcode(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                resolveOperatorName(request.getOperatorName(), httpServletRequest),
                request.getTerminalCode(),
                request.getRemarks()));
        return new SpecimenRemovalConfirmResponse(
            result.specimenId(),
            result.barcode(),
            stringify(result.specimenRemovalAt()),
            result.operatorName());
    }

    @Operation(summary = "Export removals", description = "Export current filtered removal workbench as xlsx.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10000") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String applicationNo,
        @RequestParam(required = false) String departmentId,
        @RequestParam(required = false) String specimenStatus,
        @RequestParam(required = false) Boolean abnormalFlag,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo
    ) {
        byte[] content = specimenWorkflowAppService.exportSpecimenRemovalItems(
            new SpecimenWorkflowAppService.SpecimenRemovalQuery(
                page,
                size,
                keyword,
                applicationNo,
                departmentId,
                specimenStatus,
                abnormalFlag,
                dateFrom,
                dateTo));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"specimen-removals.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(content);
    }

    private SpecimenRemovalItemResponse toRow(SpecimenWorkflowAppService.SpecimenRemovalListItem item) {
        return new SpecimenRemovalItemResponse(
            item.specimenId(),
            item.barcode(),
            item.applicationNo(),
            item.specimenNo(),
            item.patientName(),
            item.patientGender(),
            item.inpatientNo(),
            item.surgeryName(),
            item.specimenName(),
            item.specimenStatus(),
            item.specimenType(),
            stringify(item.specimenRemovalAt()),
            item.specimenRemovalOperatorName(),
            stringify(item.registeredAt()),
            item.labelPrintBatchNo(),
            item.registeredByName(),
            item.latestTrackingAt() == null ? null : stringify(item.latestTrackingAt()),
            item.abnormalFlag());
    }

    private String stringify(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String resolveUserId(String bodyUserId, HttpServletRequest request) {
        Object currentUserId = request.getAttribute(ApiPermissionContext.CURRENT_USER_ID);
        return currentUserId == null ? null : currentUserId.toString();
    }

    private String resolveOperatorName(String bodyOperatorName, HttpServletRequest request) {
        Object currentLoginName = request.getAttribute(ApiPermissionContext.CURRENT_LOGIN_NAME);
        if (currentLoginName instanceof String loginName && !loginName.isBlank()) {
            return loginName.trim();
        }
        return bodyOperatorName == null ? null : bodyOperatorName.trim();
    }
}
