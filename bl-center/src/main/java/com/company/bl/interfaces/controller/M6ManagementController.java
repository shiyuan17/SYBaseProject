package com.company.bl.interfaces.controller;

import com.company.bl.integration.application.BillingManagementService;
import com.company.bl.integration.application.HistoricalReportService;
import com.company.bl.integration.application.IntegrationManagementService;
import com.company.bl.integration.application.StatisticsService;
import com.company.bl.interfaces.auth.M6PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "M6 Management", description = "M6 integration, billing, history, and statistics management")
public class M6ManagementController {

    private final IntegrationManagementService integrationManagementService;
    private final BillingManagementService billingManagementService;
    private final HistoricalReportService historicalReportService;
    private final StatisticsService statisticsService;

    public M6ManagementController(IntegrationManagementService integrationManagementService,
                                  BillingManagementService billingManagementService,
                                  HistoricalReportService historicalReportService,
                                  StatisticsService statisticsService) {
        this.integrationManagementService = integrationManagementService;
        this.billingManagementService = billingManagementService;
        this.historicalReportService = historicalReportService;
        this.statisticsService = statisticsService;
    }

    @Operation(summary = "List integration tasks", description = "Query M6 integration task traces.")
    @RequirePermission(M6PermissionCodes.INTEGRATION_TASK_QUERY)
    @GetMapping("/integration-tasks")
    public List<IntegrationManagementService.IntegrationTaskView> listIntegrationTasks(
        @RequestParam(name = "taskType", required = false) String taskType,
        @RequestParam(name = "businessType", required = false) String businessType,
        @RequestParam(name = "businessId", required = false) String businessId,
        @RequestParam(name = "taskStatus", required = false) String taskStatus,
        @RequestParam(name = "stageCode", required = false) String stageCode,
        @RequestParam(name = "externalSystem", required = false) String externalSystem,
        @RequestParam(name = "compensationStatus", required = false) String compensationStatus,
        @RequestParam(name = "reconciliationStatus", required = false) String reconciliationStatus) {
        return integrationManagementService.listTasks(
            taskType, businessType, businessId, taskStatus, stageCode, externalSystem, compensationStatus, reconciliationStatus);
    }

    @Operation(summary = "List billing records", description = "Query billing records by status, stage, and time window.")
    @RequirePermission(M6PermissionCodes.BILLING_QUERY)
    @GetMapping("/billing-records")
    public List<BillingManagementService.BillingRecordView> listBillingRecords(
        @RequestParam(name = "billingStatus", required = false) String billingStatus,
        @RequestParam(name = "billingStage", required = false) String billingStage,
        @RequestParam(name = "externalSystem", required = false) String externalSystem,
        @RequestParam(name = "caseId", required = false) String caseId,
        @RequestParam(name = "orderId", required = false) String orderId,
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to) {
        return billingManagementService.listBillingRecords(
            billingStatus, billingStage, externalSystem, caseId, orderId, parseDateTime(from), parseDateTime(to));
    }

    @Operation(summary = "Receive billing receipt", description = "Receive billing receipt callbacks from external systems.")
    @RequirePermission(M6PermissionCodes.BILLING_RECEIPT)
    @PostMapping("/billing-records/{id}/receipt")
    public BillingManagementService.BillingRecordView receiveReceipt(@PathVariable("id") String id,
                                                                     @Valid @RequestBody BillingReceiptRequest request) {
        return billingManagementService.receiveBillingReceipt(
            id, request.externalBillNo(), request.billingStatus(), request.operatorUserId(), request.operatorName(), request.remarks());
    }

    @Operation(summary = "Retry billing", description = "Retry a failed billing submission.")
    @RequirePermission(M6PermissionCodes.BILLING_RETRY)
    @PostMapping("/billing-records/{id}/retry")
    public BillingManagementService.BillingRecordView retryBilling(@PathVariable("id") String id,
                                                                   @Valid @RequestBody OperatorRequest request) {
        return billingManagementService.retryBilling(id, request.operatorUserId(), request.operatorName());
    }

