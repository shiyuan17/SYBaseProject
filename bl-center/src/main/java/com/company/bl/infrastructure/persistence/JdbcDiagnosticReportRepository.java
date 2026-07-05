package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.DiagnosticReportRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcDiagnosticReportRepository implements DiagnosticReportRepository {

    private final JdbcDiagnosticTaskStore diagnosticTaskStore;
    private final JdbcPathologyReportStore pathologyReportStore;

    public JdbcDiagnosticReportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.diagnosticTaskStore = new JdbcDiagnosticTaskStore(jdbcTemplate);
        this.pathologyReportStore = new JdbcPathologyReportStore(jdbcTemplate);
    }

    @Override
    public Optional<DiagnosticTask> findDiagnosticTaskById(String taskId) {
        return diagnosticTaskStore.findDiagnosticTaskById(taskId);
    }

    @Override
    public List<DiagnosticTask> findDiagnosticTasksByCaseId(String caseId) {
        return diagnosticTaskStore.findDiagnosticTasksByCaseId(caseId);
    }

    @Override
    public List<DiagnosticTask> findActiveDiagnosticTasksByCaseIdAndType(String caseId, String taskType) {
        return diagnosticTaskStore.findActiveDiagnosticTasksByCaseIdAndType(caseId, taskType);
    }

    @Override
    public PagedDiagnosticTasks findDiagnosticTasks(PendingDiagnosticTaskQuery query) {
        return diagnosticTaskStore.findDiagnosticTasks(query);
    }

    @Override
    public void insertDiagnosticTask(CreateDiagnosticTaskCommand command) {
        diagnosticTaskStore.insertDiagnosticTask(command);
    }

    @Override
    public void assignDiagnosticTask(AssignDiagnosticTaskCommand command) {
        diagnosticTaskStore.assignDiagnosticTask(command);
    }

    @Override
    public void acceptDiagnosticTask(String taskId, String remarks, LocalDateTime acceptedAt) {
        diagnosticTaskStore.acceptDiagnosticTask(taskId, remarks, acceptedAt);
    }

    @Override
    public void startDiagnosticTask(String taskId, String remarks, LocalDateTime startedAt) {
        diagnosticTaskStore.startDiagnosticTask(taskId, remarks, startedAt);
    }

    @Override
    public void markDiagnosticTaskSubmitted(String taskId, String remarks, LocalDateTime primaryDiagnosedAt) {
        diagnosticTaskStore.markDiagnosticTaskSubmitted(taskId, remarks, primaryDiagnosedAt);
    }

    @Override
    public void markDiagnosticTaskReviewed(String taskId,
                                           String reviewerUserId,
                                           String reviewerName,
                                           String remarks,
                                           LocalDateTime reviewedAt) {
        diagnosticTaskStore.markDiagnosticTaskReviewed(taskId, reviewerUserId, reviewerName, remarks, reviewedAt);
    }

    @Override
    public void revertDiagnosticTaskToInProgress(String taskId, String remarks) {
        diagnosticTaskStore.revertDiagnosticTaskToInProgress(taskId, remarks);
    }

    @Override
    public void completeDiagnosticTask(String taskId, String remarks, LocalDateTime completedAt) {
        diagnosticTaskStore.completeDiagnosticTask(taskId, remarks, completedAt);
    }

    @Override
    public void updateFrozenDiagnosisResult(String taskId,
                                            String frozenDiagnosisResult,
                                            String remarks,
                                            LocalDateTime updatedAt) {
        diagnosticTaskStore.updateFrozenDiagnosisResult(taskId, frozenDiagnosisResult, remarks, updatedAt);
    }

    @Override
    public Optional<PathologyReport> findCurrentReportByCaseIdAndScope(String caseId, String reportScope) {
        return pathologyReportStore.findCurrentReportByCaseIdAndScope(caseId, reportScope);
    }

    @Override
    public Optional<PathologyReport> findPathologyReportById(String reportId) {
        return pathologyReportStore.findPathologyReportById(reportId);
    }

    @Override
    public List<PathologyReport> findPathologyReportsByCaseId(String caseId) {
        return pathologyReportStore.findPathologyReportsByCaseId(caseId);
    }

    @Override
    public void insertPathologyReport(CreatePathologyReportCommand command) {
        pathologyReportStore.insertPathologyReport(command);
    }

    @Override
    public void updatePathologyReportDraft(UpdatePathologyReportDraftCommand command) {
        pathologyReportStore.updatePathologyReportDraft(command);
    }

    @Override
    public void submitPathologyReport(String reportId, String remarks, LocalDateTime submittedAt) {
        pathologyReportStore.submitPathologyReport(reportId, remarks, submittedAt);
    }

    @Override
    public void reviewPathologyReport(String reportId,
                                      String reviewerUserId,
                                      String reviewerName,
                                      String remarks,
                                      LocalDateTime reviewedAt) {
        pathologyReportStore.reviewPathologyReport(reportId, reviewerUserId, reviewerName, remarks, reviewedAt);
    }

    @Override
    public void rejectPathologyReport(String reportId, String rejectReason) {
        pathologyReportStore.rejectPathologyReport(reportId, rejectReason);
    }

    @Override
    public void resetPathologyReportForRevision(String reportId,
                                                int versionNo,
                                                String remarks,
                                                LocalDateTime updatedAt) {
        pathologyReportStore.resetPathologyReportForRevision(reportId, versionNo, remarks, updatedAt);
    }

    @Override
    public void signPathologyReport(String reportId,
                                    String signedByUserId,
                                    String signedByName,
                                    String remarks,
                                    LocalDateTime signedAt) {
        pathologyReportStore.signPathologyReport(reportId, signedByUserId, signedByName, remarks, signedAt);
    }

    @Override
    public void publishPathologyReport(String reportId, String remarks, LocalDateTime publishedAt) {
        pathologyReportStore.publishPathologyReport(reportId, remarks, publishedAt);
    }

    @Override
    public void insertReportVersion(CreateReportVersionCommand command) {
        pathologyReportStore.insertReportVersion(command);
    }

    @Override
    public Optional<ReportVersion> findReportVersionById(String versionId) {
        return pathologyReportStore.findReportVersionById(versionId);
    }

    @Override
    public List<ReportVersion> findReportVersionsByCaseId(String caseId) {
        return pathologyReportStore.findReportVersionsByCaseId(caseId);
    }

    @Override
    public List<ReportVersion> findScheduledReportVersionsDue(LocalDateTime scheduledBeforeOrAt) {
        return pathologyReportStore.findScheduledReportVersionsDue(scheduledBeforeOrAt);
    }

    @Override
    public List<ReportVersion> findFormalReportVersionsByCaseId(String caseId) {
        return pathologyReportStore.findFormalReportVersionsByCaseId(caseId);
    }

    @Override
    public void markReportVersionsPrinted(List<String> versionIds, LocalDateTime printedAt) {
        pathologyReportStore.markReportVersionsPrinted(versionIds, printedAt);
    }

    @Override
    public void markReportVersionsIssued(List<String> versionIds, LocalDateTime issuedAt) {
        pathologyReportStore.markReportVersionsIssued(versionIds, issuedAt);
    }

    @Override
    public void scheduleReportVersionsIssue(List<String> versionIds, LocalDateTime plannedIssueAt) {
        pathologyReportStore.scheduleReportVersionsIssue(versionIds, plannedIssueAt);
    }

    @Override
    public void markReportVersionsRecalled(List<String> versionIds, LocalDateTime recalledAt) {
        pathologyReportStore.markReportVersionsRecalled(versionIds, recalledAt);
    }
}
