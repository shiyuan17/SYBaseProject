package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.ArchiveRepository;
import com.company.bl.domain.repository.ConsultationRepository;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.DiagnosticTrackingQueryRepository;
import com.company.bl.domain.repository.MedicalOrderRepository;
import com.company.bl.domain.repository.ReportRevisionRepository;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class JdbcDiagnosticTrackingQueryRepository implements DiagnosticTrackingQueryRepository {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final ReportRevisionRepository reportRevisionRepository;
    private final MedicalOrderRepository medicalOrderRepository;
    private final ConsultationRepository consultationRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final ApplicationRepository applicationRepository;
    private final ArchiveRepository archiveRepository;

    public JdbcDiagnosticTrackingQueryRepository(DiagnosticReportRepository diagnosticReportRepository,
                                                 ReportRevisionRepository reportRevisionRepository,
                                                 MedicalOrderRepository medicalOrderRepository,
                                                 ConsultationRepository consultationRepository,
                                                 TechnicalWorkflowRepository technicalWorkflowRepository,
                                                 ApplicationRepository applicationRepository,
                                                 ArchiveRepository archiveRepository) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.reportRevisionRepository = reportRevisionRepository;
        this.medicalOrderRepository = medicalOrderRepository;
        this.consultationRepository = consultationRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.applicationRepository = applicationRepository;
        this.archiveRepository = archiveRepository;
    }

    @Override
    public DiagnosticWorkbenchAggregate getDiagnosticWorkbench(String caseId) {
        PathologyCase pathologyCase = technicalWorkflowRepository.findPathologyCaseById(caseId).orElseThrow();
        Application application = applicationRepository.findById(new ApplicationId(pathologyCase.applicationId())).orElseThrow();
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        List<TechnicalWorkflowRecords.SamplingBlock> blocks = technicalWorkflowRepository.findSamplingBlocksByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.Slide> slides = technicalWorkflowRepository.findSlidesByCaseId(caseId);
        List<TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxes = technicalWorkflowRepository.findEmbeddingBoxesByCaseId(caseId);
        List<TrackingEvent> recentEvents = technicalWorkflowRepository.findRecentTrackingEventsByCaseId(caseId, 10);
        List<DiagnosticReportRepository.DiagnosticTask> tasks = diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId);
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(caseId, "ROUTINE")
            .orElse(null);
        List<ReportRevisionRepository.ReportRevisionRequest> revisions = reportRevisionRepository.findRevisionRequestsByCaseId(caseId);
        List<MedicalOrderRepository.MedicalOrder> medicalOrders = medicalOrderRepository.findMedicalOrdersByCaseId(caseId);
        List<ConsultationView> consultations = buildConsultationViews(caseId);
        ArchiveRepository.ApplicationArchiveSummary applicationFormArchive = archiveRepository
            .findApplicationArchiveSummary(caseId, application.getId().value())
            .orElse(null);
        List<ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchives = archiveRepository.findEmbeddingBoxArchiveSummaries(caseId);
        List<ArchiveRepository.ObjectArchiveSummary> slideArchives = archiveRepository.findSlideArchiveSummaries(caseId);
        boolean hasPendingRevision = revisions.stream().anyMatch(item -> "PENDING".equals(item.requestStatus()));
        return new DiagnosticWorkbenchAggregate(
            caseId,
            application.getApplicationNo(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            application.getPatientName(),
            application.getSubmittingDepartmentName(),
            application.getSubmittingDoctorName(),
            application.getClinicalDiagnosis(),
            applicationFormArchive,
            tasks,
            report,
            blocks,
            slides,
            embeddingBoxes,
            embeddingBoxArchives,
            slideArchives,
            specimens,
            recentEvents,
            revisions,
            medicalOrders,
            consultations,
            hasPendingRevision);
    }

    @Override
    public ReportTrackingAggregate getReportTracking(String caseId) {
        PathologyCase pathologyCase = technicalWorkflowRepository.findPathologyCaseById(caseId).orElseThrow();
        Application application = applicationRepository.findById(new ApplicationId(pathologyCase.applicationId())).orElseThrow();
        List<DiagnosticReportRepository.DiagnosticTask> tasks = diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId);
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(caseId, "ROUTINE")
            .orElse(null);
        List<DiagnosticReportRepository.ReportVersion> versions = diagnosticReportRepository.findReportVersionsByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(caseId);
        List<ReportRevisionRepository.ReportRevisionRequest> revisions = reportRevisionRepository.findRevisionRequestsByCaseId(caseId);
        List<MedicalOrderRepository.MedicalOrder> medicalOrders = medicalOrderRepository.findMedicalOrdersByCaseId(caseId);
        List<ConsultationView> consultations = buildConsultationViews(caseId);
        ArchiveRepository.ApplicationArchiveSummary applicationFormArchive = archiveRepository
            .findApplicationArchiveSummary(caseId, application.getId().value())
            .orElse(null);
        Integer latestEffectiveVersionNo = versions.stream()
            .filter(item -> "SIGNED".equals(item.versionStatus()) || "PUBLISHED".equals(item.versionStatus()))
            .map(DiagnosticReportRepository.ReportVersion::versionNo)
            .max(Comparator.naturalOrder())
            .orElse(null);
        Integer currentDraftVersionNo = report != null ? report.versionNo() : null;
        boolean hasPendingRevision = revisions.stream().anyMatch(item -> "PENDING".equals(item.requestStatus()));
        return new ReportTrackingAggregate(
            caseId,
            application.getApplicationNo(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            application.getPatientName(),
            applicationFormArchive,
            tasks,
            report,
            versions,
            events,
            revisions,
            medicalOrders,
            consultations,
            latestEffectiveVersionNo,
            currentDraftVersionNo,
            hasPendingRevision);
    }

    private List<ConsultationView> buildConsultationViews(String caseId) {
        return consultationRepository.findConsultationsByCaseId(caseId).stream()
            .map(item -> new ConsultationView(item, consultationRepository.findConsultationParticipants(item.id())))
            .toList();
    }
}
