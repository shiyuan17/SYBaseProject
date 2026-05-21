package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CommentConsultationParticipantRequest;
import com.company.bl.interfaces.dto.CompleteConsultationRequest;
import com.company.bl.interfaces.dto.CreateConsultationRequest;
import com.company.bl.interfaces.vo.ConsultationOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consultations")
@Tag(name = "医生流程", description = "科内会诊闭环接口")
public class ConsultationController extends TechnicalControllerSupport {

    private final DiagnosticReportAppService diagnosticReportAppService;

    public ConsultationController(DiagnosticReportAppService diagnosticReportAppService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
    }

    @Operation(summary = "发起科内会诊", description = "由诊断医生发起院内会诊并带入参与人。")
    @RequirePermission(M4PermissionCodes.CONSULTATION_CREATE)
    @PostMapping
    public ConsultationOperationResponse create(@Valid @RequestBody CreateConsultationRequest request,
                                                HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.ConsultationResult result = diagnosticReportAppService.createConsultation(
            new DiagnosticReportModels.CreateConsultationCommand(
                request.getCaseId(),
                request.getParticipants().stream().map(item -> new DiagnosticReportModels.ConsultationParticipantInput(
                    item.getParticipantUserId(),
                    item.getParticipantName(),
                    item.getParticipantRole())).toList(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new ConsultationOperationResponse(result.consultationId(), result.caseId(), result.status());
    }

    @Operation(summary = "录入会诊意见", description = "仅被邀请参与人可录入会诊意见。")
    @RequirePermission(M4PermissionCodes.CONSULTATION_COMMENT)
    @PostMapping("/{id}/participants/{participantId}/comment")
    public ConsultationOperationResponse comment(@PathVariable("id") String consultationId,
                                                 @PathVariable("participantId") String participantId,
                                                 @Valid @RequestBody CommentConsultationParticipantRequest request,
                                                 HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.ConsultationResult result = diagnosticReportAppService.commentConsultationParticipant(
            new DiagnosticReportModels.CommentConsultationParticipantCommand(
                consultationId,
                participantId,
                request.getOpinion(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new ConsultationOperationResponse(result.consultationId(), result.caseId(), result.status());
    }

    @Operation(summary = "完成科内会诊", description = "仅主持人可完成科内会诊。")
    @RequirePermission(M4PermissionCodes.CONSULTATION_COMPLETE)
    @PostMapping("/{id}/complete")
    public ConsultationOperationResponse complete(@PathVariable("id") String consultationId,
                                                  @Valid @RequestBody CompleteConsultationRequest request,
                                                  HttpServletRequest httpServletRequest) {
        DiagnosticReportModels.ConsultationResult result = diagnosticReportAppService.completeConsultation(
            new DiagnosticReportModels.CompleteConsultationCommand(
                consultationId,
                request.getOpinion(),
                resolveUserId(request.getOperatorUserId(), httpServletRequest),
                request.getOperatorName(),
                request.getTerminalCode(),
                request.getRemarks()));
        return new ConsultationOperationResponse(result.consultationId(), result.caseId(), result.status());
    }
}
