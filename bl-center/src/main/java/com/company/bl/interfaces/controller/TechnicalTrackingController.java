package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M3PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.vo.TechnicalTrackingCaseListItemResponse;
import com.company.bl.interfaces.vo.TechnicalTrackingCaseListPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/technical-tracking")
@Tag(name = "技术流程", description = "技术追踪病例列表接口")
public class TechnicalTrackingController {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public TechnicalTrackingController(
        TechnicalWorkflowAppService technicalWorkflowAppService
    ) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "查询技术追踪病例列表", description = "按日期范围查询命中技术活动的病例列表。")
    @RequirePermission(M3PermissionCodes.TECHNICAL_TRACKING_QUERY)
    @GetMapping("/cases")
    public TechnicalTrackingCaseListPageResponse listCases(
        @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "开始日期，格式 YYYY-MM-DD") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @Parameter(description = "结束日期，格式 YYYY-MM-DD") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @Parameter(description = "工作日期，格式 YYYY-MM-DD") @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate
    ) {
        TechnicalWorkflowModels.TechnicalTrackingCaseListPage result =
            technicalWorkflowAppService.listTechnicalTrackingCases(
                new TechnicalWorkflowModels.TechnicalTrackingCaseListQuery(
                    page,
                    size,
                    dateFrom,
                    dateTo,
                    workDate
                )
            );
        return new TechnicalTrackingCaseListPageResponse(
            result.items().stream().map(item -> new TechnicalTrackingCaseListItemResponse(
                item.caseId(),
                item.pathologyNo(),
                item.patientName(),
                item.patientIdDisplay(),
                item.applicationNo(),
                item.applicationType(),
                item.submittingDepartmentName(),
                item.caseStatus(),
                item.latestActivityAt(),
                item.matchedActivityTypes())).toList(),
            result.page(),
            result.size(),
            result.total()
        );
    }
}
