package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import com.company.bl.notification.application.WorkflowNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
class DiagnosticTaskWorkflowService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;
    private final WorkflowNotificationService workflowNotificationService;

    DiagnosticTaskWorkflowService(DiagnosticReportRepository diagnosticReportRepository,
                                  TechnicalWorkflowRepository technicalWorkflowRepository,
                                  DiagnosticReportSupport diagnosticReportSupport,
                                  WorkflowNotificationService workflowNotificationService) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
        this.workflowNotificationService = workflowNotificationService;
    }

    @Transactional
    void createPrimaryDiagnosticTaskIfAbsent(String caseId, String remarks) {
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(caseId);
        if (!diagnosticReportRepository.findActiveDiagnosticTasksByCaseIdAndType(
            caseId, DiagnosticReportConstants.TASK_PRIMARY).isEmpty()) {
            return;
        }
        diagnosticReportRepository.insertDiagnosticTask(new DiagnosticReportRepository.CreateDiagnosticTaskCommand(
            diagnosticReportSupport.nextId("DT"),
            caseId,
            null,
            pathologyCase.pathologyNo(),
            DiagnosticReportConstants.TASK_PRIMARY,
            DiagnosticReportConstants.TASK_PENDING,
            "NORMAL",
            remarks,
            LocalDateTime.now()));
    }

    @Transactional
    DiagnosticReportModels.DiagnosticTaskResult assignTask(DiagnosticReportModels.AssignDiagnosticTaskCommand command) {
        DiagnosticReportRepository.DiagnosticTask task = diagnosticReportSupport.getDiagnosticTask(command.taskId());
        if (!isAssignableStatus(task.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Diagnostic task is not assignable");
        }

        EffectiveAssignment effectiveAssignment = mergeAssignment(task, command);
        boolean reassignment = DiagnosticReportConstants.TASK_ASSIGNED.equals(task.status());
        if (reassignment && isSameAssignment(task, effectiveAssignment, command.remarks())) {
            return new DiagnosticReportModels.DiagnosticTaskResult(task.id(), task.caseId(), "DIAGNOSIS_PENDING", task.status());
        }

        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.assignDiagnosticTask(new DiagnosticReportRepository.AssignDiagnosticTaskCommand(
            command.taskId(),
            command.operatorUserId(),
            command.operatorName(),
            effectiveAssignment.diagnosisDoctorUserId(),
            effectiveAssignment.diagnosisDoctorName(),
            effectiveAssignment.primaryDoctorUserId(),
            effectiveAssignment.primaryDoctorName(),
            effectiveAssignment.reviewerUserId(),
            effectiveAssignment.reviewerName(),
            command.remarks(),
            now));

        diagnosticReportSupport.insertWorkflowEvent(
            task.caseId(),
            "DIAGNOSIS_ASSIGN",
            reassignment ? "REASSIGN" : "ASSIGN",
            "SUCCESS",
            command.operatorUserId(),
            command.operatorName(),
            command.terminalCode(),
            reassignment ? "Diagnostic task reassigned" : "Diagnostic task assigned");

        DiagnosticReportRepository.DiagnosticTask updated = diagnosticReportSupport.getDiagnosticTask(command.taskId());
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(task.caseId());
        workflowNotificationService.notifyUsers(new WorkflowNotificationService.BulkNotificationCommand(
            WorkflowNotificationService.TOPIC_DIAG_TASK_ASSIGN,
            WorkflowNotificationService.CATEGORY_TODO_TASK,
            WorkflowNotificationService.LEVEL_MEDIUM,
            reassignment ? "Diagnostic task reassigned" : "Diagnostic task assigned",
            assignmentNotificationBody(pathologyCase.pathologyNo(), reassignment),
            assignmentNotificationSummary(pathologyCase.pathologyNo(), reassignment),
            null,
            "/doctor-workflow/assignment",
            buildTaskQuery(updated, pathologyCase.pathologyNo()),
            "View task",
            command.operatorUserId(),
            false,
            buildAssignmentRecipients(updated)
        ));
        return new DiagnosticReportModels.DiagnosticTaskResult(updated.id(), updated.caseId(), "DIAGNOSIS_PENDING", updated.status());
    }

    @Transactional
    DiagnosticReportModels.DiagnosticTaskResult acceptTask(DiagnosticReportModels.TaskActionCommand command) {
        DiagnosticReportRepository.DiagnosticTask task = diagnosticReportSupport.getDiagnosticTask(command.taskId());
        if (!DiagnosticReportConstants.TASK_ASSIGNED.equals(task.status())
            && !DiagnosticReportConstants.TASK_PENDING.equals(task.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Diagnostic task is not assignable");
        }
        diagnosticReportSupport.ensureAssignedDoctor(task, command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.acceptDiagnosticTask(task.id(), command.remarks(), now);
        diagnosticReportSupport.insertWorkflowEvent(task.caseId(), "DIAGNOSIS_ACCEPT", "ACCEPT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Diagnostic task accepted");
        DiagnosticReportRepository.DiagnosticTask updated = diagnosticReportSupport.getDiagnosticTask(task.id());
        return new DiagnosticReportModels.DiagnosticTaskResult(updated.id(), updated.caseId(), "DIAGNOSIS_PENDING", updated.status());
    }

    @Transactional
    DiagnosticReportModels.DiagnosticTaskResult startTask(DiagnosticReportModels.TaskActionCommand command) {
        DiagnosticReportRepository.DiagnosticTask task = diagnosticReportSupport.getDiagnosticTask(command.taskId());
        if (!DiagnosticReportConstants.TASK_ASSIGNED.equals(task.status())
            && !DiagnosticReportConstants.TASK_ACCEPTED.equals(task.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Diagnostic task cannot be started");
        }
        diagnosticReportSupport.ensureAssignedDoctor(task, command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        diagnosticReportRepository.startDiagnosticTask(task.id(), command.remarks(), now);
        technicalWorkflowRepository.updatePathologyCaseStatus(task.caseId(), "DIAGNOSING");
        diagnosticReportSupport.insertWorkflowEvent(task.caseId(), "DIAGNOSIS_START", "START", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Diagnostic task started");
        DiagnosticReportRepository.DiagnosticTask updated = diagnosticReportSupport.getDiagnosticTask(task.id());
        return new DiagnosticReportModels.DiagnosticTaskResult(updated.id(), updated.caseId(), "DIAGNOSING", updated.status());
    }

    private boolean isAssignableStatus(String status) {
        return DiagnosticReportConstants.TASK_PENDING.equals(status)
            || DiagnosticReportConstants.TASK_ASSIGNED.equals(status);
    }

    private boolean isSameAssignment(DiagnosticReportRepository.DiagnosticTask task,
                                     EffectiveAssignment assignment,
                                     String remarks) {
        return Objects.equals(task.diagnosisDoctorUserId(), assignment.diagnosisDoctorUserId())
            && Objects.equals(task.diagnosisDoctorName(), assignment.diagnosisDoctorName())
            && Objects.equals(task.primaryDoctorUserId(), assignment.primaryDoctorUserId())
            && Objects.equals(task.primaryDoctorName(), assignment.primaryDoctorName())
            && Objects.equals(task.reviewerUserId(), assignment.reviewerUserId())
            && Objects.equals(task.reviewerName(), assignment.reviewerName())
            && Objects.equals(task.remarks(), remarks);
    }

    private EffectiveAssignment mergeAssignment(DiagnosticReportRepository.DiagnosticTask task,
                                                DiagnosticReportModels.AssignDiagnosticTaskCommand command) {
        return new EffectiveAssignment(
            pickField(command.diagnosisDoctorUserId(), task.diagnosisDoctorUserId()),
            pickField(command.diagnosisDoctorName(), task.diagnosisDoctorName()),
            pickField(command.primaryDoctorUserId(), task.primaryDoctorUserId()),
            pickField(command.primaryDoctorName(), task.primaryDoctorName()),
            pickField(command.reviewerUserId(), task.reviewerUserId()),
            pickField(command.reviewerName(), task.reviewerName())
        );
    }

    private List<WorkflowNotificationService.Recipient> buildAssignmentRecipients(
        DiagnosticReportRepository.DiagnosticTask task
    ) {
        List<WorkflowNotificationService.Recipient> recipients = new ArrayList<>();
        addRecipient(recipients, task.diagnosisDoctorUserId(), task.diagnosisDoctorName());
        addRecipient(recipients, task.primaryDoctorUserId(), task.primaryDoctorName());
        addRecipient(recipients, task.reviewerUserId(), task.reviewerName());
        return recipients;
    }

    private void addRecipient(List<WorkflowNotificationService.Recipient> recipients,
                              String userId,
                              String name) {
        if (hasText(userId) && hasText(name)) {
            recipients.add(new WorkflowNotificationService.Recipient(userId, name));
        }
    }

    private String pickField(String incomingValue, String currentValue) {
        return hasText(incomingValue) ? incomingValue.trim() : currentValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String assignmentNotificationBody(String pathologyNo, boolean reassignment) {
        return reassignment
            ? "Pathology case %s has been reassigned to you. Please review it in time.".formatted(pathologyNo)
            : "Pathology case %s has been assigned to you. Please review it in time.".formatted(pathologyNo);
    }

    private String assignmentNotificationSummary(String pathologyNo, boolean reassignment) {
        return reassignment
            ? "Pathology case %s has been reassigned".formatted(pathologyNo)
            : "Pathology case %s has been assigned".formatted(pathologyNo);
    }

    private Map<String, String> buildTaskQuery(
        DiagnosticReportRepository.DiagnosticTask task,
        String pathologyNo
    ) {
        Map<String, String> query = new LinkedHashMap<>();
        putIfPresent(query, "taskId", task.id());
        putIfPresent(query, "caseId", task.caseId());
        putIfPresent(query, "pathologyNo", pathologyNo);
        return query;
    }

    private void putIfPresent(Map<String, String> query, String key, String value) {
        if (value != null && !value.isBlank()) {
            query.put(key, value);
        }
    }

    private record EffectiveAssignment(
        String diagnosisDoctorUserId,
        String diagnosisDoctorName,
        String primaryDoctorUserId,
        String primaryDoctorName,
        String reviewerUserId,
        String reviewerName
    ) {
    }
}
