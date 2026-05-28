package com.company.bl.application.service;

import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ArchiveRepository;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.DiagnosticTrackingQueryRepository;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.domain.repository.ReportRevisionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class DiagnosticReportQueryService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final DiagnosticTrackingQueryRepository diagnosticTrackingQueryRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;

    DiagnosticReportQueryService(DiagnosticReportRepository diagnosticReportRepository,
                                 DiagnosticTrackingQueryRepository diagnosticTrackingQueryRepository,
                                 DiagnosticReportSupport diagnosticReportSupport) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.diagnosticTrackingQueryRepository = diagnosticTrackingQueryRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
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
        Map<String, ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchiveByObjectId = indexObjectArchives(aggregate.embeddingBoxArchives());
        Map<String, ArchiveRepository.ObjectArchiveSummary> slideArchiveByObjectId = indexObjectArchives(aggregate.slideArchives());
        return new DiagnosticReportViews.DiagnosticWorkbenchView(
            aggregate.caseId(),
            aggregate.applicationNo(),
            aggregate.pathologyNo(),
            aggregate.caseStatus(),
            aggregate.patientName(),
            aggregate.submittingDepartmentName(),
            aggregate.submittingDoctorName(),
            aggregate.clinicalDiagnosis(),
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
                stringify(item.signedAt()), stringify(item.createdAt()))).toList(),
            aggregate.events().stream().map(this::toTrackingEvent).toList(),
            aggregate.revisions().stream().map(this::toRevisionView).toList(),
            aggregate.medicalOrders().stream().map(this::toMedicalOrderView).toList(),
            aggregate.consultations().stream().map(this::toConsultationView).toList(),
            aggregate.latestEffectiveVersionNo(),
            aggregate.currentDraftVersionNo(),
            aggregate.hasPendingRevision());
    }

    private DiagnosticReportModels.TaskView toTaskView(DiagnosticReportRepository.DiagnosticTask task) {
        return new DiagnosticReportModels.TaskView(
            task.id(),
            task.applicationId(),
            task.applicationNo(),
            task.patientName(),
            task.caseId(),
            task.pathologyNo(),
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
            consultation.participants().size());
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
