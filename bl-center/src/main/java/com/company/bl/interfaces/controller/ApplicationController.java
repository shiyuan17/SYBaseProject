package com.company.bl.interfaces.controller;

import com.company.bl.application.query.GetApplicationByIdQuery;
import com.company.bl.application.service.CreateApplicationAppService;
import com.company.bl.application.service.GetApplicationAppService;
import com.company.bl.application.service.SpecimenWorkflowAppService;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.interfaces.assembler.ApplicationRepresentationAssembler;
import com.company.bl.interfaces.auth.M2PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateApplicationRequest;
import com.company.bl.interfaces.vo.ApplicationDetailResponse;
import com.company.bl.interfaces.vo.ApplicationIdResponse;
import com.company.bl.interfaces.vo.SpecimenSummaryResponse;
import com.company.bl.interfaces.vo.TrackingEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "临床送检", description = "病理申请单创建、详情查询与流程追踪接口")
public class ApplicationController {

    private final CreateApplicationAppService createApplicationAppService;
    private final GetApplicationAppService getApplicationAppService;
    private final SpecimenWorkflowAppService specimenWorkflowAppService;
    private final ApplicationRepresentationAssembler applicationRepresentationAssembler;

    @Operation(summary = "创建病理申请单", description = "创建新的病理申请单。")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "创建成功", useReturnTypeSchema = true))
    @PostMapping
    public ResponseEntity<ApplicationIdResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationIdResponse response = applicationRepresentationAssembler.toIdResponse(
            createApplicationAppService.create(applicationRepresentationAssembler.toCommand(request)));
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "查询申请单详情", description = "按申请单 ID 查询申请单、标本与最近追踪事件。")
    @GetMapping("/{id}")
    public ApplicationDetailResponse getById(@Parameter(description = "申请单 ID") @PathVariable("id") String id) {
        ApplicationTracking tracking = specimenWorkflowAppService.getApplicationTracking(id);
        return toDetailResponse(tracking);
    }

    @Operation(summary = "查询申请单追踪", description = "查询申请单当前节点、标本列表与追踪事件。")
    @RequirePermission(M2PermissionCodes.SPECIMEN_TRACKING_QUERY)
    @GetMapping("/{id}/tracking")
    public ApplicationDetailResponse getTracking(@Parameter(description = "申请单 ID") @PathVariable("id") String id) {
        return toDetailResponse(specimenWorkflowAppService.getApplicationTracking(id));
    }

    private ApplicationDetailResponse toDetailResponse(ApplicationTracking tracking) {
        return new ApplicationDetailResponse(
            tracking.application().getId().value(),
            tracking.application().getApplicationNo(),
            tracking.application().getPatientId(),
            tracking.application().getPatientName(),
            tracking.application().getPatientGender(),
            tracking.application().getPatientAge(),
            tracking.application().getApplicationType(),
            tracking.application().getStatus().name(),
            tracking.application().getApplicationFormStatus().name(),
            tracking.application().getExternalOrderNo(),
            tracking.application().getThirdPartySource(),
            tracking.application().getSourceHospitalId(),
            tracking.application().getSourceHospitalName(),
            tracking.application().getSubmittingDepartmentId(),
            tracking.application().getSubmittingDepartmentName(),
            tracking.application().getSubmittingDoctorUserId(),
            tracking.application().getSubmittingDoctorName(),
            tracking.application().getClinicalDiagnosis(),
            tracking.application().getClinicalSymptom(),
            tracking.application().getSpecimenSite(),
            stringify(tracking.application().getApplicationDate()),
            stringify(tracking.application().getSubmissionDate()),
            tracking.currentNode(),
            tracking.abnormal(),
            tracking.specimens().stream().map(this::toSpecimenSummary).toList(),
            tracking.events().stream().map(event -> new TrackingEventResponse(
                event.nodeCode(),
                event.eventType(),
                event.eventStatus(),
                stringify(event.eventTime()),
                event.operatorName(),
                event.sourceTerminal(),
                event.eventContent()))
                .toList(),
            tracking.application().getRemarks(),
            stringify(tracking.application().getCreatedAt()),
            stringify(tracking.application().getUpdatedAt()));
    }

    private SpecimenSummaryResponse toSpecimenSummary(Specimen specimen) {
        return new SpecimenSummaryResponse(
            specimen.id(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenNameStandardized(),
            specimen.specimenType(),
            specimen.specimenSite(),
            specimen.specimenCount(),
            specimen.specimenStatus().name(),
            specimen.fixationStatus().name(),
            specimen.labelPrintStatus());
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
