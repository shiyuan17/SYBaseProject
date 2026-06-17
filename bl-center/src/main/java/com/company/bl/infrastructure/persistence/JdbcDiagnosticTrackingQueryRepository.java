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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcDiagnosticTrackingQueryRepository implements DiagnosticTrackingQueryRepository {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final ReportRevisionRepository reportRevisionRepository;
    private final MedicalOrderRepository medicalOrderRepository;
    private final ConsultationRepository consultationRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final ApplicationRepository applicationRepository;
    private final ArchiveRepository archiveRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcDiagnosticTrackingQueryRepository(DiagnosticReportRepository diagnosticReportRepository,
                                                 ReportRevisionRepository reportRevisionRepository,
                                                 MedicalOrderRepository medicalOrderRepository,
                                                 ConsultationRepository consultationRepository,
                                                 TechnicalWorkflowRepository technicalWorkflowRepository,
                                                 ApplicationRepository applicationRepository,
                                                 ArchiveRepository archiveRepository,
                                                 NamedParameterJdbcTemplate jdbcTemplate) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.reportRevisionRepository = reportRevisionRepository;
        this.medicalOrderRepository = medicalOrderRepository;
        this.consultationRepository = consultationRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.applicationRepository = applicationRepository;
        this.archiveRepository = archiveRepository;
        this.jdbcTemplate = jdbcTemplate;
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
        RegistrationPatientExtension registrationExtension = findRegistrationPatientExtension(application.getId().value());
        List<HistoricalPathology> historicalPathologies = findHistoricalPathologies(
            application.getPatientId(),
            pathologyCase.pathologyNo());
        List<ChargeItem> chargeItems = findChargeItems(caseId);
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
            application.getPatientId(),
            registrationExtension.idNo(),
            application.getPatientGender(),
            application.getPatientAge(),
            application.getApplicationType(),
            registrationExtension.inpatientNo(),
            null,
            registrationExtension.bedNo(),
            registrationExtension.phone(),
            application.getSubmittingDepartmentName(),
            application.getSubmittingDoctorName(),
            application.getClinicalDiagnosis(),
            application.getRemarks(),
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
            historicalPathologies,
            chargeItems,
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

    private RegistrationPatientExtension findRegistrationPatientExtension(String applicationId) {
        List<RegistrationPatientExtension> rows = jdbcTemplate.query("""
            select id_no, inpatient_no, bed_no, phone
            from application_registration_workbench
            where application_id = :applicationId
            order by updated_at desc, application_id desc
            fetch first 1 rows only
            """, Map.of("applicationId", applicationId), (rs, rowNum) -> new RegistrationPatientExtension(
            rs.getString("id_no"),
            rs.getString("inpatient_no"),
            rs.getString("bed_no"),
            rs.getString("phone")));
        return rows.stream().findFirst().orElse(RegistrationPatientExtension.EMPTY);
    }

    private List<HistoricalPathology> findHistoricalPathologies(String patientId, String currentPathologyNo) {
        if (patientId == null || patientId.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query("""
            select patient_id, external_report_no, source_system, report_date, final_diagnosis
            from historical_reports
            where patient_id = :patientId
              and (pathology_no is null or pathology_no <> :currentPathologyNo)
            order by report_date desc, created_at desc, id desc
            fetch first 50 rows only
            """, Map.of(
            "patientId", patientId,
            "currentPathologyNo", currentPathologyNo == null ? "" : currentPathologyNo
        ), (rs, rowNum) -> new HistoricalPathology(
            null,
            null,
            rs.getString("external_report_no"),
            rs.getString("source_system"),
            toLocalDateTime(rs.getTimestamp("report_date")),
            rs.getString("final_diagnosis")));
    }

    private List<ChargeItem> findChargeItems(String caseId) {
        return jdbcTemplate.query("""
            select item_name, billed_at, operator_name
            from billing_records
            where case_id = :caseId
            order by billed_at desc, created_at desc, id desc
            """, Map.of("caseId", caseId), (rs, rowNum) -> new ChargeItem(
            rs.getString("item_name"),
            toLocalDateTime(rs.getTimestamp("billed_at")),
            rs.getString("operator_name")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record RegistrationPatientExtension(
        String idNo,
        String inpatientNo,
        String bedNo,
        String phone
    ) {
        private static final RegistrationPatientExtension EMPTY = new RegistrationPatientExtension(null, null, null, null);
    }
}
