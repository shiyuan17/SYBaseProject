package com.company.bl.application.service;

import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
class DiagnosticReportQueryService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;

    DiagnosticReportQueryService(DiagnosticReportRepository diagnosticReportRepository,
                                 TechnicalWorkflowRepository technicalWorkflowRepository,
                                 DiagnosticReportSupport diagnosticReportSupport) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
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
                query.pathologyNo()));
        return new DiagnosticReportModels.PendingDiagnosticTaskPage(
            paged.items().stream().map(this::toTaskView).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional(readOnly = true)
    DiagnosticReportModels.DiagnosticWorkbenchView getDiagnosticWorkbench(String caseId) {
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(caseId);
        Application application = diagnosticReportSupport.getApplication(pathologyCase.applicationId());
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        List<TechnicalWorkflowRecords.SamplingBlock> blocks = technicalWorkflowRepository.findSamplingBlocksByCaseId(caseId);
        List<TechnicalWorkflowProcessingRecords.Slide> slides = technicalWorkflowRepository.findSlidesByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findRecentTrackingEventsByCaseId(caseId, 10);
        List<DiagnosticReportRepository.DiagnosticTask> tasks = diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId);
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(caseId, DiagnosticReportConstants.REPORT_SCOPE_ROUTINE)
            .orElse(null);
        return new DiagnosticReportModels.DiagnosticWorkbenchView(
            pathologyCase.id(),
            application.getApplicationNo(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            application.getPatientName(),
            application.getSubmittingDepartmentName(),
            application.getSubmittingDoctorName(),
            application.getClinicalDiagnosis(),
            specimens.stream().map(item -> new DiagnosticReportModels.WorkbenchSpecimenSummary(
                item.id(), item.specimenNo(), item.barcode(), item.specimenNameStandardized(), item.specimenStatus().name())).toList(),
            blocks.stream().map(item -> new DiagnosticReportModels.WorkbenchBlockSummary(
                item.id(), item.specimenId(), item.blockCode(), item.embeddingBoxNo(), item.blockDescription())).toList(),
            slides.stream().map(item -> new DiagnosticReportModels.WorkbenchSlideSummary(
                item.id(), item.specimenId(), item.embeddingBoxId(), item.slideNo(), item.slideStatus(), item.qualityStatus())).toList(),
            tasks.stream().map(this::toTaskView).toList(),
            report == null ? null : toReportView(report),
            events.stream().map(this::toTrackingEvent).toList());
    }

    @Transactional(readOnly = true)
    DiagnosticReportModels.ReportTrackingView getReportTracking(String caseId) {
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(caseId);
        Application application = diagnosticReportSupport.getApplication(pathologyCase.applicationId());
        List<DiagnosticReportRepository.DiagnosticTask> tasks = diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId);
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(caseId, DiagnosticReportConstants.REPORT_SCOPE_ROUTINE)
            .orElse(null);
        List<DiagnosticReportRepository.ReportVersion> versions = diagnosticReportRepository.findReportVersionsByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(caseId);
        return new DiagnosticReportModels.ReportTrackingView(
            pathologyCase.id(),
            application.getApplicationNo(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            application.getPatientName(),
            tasks.stream().map(this::toTaskView).toList(),
            report == null ? null : toReportView(report),
            versions.stream().map(item -> new DiagnosticReportModels.ReportVersionView(
                item.id(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(),
                stringify(item.signedAt()), stringify(item.createdAt()))).toList(),
            events.stream().map(this::toTrackingEvent).toList());
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

    private DiagnosticReportModels.PathologyReportView toReportView(DiagnosticReportRepository.PathologyReport report) {
        return new DiagnosticReportModels.PathologyReportView(
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

    private DiagnosticReportModels.TrackingEventView toTrackingEvent(TrackingEvent event) {
        return new DiagnosticReportModels.TrackingEventView(
            event.nodeCode(),
            event.eventType(),
            event.eventStatus(),
            stringify(event.eventTime()),
            event.operatorName(),
            event.eventContent());
    }

    private String stringify(LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
