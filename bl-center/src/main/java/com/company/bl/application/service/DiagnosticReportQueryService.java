package com.company.bl.application.service;

import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ArchiveRepository;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.DiagnosticTrackingQueryRepository;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.domain.repository.ReportRevisionRepository;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class DiagnosticReportQueryService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final DiagnosticTrackingQueryRepository diagnosticTrackingQueryRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;
    private final ArchiveRepository archiveRepository;
    private final DiagnosticCaseLifecycleAssembler diagnosticCaseLifecycleAssembler = new DiagnosticCaseLifecycleAssembler();

    DiagnosticReportQueryService(DiagnosticReportRepository diagnosticReportRepository,
                                 DiagnosticTrackingQueryRepository diagnosticTrackingQueryRepository,
                                 DiagnosticReportSupport diagnosticReportSupport,
                                 ArchiveRepository archiveRepository) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.diagnosticTrackingQueryRepository = diagnosticTrackingQueryRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
        this.archiveRepository = archiveRepository;
    }

    @Transactional(readOnly = true)
    DiagnosticReportModels.PendingDiagnosticTaskPage listPendingTasks(DiagnosticReportModels.PendingDiagnosticTaskQuery query) {
        DiagnosticReportRepository.PagedDiagnosticTasks paged = diagnosticReportRepository.findDiagnosticTasks(
            new DiagnosticReportRepository.PendingDiagnosticTaskQuery(
                query.page(),
                query.size(),
                query.taskType(),
                query.taskStatus(),
                query.pathologyNo(),
                query.currentUserId(),
                query.currentRoleCode()));
        return new DiagnosticReportModels.PendingDiagnosticTaskPage(
            paged.items().stream().map(this::toTaskView).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    DiagnosticReportViews.DiagnosticWorkbenchView getDiagnosticWorkbench(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate aggregate =
            diagnosticTrackingQueryRepository.getDiagnosticWorkbench(caseId);
        Map<String, com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxesByNo =
            aggregate.embeddingBoxes().stream()
                .collect(Collectors.toMap(
                    com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox::embeddingBoxNo,
                    Function.identity(),
                    (left, right) -> left));
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId =
            indexObjectArchives(aggregate.embeddingBoxArchives());
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId =
            indexObjectArchives(aggregate.slideArchives());
        return new DiagnosticReportViews.DiagnosticWorkbenchView(
            aggregate.caseId(),
            aggregate.applicationNo(),
            aggregate.pathologyNo(),
            aggregate.caseStatus(),
            aggregate.patientName(),
            aggregate.patientId(),
            aggregate.patientIdDisplay(),
            aggregate.patientGender(),
            aggregate.patientAge(),
            aggregate.applicationType(),
            aggregate.inpatientNo(),
            aggregate.outpatientNo(),
            aggregate.bedNo(),
            aggregate.phone(),
            aggregate.submittingDepartmentName(),
            aggregate.submittingDoctorName(),
            aggregate.clinicalDiagnosis(),
            aggregate.applicationRemarks(),
            archiveStatus(aggregate.applicationFormArchive()),
            archiveLocation(aggregate.applicationFormArchive()),
            archiveImageUrl(aggregate.applicationFormArchive()),
            aggregate.specimens().stream().map(item -> new DiagnosticReportViews.WorkbenchSpecimenSummary(
                item.id(), item.specimenNo(), item.barcode(), item.specimenNameStandardized(), item.specimenStatus().name())).toList(),
            aggregate.blocks().stream().map(item -> toBlockSummary(item, embeddingBoxesByNo, embeddingBoxArchiveByObjectId)).toList(),
            aggregate.slides().stream().map(item -> toSlideSummary(item, slideArchiveByObjectId)).toList(),
            aggregate.diagnosticTasks().stream().map(this::toTaskView).toList(),
            aggregate.currentReport() == null ? null : toReportView(aggregate.currentReport()),
            aggregate.recentEvents().stream().map(this::toTrackingEvent).toList(),
            aggregate.revisions().stream().map(this::toRevisionView).toList(),
            aggregate.medicalOrders().stream().map(this::toMedicalOrderView).toList(),
            aggregate.consultations().stream().map(this::toConsultationView).toList(),
            aggregate.historicalPathologies().stream().map(this::toHistoricalPathologyView).toList(),
            List.of(),
            buildReportTraces(aggregate),
            buildRemarkSections(aggregate),
            buildChargeItemViews(aggregate),
            aggregate.hasPendingRevision());
    }

    @Transactional(readOnly = true)
    DiagnosticReportViews.ReportTrackingView getReportTracking(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        DiagnosticTrackingQueryRepository.ReportTrackingAggregate aggregate =
            diagnosticTrackingQueryRepository.getReportTracking(caseId);
        return new DiagnosticReportViews.ReportTrackingView(
            aggregate.caseId(),
            aggregate.applicationNo(),
            aggregate.pathologyNo(),
            aggregate.caseStatus(),
            aggregate.patientName(),
            archiveStatus(aggregate.applicationFormArchive()),
            archiveLocation(aggregate.applicationFormArchive()),
            archiveImageUrl(aggregate.applicationFormArchive()),
            aggregate.diagnosticTasks().stream().map(this::toTaskView).toList(),
            aggregate.currentReport() == null ? null : toReportView(aggregate.currentReport()),
            aggregate.versions().stream().map(item -> new DiagnosticReportViews.ReportVersionView(
                item.id(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(),
                stringify(item.signedAt()), stringify(item.createdAt()), item.deliveryStatus(),
                stringify(item.issuedAt()), stringify(item.plannedIssueAt()))).toList(),
            aggregate.events().stream().map(this::toTrackingEvent).toList(),
            aggregate.revisions().stream().map(this::toRevisionView).toList(),
            aggregate.medicalOrders().stream().map(this::toMedicalOrderView).toList(),
            aggregate.consultations().stream().map(this::toConsultationView).toList(),
            aggregate.latestEffectiveVersionNo(),
            aggregate.currentDraftVersionNo(),
            aggregate.hasPendingRevision());
    }

    @Transactional(readOnly = true)
    DiagnosticReportViews.CaseLifecycleTrackingView getCaseLifecycleTracking(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate workbenchAggregate =
            diagnosticTrackingQueryRepository.getDiagnosticWorkbench(caseId);
        DiagnosticTrackingQueryRepository.ReportTrackingAggregate reportTrackingAggregate =
            diagnosticTrackingQueryRepository.getReportTracking(caseId);

        Map<String, ArchiveRepository.ObjectArchiveSummary> specimenArchiveByObjectId =
            indexObjectArchives(archiveRepository.findSpecimenArchiveSummaries(caseId));
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId =
            indexObjectArchives(workbenchAggregate.embeddingBoxArchives());
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId =
            indexObjectArchives(workbenchAggregate.slideArchives());
        Map<String, TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxesByNo =
            workbenchAggregate.embeddingBoxes().stream().collect(Collectors.toMap(
                TechnicalWorkflowRecords.EmbeddingBox::embeddingBoxNo,
                Function.identity(),
                (left, right) -> left));
        Map<String, List<TechnicalWorkflowRecords.SamplingBlock>> blocksBySpecimenId =
            workbenchAggregate.blocks().stream().collect(Collectors.groupingBy(
                TechnicalWorkflowRecords.SamplingBlock::specimenId));
        Map<String, List<TechnicalWorkflowProcessingRecords.Slide>> slidesByEmbeddingBoxId =
            workbenchAggregate.slides().stream()
                .filter(item -> item.embeddingBoxId() != null)
                .collect(Collectors.groupingBy(TechnicalWorkflowProcessingRecords.Slide::embeddingBoxId));

        DiagnosticCaseLifecycleAssembler.LifecycleContent lifecycleContent = diagnosticCaseLifecycleAssembler.build(
            workbenchAggregate,
            reportTrackingAggregate,
            specimenArchiveByObjectId,
            embeddingBoxArchiveByObjectId,
            slideArchiveByObjectId,
            embeddingBoxesByNo,
            blocksBySpecimenId,
            slidesByEmbeddingBoxId
        );

        return new DiagnosticReportViews.CaseLifecycleTrackingView(
            new DiagnosticReportViews.CaseSummaryView(
                workbenchAggregate.caseId(),
                workbenchAggregate.applicationNo(),
                workbenchAggregate.pathologyNo(),
                workbenchAggregate.caseStatus(),
                workbenchAggregate.patientName(),
                workbenchAggregate.patientGender(),
                workbenchAggregate.patientAge(),
                workbenchAggregate.applicationType(),
                workbenchAggregate.submittingDepartmentName(),
                workbenchAggregate.submittingDoctorName(),
                null,
                firstPresent(
                    reportTrackingAggregate.currentReport() == null ? null : reportTrackingAggregate.currentReport().reportStatus(),
                    workbenchAggregate.caseStatus()),
                workbenchAggregate.hasPendingRevision()),
            new DiagnosticReportViews.ApplicationFormView(
                archiveStatus(workbenchAggregate.applicationFormArchive()),
                archiveLocation(workbenchAggregate.applicationFormArchive()),
                archiveImageUrl(workbenchAggregate.applicationFormArchive()),
                workbenchAggregate.submittingDoctorName(),
                null,
                workbenchAggregate.applicationRemarks()),
            lifecycleContent.overallTimeline(),
            lifecycleContent.specimenViews(),
            new DiagnosticReportViews.ReportLifecycleView(
                reportTrackingAggregate.currentReport() == null ? null : toReportView(reportTrackingAggregate.currentReport()),
                reportTrackingAggregate.diagnosticTasks().stream().map(this::toTaskView).toList(),
                reportTrackingAggregate.versions().stream().map(item -> new DiagnosticReportViews.ReportVersionView(
                    item.id(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(),
                    stringify(item.signedAt()), stringify(item.createdAt()), item.deliveryStatus(),
                    stringify(item.issuedAt()), stringify(item.plannedIssueAt()))).toList(),
                reportTrackingAggregate.revisions().stream().map(this::toRevisionView).toList(),
                reportTrackingAggregate.consultations().stream().map(this::toConsultationView).toList(),
                reportTrackingAggregate.medicalOrders().stream().map(this::toMedicalOrderView).toList()));
    }

    @Transactional(readOnly = true)
    List<DiagnosticReportModels.FormalReportVersionView> listFormalReportVersions(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        return diagnosticReportRepository.findFormalReportVersionsByCaseId(caseId).stream()
            .map(item -> {
                DiagnosticReportRepository.PathologyReport report =
                    diagnosticReportRepository.findPathologyReportById(item.reportId()).orElse(null);
                return new DiagnosticReportModels.FormalReportVersionView(
                    item.id(),
                    item.reportId(),
                    report == null ? null : report.reportNo(),
                    item.versionNo(),
                    item.versionStatus(),
                    item.signedByName(),
                    stringify(item.signedAt()),
                    report == null ? null : stringify(report.publishedAt()),
                    item.printStatus(),
                    stringify(item.printedAt()),
                    item.deliveryStatus(),
                    stringify(item.plannedIssueAt()),
                    stringify(item.issuedAt()),
                    stringify(item.recalledAt()));
            })
            .toList();
    }

    @Transactional(readOnly = true)
    List<DiagnosticReportModels.CaseReportVersionView> listCaseReportVersions(String caseIdentifier) {
        String caseId = diagnosticReportSupport.resolveCaseIdentifier(caseIdentifier).id();
        Map<String, DiagnosticReportRepository.ReportVersion> formalVersionByReportAndStatus = diagnosticReportRepository
            .findFormalReportVersionsByCaseId(caseId)
            .stream()
            .collect(Collectors.toMap(
                item -> reportVersionKey(item.reportId(), item.versionStatus()),
                Function.identity(),
                (left, right) -> right));
        return diagnosticReportRepository.findPathologyReportsByCaseId(caseId).stream()
            .map(report -> {
                DiagnosticReportRepository.ReportVersion matchedVersion =
                    formalVersionByReportAndStatus.get(reportVersionKey(report.id(), report.reportStatus()));
                return new DiagnosticReportModels.CaseReportVersionView(
                    matchedVersion == null ? report.id() : matchedVersion.id(),
                    report.id(),
                    report.reportNo(),
                    report.versionNo(),
                    report.reportStatus(),
                    report.signedByName(),
                    stringify(report.submittedAt()),
                    stringify(report.reviewedAt()),
                    stringify(report.signedAt()),
                    stringify(report.publishedAt()),
                    matchedVersion == null ? null : matchedVersion.printStatus(),
                    matchedVersion == null ? null : stringify(matchedVersion.printedAt()),
                    matchedVersion == null ? null : matchedVersion.deliveryStatus(),
                    matchedVersion == null ? null : stringify(matchedVersion.plannedIssueAt()),
                    matchedVersion == null ? null : stringify(matchedVersion.issuedAt()),
                    matchedVersion == null ? null : stringify(matchedVersion.recalledAt()));
            })
            .toList();
    }

    private DiagnosticReportModels.TaskView toTaskView(DiagnosticReportRepository.DiagnosticTask task) {
        return new DiagnosticReportModels.TaskView(
            task.id(),
            task.applicationId(),
            task.applicationNo(),
            task.patientName(),
            task.patientId(),
            task.patientIdDisplay(),
            task.caseId(),
            task.pathologyNo(),
            task.applicationType(),
            task.checkItem(),
            task.blockCount(),
            task.submittingDepartmentName(),
            task.specimenName(),
            task.taskType(),
            task.status(),
            task.diagnosisDoctorUserId(),
            task.diagnosisDoctorName(),
            task.primaryDoctorUserId(),
            task.primaryDoctorName(),
            task.reviewerUserId(),
            task.reviewerName(),
            stringify(task.assignedAt()),
            stringify(task.acceptedAt()),
            stringify(task.completedAt()),
            task.remarks());
    }

    private DiagnosticReportViews.PathologyReportView toReportView(DiagnosticReportRepository.PathologyReport report) {
        return new DiagnosticReportViews.PathologyReportView(
            report.id(),
            report.reportNo(),
            report.reportStatus(),
            report.clinicalDiagnosis(),
            report.grossExam(),
            report.microscopicExam(),
            report.finalDiagnosis(),
            report.richTextContent(),
            stringify(report.submittedAt()),
            stringify(report.reviewedAt()),
            stringify(report.signedAt()),
            stringify(report.publishedAt()),
            report.reviewerName(),
            report.signedByName(),
            null,
            null,
            null,
            report.versionNo());
    }

    private DiagnosticReportViews.TrackingEventView toTrackingEvent(TrackingEvent event) {
        return new DiagnosticReportViews.TrackingEventView(
            event.nodeCode(),
            event.eventType(),
            event.eventStatus(),
            stringify(event.eventTime()),
            event.operatorName(),
            event.eventContent());
    }

    private DiagnosticReportViews.RevisionRequestView toRevisionView(ReportRevisionRepository.ReportRevisionRequest request) {
        return new DiagnosticReportViews.RevisionRequestView(
            request.id(),
            request.reportId(),
            request.currentVersionNo(),
            request.requestStatus(),
            request.requestReason(),
            request.requestedByName(),
            stringify(request.requestedAt()),
            request.reviewedByName(),
            stringify(request.reviewedAt()),
            request.rejectReason(),
            request.approvedVersionNo());
    }

    private DiagnosticReportViews.MedicalOrderView toMedicalOrderView(MedicalOrderRepository.MedicalOrder order) {
        return new DiagnosticReportViews.MedicalOrderView(
            order.id(),
            order.caseId(),
            order.pathologyNo(),
            order.applicationNo(),
            order.patientName(),
            order.orderNumber(),
            order.orderType(),
            order.orderContent(),
            order.orderItemId(),
            order.orderItemCode(),
            order.orderItemName(),
            order.orderCategoryId(),
            order.orderCategoryCode(),
            order.orderCategoryName(),
            order.executionScope(),
            order.billingStatus(),
            order.status(),
            order.doctorName(),
            order.executorName(),
            stringify(order.orderDate()),
            stringify(order.acceptedAt()),
            stringify(order.completedAt()),
            stringify(order.cancelledAt()),
            order.remarks());
    }

    private DiagnosticReportViews.ConsultationView toConsultationView(DiagnosticTrackingQueryRepository.ConsultationView consultation) {
        return new DiagnosticReportViews.ConsultationView(
            consultation.consultationCase().id(),
            consultation.consultationCase().consultationType(),
            consultation.consultationCase().status(),
            consultation.consultationCase().requestedByName(),
            stringify(consultation.consultationCase().requestedAt()),
            consultation.consultationCase().hostName(),
            stringify(consultation.consultationCase().completedAt()),
            consultation.consultationCase().opinion(),
            consultation.participants().size(),
            consultation.participants().stream()
                .map(item -> new DiagnosticReportViews.ConsultationParticipantView(
                    item.id(),
                    item.participantUserId(),
                    item.participantName(),
                    item.participantRole(),
                    item.opinion(),
                    item.draftedByName(),
                    stringify(item.commentedAt())))
                .toList());
    }

    private DiagnosticReportViews.HistoricalPathologyView toHistoricalPathologyView(
        DiagnosticTrackingQueryRepository.HistoricalPathology item
    ) {
        return new DiagnosticReportViews.HistoricalPathologyView(
            item.age(),
            item.inpatientNo(),
            item.examinationNo(),
            item.submissionType(),
            stringify(item.reportTime()),
            item.diagnosis());
    }

    private DiagnosticReportViews.ChargeItemView toChargeItemView(
        DiagnosticTrackingQueryRepository.ChargeItem item
    ) {
        return new DiagnosticReportViews.ChargeItemView(
            item.itemName(),
            stringify(item.chargedAt()),
            item.chargedByName());
    }

    private List<DiagnosticReportViews.ChargeItemView> buildChargeItemViews(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate aggregate
    ) {
        if (!aggregate.chargeItems().isEmpty()) {
            return aggregate.chargeItems().stream().map(this::toChargeItemView).toList();
        }
        return aggregate.medicalOrders().stream()
            .filter(order -> order.billingStatus() != null && !order.billingStatus().isBlank())
            .map(order -> new DiagnosticReportViews.ChargeItemView(
                firstPresent(order.orderContent(), order.orderNumber()),
                stringify(firstPresent(order.completedAt(), order.acceptedAt(), order.orderDate())),
                firstPresent(order.executorName(), order.doctorName())))
            .toList();
    }

    private List<DiagnosticReportViews.ReportTraceView> buildReportTraces(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate aggregate
    ) {
        List<DiagnosticReportViews.ReportTraceView> traces = new ArrayList<>();
        DiagnosticReportRepository.PathologyReport report = aggregate.currentReport();
        if (report != null) {
            traces.add(new DiagnosticReportViews.ReportTraceView(
                traces.size() + 1,
                firstPresent(report.signedByName(), report.reviewerName()),
                stringify(firstPresent(report.publishedAt(), report.signedAt(), report.reviewedAt(), report.submittedAt(), report.reportDate())),
                report.reportStatus(),
                report.finalDiagnosis()));
        }
        for (DiagnosticReportRepository.DiagnosticTask task : aggregate.diagnosticTasks()) {
            traces.add(new DiagnosticReportViews.ReportTraceView(
                traces.size() + 1,
                firstPresent(task.primaryDoctorName(), task.diagnosisDoctorName(), task.reviewerName()),
                stringify(firstPresent(task.completedAt(), task.reviewedAt(), task.primaryDiagnosedAt(), task.acceptedAt(), task.assignedAt())),
                task.status(),
                task.remarks()));
        }
        return traces;
    }

    private List<DiagnosticReportViews.RemarkSectionView> buildRemarkSections(
        DiagnosticTrackingQueryRepository.DiagnosticWorkbenchAggregate aggregate
    ) {
        List<DiagnosticReportViews.RemarkSectionView> sections = new ArrayList<>();
        sections.add(new DiagnosticReportViews.RemarkSectionView(
            "APPLICATION",
            "申请备注",
            aggregate.applicationNo(),
            aggregate.applicationRemarks()));
        sections.add(new DiagnosticReportViews.RemarkSectionView(
            "GROSSING",
            "取材备注",
            aggregate.pathologyNo(),
            aggregate.blocks().stream()
                .map(DiagnosticReportQueryService::firstNonBlankBlockRemark)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null)));
        sections.add(new DiagnosticReportViews.RemarkSectionView(
            "DIAGNOSIS",
            "诊断备注",
            aggregate.pathologyNo(),
            aggregate.currentReport() == null ? null : aggregate.currentReport().remarks()));
        aggregate.medicalOrders().stream()
            .filter(order -> order.remarks() != null && !order.remarks().isBlank())
            .forEach(order -> sections.add(new DiagnosticReportViews.RemarkSectionView(
                "MEDICAL_ORDER",
                "医嘱备注",
                order.orderNumber(),
                order.remarks())));
        return sections;
    }

    private static String firstNonBlankBlockRemark(TechnicalWorkflowRecords.SamplingBlock block) {
        if (block.specialRequirement() != null && !block.specialRequirement().isBlank()) {
            return block.specialRequirement();
        }
        return block.grossDescription();
    }

    @SafeVarargs
    private static <T> T firstPresent(T... values) {
        for (T value : values) {
            if (value instanceof String text && text.isBlank()) {
                continue;
            }
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private DiagnosticReportViews.WorkbenchBlockSummary toBlockSummary(
        com.company.bl.domain.repository.TechnicalWorkflowRecords.SamplingBlock block,
        Map<String, com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxesByNo,
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId
    ) {
        com.company.bl.domain.repository.TechnicalWorkflowRecords.EmbeddingBox embeddingBox = embeddingBoxesByNo.get(block.embeddingBoxNo());
        ArchiveRepository.ObjectArchiveSummary archiveSummary = embeddingBox == null ? null : embeddingBoxArchiveByObjectId.get(embeddingBox.id());
        return new DiagnosticReportViews.WorkbenchBlockSummary(
            block.id(),
            block.specimenId(),
            block.blockCode(),
            block.embeddingBoxNo(),
            block.blockDescription(),
            archiveStatus(archiveSummary),
            archiveLocation(archiveSummary),
            loanStatus(archiveSummary));
    }

    private DiagnosticReportViews.WorkbenchSlideSummary toSlideSummary(
        com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords.Slide slide,
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId
    ) {
        ArchiveRepository.ObjectArchiveSummary archiveSummary = slideArchiveByObjectId.get(slide.id());
        return new DiagnosticReportViews.WorkbenchSlideSummary(
            slide.id(),
            slide.specimenId(),
            slide.embeddingBoxId(),
            slide.slideNo(),
            slide.slideStatus(),
            slide.qualityStatus(),
            archiveStatus(archiveSummary),
            archiveLocation(archiveSummary),
            loanStatus(archiveSummary));
    }

    private String reportVersionKey(String reportId, String versionStatus) {
        return reportId + "::" + (versionStatus == null ? "" : versionStatus);
    }

    private Map<String, ArchiveRepository.ObjectArchiveSummary> indexObjectArchives(List<ArchiveRepository.ObjectArchiveSummary> archives) {
        return archives.stream().collect(Collectors.toMap(
            ArchiveRepository.ObjectArchiveSummary::objectId,
            Function.identity(),
            (left, right) -> left));
    }

    private String archiveStatus(ArchiveRepository.ApplicationArchiveSummary summary) {
        return summary == null ? null : summary.archiveStatus();
    }

    private String archiveLocation(ArchiveRepository.ApplicationArchiveSummary summary) {
        return summary == null ? null : summary.archiveLocation();
    }

    private String archiveImageUrl(ArchiveRepository.ApplicationArchiveSummary summary) {
        return summary == null ? null : summary.imageUrl();
    }

    private String archiveStatus(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.archiveStatus();
    }

    private String archiveLocation(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.archiveLocation();
    }

    private String loanStatus(ArchiveRepository.ObjectArchiveSummary summary) {
        return summary == null ? null : summary.loanStatus();
    }

    private String stringify(LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
