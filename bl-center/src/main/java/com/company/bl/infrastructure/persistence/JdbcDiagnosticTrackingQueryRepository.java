package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Repository
public class JdbcDiagnosticTrackingQueryRepository implements DiagnosticTrackingQueryRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcDiagnosticTrackingQueryRepository.class);

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
        PathologyCase pathologyCase = readStage("case", () -> technicalWorkflowRepository.findPathologyCaseById(caseId))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology case not found"));
        String applicationId = pathologyCase.applicationId();
        if (!hasText(applicationId)) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found");
        }
        Application application = readStage("case", () -> applicationRepository.findById(new ApplicationId(applicationId)))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
        List<Specimen> specimens = readStage("material", () -> technicalWorkflowRepository.findSpecimensByCaseId(caseId));
        List<TechnicalWorkflowRecords.SamplingBlock> blocks = readStage("material", () -> technicalWorkflowRepository.findSamplingBlocksByCaseId(caseId));
        List<String> samplingDoctorNames = readStage("material", () -> findSamplingDoctorNames(caseId));
        List<MedicalOrderRepository.MedicalOrderBlock> medicalOrderBlocks = readStage("material", () -> medicalOrderRepository.findMedicalOrderBlocksByCaseId(caseId));
        List<TechnicalWorkflowProcessingRecords.Slide> slides = readStage("material", () -> technicalWorkflowRepository.findSlidesByCaseId(caseId));
        List<TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxes = readStage("material", () -> technicalWorkflowRepository.findEmbeddingBoxesByCaseId(caseId));
        List<TrackingEvent> recentEvents = readStage("case", () -> technicalWorkflowRepository.findRecentTrackingEventsByCaseId(caseId, 10));
        List<DiagnosticReportRepository.DiagnosticTask> tasks = readStage("report", () -> diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId));
        String preferredReportScope = preferredReportScope(application);
        DiagnosticReportRepository.PathologyReport report = readStage("report", () -> diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(caseId, preferredReportScope))
            .orElse(null);
        List<ReportRevisionRepository.ReportRevisionRequest> revisions = readStage("report", () -> reportRevisionRepository.findRevisionRequestsByCaseId(caseId));
        List<MedicalOrderRepository.MedicalOrder> medicalOrders = readStage("report", () -> medicalOrderRepository.findMedicalOrdersByCaseId(caseId));
        List<ConsultationView> consultations = readStage("consultation", () -> buildConsultationViews(caseId));
        RegistrationPatientExtension registrationExtension = readStage("case", () -> findRegistrationPatientExtension(application.getId().value()));
        List<HistoricalPathology> historicalPathologies = readStage("history", () -> findHistoricalPathologies(
            application.getPatientId(),
            pathologyCase.pathologyNo()));
        List<ChargeItem> chargeItems = readStage("charge", () -> findChargeItems(caseId));
        ArchiveRepository.ApplicationArchiveSummary applicationFormArchive = readStage("archive", () -> archiveRepository
            .findApplicationArchiveSummary(caseId, application.getId().value()))
            .orElse(null);
        List<ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchives = readStage("archive", () -> archiveRepository.findEmbeddingBoxArchiveSummaries(caseId));
        List<ArchiveRepository.ObjectArchiveSummary> slideArchives = readStage("archive", () -> archiveRepository.findSlideArchiveSummaries(caseId));
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
            registrationExtension.wardName(),
            samplingDoctorNames,
            application.getClinicalDiagnosis(),
            registrationExtension.checkItem(),
            application.getSubmissionDate() == null ? null : application.getSubmissionDate().toString(),
            application.getRemarks(),
            applicationFormArchive,
            tasks,
            report,
            blocks,
            medicalOrderBlocks,
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
        PathologyCase pathologyCase = technicalWorkflowRepository.findPathologyCaseById(caseId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology case not found"));
        String applicationId = pathologyCase.applicationId();
        if (!hasText(applicationId)) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found");
        }
        Application application = applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
        List<DiagnosticReportRepository.DiagnosticTask> tasks = diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId);
        String preferredReportScope = preferredReportScope(application);
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(caseId, preferredReportScope)
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> T readStage(String stage, Supplier<T> reader) {
        try {
            return reader.get();
        } catch (RuntimeException exception) {
            SQLException sqlException = findSqlException(exception);
            log.warn(
                "Diagnostic workbench aggregation failed at stage={}, exceptionType={}, sqlState={}, vendorCode={}",
                stage,
                exception.getClass().getSimpleName(),
                sqlException == null ? null : sqlException.getSQLState(),
                sqlException == null ? null : sqlException.getErrorCode());
            throw exception;
        }
    }

    private SQLException findSqlException(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private RegistrationPatientExtension findRegistrationPatientExtension(String applicationId) {
        List<RegistrationPatientExtension> rows = jdbcTemplate.query("""
            select id_no, inpatient_no, bed_no, phone, check_item, ward_name
            from application_registration_workbench
            where application_id = :applicationId
            order by updated_at desc, application_id desc
            fetch first 1 rows only
            """, Map.of("applicationId", applicationId), (rs, rowNum) -> new RegistrationPatientExtension(
            rs.getString("id_no"),
            rs.getString("inpatient_no"),
            rs.getString("bed_no"),
            rs.getString("phone"),
            rs.getString("check_item"),
            rs.getString("ward_name")));
        return rows.stream().findFirst().orElse(RegistrationPatientExtension.EMPTY);
    }

    private List<String> findSamplingDoctorNames(String caseId) {
        return jdbcTemplate.query("""
            select sampled_by_name
            from samplings
            where case_id = :caseId
              and sampled_by_name is not null
              and trim(sampled_by_name) <> ''
            order by sampled_at asc, created_at asc, id asc
            """, Map.of("caseId", caseId), (rs, rowNum) -> rs.getString("sampled_by_name"))
            .stream()
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .toList();
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

    private String preferredReportScope(Application application) {
        if (application != null && "FROZEN".equalsIgnoreCase(application.getApplicationType())) {
            return "FROZEN";
        }
        return "ROUTINE";
    }

    private record RegistrationPatientExtension(
        String idNo,
        String inpatientNo,
        String bedNo,
        String phone,
        String checkItem,
        String wardName
    ) {
        private static final RegistrationPatientExtension EMPTY = new RegistrationPatientExtension(null, null, null, null, null, null);
    }
}
