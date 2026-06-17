package com.company.bl.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface DiagnosticTrackingQueryRepository {

    DiagnosticWorkbenchAggregate getDiagnosticWorkbench(String caseId);

    ReportTrackingAggregate getReportTracking(String caseId);

    record DiagnosticWorkbenchAggregate(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        String patientId,
        String patientIdDisplay,
        String patientGender,
        String patientAge,
        String applicationType,
        String inpatientNo,
        String outpatientNo,
        String bedNo,
        String phone,
        String submittingDepartmentName,
        String submittingDoctorName,
        String clinicalDiagnosis,
        String applicationRemarks,
        ArchiveRepository.ApplicationArchiveSummary applicationFormArchive,
        List<DiagnosticReportRepository.DiagnosticTask> diagnosticTasks,
        DiagnosticReportRepository.PathologyReport currentReport,
        List<TechnicalWorkflowRecords.SamplingBlock> blocks,
        List<TechnicalWorkflowProcessingRecords.Slide> slides,
        List<TechnicalWorkflowRecords.EmbeddingBox> embeddingBoxes,
        List<ArchiveRepository.ObjectArchiveSummary> embeddingBoxArchives,
        List<ArchiveRepository.ObjectArchiveSummary> slideArchives,
        List<com.company.bl.domain.model.Specimen> specimens,
        List<com.company.bl.domain.model.TrackingEvent> recentEvents,
        List<ReportRevisionRepository.ReportRevisionRequest> revisions,
        List<MedicalOrderRepository.MedicalOrder> medicalOrders,
        List<ConsultationView> consultations,
        List<HistoricalPathology> historicalPathologies,
        List<ChargeItem> chargeItems,
        boolean hasPendingRevision
    ) {
    }

    record ReportTrackingAggregate(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        ArchiveRepository.ApplicationArchiveSummary applicationFormArchive,
        List<DiagnosticReportRepository.DiagnosticTask> diagnosticTasks,
        DiagnosticReportRepository.PathologyReport currentReport,
        List<DiagnosticReportRepository.ReportVersion> versions,
        List<com.company.bl.domain.model.TrackingEvent> events,
        List<ReportRevisionRepository.ReportRevisionRequest> revisions,
        List<MedicalOrderRepository.MedicalOrder> medicalOrders,
        List<ConsultationView> consultations,
        Integer latestEffectiveVersionNo,
        Integer currentDraftVersionNo,
        boolean hasPendingRevision
    ) {
    }

    record ConsultationView(
        ConsultationRepository.ConsultationCase consultationCase,
        List<ConsultationRepository.ConsultationParticipant> participants
    ) {
    }

    record HistoricalPathology(
        String age,
        String inpatientNo,
        String examinationNo,
        String submissionType,
        LocalDateTime reportTime,
        String diagnosis
    ) {
    }

    record ChargeItem(
        String itemName,
        LocalDateTime chargedAt,
        String chargedByName
    ) {
    }
}
