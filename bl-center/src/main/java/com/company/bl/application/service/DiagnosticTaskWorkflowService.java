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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        if (!DiagnosticReportConstants.TASK_PENDING.equals(task.status())) {
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
        diagnosticReportSupport.insertWorkflowEvent(task.caseId(), "DIAGNOSIS_ASSIGN", "ASSIGN", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), "Diagnostic task assigned");
        DiagnosticReportRepository.DiagnosticTask updated = diagnosticReportSupport.getDiagnosticTask(command.taskId());
        PathologyCase pathologyCase = diagnosticReportSupport.getCase(task.caseId());
        workflowNotificationService.notifyUsers(new WorkflowNotificationService.BulkNotificationCommand(
            WorkflowNotificationService.TOPIC_DIAG_TASK_ASSIGN,
            WorkflowNotificationService.CATEGORY_TODO_TASK,
            WorkflowNotificationService.LEVEL_MEDIUM,
            "诊断任务已分派",
            "病理号 %s 的诊断任务已分派，请及时处理。".formatted(pathologyCase.pathologyNo()),
            "病理号 %s 的诊断任务已分派".formatted(pathologyCase.pathologyNo()),
            null,
            "/doctor-workflow/assignment",
            buildTaskQuery(updated, pathologyCase.pathologyNo()),
            "查看任务",
            command.operatorUserId(),
            false,
            List.of(
                new WorkflowNotificationService.Recipient(updated.diagnosisDoctorUserId(), updated.diagnosisDoctorName()),
                new WorkflowNotificationService.Recipient(updated.primaryDoctorUserId(), updated.primaryDoctorName()),
                new WorkflowNotificationService.Recipient(updated.reviewerUserId(), updated.reviewerName())
            )
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
}
