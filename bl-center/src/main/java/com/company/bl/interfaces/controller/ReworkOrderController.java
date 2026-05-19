package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateReworkOrderRequest;
import com.company.bl.interfaces.dto.ExecuteReworkOrderRequest;
import com.company.bl.interfaces.vo.ReworkOrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rework-orders")
public class ReworkOrderController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public ReworkOrderController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @RequirePermission(M3PermissionCodes.REWORK)
    @PostMapping
    public ReworkOrderResponse create(@Valid @RequestBody CreateReworkOrderRequest request,
                                      HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.ReworkOrderResult result = technicalWorkflowAppService.createReworkOrder(
            new TechnicalWorkflowAppService.CreateReworkOrderCommand(
                request.getCaseId(),
                request.getSpecimenId(),
                request.getSamplingBlockId(),
                request.getEmbeddingBoxId(),
                request.getSlideId(),
                request.getReworkType(),
                request.getReason(),
                request.getQcType(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new ReworkOrderResponse(result.caseId(), result.reworkType(), result.status());
    }

    @RequirePermission(M3PermissionCodes.REWORK)
    @PostMapping("/{id}/execute")
    public ReworkOrderResponse execute(@PathVariable("id") String id,
                                       @Valid @RequestBody ExecuteReworkOrderRequest request,
                                       HttpServletRequest httpServletRequest) {
        TechnicalWorkflowAppService.ReworkOrderResult result = technicalWorkflowAppService.executeReworkOrder(
            new TechnicalWorkflowAppService.ExecuteReworkOrderCommand(
                id,
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new ReworkOrderResponse(result.caseId(), result.reworkType(), result.status());
    }
}