    @Operation(summary = "Reconcile billing", description = "Run billing reconciliation in a time window.")
    @RequirePermission(M6PermissionCodes.BILLING_RECONCILE)
    @PostMapping("/billing-records/reconcile")
    public BillingManagementService.ReconciliationResult reconcile(@Valid @RequestBody ReconcileBillingRequest request) {
        return billingManagementService.reconcile(parseDateTime(request.from()), parseDateTime(request.to()),
            request.operatorUserId(), request.operatorName());
    }

    @Operation(summary = "Import historical reports", description = "Create a historical report import job.")
    @RequirePermission(M6PermissionCodes.HISTORY_IMPORT)
    @PostMapping("/historical-report-import-jobs")
    public HistoricalReportService.HistoricalImportJobView importHistoricalReports(@Valid @RequestBody ImportHistoricalReportsRequest request) {
        return historicalReportService.importReports(new HistoricalReportService.ImportHistoricalReportsCommand(
            request.sourceSystem(), request.patientId(), request.pathologyNo(), request.applicationNo(),
            parseDateTime(request.from()), parseDateTime(request.to()), request.operatorUserId(), request.operatorName(), request.remarks()));
    }

    @Operation(summary = "List historical import jobs", description = "Query historical report import jobs.")
    @RequirePermission(M6PermissionCodes.HISTORY_QUERY)
    @GetMapping("/historical-report-import-jobs")
    public List<HistoricalReportService.HistoricalImportJobView> listImportJobs(
        @RequestParam(name = "sourceSystem", required = false) String sourceSystem,
        @RequestParam(name = "importStatus", required = false) String importStatus,
        @RequestParam(name = "patientId", required = false) String patientId,
        @RequestParam(name = "pathologyNo", required = false) String pathologyNo,
        @RequestParam(name = "applicationNo", required = false) String applicationNo) {
        return historicalReportService.listImportJobs(sourceSystem, importStatus, patientId, pathologyNo, applicationNo);
    }

    @Operation(summary = "List historical reports", description = "Query imported historical reports.")
    @RequirePermission(M6PermissionCodes.HISTORY_QUERY)
    @GetMapping("/historical-reports")
    public List<HistoricalReportService.HistoricalReportView> listHistoricalReports(
        @RequestParam(name = "sourceSystem", required = false) String sourceSystem,
        @RequestParam(name = "patientId", required = false) String patientId,
        @RequestParam(name = "pathologyNo", required = false) String pathologyNo,
        @RequestParam(name = "applicationNo", required = false) String applicationNo,
        @RequestParam(name = "externalReportNo", required = false) String externalReportNo,
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to) {
        return historicalReportService.listHistoricalReports(
            sourceSystem, patientId, pathologyNo, applicationNo, externalReportNo, parseDateTime(from), parseDateTime(to));
    }

    @Operation(summary = "List stat indicators", description = "Query M6 statistic indicator definitions.")
    @RequirePermission(M6PermissionCodes.STAT_INDICATOR_QUERY)
    @GetMapping("/stat-indicators")
    public List<StatisticsService.IndicatorDefinitionView> listIndicators(
        @RequestParam(name = "category", required = false) String category) {
        return statisticsService.listIndicators(category);
    }

    @Operation(summary = "List stat templates", description = "Query M6 statistic report templates.")
    @RequirePermission(M6PermissionCodes.STAT_TEMPLATE_QUERY)
    @GetMapping("/stat-report-templates")
    public List<StatisticsService.ReportTemplateView> listTemplates(
        @RequestParam(name = "templateType", required = false) String templateType) {
        return statisticsService.listTemplates(templateType);
    }

