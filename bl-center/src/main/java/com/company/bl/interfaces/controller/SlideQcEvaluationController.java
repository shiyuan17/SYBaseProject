package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.SlideQcEvaluationCreateRequest;
import com.company.bl.interfaces.vo.SlideQcEvaluationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slide-qc-evaluations")
@Tag(name = "技术流程", description = "切片质控评价接口")
public class SlideQcEvaluationController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public SlideQcEvaluationController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "创建切片质控评价", description = "为切片工作站录入切片质控评价结果。")
    @RequirePermission(M3PermissionCodes.SLICING)
    @PostMapping
    public SlideQcEvaluationResponse create(
        @Valid @RequestBody SlideQcEvaluationCreateRequest request,
        HttpServletRequest httpServletRequest
    ) {
        TechnicalWorkflowModels.SlideQcEvaluationResult result =
            technicalWorkflowAppService.createSlideQcEvaluation(
                new TechnicalWorkflowModels.CreateSlideQcEvaluationCommand(
                    request.getCaseId(),
                    request.getSpecimenId(),
                    request.getSlideId(),
                    request.getQcType(),
                    request.getEvaluationResult(),
                    request.getIssueDescription(),
                    request.getImprovementSuggestion(),
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    request.getTerminalCode(),
                    request.getRemarks()));
        return new SlideQcEvaluationResponse(
            result.qcEvaluationId(),
            result.slideId(),
            result.evaluationResult(),
            result.qualityStatus());
    }
}
