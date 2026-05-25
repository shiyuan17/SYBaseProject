package com.company.bl.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiagnosticReportRepository {

    Optional<DiagnosticTask> findDiagnosticTaskById(String taskId);

    List<DiagnosticTask> findDiagnosticTasksByCaseId(String caseId);

    List<DiagnosticTask> findActiveDiagnosticTasksByCaseIdAndType(String caseId, String taskType);

    PagedDiagnosticTasks findDiagnosticTasks(PendingDiagnosticTaskQuery query);

    void insertDiagnosticTask(CreateDiagnosticTaskCommand command);

    void assignDiagnosticTask(AssignDiagnosticTaskCommand command);

    void acceptDiagnosticTask(String taskId, String remarks, LocalDateTime acceptedAt);

    void startDiagnosticTask(String taskId, String remarks, LocalDateTime startedAt);

    void markDiagnosticTaskSubmitted(String taskId, String remarks, LocalDateTime primaryDiagnosedAt);

    void markDiagnosticTaskReviewed(String taskId,
                                    String reviewerUserId,
                                    String reviewerName,
                                    String remarks,
                                    LocalDateTime reviewedAt);

    void revertDiagnosticTaskToInProgress(String taskId, String remarks);

    void completeDiagnosticTask(String taskId, String remarks, LocalDateTime completedAt);

    Optional<PathologyReport> findCurrentReportByCaseIdAndScope(String caseId, String reportScope);

    Optional<PathologyReport> findPathologyReportById(String reportId);

    void insertPathologyReport(CreatePathologyReportCommand command);

    void updatePathologyReportDraft(UpdatePathologyReportDraftCommand command);

    void submitPathologyReport(String reportId, String remarks, LocalDateTime submittedAt);

    void reviewPathologyReport(String reportId,
                               String reviewerUserId,
                               String reviewerName,
                               String remarks,
                               LocalDateTime reviewedAt);

    void rejectPathologyReport(String reportId, String rejectReason);

    void resetPathologyReportForRevision(String reportId,
                                         int versionNo,
                                         String remarks,
                                         LocalDateTime updatedAt);

    void signPathologyReport(String reportId,
                             String signedByUserId,
                             String signedByName,
                             String remarks,
                             LocalDateTime signedAt);

    void publishPathologyReport(String reportId, String remarks, LocalDateTime publishedAt);

    void insertReportVersion(CreateReportVersionCommand command);

    List<ReportVersion> findReportVersionsByCaseId(String caseId);

    record PendingDiagnosticTaskQuery(
        int page,
        int size,
        String taskType,
        String taskStatus,
        String pathologyNo,
        String currentUserId,
        String currentRoleCode
    ) {
    }

    record PagedDiagnosticTasks(List<DiagnosticTask> items, long total) {
    }

    record DiagnosticTask(
        String id,
        String applicationId,
        String applicationNo,
        String patientName,
        String caseId,
        String pathologyNo,
        String specimenId,
        String taskType,
        String status,
        String priority,
        String assignmentMode,
        String assignedByUserId,
        String assignedByName,
        String diagnosisDoctorUserId,
        String diagnosisDoctorName,
        String primaryDoctorUserId,
        String primaryDoctorName,
        String reviewerUserId,
        String reviewerName,
        LocalDateTime primaryDiagnosedAt,
        LocalDateTime reviewedAt,
        LocalDateTime assignedAt,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    record CreateDiagnosticTaskCommand(
        String id,
        String caseId,
        String specimenId,
        String pathologyNo,
        String taskType,
        String status,
        String priority,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    record AssignDiagnosticTaskCommand(
        String taskId,
        String assignedByUserId,
        String assignedByName,
        String diagnosisDoctorUserId,
        String diagnosisDoctorName,
        String primaryDoctorUserId,
        String primaryDoctorName,
        String reviewerUserId,
        String reviewerName,
        String remarks,
        LocalDateTime assignedAt
    ) {
    }

    record PathologyReport(
        String id,
        String caseId,
        String taskId,
        String reportNo,
        String pathologyNo,
        String reportScope,
        int reportSeq,
        String reportStatus,
        int versionNo,
        String specimenType,
        String patientName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        LocalDateTime reportDate,
        String grossExam,
        String microscopicExam,
        String clinicalDiagnosis,
        String finalDiagnosis,
        LocalDateTime submittedAt,
        String reviewerUserId,
        String reviewerName,
        LocalDateTime reviewedAt,
        String signedByUserId,
        String signedByName,
        LocalDateTime signedAt,
        LocalDateTime publishedAt,
        String richTextContent,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record CreatePathologyReportCommand(
        String id,
        String caseId,
        String taskId,
        String reportNo,
        String pathologyNo,
        String reportScope,
        int reportSeq,
        String reportStatus,
        int versionNo,
        String specimenType,
        String patientName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        LocalDateTime reportDate,
        String grossExam,
        String microscopicExam,
        String clinicalDiagnosis,
        String finalDiagnosis,
        String richTextContent,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    record UpdatePathologyReportDraftCommand(
        String reportId,
        String grossExam,
        String microscopicExam,
        String clinicalDiagnosis,
        String finalDiagnosis,
        String richTextContent,
        String remarks,
        LocalDateTime updatedAt
    ) {
    }

    record CreateReportVersionCommand(
        String id,
        String reportId,
        String caseId,
        String reportScope,
        int reportSeq,
        int versionNo,
        String versionStatus,
        String finalDiagnosisSnapshot,
        String contentSnapshot,
        String signedByUserId,
        String signedByName,
        LocalDateTime signedAt,
        LocalDateTime createdAt
    ) {
    }

    record ReportVersion(
        String id,
        String reportId,
        String caseId,
        String reportScope,
        int reportSeq,
        int versionNo,
        String versionStatus,
        String finalDiagnosisSnapshot,
        String contentSnapshot,
        String signedByUserId,
        String signedByName,
        LocalDateTime signedAt,
        LocalDateTime createdAt
    ) {
    }
}
