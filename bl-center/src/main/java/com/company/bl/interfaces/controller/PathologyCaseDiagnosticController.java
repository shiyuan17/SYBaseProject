package com.company.bl.interfaces.controller;

import com.company.bl.application.service.DiagnosticReportAppService;
import com.company.bl.application.service.DiagnosticReportModels;
import com.company.bl.application.service.DiagnosticReportViews;
import com.company.bl.interfaces.auth.M4PermissionCodes;
import com.company.bl.interfaces.auth.RequirePermission;
import com.company.bl.interfaces.dto.CreateMedicalOrderBlockRequest;
import com.company.bl.interfaces.vo.CaseReportVersionListItemResponse;
import com.company.bl.interfaces.vo.CaseLifecycleTrackingResponse;
import com.company.bl.interfaces.vo.DiagnosticWorkbenchResponse;
import com.company.bl.interfaces.vo.FormalReportVersionListItemResponse;
import com.company.bl.interfaces.vo.MedicalOrderBlockResponse;
import com.company.bl.interfaces.vo.PendingDiagnosticTaskResponse;
import com.company.bl.interfaces.vo.ReportTrackingResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pathology-cases")
@Tag(name = "医生流程", description = "病例级诊断工作台与报告追踪接口")
public class PathologyCaseDiagnosticController extends TechnicalControllerSupport {

    private final DiagnosticReportAppService diagnosticReportAppService;

    public PathologyCaseDiagnosticController(DiagnosticReportAppService diagnosticReportAppService) {
        this.diagnosticReportAppService = diagnosticReportAppService;
    }

    @Operation(summary = "查询病例诊断工作台", description = "按病例 ID 或病理号查询诊断工作台聚合信息。")
    @RequirePermission(M4PermissionCodes.WORKBENCH_QUERY)
    @GetMapping("/{id}/diagnostic-workbench")
    public DiagnosticWorkbenchResponse getDiagnosticWorkbench(
        @Parameter(description = "病例 ID 或病理号") @PathVariable("id") String caseIdentifier
    ) {
        DiagnosticReportViews.DiagnosticWorkbenchView result =
            diagnosticReportAppService.getDiagnosticWorkbench(caseIdentifier);
        return new DiagnosticWorkbenchResponse(
            result.caseId(),
            result.applicationNo(),
            result.pathologyNo(),
            result.caseStatus(),
            result.patientName(),
            result.patientId(),
            result.patientIdDisplay(),
            result.patientGender(),
            result.patientAge(),
            result.applicationType(),
            result.inpatientNo(),
            result.outpatientNo(),
            result.bedNo(),
            result.phone(),
            result.submittingDepartmentName(),
            result.submittingDoctorName(),
            result.clinicalDiagnosis(),
            result.applicationRemarks(),
            result.applicationFormArchiveStatus(),
            result.applicationFormArchiveLocation(),
            result.applicationFormImageUrl(),
            result.specimens().stream().map(item -> new DiagnosticWorkbenchResponse.SpecimenSummary(
                item.specimenId(), item.specimenNo(), item.barcode(), item.specimenName(), item.specimenStatus())).toList(),
            result.blocks().stream().map(item -> new DiagnosticWorkbenchResponse.BlockSummary(
                item.blockId(), item.specimenId(), item.blockCode(), item.embeddingBoxNo(), item.description(),
                item.archiveStatus(), item.archiveLocation(), item.loanStatus())).toList(),
            result.medicalOrderBlocks().stream().map(item -> new DiagnosticWorkbenchResponse.MedicalOrderBlockSummary(
                item.medicalOrderBlockId(), item.blockNo())).toList(),
            result.slides().stream().map(item -> new DiagnosticWorkbenchResponse.SlideSummary(
                item.slideId(), item.specimenId(), item.embeddingBoxId(), item.slideNo(), item.slideStatus(), item.qualityStatus(),
                item.archiveStatus(), item.archiveLocation(), item.loanStatus())).toList(),
            result.diagnosticTasks().stream().map(this::toTaskResponse).toList(),
            toCurrentReport(result.currentReport()),
            result.recentEvents().stream().map(item -> new DiagnosticWorkbenchResponse.EventSummary(
                item.nodeCode(), item.eventType(), item.eventStatus(), item.eventTime(), item.operatorName(), item.eventContent())).toList(),
            result.revisions().stream().map(this::toRevisionSummary).toList(),
            result.medicalOrders().stream().map(this::toMedicalOrderSummary).toList(),
            result.consultations().stream().map(this::toConsultationSummary).toList(),
            result.historicalPathologies().stream().map(this::toHistoricalPathologySummary).toList(),
            result.pacsExaminations().stream().map(this::toPacsExaminationSummary).toList(),
            result.reportTraces().stream().map(this::toReportTraceSummary).toList(),
            result.remarkSections().stream().map(this::toRemarkSectionSummary).toList(),
            result.chargeItems().stream().map(this::toChargeItemSummary).toList(),
            result.hasPendingRevision());
    }

