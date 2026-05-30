package com.company.bl.interfaces.controller;

import com.company.bl.application.service.OperationSupportModels;
import com.company.bl.application.service.OperationSupportService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateReagentRequest;
import com.company.bl.interfaces.dto.CreateReagentStockRequest;
import com.company.bl.interfaces.dto.UpdateReagentRequest;
import com.company.bl.interfaces.dto.UpdateReagentStockRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
                                                                 @RequestParam(required = false) Boolean enabled) {
        return operationSupportService.listReagents(keyword, enabled);
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
            request.getDefaultLowStockThreshold(),
            request.getDefaultNearExpiryDays(),
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
            request.getDefaultLowStockThreshold(),
            request.getDefaultNearExpiryDays(),
            request.isEnabled(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_QUERY)
    @GetMapping("/reagent-stocks")
    public List<OperationSupportModels.ReagentStockView> listReagentStocks(@RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) String stockStatus) {
        return operationSupportService.listReagentStocks(keyword, stockStatus);
    }

    @RequirePermission(M5PermissionCodes.REAGENT_STOCK_UPDATE)
    @PostMapping("/reagent-stocks")
    public OperationSupportModels.ReagentStockView createReagentStock(@Valid @RequestBody CreateReagentStockRequest request,
                                                                      HttpServletRequest httpServletRequest) {
        return operationSupportService.createReagentStock(new OperationSupportModels.CreateReagentStockCommand(
            request.getReagentId(),
            request.getBatchNo(),
            request.getStockQuantity(),
            request.getStockStatus(),
            request.getExpiryDate(),
            request.getStorageLocation(),
            request.getLowStockThreshold(),
            request.getNearExpiryDays(),
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
            request.getStockQuantity(),
            request.getStockStatus(),
            request.getExpiryDate(),
            request.getStorageLocation(),
            request.getLowStockThreshold(),
            request.getNearExpiryDays(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.REAGENT_WARNING_QUERY)
    @GetMapping("/reagent-stocks/warnings")
    public List<OperationSupportModels.ReagentWarningView> listReagentWarnings() {
        return operationSupportService.listReagentWarnings();
    }
}
