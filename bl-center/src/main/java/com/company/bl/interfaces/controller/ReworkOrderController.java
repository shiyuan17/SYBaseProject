package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateReworkOrderRequest;
import com.company.bl.interfaces.dto.ExecuteReworkOrderRequest;
import com.company.bl.interfaces.vo.ReworkOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rework-orders")
@Tag(name = "技术流程", description = "补做单创建与执行接口")
public class ReworkOrderController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public ReworkOrderController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "创建补做单", description = "对病例、标本、蜡块、包埋盒或切片创建补做单。")
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

    @Operation(summary = "执行补做单", description = "执行指定补做单，推动病例进入补做流程。")
    @RequirePermission(M3PermissionCodes.REWORK)
    @PostMapping("/{id}/execute")
    public ReworkOrderResponse execute(@Parameter(description = "补做单 ID") @PathVariable("id") String id,
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
