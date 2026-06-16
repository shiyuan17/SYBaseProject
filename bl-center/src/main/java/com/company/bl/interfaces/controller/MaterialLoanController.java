package com.company.bl.interfaces.controller;

import com.company.bl.application.service.ArchiveModels;
import com.company.bl.application.service.ArchiveQueryService;
import com.company.bl.application.service.ArchiveWorkflowService;
import com.company.bl.interfaces.auth.M5PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateMaterialLoanAbnormalRecordRequest;
import com.company.bl.interfaces.dto.CreateMaterialLoanRequest;
import com.company.bl.interfaces.dto.ReturnMaterialLoanRequest;
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
@RequestMapping("/api/v1/material-loans")
public class MaterialLoanController extends TechnicalControllerSupport {

    private final ArchiveQueryService archiveQueryService;
    private final ArchiveWorkflowService archiveWorkflowService;

    public MaterialLoanController(ArchiveQueryService archiveQueryService,
                                  ArchiveWorkflowService archiveWorkflowService) {
        this.archiveQueryService = archiveQueryService;
        this.archiveWorkflowService = archiveWorkflowService;
    }

    @RequirePermission(M5PermissionCodes.LOAN_QUERY)
    @GetMapping
    public List<ArchiveModels.MaterialLoanView> listMaterialLoans(@RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) String materialType,
                                                                  @RequestParam(required = false) String loanStatus) {
        return archiveQueryService.listMaterialLoans(keyword, materialType, loanStatus);
    }

    @RequirePermission(M5PermissionCodes.LOAN_QUERY)
    @GetMapping("/pending")
    public List<ArchiveModels.MaterialLoanView> listPendingMaterialLoans(@RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) String materialType) {
        return archiveQueryService.listPendingMaterialLoans(keyword, materialType);
    }

    @RequirePermission(M5PermissionCodes.LOAN_CREATE)
    @PostMapping
    public ArchiveModels.MaterialLoanView createMaterialLoan(@Valid @RequestBody CreateMaterialLoanRequest request,
                                                             HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.createMaterialLoan(new ArchiveModels.CreateMaterialLoanCommand(
            request.getMaterialType(),
            request.getMaterialId(),
            request.getBorrowedByUserId(),
            request.getBorrowedByName(),
            request.getBorrowerPhone(),
            request.getBorrowerUnit(),
            request.getBorrowPurpose(),
            request.getDepositAmount(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.LOAN_RETURN)
    @PostMapping("/{id}/return")
    public ArchiveModels.MaterialLoanView returnMaterialLoan(@PathVariable("id") String loanId,
                                                             @Valid @RequestBody ReturnMaterialLoanRequest request,
                                                             HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.returnMaterialLoan(new ArchiveModels.ReturnMaterialLoanCommand(
            loanId,
            request.getArchivePositionId(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode(),
            request.getRemarks()));
    }

    @RequirePermission(M5PermissionCodes.LOAN_ABNORMAL_REGISTER)
    @PostMapping("/abnormal-records")
    public ArchiveModels.MaterialLoanAbnormalRecordView createMaterialLoanAbnormalRecord(
        @Valid @RequestBody CreateMaterialLoanAbnormalRecordRequest request,
        HttpServletRequest httpServletRequest) {
        return archiveWorkflowService.createMaterialLoanAbnormalRecord(new ArchiveModels.CreateMaterialLoanAbnormalRecordCommand(
            request.getMaterialType(),
            request.getMaterialId(),
            request.getLoanId(),
            request.getAbnormalReason(),
            request.getContacted(),
            request.getContactResult(),
            request.getBorrowedSlideNo(),
            request.getBorrowerName(),
            request.getBorrowerRelationship(),
            request.getBorrowerPhone(),
            request.getBorrowerUnit(),
            request.getBorrowerIdentityNo(),
            request.getBorrowedAt(),
            request.getExpectedReturnAt(),
            request.getSlideCount(),
            request.getDepositAmount(),
            request.getBorrowedContent(),
            request.getReturnAbnormalInfo(),
            resolveUserId(httpServletRequest),
            resolveOperatorName(httpServletRequest),
            request.getTerminalCode()));
    }
}