    @Operation(summary = "创建病例医嘱区专用蜡块", description = "为诊断工作站医嘱区创建可持久化复用的蜡块号。")
    @RequirePermission(M4PermissionCodes.MEDICAL_ORDER_CREATE)
    @PostMapping("/{id}/medical-order-blocks")
    public MedicalOrderBlockResponse createMedicalOrderBlock(
        @Parameter(description = "病例 ID 或病理号") @PathVariable("id") String caseIdentifier,
        @Valid @RequestBody CreateMedicalOrderBlockRequest request,
        HttpServletRequest servletRequest
    ) {
        DiagnosticReportModels.MedicalOrderBlockResult result = diagnosticReportAppService.createMedicalOrderBlock(
            new DiagnosticReportModels.CreateMedicalOrderBlockCommand(
                caseIdentifier,
                request.getBlockNo(),
                resolveUserId(servletRequest),
                resolveOperatorName(servletRequest),
                null));
        return new MedicalOrderBlockResponse(result.medicalOrderBlockId(), result.blockNo());
    }

    @Operation(summary = "查询病例报告追踪", description = "按病例 ID 或病理号查询诊断任务、报告状态、版本摘要和关键时间线。")
    @RequirePermission(M4PermissionCodes.REPORT_TRACKING_QUERY)
    @GetMapping("/{id}/report-tracking")
    public ReportTrackingResponse getReportTracking(
        @Parameter(description = "病例 ID 或病理号") @PathVariable("id") String caseIdentifier
    ) {
        DiagnosticReportViews.ReportTrackingView result =
            diagnosticReportAppService.getReportTracking(caseIdentifier);
        return new ReportTrackingResponse(
            result.caseId(),
            result.applicationNo(),
            result.pathologyNo(),
            result.caseStatus(),
            result.patientName(),
            result.applicationFormArchiveStatus(),
            result.applicationFormArchiveLocation(),
            result.applicationFormImageUrl(),
            result.diagnosticTasks().stream().map(this::toTaskResponse).toList(),
            toCurrentReport(result.currentReport()),
            result.versions().stream().map(item -> new ReportTrackingResponse.ReportVersionSummary(
                item.versionId(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(), item.signedAt(), item.createdAt())).toList(),
            result.events().stream().map(item -> new DiagnosticWorkbenchResponse.EventSummary(
                item.nodeCode(), item.eventType(), item.eventStatus(), item.eventTime(), item.operatorName(), item.eventContent())).toList(),
            result.revisions().stream().map(this::toRevisionSummary).toList(),
            result.medicalOrders().stream().map(this::toMedicalOrderSummary).toList(),
            result.consultations().stream().map(this::toConsultationSummary).toList(),
            result.latestEffectiveVersionNo(),
            result.currentDraftVersionNo(),
            result.hasPendingRevision());
    }

    @Operation(summary = "查询病例全生命周期追踪", description = "按病例 ID 或病理号查询申请、标本、技术处理、报告与归档借阅的完整链路。")
    @RequirePermission(M4PermissionCodes.REPORT_TRACKING_QUERY)
    @GetMapping("/{id}/lifecycle-tracking")
    public CaseLifecycleTrackingResponse getCaseLifecycleTracking(
        @Parameter(description = "病例 ID 或病理号") @PathVariable("id") String caseIdentifier
    ) {
        DiagnosticReportViews.CaseLifecycleTrackingView result =
            diagnosticReportAppService.getCaseLifecycleTracking(caseIdentifier);
        return new CaseLifecycleTrackingResponse(
            new CaseLifecycleTrackingResponse.CaseSummary(
                result.caseSummary().caseId(),
                result.caseSummary().applicationNo(),
                result.caseSummary().pathologyNo(),
                result.caseSummary().caseStatus(),
                result.caseSummary().patientName(),
                result.caseSummary().patientGender(),
                result.caseSummary().patientAge(),
                result.caseSummary().applicationType(),
                result.caseSummary().submittingDepartmentName(),
                result.caseSummary().submittingDoctorName(),
                result.caseSummary().applicationDate(),
                result.caseSummary().currentStage(),
                result.caseSummary().hasPendingRevision()),
            new CaseLifecycleTrackingResponse.ApplicationForm(
                result.applicationForm().archiveStatus(),
                result.applicationForm().archiveLocation(),
                result.applicationForm().imageUrl(),
                result.applicationForm().applicantDoctorName(),
                result.applicationForm().applicationDate(),
                result.applicationForm().remarks()),
            result.overallTimeline().stream().map(this::toLifecycleStageGroup).toList(),
            result.specimens().stream().map(this::toLifecycleSpecimen).toList(),
            new CaseLifecycleTrackingResponse.ReportLifecycle(
                toCurrentReport(result.reportLifecycle().currentReport()),
                result.reportLifecycle().diagnosticTasks().stream().map(this::toTaskResponse).toList(),
                result.reportLifecycle().versions().stream().map(item -> new ReportTrackingResponse.ReportVersionSummary(
                    item.versionId(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(), item.signedAt(), item.createdAt())).toList(),
                result.reportLifecycle().revisions().stream().map(this::toRevisionSummary).toList(),
                result.reportLifecycle().consultations().stream().map(this::toConsultationSummary).toList(),
                result.reportLifecycle().medicalOrders().stream().map(this::toMedicalOrderSummary).toList()));
    }

    @Operation(summary = "查询病例正式报告列表", description = "按病例 ID 或病理号查询当前病例已签发/已发布的正式报告版本列表。")
    @RequirePermission(M4PermissionCodes.REPORT_PUBLISH)
    @GetMapping("/{id}/formal-report-versions")
    public List<FormalReportVersionListItemResponse> listFormalReportVersions(
        @Parameter(description = "病例 ID 或病理号") @PathVariable("id") String caseIdentifier
    ) {
        return diagnosticReportAppService.listFormalReportVersions(caseIdentifier).stream()
            .map(item -> new FormalReportVersionListItemResponse(
                item.versionId(),
                item.reportId(),
                item.reportNo(),
                item.versionNo(),
                item.versionStatus(),
                item.signedByName(),
                item.signedAt(),
                item.publishedAt(),
                item.printStatus(),
                item.printedAt(),
                item.deliveryStatus(),
                item.plannedIssueAt(),
                item.issuedAt(),
                item.recalledAt()))
            .toList();
    }

    @Operation(summary = "查询病例报告版本列表", description = "按病例 ID 或病理号查询当前病例下的全状态报告版本列表。")
    @RequirePermission(M4PermissionCodes.REPORT_REVIEW)
    @GetMapping("/{id}/report-versions")
    public List<CaseReportVersionListItemResponse> listCaseReportVersions(
        @Parameter(description = "病例 ID 或病理号") @PathVariable("id") String caseIdentifier
    ) {
        return diagnosticReportAppService.listCaseReportVersions(caseIdentifier).stream()
            .map(item -> new CaseReportVersionListItemResponse(
                item.versionId(),
                item.reportId(),
                item.reportNo(),
                item.versionNo(),
                item.versionStatus(),
                item.signedByName(),
                item.submittedAt(),
                item.reviewedAt(),
                item.signedAt(),
                item.publishedAt(),
                item.printStatus(),
                item.printedAt(),
                item.deliveryStatus(),
                item.plannedIssueAt(),
                item.issuedAt(),
                item.recalledAt()))
            .toList();
    }

    private PendingDiagnosticTaskResponse toTaskResponse(DiagnosticReportModels.TaskView item) {
        return new PendingDiagnosticTaskResponse(
            item.id(),
            item.applicationId(),
            item.applicationNo(),
            item.patientName(),
            item.patientId(),
            item.patientIdDisplay(),
            item.caseId(),
            item.pathologyNo(),
            item.applicationType(),
            item.checkItem(),
            item.blockCount(),
            item.submittingDepartmentName(),
            item.specimenName(),
            item.taskType(),
            item.taskStatus(),
            item.diagnosisDoctorUserId(),
            item.diagnosisDoctorName(),
            item.primaryDoctorUserId(),
            item.primaryDoctorName(),
            item.reviewerUserId(),
            item.reviewerName(),
            item.assignedAt(),
            item.acceptedAt(),
            item.completedAt(),
            item.remarks());
    }

    private DiagnosticWorkbenchResponse.CurrentReportSummary toCurrentReport(DiagnosticReportViews.PathologyReportView item) {
        if (item == null) {
            return null;
        }
        return new DiagnosticWorkbenchResponse.CurrentReportSummary(
            item.reportId(),
            item.reportNo(),
            item.reportStatus(),
            item.clinicalDiagnosis(),
            item.grossExam(),
            item.microscopicExam(),
            item.finalDiagnosis(),
            item.richTextContent(),
            item.submittedAt(),
            item.reviewedAt(),
            item.signedAt(),
            item.publishedAt(),
            item.reviewerName(),
            item.signedByName(),
            item.versionNo());
    }

    private DiagnosticWorkbenchResponse.RevisionRequestSummary toRevisionSummary(DiagnosticReportViews.RevisionRequestView item) {
        return new DiagnosticWorkbenchResponse.RevisionRequestSummary(
            item.requestId(),
            item.reportId(),
            item.currentVersionNo(),
            item.requestStatus(),
            item.requestReason(),
            item.requestedByName(),
            item.requestedAt(),
            item.reviewedByName(),
            item.reviewedAt(),
            item.rejectReason(),
            item.approvedVersionNo());
    }

    private DiagnosticWorkbenchResponse.MedicalOrderSummary toMedicalOrderSummary(DiagnosticReportViews.MedicalOrderView item) {
        return new DiagnosticWorkbenchResponse.MedicalOrderSummary(
            item.orderId(),
            item.caseId(),
            item.pathologyNo(),
            item.applicationNo(),
            item.patientName(),
            item.patientId(),
            item.patientIdDisplay(),
            item.orderNumber(),
            item.orderType(),
            item.orderContent(),
            item.orderItemId(),
            item.orderItemCode(),
            item.orderItemName(),
            item.orderCategoryId(),
            item.orderCategoryCode(),
            item.orderCategoryName(),
            item.executionScope(),
            item.billingStatus(),
            item.status(),
            item.doctorName(),
            item.executorName(),
            item.orderDate(),
            item.acceptedAt(),
            item.printedAt(),
            item.printedByName(),
            item.releasedAt(),
            item.releasedByName(),
            item.completedAt(),
            item.cancelledAt(),
            item.terminatedAt(),
            item.terminatedByName(),
            item.terminationReasonCode(),
            item.terminationReasonLabel(),
            item.terminationRemarks(),
            item.remarks(),
            item.targetType(),
            item.targetSpecimenId(),
            item.targetSpecimenNo(),
            item.targetBlockId(),
            item.targetBlockNo(),
            item.targetSlideId(),
            item.targetSlideNo(),
            item.specimenNo(),
            item.blockNo(),
            item.slideNo());
    }

    private DiagnosticWorkbenchResponse.ConsultationSummary toConsultationSummary(DiagnosticReportViews.ConsultationView item) {
        return new DiagnosticWorkbenchResponse.ConsultationSummary(
            item.consultationId(),
            item.consultationType(),
            item.status(),
            item.requestedByName(),
            item.requestedAt(),
            item.hostName(),
            item.completedAt(),
            item.opinion(),
            item.participantCount(),
            item.participants().stream().map(participant -> new DiagnosticWorkbenchResponse.ConsultationParticipantSummary(
                participant.participantId(),
                participant.participantUserId(),
                participant.participantName(),
                participant.participantRole(),
                participant.opinion(),
                participant.draftedByName(),
                participant.commentedAt())).toList());
    }

    private DiagnosticWorkbenchResponse.HistoricalPathologySummary toHistoricalPathologySummary(
        DiagnosticReportViews.HistoricalPathologyView item
    ) {
        return new DiagnosticWorkbenchResponse.HistoricalPathologySummary(
            item.age(),
            item.inpatientNo(),
            item.examinationNo(),
            item.submissionType(),
            item.reportTime(),
            item.diagnosis());
    }

    private DiagnosticWorkbenchResponse.PacsExaminationSummary toPacsExaminationSummary(
        DiagnosticReportViews.PacsExaminationView item
    ) {
        return new DiagnosticWorkbenchResponse.PacsExaminationSummary(
            item.submissionType(),
            item.imagingDiagnosis(),
            item.reportTime(),
            item.examinationNo(),
            item.imagingDescription(),
            item.reportStatus());
    }

    private DiagnosticWorkbenchResponse.ReportTraceSummary toReportTraceSummary(
        DiagnosticReportViews.ReportTraceView item
    ) {
        return new DiagnosticWorkbenchResponse.ReportTraceSummary(
            item.sequenceNo(),
            item.reportDoctorName(),
            item.reportTime(),
            item.reportStatus(),
            item.diagnosisInfo());
    }

    private DiagnosticWorkbenchResponse.RemarkSectionSummary toRemarkSectionSummary(
        DiagnosticReportViews.RemarkSectionView item
    ) {
        return new DiagnosticWorkbenchResponse.RemarkSectionSummary(
            item.sectionKey(),
            item.title(),
            item.relatedNo(),
            item.content());
    }

    private DiagnosticWorkbenchResponse.ChargeItemSummary toChargeItemSummary(
        DiagnosticReportViews.ChargeItemView item
    ) {
        return new DiagnosticWorkbenchResponse.ChargeItemSummary(
            item.itemName(),
            item.chargedAt(),
            item.chargedByName());
    }

    private CaseLifecycleTrackingResponse.StageGroup toLifecycleStageGroup(
        DiagnosticReportViews.LifecycleStageGroupView item
    ) {
        return new CaseLifecycleTrackingResponse.StageGroup(
            item.stageCode(),
            item.stageTitle(),
            item.nodes().stream().map(this::toLifecycleNode).toList());
    }

    private CaseLifecycleTrackingResponse.LifecycleNode toLifecycleNode(
        DiagnosticReportViews.LifecycleNodeView item
    ) {
        return new CaseLifecycleTrackingResponse.LifecycleNode(
            item.stageCode(),
            item.nodeCode(),
            item.title(),
            item.status(),
            item.occurredAt(),
            item.operatorName(),
            item.operatorIp(),
            item.operatorDevice(),
            item.keyFacts().stream()
                .map(fact -> new CaseLifecycleTrackingResponse.KeyFact(fact.label(), fact.value()))
                .toList(),
            item.eventContent());
    }

    private CaseLifecycleTrackingResponse.SpecimenItem toLifecycleSpecimen(
        DiagnosticReportViews.LifecycleSpecimenView item
    ) {
        return new CaseLifecycleTrackingResponse.SpecimenItem(
            item.specimenId(),
            item.specimenNo(),
            item.barcode(),
            item.specimenName(),
            item.specimenStatus(),
            item.archiveStatus(),
            item.archiveLocation(),
            item.loanStatus(),
            item.createdAt(),
            item.removalAt(),
            item.fixedAt(),
            item.confirmedAt(),
            item.checkedInAt(),
            item.receiptStatus(),
            item.receivedAt(),
            item.contentDescribedByName(),
            item.specimenEvents().stream().map(this::toLifecycleNode).toList(),
            item.blocks().stream().map(this::toLifecycleBlock).toList());
    }

    private CaseLifecycleTrackingResponse.BlockItem toLifecycleBlock(
        DiagnosticReportViews.LifecycleBlockView item
    ) {
        return new CaseLifecycleTrackingResponse.BlockItem(
            item.blockId(),
            item.specimenId(),
            item.blockCode(),
            item.embeddingBoxNo(),
            item.description(),
            item.specimenName(),
            item.grossDescription(),
            item.archiveStatus(),
            item.archiveLocation(),
            item.loanStatus(),
            item.sampledByName(),
            item.sampledAt(),
            item.embeddedByName(),
            item.embeddingStartedAt(),
            item.embeddingEndedAt(),
            item.sliceNotice(),
            item.evaluationLevel(),
            item.samplingEvaluation(),
            item.embeddingRemarks(),
            item.blockEvents().stream().map(this::toLifecycleNode).toList(),
            item.slides().stream().map(this::toLifecycleSlide).toList());
    }

    private CaseLifecycleTrackingResponse.SlideItem toLifecycleSlide(
        DiagnosticReportViews.LifecycleSlideView item
    ) {
        return new CaseLifecycleTrackingResponse.SlideItem(
            item.slideId(),
            item.specimenId(),
            item.embeddingBoxId(),
            item.slideNo(),
            item.slideStatus(),
            item.qualityStatus(),
            item.archiveStatus(),
            item.archiveLocation(),
            item.loanStatus(),
            item.printedAt(),
            item.slicedAt(),
            item.slicedByName(),
            item.stainedAt(),
            item.stainedByName(),
            item.qcResult(),
            item.qcEvaluatedAt(),
            item.qcEvaluatorName(),
            item.reworkStatus(),
            item.reworkReason(),
            item.slideEvents().stream().map(this::toLifecycleNode).toList());
    }

}