    @Operation(summary = "Query stat report", description = "Query statistic reports with time, department, role, and operator filters.")
    @RequirePermission(M6PermissionCodes.STAT_REPORT_QUERY)
    @PostMapping("/stat-reports/query")
    public StatisticsService.StatReportResult queryReport(@Valid @RequestBody QueryStatReportRequest request) {
        return statisticsService.queryReport(new StatisticsService.QueryStatReportCommand(
            request.templateCode(), request.indicatorCode(), request.category(), parseDateTime(request.from()), parseDateTime(request.to()),
            request.departmentId(), request.roleId(), request.operatorUserId(), request.operatorName()));
    }

    @Operation(summary = "Export stat report", description = "Export statistic reports as UTF-8 BOM CSV.")
    @RequirePermission(M6PermissionCodes.STAT_REPORT_EXPORT)
    @PostMapping("/stat-reports/export")
    public ResponseEntity<byte[]> exportReport(@Valid @RequestBody QueryStatReportRequest request) {
        byte[] content = statisticsService.exportReport(new StatisticsService.QueryStatReportCommand(
            request.templateCode(), request.indicatorCode(), request.category(), parseDateTime(request.from()), parseDateTime(request.to()),
            request.departmentId(), request.roleId(), request.operatorUserId(), request.operatorName()));
        String fileName = (request.templateCode() == null || request.templateCode().isBlank() ? "stat-report" : request.templateCode()) + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(content);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value);
    }

    @Schema(name = "M6OperatorRequest", description = "Operator request")
    public record OperatorRequest(
        @Schema(description = "Operator user id") @Size(max = 64) String operatorUserId,
        @Schema(description = "Operator name") @Size(max = 100) String operatorName
    ) {
    }

    @Schema(name = "M6BillingReceiptRequest", description = "Billing receipt request")
    public record BillingReceiptRequest(
        @Schema(description = "External bill no") @Size(max = 64) String externalBillNo,
        @Schema(description = "Billing status") @Size(max = 32) String billingStatus,
        @Schema(description = "Operator user id") @Size(max = 64) String operatorUserId,
        @Schema(description = "Operator name") @Size(max = 100) String operatorName,
        @Schema(description = "Remarks") @Size(max = 500) String remarks
    ) {
    }

    @Schema(name = "M6ReconcileBillingRequest", description = "Billing reconcile request")
    public record ReconcileBillingRequest(
        @Schema(description = "Start time") String from,
        @Schema(description = "End time") String to,
        @Schema(description = "Operator user id") @Size(max = 64) String operatorUserId,
        @Schema(description = "Operator name") @Size(max = 100) String operatorName
    ) {
    }

    @Schema(name = "M6ImportHistoricalReportsRequest", description = "Historical report import request")
    public record ImportHistoricalReportsRequest(
        @Schema(description = "Source system") @Size(max = 64) String sourceSystem,
        @Schema(description = "Patient id") @Size(max = 64) String patientId,
        @Schema(description = "Pathology no") @Size(max = 64) String pathologyNo,
        @Schema(description = "Application no") @Size(max = 64) String applicationNo,
        @Schema(description = "Start time") String from,
        @Schema(description = "End time") String to,
        @Schema(description = "Operator user id") @Size(max = 64) String operatorUserId,
        @Schema(description = "Operator name") @Size(max = 100) String operatorName,
        @Schema(description = "Remarks") @Size(max = 500) String remarks
    ) {
    }

    @Schema(name = "M6QueryStatReportRequest", description = "Stat report query request")
    public record QueryStatReportRequest(
        @Schema(description = "Template code") @Size(max = 64) String templateCode,
        @Schema(description = "Indicator code") @Size(max = 64) String indicatorCode,
        @Schema(description = "Indicator category") @Size(max = 32) String category,
        @Schema(description = "Start time") String from,
        @Schema(description = "End time") String to,
        @Schema(description = "Submitting department id") @Size(max = 64) String departmentId,
        @Schema(description = "Role id") @Size(max = 64) String roleId,
        @Schema(description = "Operator user id") @Size(max = 64) String operatorUserId,
        @Schema(description = "Operator name") @Size(max = 100) String operatorName
    ) {
    }
}
