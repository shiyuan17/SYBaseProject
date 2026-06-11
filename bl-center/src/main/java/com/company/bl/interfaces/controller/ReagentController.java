package com.company.bl.interfaces.controller;

import com.company.bl.application.service.OperationSupportModels;
import com.company.bl.application.service.OperationSupportService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateReagentRequest;
import com.company.bl.interfaces.dto.CreateReagentStockRequest;
import com.company.bl.interfaces.dto.ReagentStockActionRequest;
import com.company.bl.interfaces.dto.UpdateReagentRequest;
import com.company.bl.interfaces.dto.UpdateReagentStockRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReagentController extends TechnicalControllerSupport {

    private final OperationSupportService operationSupportService;

    public ReagentController(OperationSupportService operationSupportService) {
        this.operationSupportService = operationSupportService;
    }

    @RequirePermission(M5PermissionCodes.REAGENT_QUERY)
    @GetMapping("/reagents")
    public List<OperationSupportModels.ReagentView> listReagents(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) Boolean enabled,
                                                                 @RequestParam(required = false) String reagentType,
                                                                 @RequestParam(required = false) String templateStatus) {
        return operationSupportService.listReagents(keyword, enabled, reagentType, templateStatus);
    }

    @RequirePermission(M5PermissionCodes.REAGENT_CREATE)
    @PostMapping("/reagents")
    public OperationSupportModels.ReagentView createReagent(@Valid @RequestBody CreateReagentRequest request,
                                                            HttpServletRequest httpServletRequest) {
        return operationSupportService.createReagent(new OperationSupportModels.CreateReagentCommand(
            request.getReagentCode(),
            request.getReagentName(),
            request.getSpecification(),
            request.getUnit(),
            request.getManufacturer(),
            request.getReagentType(),
            request.getReagentUsage(),
            request.getOrderDictItemId(),
            request.getCloneNo(),
            request.getRecommendedDilution(),
            request.getApplicationDilution(),
            request.getTemplateStatus(),
            request.getValidityDays(),
            request.getDefaultLowStockThreshold(),
            request.getDefaultStockThreshold(),
            request.getDefaultNearExpiryDays(),
            request.getStainCapacity(),
            request.getStainThreshold(),
            request.isEnabled(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_UPDATE)
    @PatchMapping("/reagents/{id}")
    public OperationSupportModels.ReagentView updateReagent(@PathVariable("id") String reagentId,
                                                            @Valid @RequestBody UpdateReagentRequest request,
                                                            HttpServletRequest httpServletRequest) {
        return operationSupportService.updateReagent(new OperationSupportModels.UpdateReagentCommand(
            reagentId,
            request.getReagentName(),
            request.getSpecification(),
            request.getUnit(),
            request.getManufacturer(),
            request.getReagentType(),
            request.getReagentUsage(),
            request.getOrderDictItemId(),
            request.getCloneNo(),
            request.getRecommendedDilution(),
            request.getApplicationDilution(),
            request.getTemplateStatus(),
            request.getValidityDays(),
            request.getDefaultLowStockThreshold(),
            request.getDefaultStockThreshold(),
            request.getDefaultNearExpiryDays(),
            request.getStainCapacity(),
            request.getStainThreshold(),
            request.isEnabled(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_QUERY)
    @GetMapping("/reagent-stocks")
    public List<OperationSupportModels.ReagentStockView> listReagentStocks(@RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) String stockStatus,
                                                                           @RequestParam(required = false) String reagentType,
                                                                           @RequestParam(required = false) LocalDate dateFrom,
                                                                           @RequestParam(required = false) LocalDate dateTo) {
        return operationSupportService.listReagentStocks(keyword, stockStatus, reagentType, dateFrom, dateTo);
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_UPDATE)
    @PostMapping("/reagent-stocks")
    public OperationSupportModels.ReagentStockView createReagentStock(@Valid @RequestBody CreateReagentStockRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        return operationSupportService.createReagentStock(new OperationSupportModels.CreateReagentStockCommand(
            request.getReagentId(),
            request.getBatchNo(),
            request.getInitialQuantity(),
            request.getStockQuantity(),
            request.getRemainingQuantity(),
            request.getStockStatus(),
            request.getProductionDate(),
            request.getInboundAt(),
            request.getExpiryDate(),
            request.getStorageLocation(),
            request.getLowStockThreshold(),
            request.getNearExpiryDays(),
            request.getTestReminderThreshold(),
            request.getExpiryReminderThreshold(),
            request.getRecommendedDilution(),
            request.getApplicationDilution(),
            request.getStainCapacity(),
            request.getStainThreshold(),
            request.getValidityDays(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_UPDATE)
    @PatchMapping("/reagent-stocks/{id}")
    public OperationSupportModels.ReagentStockView updateReagentStock(@PathVariable("id") String stockId,
                                                                      @Valid @RequestBody UpdateReagentStockRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        return operationSupportService.updateReagentStock(new OperationSupportModels.UpdateReagentStockCommand(
            stockId,
            request.getInitialQuantity(),
            request.getStockQuantity(),
            request.getRemainingQuantity(),
            request.getStockStatus(),
            request.getProductionDate(),
            request.getInboundAt(),
            request.getExpiryDate(),
            request.getStorageLocation(),
            request.getLowStockThreshold(),
            request.getNearExpiryDays(),
            request.getTestReminderThreshold(),
            request.getExpiryReminderThreshold(),
            request.getRecommendedDilution(),
            request.getApplicationDilution(),
            request.getStainCapacity(),
            request.getStainThreshold(),
            request.getValidityDays(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_UPDATE)
    @PostMapping("/reagent-stocks/{id}/test")
    public OperationSupportModels.ReagentStockView testReagentStock(@PathVariable("id") String stockId,
                                                                    @Valid @RequestBody ReagentStockActionRequest request,
                                                                    HttpServletRequest httpServletRequest) {
        return operationSupportService.testReagentStock(new OperationSupportModels.ReagentStockActionCommand(
            stockId,
            request.getQuantity(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_UPDATE)
    @PostMapping("/reagent-stocks/{id}/consume")
    public OperationSupportModels.ReagentStockView consumeReagentStock(@PathVariable("id") String stockId,
                                                                       @Valid @RequestBody ReagentStockActionRequest request,
                                                                       HttpServletRequest httpServletRequest) {
        return operationSupportService.consumeReagentStock(new OperationSupportModels.ReagentStockActionCommand(
            stockId,
            request.getQuantity(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_UPDATE)
    @PostMapping("/reagent-stocks/{id}/start-use")
    public OperationSupportModels.ReagentStockView startUsingReagentStock(@PathVariable("id") String stockId,
                                                                         @Valid @RequestBody ReagentStockActionRequest request,
                                                                         HttpServletRequest httpServletRequest) {
        return operationSupportService.startUsingReagentStock(new OperationSupportModels.ReagentStockActionCommand(
            stockId,
            request.getQuantity(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_UPDATE)
    @PostMapping("/reagent-stocks/{id}/finish-use")
    public OperationSupportModels.ReagentStockView finishUsingReagentStock(@PathVariable("id") String stockId,
                                                                          @Valid @RequestBody ReagentStockActionRequest request,
                                                                          HttpServletRequest httpServletRequest) {
        return operationSupportService.finishUsingReagentStock(new OperationSupportModels.ReagentStockActionCommand(
            stockId,
            request.getQuantity(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_QUERY)
    @GetMapping("/reagent-stocks/{id}/events")
    public List<OperationSupportModels.ReagentStockEventView> listReagentStockEvents(@PathVariable("id") String stockId) {
        return operationSupportService.listReagentStockEvents(stockId);
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_QUERY)
    @GetMapping("/reagent-stocks/export")
    public ResponseEntity<byte[]> exportReagentStocks(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String stockStatus,
                                                      @RequestParam(required = false) String reagentType,
                                                      @RequestParam(required = false) LocalDate dateFrom,
                                                      @RequestParam(required = false) LocalDate dateTo) {
        byte[] content = operationSupportService.exportReagentStocks(keyword, stockStatus, reagentType, dateFrom, dateTo);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reagent-stocks.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(content);
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_UPDATE)
    @PostMapping(value = "/reagent-stocks/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OperationSupportModels.ReagentStockImportResult importReagentStocks(@RequestParam("file") MultipartFile file,
                                                                               HttpServletRequest httpServletRequest) throws Exception {
        return operationSupportService.importReagentStocks(file.getBytes(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_WARNING_QUERY)
    @GetMapping("/reagent-stocks/warnings")
    public List<OperationSupportModels.ReagentWarningView> listReagentWarnings() {
        return operationSupportService.listReagentWarnings();
    }
}
