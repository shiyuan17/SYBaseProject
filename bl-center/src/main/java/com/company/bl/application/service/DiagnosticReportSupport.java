package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.ConsultationRepository;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.TechnicalWorkflowProcessingRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRecords;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
class DiagnosticReportSupport {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final ApplicationRepository applicationRepository;
    private final ConsultationRepository consultationRepository;

    DiagnosticReportSupport(DiagnosticReportRepository diagnosticReportRepository,
                            TechnicalWorkflowRepository technicalWorkflowRepository,
                            ApplicationRepository applicationRepository,
                            ConsultationRepository consultationRepository) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.applicationRepository = applicationRepository;
        this.consultationRepository = consultationRepository;
    }

    DiagnosticReportRepository.DiagnosticTask getDiagnosticTask(String taskId) {
        return diagnosticReportRepository.findDiagnosticTaskById(taskId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Diagnostic task not found"));
    }

    DiagnosticReportRepository.PathologyReport getReport(String reportId) {
        return diagnosticReportRepository.findPathologyReportById(reportId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology report not found"));
    }

    DiagnosticReportRepository.ReportVersion getReportVersion(String versionId) {
        return diagnosticReportRepository.findReportVersionById(versionId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Report version not found"));
    }

    PathologyCase getCase(String caseId) {
        return technicalWorkflowRepository.findPathologyCaseById(caseId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology case not found"));
    }

    PathologyCase resolveCaseIdentifier(String caseIdentifier) {
        if (caseIdentifier == null || caseIdentifier.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Pathology case identifier is required");
        }
        String normalizedIdentifier = caseIdentifier.trim();
        return technicalWorkflowRepository.findPathologyCaseById(normalizedIdentifier)
            .or(() -> technicalWorkflowRepository.findPathologyCaseByPathologyNo(normalizedIdentifier))
            .or(() -> technicalWorkflowRepository.findSpecimenById(normalizedIdentifier)
                .map(Specimen::caseId)
                .flatMap(technicalWorkflowRepository::findPathologyCaseById))
            .or(() -> technicalWorkflowRepository.findSamplingBlockById(normalizedIdentifier)
                .map(TechnicalWorkflowRecords.SamplingBlock::caseId)
                .flatMap(technicalWorkflowRepository::findPathologyCaseById))
            .or(() -> technicalWorkflowRepository.findEmbeddingBoxById(normalizedIdentifier)
                .map(TechnicalWorkflowRecords.EmbeddingBox::caseId)
                .flatMap(technicalWorkflowRepository::findPathologyCaseById))
            .or(() -> technicalWorkflowRepository.findSlideById(normalizedIdentifier)
                .map(TechnicalWorkflowProcessingRecords.Slide::caseId)
                .flatMap(technicalWorkflowRepository::findPathologyCaseById))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology case not found"));
    }

    Application getApplication(String applicationId) {
        return applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
    }

    void ensureAssignedDoctor(DiagnosticReportRepository.DiagnosticTask task, String userId) {
        boolean allowed = userId != null && (userId.equals(task.diagnosisDoctorUserId()) || userId.equals(task.primaryDoctorUserId()));
        if (!allowed) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "User is not assigned to diagnostic task");
        }
    }

    void ensureReviewer(DiagnosticReportRepository.DiagnosticTask task, String userId) {
        boolean allowed = userId != null && userId.equals(task.reviewerUserId());
        if (!allowed) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "User is not assigned to review the report");
        }
    }

    void ensureSignedByCurrentUser(DiagnosticReportRepository.PathologyReport report, String userId) {
        boolean allowed = userId != null && userId.equals(report.signedByUserId());
        if (!allowed) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "User is not assigned to publish the report");
        }
    }

    void ensureDraftReport(DiagnosticReportRepository.PathologyReport report) {
        if (!DiagnosticReportConstants.REPORT_DRAFT.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report is not editable draft");
        }
    }

    DiagnosticReportRepository.DiagnosticTask getLatestDiagnosticTask(String caseId) {
        return diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId).stream()
            .max(java.util.Comparator.comparing(DiagnosticReportRepository.DiagnosticTask::createdAt))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Diagnostic task not found"));
    }

    void ensureConsultationHost(ConsultationRepository.ConsultationCase consultationCase, String userId) {
        boolean allowed = userId != null && userId.equals(consultationCase.hostUserId());
        if (!allowed) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "User is not consultation host");
        }
    }

    void ensureConsultationParticipant(ConsultationRepository.ConsultationParticipant participant, String userId) {
        boolean allowed = userId != null && userId.equals(participant.participantUserId());
        if (!allowed) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "User is not invited to consultation");
        }
    }

    void insertWorkflowEvent(String caseId,
                             String nodeCode,
                             String eventType,
                             String eventStatus,
                             String operatorUserId,
                             String operatorName,
                             String terminalCode,
                             String content) {
        PathologyCase pathologyCase = getCase(caseId);
        technicalWorkflowRepository.insertWorkflowEvent(new TrackingEvent(
            nextId("EVT"),
            pathologyCase.applicationId(),
            null,
            caseId,
            null,
            nodeCode,
            eventType,
            eventStatus,
            LocalDateTime.now(),
            operatorUserId,
            operatorName,
            terminalCode,
            content));
    }

    String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
