package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.integration.application.BillingManagementService;
import com.company.bl.support.application.NumberingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
class DiagnosticReportLifecycleService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final NumberingService numberingService;
    private final DiagnosticReportSupport diagnosticReportSupport;
    private final BillingManagementService billingManagementService;

    DiagnosticReportLifecycleService(DiagnosticReportRepository diagnosticReportRepository,
                                     TechnicalWorkflowRepository technicalWorkflowRepository,
                                     NumberingService numberingService,
                                     DiagnosticReportSupport diagnosticReportSupport,
                                     BillingManagementService billingManagementService) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.numberingService = numberingService;
        this.diagnosticReportSupport = diagnosticReportSupport;
        this.billingManagementService = billingManagementService;
    }

    @Transactional
    DiagnosticReportModels.PathologyReportResult createReport(DiagnosticReportModels.CreatePathologyReportCommand command) {
        DiagnosticReportRepository.DiagnosticTask task = diagnosticReportSupport.getDiagnosticTask(command.taskId());
        if (!task.caseId().equals(command.caseId())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Task does not belong to case");
        }
        if (!DiagnosticReportConstants.TASK_IN_PROGRESS.equals(task.status())
            && !DiagnosticReportConstants.TASK_ACCEPTED.equals(task.status())
            && !DiagnosticReportConstants.TASK_ASSIGNED.equals(task.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Diagnostic task is not editable");
        }
        diagnosticReportSupport.ensureAssignedDoctor(task, command.operatorUserId());
        String reportScope = resolveReportScope(task);
        DiagnosticReportRepository.PathologyReport existing = diagnosticReportRepository
            .findCurrentReportByCaseIdAndScope(command.caseId(), reportScope)
            .orElse(null);
        if (existing != null && DiagnosticReportConstants.REPORT_DRAFT.equals(existing.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Draft report already exists");
        }
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(command.caseId());
        Application application = diagnosticReportSupport.getApplication(pathologyCase.applicationId());
        LocalDateTime now = LocalDateTime.now();
        String reportId = diagnosticReportSupport.nextId("RPT");
        diagnosticReportRepository.insertPathologyReport(new DiagnosticReportRepository.CreatePathologyReportCommand(
            reportId,
            command.caseId(),
            command.taskId(),
            numberingService.generateReportNo(),
            pathologyCase.pathologyNo(),
            reportScope,
            1,
            DiagnosticReportConstants.REPORT_DRAFT,
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
        diagnosticReportSupport.insertWorkflowEvent(command.caseId(), "REPORT_DRAFT", "CREATE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Draft report created");
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(reportId);
        return new DiagnosticReportModels.PathologyReportResult(report.id(), report.caseId(), report.reportNo(), report.reportStatus(), null, null);
    }

    private String resolveReportScope(DiagnosticReportRepository.DiagnosticTask task) {
        if (DiagnosticReportConstants.TASK_FROZEN.equalsIgnoreCase(task.taskType())) {
            return DiagnosticReportConstants.REPORT_SCOPE_FROZEN;
        }
        return DiagnosticReportConstants.REPORT_SCOPE_ROUTINE;
    }

    @Transactional
    DiagnosticReportModels.PathologyReportResult saveDraft(DiagnosticReportModels.UpdateReportDraftCommand command) {
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(command.reportId());
        DiagnosticReportRepository.DiagnosticTask task = diagnosticReportSupport.getDiagnosticTask(report.taskId());
        String eventNodeCode;
        String eventContent;
        if (DiagnosticReportConstants.REPORT_DRAFT.equals(report.reportStatus())) {
            diagnosticReportSupport.ensureAssignedDoctor(task, command.operatorUserId());
            eventNodeCode = "REPORT_DRAFT";
            eventContent = "Draft report saved";
        } else if (DiagnosticReportConstants.REPORT_REVIEWED.equals(report.reportStatus())) {
            diagnosticReportSupport.ensureReviewer(task, command.operatorUserId());
            eventNodeCode = "REPORT_REVIEW";
            eventContent = "Reviewed report saved";
        } else {
            diagnosticReportSupport.ensureDraftReport(report);
            eventNodeCode = "REPORT_DRAFT";
            eventContent = "Draft report saved";
        }
        diagnosticReportRepository.updatePathologyReportDraft(new DiagnosticReportRepository.UpdatePathologyReportDraftCommand(
            report.id(),
            command.grossExam(),
            command.microscopicExam(),
            command.clinicalDiagnosis(),
            command.finalDiagnosis(),
            command.richTextContent(),
            command.remarks(),
            LocalDateTime.now()));
        diagnosticReportSupport.insertWorkflowEvent(report.caseId(), eventNodeCode, "SAVE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), eventContent);
        DiagnosticReportRepository.PathologyReport updated = diagnosticReportSupport.getReport(report.id());
        return new DiagnosticReportModels.PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), null, null);
    }

    @Transactional
    DiagnosticReportModels.PathologyReportResult submitReport(DiagnosticReportModels.ReportActionCommand command) {
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(command.reportId());
        diagnosticReportSupport.ensureDraftReport(report);
        diagnosticReportSupport.ensureAssignedDoctor(diagnosticReportSupport.getDiagnosticTask(report.taskId()), command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.submitPathologyReport(report.id(), command.remarks(), now);
        diagnosticReportRepository.markDiagnosticTaskSubmitted(report.taskId(), command.remarks(), now);
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "REPORT_PENDING_REVIEW");
        diagnosticReportSupport.insertWorkflowEvent(report.caseId(), "REPORT_SUBMIT", "SUBMIT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Report submitted");
        DiagnosticReportRepository.PathologyReport updated = diagnosticReportSupport.getReport(report.id());
        return new DiagnosticReportModels.PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), null, null);
    }

    @Transactional
    DiagnosticReportModels.PathologyReportResult reviewReport(DiagnosticReportModels.ReportActionCommand command) {
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(command.reportId());
        if (!DiagnosticReportConstants.REPORT_SUBMITTED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report is not submitted");
        }
        diagnosticReportSupport.ensureReviewer(diagnosticReportSupport.getDiagnosticTask(report.taskId()), command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.reviewPathologyReport(report.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        diagnosticReportRepository.markDiagnosticTaskReviewed(report.taskId(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "REPORT_REVIEWING");
        diagnosticReportSupport.insertWorkflowEvent(report.caseId(), "REPORT_REVIEW", "REVIEW", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Report reviewed");
        DiagnosticReportRepository.PathologyReport updated = diagnosticReportSupport.getReport(report.id());
        return new DiagnosticReportModels.PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), null, null);
    }

    @Transactional
    DiagnosticReportModels.PathologyReportResult rejectReport(DiagnosticReportModels.RejectReportCommand command) {
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(command.reportId());
        if (!DiagnosticReportConstants.REPORT_SUBMITTED.equals(report.reportStatus())
            && !DiagnosticReportConstants.REPORT_REVIEWED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report cannot be rejected");
        }
        diagnosticReportSupport.ensureReviewer(diagnosticReportSupport.getDiagnosticTask(report.taskId()), command.operatorUserId());
        diagnosticReportRepository.rejectPathologyReport(report.id(), command.rejectReason());
        diagnosticReportRepository.revertDiagnosticTaskToInProgress(report.taskId(), command.rejectReason());
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "DIAGNOSING");
        diagnosticReportSupport.insertWorkflowEvent(report.caseId(), "REPORT_REJECT", "REJECT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.rejectReason());
        DiagnosticReportRepository.PathologyReport updated = diagnosticReportSupport.getReport(report.id());
        return new DiagnosticReportModels.PathologyReportResult(updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), null, null);
    }

    @Transactional
    DiagnosticReportModels.PathologyReportResult signReport(DiagnosticReportModels.ReportActionCommand command) {
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(command.reportId());
        if (!DiagnosticReportConstants.REPORT_REVIEWED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report is not reviewed");
        }
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.signPathologyReport(report.id(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        DiagnosticReportRepository.PathologyReport updated = diagnosticReportSupport.getReport(report.id());
        diagnosticReportRepository.insertReportVersion(new DiagnosticReportRepository.CreateReportVersionCommand(
            diagnosticReportSupport.nextId("RV"),
            updated.id(),
            updated.caseId(),
            updated.reportScope(),
            updated.reportSeq(),
            updated.versionNo(),
            DiagnosticReportConstants.REPORT_SIGNED,
            updated.finalDiagnosis(),
            updated.richTextContent(),
            command.operatorUserId(),
            command.operatorName(),
            now,
            now));
        technicalWorkflowRepository.updatePathologyCaseStatus(report.caseId(), "REPORT_SIGNED");
        diagnosticReportSupport.insertWorkflowEvent(report.caseId(), "REPORT_SIGN", "SIGN", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Report signed");
        return new DiagnosticReportModels.PathologyReportResult(
            updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), updated.versionNo(), DiagnosticReportConstants.REPORT_SIGNED);
    }

    @Transactional
    DiagnosticReportModels.PathologyReportResult publishReport(DiagnosticReportModels.ReportActionCommand command) {
        DiagnosticReportRepository.PathologyReport report = diagnosticReportSupport.getReport(command.reportId());
        if (!DiagnosticReportConstants.REPORT_SIGNED.equals(report.reportStatus())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Report is not signed");
        }
        diagnosticReportSupport.ensureSignedByCurrentUser(report, command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.publishPathologyReport(report.id(), command.remarks(), now);
        DiagnosticReportRepository.PathologyReport updated = diagnosticReportSupport.getReport(report.id());
        diagnosticReportRepository.insertReportVersion(new DiagnosticReportRepository.CreateReportVersionCommand(
            diagnosticReportSupport.nextId("RV"),
            updated.id(),
            updated.caseId(),
            updated.reportScope(),
            updated.reportSeq(),
            updated.versionNo(),
            DiagnosticReportConstants.REPORT_PUBLISHED,
            updated.finalDiagnosis(),
            updated.richTextContent(),
            updated.signedByUserId(),
            updated.signedByName(),
            updated.signedAt(),
            now));
        diagnosticReportRepository.completeDiagnosticTask(updated.taskId(), command.remarks(), now);
        technicalWorkflowRepository.updatePathologyCaseStatus(updated.caseId(), "REPORT_PUBLISHED");
        diagnosticReportSupport.insertWorkflowEvent(updated.caseId(), "REPORT_PUBLISH", "PUBLISH", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Report published");
        billingManagementService.triggerReportPublishBilling(
            updated.caseId(),
            updated.id(),
            updated.reportNo(),
            updated.finalDiagnosis(),
            command.operatorUserId(),
            command.operatorName());
        return new DiagnosticReportModels.PathologyReportResult(
            updated.id(), updated.caseId(), updated.reportNo(), updated.reportStatus(), updated.versionNo(), DiagnosticReportConstants.REPORT_PUBLISHED);
    }

    @Transactional
    DiagnosticReportModels.FormalReportVersionBatchActionResult printFormalReportVersions(
        DiagnosticReportModels.FormalReportVersionBatchActionCommand command
    ) {
        List<DiagnosticReportRepository.ReportVersion> versions = loadRequestedVersions(command.versionIds());
        LocalDateTime now = LocalDateTime.now();
        List<String> successIds = new ArrayList<>();
        List<DiagnosticReportModels.FormalReportVersionBatchActionItemResult> items = new ArrayList<>();
        for (DiagnosticReportRepository.ReportVersion version : versions) {
            if (!isFormalVersion(version)) {
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), false, "仅正式报告支持打印"));
                continue;
            }
            if ("PRINTED".equals(version.printStatus())) {
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), false, "报告已打印"));
                continue;
            }
            successIds.add(version.id());
            items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), true, "打印时间已记录"));
        }
        if (!successIds.isEmpty()) {
            diagnosticReportRepository.markReportVersionsPrinted(successIds, now);
            versions.stream()
                .filter(version -> successIds.contains(version.id()))
                .forEach(version -> diagnosticReportSupport.insertWorkflowEvent(
                    version.caseId(),
                    "REPORT_PRINT",
                    "PRINT",
                    "SUCCESS",
                    command.operatorUserId(),
                    command.operatorName(),
                    command.terminalCode(),
                    buildDistributionEventContent("已打印", version)));
        }
        return new DiagnosticReportModels.FormalReportVersionBatchActionResult(
            items.size(),
            successIds.size(),
            items.size() - successIds.size(),
            items);
    }

    @Transactional
    DiagnosticReportModels.FormalReportVersionBatchActionResult issueFormalReportVersions(
        DiagnosticReportModels.FormalReportVersionBatchActionCommand command
    ) {
        List<DiagnosticReportRepository.ReportVersion> versions = loadRequestedVersions(command.versionIds());
        LocalDateTime now = LocalDateTime.now();
        List<String> successIds = new ArrayList<>();
        List<String> scheduledIds = new ArrayList<>();
        List<DiagnosticReportModels.FormalReportVersionBatchActionItemResult> items = new ArrayList<>();
        String issueMode = command.issueMode() == null || command.issueMode().isBlank()
            ? "IMMEDIATE"
            : command.issueMode().trim();
        LocalDateTime plannedIssueAt = resolvePlannedIssueAt(issueMode, now);
        for (DiagnosticReportRepository.ReportVersion version : versions) {
            if (!isFormalVersion(version)) {
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), false, "仅正式报告支持发放"));
                continue;
            }
            if (!"PRINTED".equals(version.printStatus())) {
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), false, "未打印报告不可发放"));
                continue;
            }
            if (!"PENDING".equals(version.deliveryStatus())) {
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), false, "仅待发放报告可执行发放"));
                continue;
            }
            if ("IMMEDIATE".equals(issueMode)) {
                successIds.add(version.id());
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), true, "报告已发放"));
            } else {
                scheduledIds.add(version.id());
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(
                    version.id(),
                    true,
                    "报告已计划发放"));
            }
        }
        if (!successIds.isEmpty()) {
            diagnosticReportRepository.markReportVersionsIssued(successIds, now);
            versions.stream()
                .filter(version -> successIds.contains(version.id()))
                .forEach(version -> diagnosticReportSupport.insertWorkflowEvent(
                    version.caseId(),
                    "REPORT_ISSUE",
                    "ISSUE",
                    "SUCCESS",
                    command.operatorUserId(),
                    command.operatorName(),
                    command.terminalCode(),
                    buildDistributionEventContent("已发放", version)));
        }
        if (!scheduledIds.isEmpty() && plannedIssueAt != null) {
            diagnosticReportRepository.scheduleReportVersionsIssue(scheduledIds, plannedIssueAt);
            versions.stream()
                .filter(version -> scheduledIds.contains(version.id()))
                .forEach(version -> diagnosticReportSupport.insertWorkflowEvent(
                    version.caseId(),
                    "REPORT_SCHEDULE_ISSUE",
                    "SCHEDULE_ISSUE",
                    "SUCCESS",
                    command.operatorUserId(),
                    command.operatorName(),
                    command.terminalCode(),
                    buildScheduledDistributionEventContent(version, plannedIssueAt)));
        }
        return new DiagnosticReportModels.FormalReportVersionBatchActionResult(
            items.size(),
            successIds.size() + scheduledIds.size(),
            items.size() - successIds.size() - scheduledIds.size(),
            items);
    }

    @Transactional
    DiagnosticReportModels.FormalReportVersionBatchActionResult recallFormalReportVersions(
        DiagnosticReportModels.FormalReportVersionBatchActionCommand command
    ) {
        List<DiagnosticReportRepository.ReportVersion> versions = loadRequestedVersions(command.versionIds());
        LocalDateTime now = LocalDateTime.now();
        List<String> successIds = new ArrayList<>();
        List<DiagnosticReportModels.FormalReportVersionBatchActionItemResult> items = new ArrayList<>();
        for (DiagnosticReportRepository.ReportVersion version : versions) {
            if (!isFormalVersion(version)) {
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), false, "仅正式报告支持回收"));
                continue;
            }
            if (!"ISSUED".equals(version.deliveryStatus())) {
                items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), false, "仅已发放报告可执行回收"));
                continue;
            }
            successIds.add(version.id());
            items.add(new DiagnosticReportModels.FormalReportVersionBatchActionItemResult(version.id(), true, "报告已回收"));
        }
        if (!successIds.isEmpty()) {
            diagnosticReportRepository.markReportVersionsRecalled(successIds, now);
            versions.stream()
                .filter(version -> successIds.contains(version.id()))
                .forEach(version -> diagnosticReportSupport.insertWorkflowEvent(
                    version.caseId(),
                    "REPORT_RECALL",
                    "RECALL",
                    "SUCCESS",
                    command.operatorUserId(),
                    command.operatorName(),
                    command.terminalCode(),
                    buildDistributionEventContent("已回收", version)));
        }
        return new DiagnosticReportModels.FormalReportVersionBatchActionResult(
            items.size(),
            successIds.size(),
            items.size() - successIds.size(),
            items);
    }

    private String buildDistributionEventContent(String actionLabel,
                                                 DiagnosticReportRepository.ReportVersion version) {
        return "正式报告" + buildReportVersionLabel(version) + actionLabel;
    }

    private String buildScheduledDistributionEventContent(DiagnosticReportRepository.ReportVersion version,
                                                          LocalDateTime plannedIssueAt) {
        return buildDistributionEventContent("已计划发放", version) + "，计划时间 " + plannedIssueAt;
    }

    private String buildReportVersionLabel(DiagnosticReportRepository.ReportVersion version) {
        return "V" + version.versionNo();
    }

    private List<DiagnosticReportRepository.ReportVersion> loadRequestedVersions(List<String> versionIds) {
        if (versionIds == null || versionIds.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Version IDs are required");
        }
        Set<String> uniqueVersionIds = new LinkedHashSet<>(versionIds);
        List<DiagnosticReportRepository.ReportVersion> versions = new ArrayList<>();
        for (String versionId : uniqueVersionIds) {
            versions.add(diagnosticReportSupport.getReportVersion(versionId));
        }
        return versions;
    }

    private boolean isFormalVersion(DiagnosticReportRepository.ReportVersion version) {
        return DiagnosticReportConstants.REPORT_SIGNED.equals(version.versionStatus())
            || DiagnosticReportConstants.REPORT_PUBLISHED.equals(version.versionStatus());
    }

    private LocalDateTime resolvePlannedIssueAt(String issueMode, LocalDateTime now) {
        return switch (issueMode) {
            case "DELAY_2_HOURS" -> now.plusHours(2);
            case "DELAY_3_HOURS" -> now.plusHours(3);
            default -> null;
        };
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
}
