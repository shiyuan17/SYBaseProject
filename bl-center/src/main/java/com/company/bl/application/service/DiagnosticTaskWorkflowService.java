package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.repository.DiagnosticReportRepository;
import com.company.bl.domain.repository.TechnicalWorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
class DiagnosticTaskWorkflowService {

    private final DiagnosticReportRepository diagnosticReportRepository;
    private final TechnicalWorkflowRepository technicalWorkflowRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;

    DiagnosticTaskWorkflowService(DiagnosticReportRepository diagnosticReportRepository,
                                  TechnicalWorkflowRepository technicalWorkflowRepository,
                                  DiagnosticReportSupport diagnosticReportSupport) {
        this.diagnosticReportRepository = diagnosticReportRepository;
        this.technicalWorkflowRepository = technicalWorkflowRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
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
}
