package com.company.bl.interfaces.controller;

import com.company.bl.application.service.TechnicalWorkflowAppService;
import com.company.bl.application.service.TechnicalWorkflowModels;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.TechnicalSpecimenRegistrationCompleteRequest;
import com.company.bl.interfaces.vo.PendingTechnicalSpecimenRegistrationPageResponse;
import com.company.bl.interfaces.vo.PendingTechnicalSpecimenRegistrationResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationCheckItemResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationCompleteResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationDetailResponse;
import com.company.bl.interfaces.vo.TechnicalSpecimenRegistrationMaterialResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/technical-specimen-registrations")
@Tag(name = "技术流程", description = "接收后技术标本登记接口")
public class TechnicalSpecimenRegistrationController extends TechnicalControllerSupport {

    private final TechnicalWorkflowAppService technicalWorkflowAppService;

    public TechnicalSpecimenRegistrationController(TechnicalWorkflowAppService technicalWorkflowAppService) {
        this.technicalWorkflowAppService = technicalWorkflowAppService;
    }

    @Operation(summary = "查询待技术登记病例", description = "分页查询病理接收后待进入技术登记环节的病例。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @GetMapping("/pending")
    public PendingTechnicalSpecimenRegistrationPageResponse listPending(
        @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size,
        @Parameter(description = "病人 ID、病理号、姓名、住院号关键字") @RequestParam(required = false) String keyword
    ) {
        TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationPage result =
            technicalWorkflowAppService.listPendingTechnicalSpecimenRegistrations(
                new TechnicalWorkflowModels.PendingTechnicalSpecimenRegistrationQuery(page, size, keyword));
        return new PendingTechnicalSpecimenRegistrationPageResponse(
            result.items().stream().map(item -> new PendingTechnicalSpecimenRegistrationResponse(
                item.caseId(),
                item.applicationId(),
                item.applicationNo(),
                item.pathologyNo(),
                item.patientName(),
                item.patientId(),
                item.inpatientNo(),
                item.applicationType(),
                item.submittingDepartmentName(),
                item.checkItem(),
                item.registeredByName(),
                item.registrationStatus(),
                item.receivedAt(),
                item.registeredAt())).toList(),
            result.page(),
            result.size(),
            result.total());
    }

    @Operation(summary = "查询技术标本登记详情", description = "查询右侧送检材料、临床诊断和检查项目详情。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @GetMapping("/{caseId}")
    public TechnicalSpecimenRegistrationDetailResponse detail(@PathVariable String caseId) {
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationDetail detail =
            technicalWorkflowAppService.getTechnicalSpecimenRegistrationDetail(caseId);
        return new TechnicalSpecimenRegistrationDetailResponse(
            detail.caseId(),
            detail.applicationId(),
            detail.applicationNo(),
            detail.pathologyNo(),
            detail.patientName(),
            detail.patientId(),
            detail.inpatientNo(),
            detail.applicationType(),
            detail.submittingDepartmentName(),
            detail.clinicalDiagnosis(),
            detail.registrationStatus(),
            detail.registeredByName(),
            detail.registeredAt(),
            detail.registrationRemarks(),
            detail.receivedAt(),
            detail.materials().stream().map(item -> new TechnicalSpecimenRegistrationMaterialResponse(
                item.sequenceNo(),
                item.specimenType(),
                item.specimenName(),
                item.sourcePart())).toList(),
            detail.checkItems().stream().map(item -> new TechnicalSpecimenRegistrationCheckItemResponse(
                item.sequenceNo(),
                item.name())).toList());
    }

    @Operation(summary = "完成技术标本登记", description = "完成登记并生成病例取材任务。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_RECEIVE)
    @PostMapping("/{caseId}/complete")
    public TechnicalSpecimenRegistrationCompleteResponse complete(@PathVariable String caseId,
                                                                 @Valid @RequestBody TechnicalSpecimenRegistrationCompleteRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        TechnicalWorkflowModels.TechnicalSpecimenRegistrationCompleteResult result =
            technicalWorkflowAppService.completeTechnicalSpecimenRegistration(
                new TechnicalWorkflowModels.CompleteTechnicalSpecimenRegistrationCommand(
                    caseId,
                    resolveUserId(httpServletRequest),
                    resolveOperatorName(httpServletRequest),
                    request.getTerminalCode(),
                    request.getRemarks()));
        return new TechnicalSpecimenRegistrationCompleteResponse(
            result.caseId(),
            result.pathologyNo(),
            result.registrationStatus(),
            result.grossingTaskCreated());
    }
}
