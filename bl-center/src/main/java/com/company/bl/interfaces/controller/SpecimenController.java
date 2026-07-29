package com.company.bl.interfaces.controller;

import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.RegisterSpecimensRequest;
import com.company.bl.interfaces.dto.RetryLabelPrintRequest;
import com.company.bl.interfaces.dto.SpecimenBarcodeBindingRequest;
import com.company.bl.interfaces.dto.SpecimenCheckInRequest;
import com.company.bl.interfaces.dto.SpecimenConfirmRequest;
import com.company.bl.interfaces.vo.ApplicationDetailResponse;
import com.company.bl.interfaces.vo.ApplicationListItemResponse;
import com.company.bl.interfaces.vo.LabelPrintRetryResponse;
import com.company.bl.interfaces.vo.LatestSpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.SpecimenManagementPageResponse;
import com.company.bl.interfaces.vo.SpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import com.company.bl.interfaces.vo.SpecimenVerificationRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/specimens")
@RequiredArgsConstructor
@Tag(name = "Clinical Specimen Workflow", description = "Specimen registration, management, label retry and tracking APIs")
public class SpecimenController {

    private final SpecimenWorkflowAppService specimenWorkflowAppService;
    private final SpecimenControllerAssembler specimenControllerAssembler;

    @Operation(summary = "Register specimens", description = "Register specimens under an application and attempt label printing.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Registration succeeded", useReturnTypeSchema = true))
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/register")
    public ResponseEntity<SpecimenRegistrationResponse> register(@Valid @RequestBody RegisterSpecimensRequest request,
                                                                HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(201).body(specimenControllerAssembler.toSpecimenRegistrationResponse(
            specimenWorkflowAppService.registerSpecimens(
                specimenControllerAssembler.toRegisterSpecimensCommand(request, httpServletRequest))));
    }

    @Operation(summary = "Retry label printing", description = "Retry printing for a label batch.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/label-batches/{batchNo}/retry")
    public LabelPrintRetryResponse retryLabelPrint(
        @Parameter(description = "Label print batch number") @PathVariable("batchNo") String batchNo,
        @Valid @RequestBody RetryLabelPrintRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return specimenControllerAssembler.toLabelPrintRetryResponse(
            specimenWorkflowAppService.retryLabelPrint(
                specimenControllerAssembler.toRetryLabelPrintCommand(batchNo, request, httpServletRequest)));
    }

    @Operation(summary = "List specimen management items", description = "Query the specimen management workbench with filters and summary statistics.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping
    public SpecimenManagementPageResponse listSpecimens(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "applicationNo", required = false) String applicationNo,
        @RequestParam(value = "departmentId", required = false) String departmentId,
        @RequestParam(value = "buildingId", required = false) String buildingId,
        @RequestParam(value = "roomId", required = false) String roomId,
        @RequestParam(value = "barcodeBindingStatus", required = false) String barcodeBindingStatus,
        @RequestParam(value = "specimenStatus", required = false) String specimenStatus,
        @RequestParam(value = "labelPrintStatus", required = false) String labelPrintStatus,
        @RequestParam(value = "abnormalFlag", required = false) Boolean abnormalFlag,
        @RequestParam(value = "dateFrom", required = false) String dateFrom,
        @RequestParam(value = "dateTo", required = false) String dateTo
    ) {
        return specimenControllerAssembler.toSpecimenManagementPageResponse(
            specimenWorkflowAppService.listSpecimenManagementItems(
                specimenControllerAssembler.toSpecimenManagementListQuery(
                    page,
                    size,
                    keyword,
                    applicationNo,
                    departmentId,
                    buildingId,
                    roomId,
                    barcodeBindingStatus,
                    specimenStatus,
                    labelPrintStatus,
                    abnormalFlag,
                    dateFrom,
                    dateTo)));
    }

    @Operation(summary = "Export specimen management items", description = "Export current filtered specimen tracking rows as xlsx.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSpecimens(
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "10000") int size,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "applicationNo", required = false) String applicationNo,
        @RequestParam(value = "departmentId", required = false) String departmentId,
        @RequestParam(value = "buildingId", required = false) String buildingId,
        @RequestParam(value = "roomId", required = false) String roomId,
        @RequestParam(value = "barcodeBindingStatus", required = false) String barcodeBindingStatus,
        @RequestParam(value = "specimenStatus", required = false) String specimenStatus,
        @RequestParam(value = "labelPrintStatus", required = false) String labelPrintStatus,
        @RequestParam(value = "abnormalFlag", required = false) Boolean abnormalFlag,
        @RequestParam(value = "dateFrom", required = false) String dateFrom,
        @RequestParam(value = "dateTo", required = false) String dateTo
    ) {
        byte[] content = specimenWorkflowAppService.exportSpecimenManagementItems(
            specimenControllerAssembler.toSpecimenManagementListQuery(
                page,
                size,
                keyword,
                applicationNo,
                departmentId,
                buildingId,
                roomId,
                barcodeBindingStatus,
                specimenStatus,
                labelPrintStatus,
                abnormalFlag,
                dateFrom,
                dateTo));
        String fileName = "标本综合信息_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString())
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(content);
    }

    @Operation(summary = "Lookup application for registration", description = "Resolve registration context by application number.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping("/applications/lookup")
    public ApplicationListItemResponse lookupRegistrationApplication(
        @Parameter(description = "Application number") @RequestParam("applicationNo") String applicationNo
    ) {
        return specimenControllerAssembler.toApplicationListItemResponse(
            specimenWorkflowAppService.getRegistrationApplicationByApplicationNo(applicationNo));
    }

    @Operation(summary = "Get latest registration result", description = "Query the latest specimen registration result by application id.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @GetMapping("/applications/{applicationId}/latest-registration")
    public LatestSpecimenRegistrationResponse getLatestRegistration(
        @Parameter(description = "Application id") @PathVariable("applicationId") String applicationId
    ) {
        return specimenControllerAssembler.toLatestSpecimenRegistrationResponse(
            specimenWorkflowAppService.getLatestRegistrationResult(applicationId));
    }

    @Operation(summary = "Get tracking by barcode", description = "Query application tracking data by specimen barcode.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_TRACKING_QUERY)
    @GetMapping("/barcodes/{barcode}/tracking")
    public ApplicationDetailResponse getTrackingByBarcode(
        @Parameter(description = "Specimen barcode") @PathVariable("barcode") String barcode
    ) {
        var tracking = specimenWorkflowAppService.getTrackingByBarcode(barcode);
        return specimenControllerAssembler.toApplicationDetailResponse(
            tracking,
            specimenWorkflowAppService.resolveApplicationOperationState(tracking.application()));
    }

    @Operation(summary = "List specimen verification records", description = "Query verification records by specimen barcode.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_TRACKING_QUERY)
    @GetMapping("/barcodes/{barcode}/verification-records")
    public java.util.List<SpecimenVerificationRecordResponse> listVerificationRecords(
        @Parameter(description = "Specimen barcode") @PathVariable("barcode") String barcode
    ) {
        return specimenControllerAssembler.toSpecimenVerificationRecordResponses(
            specimenWorkflowAppService.listSpecimenVerificationRecords(barcode));
    }

    @Operation(summary = "Bind specimen barcode", description = "Bind barcode for an unbound specimen.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PostMapping("/{specimenId}/barcode-binding")
    public SpecimenSummaryResponse bindBarcode(
        @Parameter(description = "Specimen id") @PathVariable("specimenId") String specimenId,
        @Valid @RequestBody SpecimenBarcodeBindingRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return specimenControllerAssembler.toSpecimenSummaryResponse(
            specimenWorkflowAppService.bindSpecimenBarcode(
                specimenControllerAssembler.toBindSpecimenBarcodeCommand(specimenId, request, httpServletRequest)));
    }

    @Operation(summary = "Rebind specimen barcode", description = "Replace barcode for a bound specimen.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @PutMapping("/{specimenId}/barcode-binding")
    public SpecimenSummaryResponse rebindBarcode(
        @Parameter(description = "Specimen id") @PathVariable("specimenId") String specimenId,
        @Valid @RequestBody SpecimenBarcodeBindingRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return specimenControllerAssembler.toSpecimenSummaryResponse(
            specimenWorkflowAppService.rebindSpecimenBarcode(
                specimenControllerAssembler.toBindSpecimenBarcodeCommand(specimenId, request, httpServletRequest)));
    }

    @Operation(summary = "Unbind specimen barcode", description = "Clear current barcode from a bound specimen.")
    @RequirePermission(M2PermissionCodes.SPECIMEN_REGISTER)
    @DeleteMapping("/{specimenId}/barcode-binding")
    public SpecimenSummaryResponse unbindBarcode(
        @Parameter(description = "Specimen id") @PathVariable("specimenId") String specimenId,
        @RequestParam(value = "terminalCode", required = false) String terminalCode,
        @RequestParam(value = "remarks", required = false) String remarks,
        HttpServletRequest httpServletRequest
    ) {
        return specimenControllerAssembler.toSpecimenSummaryResponse(
            specimenWorkflowAppService.unbindSpecimenBarcode(
                specimenControllerAssembler.toUnbindSpecimenBarcodeCommand(
                    specimenId,
                    terminalCode,
                    remarks,
                    httpServletRequest)));
    }

    @Operation(summary = "Confirm specimen", description = "Confirm a fixed specimen before check-in.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/barcodes/{barcode}/confirm")
    public SpecimenSummaryResponse confirm(
        @Parameter(description = "Specimen barcode") @PathVariable("barcode") String barcode,
        @Valid @RequestBody SpecimenConfirmRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return specimenControllerAssembler.toSpecimenSummaryResponse(
            specimenWorkflowAppService.confirmSpecimen(
                specimenControllerAssembler.toConfirmSpecimenCommand(barcode, request, httpServletRequest)));
    }

    @Operation(summary = "Check in specimen", description = "Check in a confirmed specimen before transport.")
    @RequirePermission(M2PermissionCodes.FIXATION_VERIFY)
    @PostMapping("/barcodes/{barcode}/check-in")
    public SpecimenSummaryResponse checkIn(
        @Parameter(description = "Specimen barcode") @PathVariable("barcode") String barcode,
        @Valid @RequestBody SpecimenCheckInRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return specimenControllerAssembler.toSpecimenSummaryResponse(
            specimenWorkflowAppService.checkInSpecimen(
                specimenControllerAssembler.toCheckInSpecimenCommand(barcode, request, httpServletRequest)));
    }
}
