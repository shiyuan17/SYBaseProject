package com.company.bl.interfaces.controller;

import com.company.bl.integration.application.BillingManagementService;
import com.company.bl.integration.application.HistoricalReportService;
import com.company.bl.integration.application.IntegrationManagementService;
import com.company.bl.integration.application.StatisticsService;
import com.company.bl.interfaces.auth.M6PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "M6支撑能力", description = "集成、收费、历史数据和统计分析接口")
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

    @Operation(summary = "查询集成任务", description = "按任务类型、业务类型和状态查询 M6 集成任务。")
    @RequirePermission(M6PermissionCodes.INTEGRATION_TASK_QUERY)
    @GetMapping("/integration-tasks")
    public List<IntegrationManagementService.IntegrationTaskView> listIntegrationTasks(
        @RequestParam(name = "taskType", required = false) String taskType,
        @RequestParam(name = "businessType", required = false) String businessType,
        @RequestParam(name = "taskStatus", required = false) String taskStatus) {
        return integrationManagementService.listTasks(taskType, businessType, taskStatus);
    }

    @Operation(summary = "查询收费记录", description = "按状态、阶段和时间范围查询收费记录。")
    @RequirePermission(M6PermissionCodes.BILLING_QUERY)
    @GetMapping("/billing-records")
    public List<BillingManagementService.BillingRecordView> listBillingRecords(
        @RequestParam(name = "billingStatus", required = false) String billingStatus,
        @RequestParam(name = "billingStage", required = false) String billingStage,
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to) {
        return billingManagementService.listBillingRecords(billingStatus, billingStage, parseDateTime(from), parseDateTime(to));
    }

    @Operation(summary = "回写收费回执", description = "接收 HIS 或第三方回写的收费结果。")
    @RequirePermission(M6PermissionCodes.BILLING_RECEIPT)
    @PostMapping("/billing-records/{id}/receipt")
    public BillingManagementService.BillingRecordView receiveReceipt(@PathVariable("id") String id,
                                                                     @Valid @RequestBody BillingReceiptRequest request) {
        return billingManagementService.receiveBillingReceipt(
            id, request.externalBillNo(), request.billingStatus(), request.operatorUserId(), request.operatorName(), request.remarks());
    }

    @Operation(summary = "重试收费", description = "对失败收费执行人工重试。")
    @RequirePermission(M6PermissionCodes.BILLING_RETRY)
    @PostMapping("/billing-records/{id}/retry")
    public BillingManagementService.BillingRecordView retryBilling(@PathVariable("id") String id,
                                                                   @Valid @RequestBody OperatorRequest request) {
        return billingManagementService.retryBilling(id, request.operatorUserId(), request.operatorName());
    }

    @Operation(summary = "执行收费对账", description = "按时间范围执行收费记录对账。")
    @RequirePermission(M6PermissionCodes.BILLING_RECONCILE)
    @PostMapping("/billing-records/reconcile")
    public BillingManagementService.ReconciliationResult reconcile(@Valid @RequestBody ReconcileBillingRequest request) {
        return billingManagementService.reconcile(parseDateTime(request.from()), parseDateTime(request.to()),
            request.operatorUserId(), request.operatorName());
    }

    @Operation(summary = "创建历史报告导入任务", description = "从旧系统拉取历史报告并导入 M6 历史库。")
    @RequirePermission(M6PermissionCodes.HISTORY_IMPORT)
    @PostMapping("/historical-report-import-jobs")
    public HistoricalReportService.HistoricalImportJobView importHistoricalReports(@Valid @RequestBody ImportHistoricalReportsRequest request) {
        return historicalReportService.importReports(new HistoricalReportService.ImportHistoricalReportsCommand(
            request.sourceSystem(), request.patientId(), request.pathologyNo(), request.applicationNo(),
            parseDateTime(request.from()), parseDateTime(request.to()), request.operatorUserId(), request.operatorName(), request.remarks()));
    }

    @Operation(summary = "查询历史导入任务", description = "查询历史报告导入任务列表。")
    @RequirePermission(M6PermissionCodes.HISTORY_QUERY)
    @GetMapping("/historical-report-import-jobs")
    public List<HistoricalReportService.HistoricalImportJobView> listImportJobs(
        @RequestParam(name = "sourceSystem", required = false) String sourceSystem,
        @RequestParam(name = "importStatus", required = false) String importStatus) {
        return historicalReportService.listImportJobs(sourceSystem, importStatus);
    }

    @Operation(summary = "查询历史报告", description = "按患者、病理号、申请号、来源系统和时间范围查询历史报告。")
    @RequirePermission(M6PermissionCodes.HISTORY_QUERY)
    @GetMapping("/historical-reports")
    public List<HistoricalReportService.HistoricalReportView> listHistoricalReports(
        @RequestParam(name = "sourceSystem", required = false) String sourceSystem,
        @RequestParam(name = "patientId", required = false) String patientId,
        @RequestParam(name = "pathologyNo", required = false) String pathologyNo,
        @RequestParam(name = "applicationNo", required = false) String applicationNo,
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to) {
        return historicalReportService.listHistoricalReports(
            sourceSystem, patientId, pathologyNo, applicationNo, parseDateTime(from), parseDateTime(to));
    }

    @Operation(summary = "查询统计指标定义", description = "按类别查询 M6 统计指标定义。")
    @RequirePermission(M6PermissionCodes.STAT_INDICATOR_QUERY)
    @GetMapping("/stat-indicators")
    public List<StatisticsService.IndicatorDefinitionView> listIndicators(
        @RequestParam(name = "category", required = false) String category) {
        return statisticsService.listIndicators(category);
    }

    @Operation(summary = "查询统计模板", description = "按模板类型查询 M6 统计模板。")
    @RequirePermission(M6PermissionCodes.STAT_TEMPLATE_QUERY)
    @GetMapping("/stat-report-templates")
    public List<StatisticsService.ReportTemplateView> listTemplates(
        @RequestParam(name = "templateType", required = false) String templateType) {
        return statisticsService.listTemplates(templateType);
    }

    @Operation(summary = "查询统计报表", description = "按指标、模板和时间范围生成统计表格。")
    @RequirePermission(M6PermissionCodes.STAT_REPORT_QUERY)
    @PostMapping("/stat-reports/query")
    public StatisticsService.StatReportResult queryReport(@Valid @RequestBody QueryStatReportRequest request) {
        return statisticsService.queryReport(new StatisticsService.QueryStatReportCommand(
            request.templateCode(), request.indicatorCode(), request.category(), parseDateTime(request.from()), parseDateTime(request.to()),
            request.operatorUserId(), request.operatorName()));
    }

    @Operation(summary = "导出统计报表", description = "导出 CSV 统计报表。")
    @RequirePermission(M6PermissionCodes.STAT_REPORT_EXPORT)
    @PostMapping("/stat-reports/export")
    public ResponseEntity<byte[]> exportReport(@Valid @RequestBody QueryStatReportRequest request) {
        byte[] content = statisticsService.exportReport(new StatisticsService.QueryStatReportCommand(
            request.templateCode(), request.indicatorCode(), request.category(), parseDateTime(request.from()), parseDateTime(request.to()),
            request.operatorUserId(), request.operatorName()));
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

    @Schema(name = "M6OperatorRequest", description = "操作人请求")
    public record OperatorRequest(
        @Schema(description = "操作人用户 ID") @Size(max = 64) String operatorUserId,
        @Schema(description = "操作人姓名") @Size(max = 100) String operatorName
    ) {
    }

    @Schema(name = "M6BillingReceiptRequest", description = "收费回执请求")
    public record BillingReceiptRequest(
        @Schema(description = "第三方收费单号") @Size(max = 64) String externalBillNo,
        @Schema(description = "收费状态") @Size(max = 32) String billingStatus,
        @Schema(description = "操作人用户 ID") @Size(max = 64) String operatorUserId,
        @Schema(description = "操作人姓名") @Size(max = 100) String operatorName,
        @Schema(description = "备注") @Size(max = 500) String remarks
    ) {
    }

    @Schema(name = "M6ReconcileBillingRequest", description = "收费对账请求")
    public record ReconcileBillingRequest(
        @Schema(description = "开始时间") String from,
        @Schema(description = "结束时间") String to,
        @Schema(description = "操作人用户 ID") @Size(max = 64) String operatorUserId,
        @Schema(description = "操作人姓名") @Size(max = 100) String operatorName
    ) {
    }

    @Schema(name = "M6ImportHistoricalReportsRequest", description = "历史报告导入请求")
    public record ImportHistoricalReportsRequest(
        @Schema(description = "来源系统") @Size(max = 64) String sourceSystem,
        @Schema(description = "患者 ID") @Size(max = 64) String patientId,
        @Schema(description = "病理号") @Size(max = 64) String pathologyNo,
        @Schema(description = "申请号") @Size(max = 64) String applicationNo,
        @Schema(description = "开始时间") String from,
        @Schema(description = "结束时间") String to,
        @Schema(description = "操作人用户 ID") @Size(max = 64) String operatorUserId,
        @Schema(description = "操作人姓名") @Size(max = 100) String operatorName,
        @Schema(description = "备注") @Size(max = 500) String remarks
    ) {
    }

    @Schema(name = "M6QueryStatReportRequest", description = "统计报表查询请求")
    public record QueryStatReportRequest(
        @Schema(description = "模板编码") @Size(max = 64) String templateCode,
        @Schema(description = "指标编码") @Size(max = 64) String indicatorCode,
        @Schema(description = "指标类别") @Size(max = 32) String category,
        @Schema(description = "开始时间") String from,
        @Schema(description = "结束时间") String to,
        @Schema(description = "操作人用户 ID") @Size(max = 64) String operatorUserId,
        @Schema(description = "操作人姓名") @Size(max = 100) String operatorName
    ) {
    }
}
