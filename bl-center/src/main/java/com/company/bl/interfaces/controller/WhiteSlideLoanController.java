package com.company.bl.interfaces.controller;

import com.company.bl.application.service.OperationSupportModels;
import com.company.bl.application.service.OperationSupportService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateWhiteSlideLoanRequest;
import com.company.bl.interfaces.dto.ReturnWhiteSlideLoanRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WhiteSlideLoanController extends TechnicalControllerSupport {

    private final OperationSupportService operationSupportService;

    public WhiteSlideLoanController(OperationSupportService operationSupportService) {
        this.operationSupportService = operationSupportService;
    }

    @RequirePermission(M5PermissionCodes.WHITE_SLIDE_QUERY)
    @GetMapping("/api/v1/white-slide-stocks")
    public List<OperationSupportModels.WhiteSlideStockView> listWhiteSlideStocks(@RequestParam(required = false) String keyword,
                                                                                 @RequestParam(required = false) String status) {
        return operationSupportService.listWhiteSlideStocks(keyword, status);
    }

    @RequirePermission(M5PermissionCodes.WHITE_SLIDE_QUERY)
    @GetMapping("/api/v1/white-slide-loans")
    public List<OperationSupportModels.WhiteSlideLoanView> listWhiteSlideLoans(@RequestParam(required = false) String keyword,
                                                                               @RequestParam(required = false) String loanStatus) {
        return operationSupportService.listWhiteSlideLoans(keyword, loanStatus);
    }

    @RequirePermission(M5PermissionCodes.WHITE_SLIDE_CREATE)
    @PostMapping("/api/v1/white-slide-loans")
    public OperationSupportModels.WhiteSlideLoanView createWhiteSlideLoan(@Valid @RequestBody CreateWhiteSlideLoanRequest request,
                                                                          HttpServletRequest httpServletRequest) {
        return operationSupportService.createWhiteSlideLoan(new OperationSupportModels.CreateWhiteSlideLoanCommand(
            request.getStockId(),
            request.getQuantity(),
            request.getCaseId(),
            request.getPathologyNo(),
            request.getPatientName(),
            request.getEmbeddingBoxNo(),
            request.getSlicePurpose(),
            request.getSliceThickness(),
            request.getBorrowerName(),
            request.getBorrowerIdentityNo(),
            request.getBorrowerUnit(),
            request.getBorrowerPhone(),
            request.getUnitPrice(),
            request.getAmount(),
            Boolean.TRUE.equals(request.getSaveDirectPrint()),
            request.getWaxBlockUsage(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.WHITE_SLIDE_RETURN)
    @PostMapping("/api/v1/white-slide-loans/{id}/return")
    public OperationSupportModels.WhiteSlideLoanView returnWhiteSlideLoan(@PathVariable("id") String loanId,
                                                                          @Valid @RequestBody ReturnWhiteSlideLoanRequest request,
                                                                          HttpServletRequest httpServletRequest) {
        return operationSupportService.returnWhiteSlideLoan(new OperationSupportModels.ReturnWhiteSlideLoanCommand(
            loanId,
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks()));
    }
}
