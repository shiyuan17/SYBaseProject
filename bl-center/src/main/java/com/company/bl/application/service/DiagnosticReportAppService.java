package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.support.application.NumberingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiagnosticReportAppService {

    private static final String TASK_PRIMARY = "PRIMARY";
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_ASSIGNED = "ASSIGNED";
    private static final String TASK_ACCEPTED = "ACCEPTED";
    private static final String TASK_IN_PROGRESS = "IN_PROGRESS";
    private static final String TASK_COMPLETED = "COMPLETED";
    private static final String REPORT_SCOPE_ROUTINE = "ROUTINE";
    private static final String REPORT_DRAFT = "DRAFT";
    private static final String REPORT_SUBMITTED = "SUBMITTED";
    private static final String REPORT_REVIEWED = "REVIEWED";
    private static final String REPORT_SIGNED = "SIGNED";
    private static final String REPORT_PUBLISHED = "PUBLISHED";

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final ApplicationRepository applicationRepository;
    private final NumberingService numberingService;

    @Transactional(readOnly = true)
    public PendingDiagnosticTaskPage listPendingTasks(PendingDiagnosticTaskQuery query) {
        DiagnosticReportRepository.PagedDiagnosticTasks paged = diagnosticReportRepository.findDiagnosticTasks(
            new DiagnosticReportRepository.PendingDiagnosticTaskQuery(
                query.page(),
                query.size(),
                query.taskType(),
                query.taskStatus(),
                query.pathologyNo()));
        return new PendingDiagnosticTaskPage(
            paged.items().stream().map(this::toTaskView).toList(),
            query.page(),
            query.size(),
            paged.total());
    }

    @Transactional
    public void createPrimaryDiagnosticTaskIfAbsent(String caseId, String remarks) {
        PathologyCase pathologyCase = getCase(caseId);
        if (!diagnosticReportRepository.findActiveDiagnosticTasksByCaseIdAndType(caseId, TASK_PRIMARY).isEmpty()) {
            return;
        }
        diagnosticReportRepository.insertDiagnosticTask(new DiagnosticReportRepository.CreateDiagnosticTaskCommand(
            nextId("DT"),
            caseId,
            null,
            pathologyCase.pathologyNo(),
            TASK_PRIMARY,
            TASK_PENDING,
            "NORMAL",
            remarks,
            LocalDateTime.now()));
    }

    @Transactional
    public DiagnosticTaskResult assignTask(AssignDiagnosticTaskCommand command) {
        DiagnosticReportRepository.DiagnosticTask task = getDiagnosticTask(command.taskId());
        if (!TASK_PENDING.equals(task.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Diagnostic task is not pending");
        }
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.assignDiagnosticTask(new DiagnosticReportRepository.AssignDiagnosticTaskCommand(
            command.taskId(),
            command.operatorUserId(),
            command.operatorName(),
            command.diagnosisDoctorUserId(),
            command.diagnosisDoctorName(),
            command.primaryDoctorUserId(),
            command.primaryDoctorName(),
            command.reviewerUserId(),
            command.reviewerName(),
            command.remarks(),
            now));
        insertWorkflowEvent(task.caseId(), "DIAGNOSIS_ASSIGN", "ASSIGN", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Diagnostic task assigned");
        DiagnosticReportRepository.DiagnosticTask updated = getDiagnosticTask(command.taskId());
        return new DiagnosticTaskResult(updated.id(), updated.caseId(), "DIAGNOSIS_PENDING", updated.status());
    }

    @Transactional
    public DiagnosticTaskResult acceptTask(TaskActionCommand command) {
        DiagnosticReportRepository.DiagnosticTask task = getDiagnosticTask(command.taskId());
        if (!TASK_ASSIGNED.equals(task.status()) && !TASK_PENDING.equals(task.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Diagnostic task is not assignable");
        }
        ensureAssignedDoctor(task, command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.acceptDiagnosticTask(task.id(), command.remarks(), now);
        insertWorkflowEvent(task.caseId(), "DIAGNOSIS_ACCEPT", "ACCEPT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Diagnostic task accepted");
        DiagnosticReportRepository.DiagnosticTask updated = getDiagnosticTask(task.id());
        return new DiagnosticTaskResult(updated.id(), updated.caseId(), "DIAGNOSIS_PENDING", updated.status());
    }

    @Transactional
    public DiagnosticTaskResult startTask(TaskActionCommand command) {
        DiagnosticReportRepository.DiagnosticTask task = getDiagnosticTask(command.taskId());
        if (!TASK_ASSIGNED.equals(task.status()) && !TASK_ACCEPTED.equals(task.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Diagnostic task cannot be started");
        }
        ensureAssignedDoctor(task, command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.startDiagnosticTask(task.id(), command.remarks(), now);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "DIAGNOSING");
        insertWorkflowEvent(task.caseId(), "DIAGNOSIS_START", "START", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Diagnostic task started");
        DiagnosticReportRepository.DiagnosticTask updated = getDiagnosticTask(task.id());
        return new DiagnosticTaskResult(updated.id(), updated.caseId(), "DIAGNOSING", updated.status());
    }

    @Transactional(readOnly = true)
    public DiagnosticWorkbenchView getDiagnosticWorkbench(String caseId) {
        PathologyCase pathologyCase = getCase(caseId);
        Application application = getApplication(pathologyCase.applicationId());
        List<Specimen> specimens = technicalWorkflowRepository.findSpecimensByCaseId(caseId);
        List<TechnicalWorkflowRepository.SamplingBlock> blocks = technicalWorkflowRepository.findSamplingBlocksByCaseId(caseId);
        List<TechnicalWorkflowRepository.EmbeddingBox> boxes = technicalWorkflowRepository.findEmbeddingBoxesByCaseId(caseId);
        List<TechnicalWorkflowRepository.Slide> slides = technicalWorkflowRepository.findSlidesByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(caseId);
        List<DiagnosticReportRepository.DiagnosticTask> tasks = diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId);
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(caseId, REPORT_SCOPE_ROUTINE)
            .orElse(null);
        return new DiagnosticWorkbenchView(
            pathologyCase.id(),
            application.getApplicationNo(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            application.getPatientName(),
            application.getSubmittingDepartmentName(),
            application.getSubmittingDoctorName(),
            application.getClinicalDiagnosis(),
            specimens.stream().map(item -> new WorkbenchSpecimenSummary(
                item.id(), item.specimenNo(), item.barcode(), item.specimenNameStandardized(), item.specimenStatus().name())).toList(),
            blocks.stream().map(item -> new WorkbenchBlockSummary(
                item.id(), item.specimenId(), item.blockCode(), item.embeddingBoxNo(), item.blockDescription())).toList(),
            slides.stream().map(item -> new WorkbenchSlideSummary(
                item.id(), item.specimenId(), item.embeddingBoxId(), item.slideNo(), item.slideStatus(), item.qualityStatus())).toList(),
            tasks.stream().map(this::toTaskView).toList(),
            report == null ? null : toReportView(report),
            events.stream()
                .sorted(Comparator.comparing(TrackingEvent::eventTime).reversed())
                .limit(10)
                .map(this::toTrackingEvent)
                .toList());
    }

    @Transactional
    public PathologyReportResult createReport(CreatePathologyReportCommand command) {
        DiagnosticReportRepository.DiagnosticTask task = getDiagnosticTask(command.taskId());
        if (!task.caseId().equals(command.caseId())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Task does not belong to case");
        }
        if (!TASK_IN_PROGRESS.equals(task.status()) && !TASK_ACCEPTED.equals(task.status()) && !TASK_ASSIGNED.equals(task.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Diagnostic task is not editable");
        }
        DiagnosticReportRepository.PathologyReport existing = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(command.caseId(), REPORT_SCOPE_ROUTINE)
            .orElse(null);
        if (existing != null && REPORT_DRAFT.equals(existing.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Draft report already exists");
        }
        PathologyCase pathologyCase = getCase(command.caseId());
        Application application = getApplication(pathologyCase.applicationId());
        LocalDateTime now = LocalDateTime.now();
        String reportId = nextId("RPT");
        diagnosticReportRepository.insertPathologyReport(new DiagnosticReportRepository.CreatePathologyReportCommand(
            reportId,
            command.caseId(),
            command.taskId(),
            numberingService.generateReportNo(),
            pathologyCase.pathologyNo(),
            REPORT_SCOPE_ROUTINE,
            1,
            REPORT_DRAFT,
            1,
            specimensSummaryType(command.caseId()),
            application.getPatientName(),
            application.getSubmittingDepartmentId(),
            application.getSubmittingDepartmentName(),
            now,
            command.grossExam(),
            command.microscopicExam(),
            command.clinicalDiagnosis(),
            command.finalDiagnosis(),
            command.richTextContent(),
            command.remarks(),
            now));
        insertWorkflowEvent(command.caseId(), "REPORT_DRAFT", "CREATE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Draft report created");
        DiagnosticReportRepository.PathologyReport report = getReport(reportId);
        return new PathologyReportResult(report.id(), report.caseId(), report.reportNo(), report.reportStatus(), null, null);
    }

    @Transactional
    public PathologyReportResult saveDraft(UpdateReportDraftCommand command) {
        DiagnosticReportRepository.PathologyReport report = getReport(command.reportId());
        ensureDraftReport(report);
        diagnosticReportRepository.updatePathologyReportDraft(new DiagnosticReportRepository.UpdatePathologyReportDraftCommand(
            report.id(),
            command.grossExam(),
            command.microscopicExam(),
            command.clinicalDiagnosis(),
            command.finalDiagnosis(),
            command.richTextContent(),
            command.remarks(),
            LocalDateTime.now()));
        insertWorkflowEvent(report.caseId(), "REPORT_DRAFT", "SAVE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Draft report saved");
        DiagnosticReportRepository.PathologyReport updated = getReport(report.id());
        return new PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), null, null);
    }

    @Transactional
    public PathologyReportResult submitReport(ReportActionCommand command) {
        DiagnosticReportRepository.PathologyReport report = getReport(command.reportId());
        ensureDraftReport(report);
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.submitPathologyReport(report.id(), command.remarks(), now);
        diagnosticReportRepository.markDiagnosticTaskSubmitted(report.taskId(), command.remarks(), now);
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "REPORT_PENDING_REVIEW");
        insertWorkflowEvent(report.caseId(), "REPORT_SUBMIT", "SUBMIT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Report submitted");
        DiagnosticReportRepository.PathologyReport updated = getReport(report.id());
        return new PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), null, null);
    }

    @Transactional
    public PathologyReportResult reviewReport(ReportActionCommand command) {
        DiagnosticReportRepository.PathologyReport report = getReport(command.reportId());
        if (!REPORT_SUBMITTED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report is not submitted");
        }
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.reviewPathologyReport(report.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        diagnosticReportRepository.markDiagnosticTaskReviewed(report.taskId(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "REPORT_REVIEWING");
        insertWorkflowEvent(report.caseId(), "REPORT_REVIEW", "REVIEW", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Report reviewed");
        DiagnosticReportRepository.PathologyReport updated = getReport(report.id());
        return new PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), null, null);
    }

    @Transactional
    public PathologyReportResult rejectReport(RejectReportCommand command) {
        DiagnosticReportRepository.PathologyReport report = getReport(command.reportId());
        if (!REPORT_SUBMITTED.equals(report.reportStatus()) && !REPORT_REVIEWED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report cannot be rejected");
        }
        diagnosticReportRepository.rejectPathologyReport(report.id(), command.rejectReason());
        diagnosticReportRepository.revertDiagnosticTaskToInProgress(report.taskId(), command.rejectReason());
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "DIAGNOSING");
        insertWorkflowEvent(report.caseId(), "REPORT_REJECT", "REJECT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.rejectReason());
        DiagnosticReportRepository.PathologyReport updated = getReport(report.id());
        return new PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), null, null);
    }

    @Transactional
    public PathologyReportResult signReport(ReportActionCommand command) {
        DiagnosticReportRepository.PathologyReport report = getReport(command.reportId());
        if (!REPORT_REVIEWED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report is not reviewed");
        }
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.signPathologyReport(report.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        DiagnosticReportRepository.PathologyReport updated = getReport(report.id());
        diagnosticReportRepository.insertReportVersion(new DiagnosticReportRepository.CreateReportVersionCommand(
            nextId("RV"),
            updated.id(),
            updated.caseId(),
            updated.reportScope(),
            updated.reportSeq(),
            updated.versionNo(),
            REPORT_SIGNED,
            updated.finalDiagnosis(),
            updated.richTextContent(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            now));
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "REPORT_SIGNED");
        insertWorkflowEvent(report.caseId(), "REPORT_SIGN", "SIGN", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Report signed");
        return new PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), updated.versionNo(), REPORT_SIGNED);
    }

    @Transactional
    public PathologyReportResult publishReport(ReportActionCommand command) {
        DiagnosticReportRepository.PathologyReport report = getReport(command.reportId());
        if (!REPORT_SIGNED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report is not signed");
        }
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.publishPathologyReport(report.id(), command.remarks(), now);
        DiagnosticReportRepository.PathologyReport updated = getReport(report.id());
        diagnosticReportRepository.insertReportVersion(new DiagnosticReportRepository.CreateReportVersionCommand(
            nextId("RV"),
            updated.id(),
            updated.caseId(),
            updated.reportScope(),
            updated.reportSeq(),
            updated.versionNo(),
            REPORT_PUBLISHED,
            updated.finalDiagnosis(),
            updated.richTextContent(),
            updated.signedByUserId(),
            updated.signedByName(),
            updated.signedAt(),
            now));
        diagnosticReportRepository.completeDiagnosticTask(updated.taskId(), command.remarks(), now);
        technicalWorkflowRepository.updatePathologyCaseStatus(updated.caseId(), "REPORT_PUBLISHED");
        insertWorkflowEvent(updated.caseId(), "REPORT_PUBLISH", "PUBLISH", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Report published");
        return new PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), updated.versionNo(), REPORT_PUBLISHED);
    }

    @Transactional(readOnly = true)
    public ReportTrackingView getReportTracking(String caseId) {
        PathologyCase pathologyCase = getCase(caseId);
        Application application = getApplication(pathologyCase.applicationId());
        List<DiagnosticReportRepository.DiagnosticTask> tasks = diagnosticReportRepository.findDiagnosticTasksByCaseId(caseId);
        DiagnosticReportRepository.PathologyReport report = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(caseId, REPORT_SCOPE_ROUTINE)
            .orElse(null);
        List<DiagnosticReportRepository.ReportVersion> versions = diagnosticReportRepository.findReportVersionsByCaseId(caseId);
        List<TrackingEvent> events = technicalWorkflowRepository.findTrackingEventsByCaseId(caseId);
        return new ReportTrackingView(
            pathologyCase.id(),
            application.getApplicationNo(),
            pathologyCase.pathologyNo(),
            pathologyCase.caseStatus(),
            application.getPatientName(),
            tasks.stream().map(this::toTaskView).toList(),
            report == null ? null : toReportView(report),
            versions.stream().map(item -> new ReportVersionView(
                item.id(), item.versionNo(), item.versionStatus(), item.finalDiagnosisSnapshot(),
                stringify(item.signedAt()), stringify(item.createdAt()))).toList(),
            events.stream().map(this::toTrackingEvent).toList());
    }

    private String specimensSummaryType(String caseId) {
        return technicalWorkflowRepository.findSpecimensByCaseId(caseId).stream()
            .map(Specimen::specimenType)
            .filter(item -> item != null && !item.isBlank())
            .distinct()
            .sorted()
            .reduce((left, right) -> left + "," + right)
            .orElse(null);
    }

    private void ensureAssignedDoctor(DiagnosticReportRepository.DiagnosticTask task, String userId) {
        boolean allowed = userId != null && (userId.equals(task.diagnosisDoctorUserId()) || userId.equals(task.primaryDoctorUserId()));
        if (!allowed) {
            throw new BlBusinessException(BlErrorCode.PERMISSION_DENIED, 403, "User is not assigned to diagnostic task");
        }
    }

    private void ensureDraftReport(DiagnosticReportRepository.PathologyReport report) {
        if (!REPORT_DRAFT.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report is not editable draft");
        }
    }

    private DiagnosticReportRepository.DiagnosticTask getDiagnosticTask(String taskId) {
        return diagnosticReportRepository.findDiagnosticTaskById(taskId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Diagnostic task not found"));
    }

    private DiagnosticReportRepository.PathologyReport getReport(String reportId) {
        return diagnosticReportRepository.findPathologyReportById(reportId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology report not found"));
    }

    private PathologyCase getCase(String caseId) {
        return technicalWorkflowRepository.findPathologyCaseById(caseId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Pathology case not found"));
    }

    private Application getApplication(String applicationId) {
        return applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Application not found"));
    }

    private void insertWorkflowEvent(String caseId,
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

    private TaskView toTaskView(DiagnosticReportRepository.DiagnosticTask task) {
        return new TaskView(
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

    private PathologyReportView toReportView(DiagnosticReportRepository.PathologyReport report) {
        return new PathologyReportView(
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

    private TrackingEventView toTrackingEvent(TrackingEvent event) {
        return new TrackingEventView(
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

    private String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    public record PendingDiagnosticTaskQuery(
        int page,
        int size,
        String taskType,
        String taskStatus,
        String pathologyNo
    ) {
    }

    public record PendingDiagnosticTaskPage(List<TaskView> items, int page, int size, long total) {
    }

    public record TaskView(
        String id,
        String applicationId,
        String applicationNo,
        String patientName,
        String caseId,
        String pathologyNo,
        String taskType,
        String taskStatus,
        String diagnosisDoctorUserId,
        String diagnosisDoctorName,
        String primaryDoctorUserId,
        String primaryDoctorName,
        String reviewerUserId,
        String reviewerName,
        String assignedAt,
        String acceptedAt,
        String completedAt,
        String remarks
    ) {
    }

    public record DiagnosticTaskResult(String taskId, String caseId, String caseStatus, String taskStatus) {
    }

    public record AssignDiagnosticTaskCommand(
        String taskId,
        String diagnosisDoctorUserId,
        String diagnosisDoctorName,
        String primaryDoctorUserId,
        String primaryDoctorName,
        String reviewerUserId,
        String reviewerName,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record TaskActionCommand(
        String taskId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record CreatePathologyReportCommand(
        String caseId,
        String taskId,
        String clinicalDiagnosis,
        String grossExam,
        String microscopicExam,
        String finalDiagnosis,
        String richTextContent,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record UpdateReportDraftCommand(
        String reportId,
        String clinicalDiagnosis,
        String grossExam,
        String microscopicExam,
        String finalDiagnosis,
        String richTextContent,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record ReportActionCommand(
        String reportId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String remarks
    ) {
    }

    public record RejectReportCommand(
        String reportId,
        String operatorUserId,
        String operatorName,
        String terminalCode,
        String rejectReason
    ) {
    }

    public record PathologyReportResult(
        String reportId,
        String caseId,
        String reportNo,
        String reportStatus,
        Integer versionNo,
        String versionStatus
    ) {
    }

    public record DiagnosticWorkbenchView(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        String submittingDepartmentName,
        String submittingDoctorName,
        String clinicalDiagnosis,
        List<WorkbenchSpecimenSummary> specimens,
        List<WorkbenchBlockSummary> blocks,
        List<WorkbenchSlideSummary> slides,
        List<TaskView> diagnosticTasks,
        PathologyReportView currentReport,
        List<TrackingEventView> recentEvents
    ) {
    }

    public record WorkbenchSpecimenSummary(
        String specimenId,
        String specimenNo,
        String barcode,
        String specimenName,
        String specimenStatus
    ) {
    }

    public record WorkbenchBlockSummary(
        String blockId,
        String specimenId,
        String blockCode,
        String embeddingBoxNo,
        String description
    ) {
    }

    public record WorkbenchSlideSummary(
        String slideId,
        String specimenId,
        String embeddingBoxId,
        String slideNo,
        String slideStatus,
        String qualityStatus
    ) {
    }

    public record PathologyReportView(
        String reportId,
        String reportNo,
        String reportStatus,
        String clinicalDiagnosis,
        String grossExam,
        String microscopicExam,
        String finalDiagnosis,
        String richTextContent,
        String submittedAt,
        String reviewedAt,
        String signedAt,
        String publishedAt,
        String reviewerName,
        String signedByName,
        int versionNo
    ) {
    }

    public record TrackingEventView(
        String nodeCode,
        String eventType,
        String eventStatus,
        String eventTime,
        String operatorName,
        String eventContent
    ) {
    }

    public record ReportTrackingView(
        String caseId,
        String applicationNo,
        String pathologyNo,
        String caseStatus,
        String patientName,
        List<TaskView> diagnosticTasks,
        PathologyReportView currentReport,
        List<ReportVersionView> versions,
        List<TrackingEventView> events
    ) {
    }

    public record ReportVersionView(
        String versionId,
        int versionNo,
        String versionStatus,
        String finalDiagnosisSnapshot,
        String signedAt,
        String createdAt
    ) {
    }
}
