package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.ConsultationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
class InternalConsultationWorkflowService {

    private final ConsultationRepository consultationRepository;
    private final DiagnosticReportSupport diagnosticReportSupport;

    InternalConsultationWorkflowService(ConsultationRepository consultationRepository,
                                        DiagnosticReportSupport diagnosticReportSupport) {
        this.consultationRepository = consultationRepository;
        this.diagnosticReportSupport = diagnosticReportSupport;
    }

    @Transactional
    DiagnosticReportModels.ConsultationResult createConsultation(DiagnosticReportModels.CreateConsultationCommand command) {
        diagnosticReportSupport.getCase(command.caseId());
        diagnosticReportSupport.ensureAssignedDoctor(diagnosticReportSupport.getLatestDiagnosticTask(command.caseId()), command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        String consultationId = diagnosticReportSupport.nextId("CONS");
        consultationRepository.insertConsultationCase(new ConsultationRepository.CreateConsultationCaseCommand(
            consultationId,
            command.caseId(),
            DiagnosticReportConstants.CONSULTATION_INTERNAL,
            DiagnosticReportConstants.CONSULTATION_PENDING,
            command.operatorUserId(),
            command.operatorName(),
            now,
            command.operatorUserId(),
            command.operatorName(),
            command.remarks()));
        List<DiagnosticReportModels.ConsultationParticipantInput> participants = new ArrayList<>(command.participants());
        boolean hasHost = participants.stream().anyMatch(item -> command.operatorUserId().equals(item.participantUserId()));
        if (!hasHost) {
            participants.add(new DiagnosticReportModels.ConsultationParticipantInput(
                command.operatorUserId(),
                command.operatorName(),
                DiagnosticReportConstants.PARTICIPANT_HOST));
        }
        for (DiagnosticReportModels.ConsultationParticipantInput participant : participants) {
            String role = participant.participantUserId().equals(command.operatorUserId())
                ? DiagnosticReportConstants.PARTICIPANT_HOST
                : participant.participantRole();
            consultationRepository.insertConsultationParticipant(new ConsultationRepository.CreateConsultationParticipantCommand(
                diagnosticReportSupport.nextId("CP"),
                consultationId,
                command.caseId(),
                participant.participantUserId(),
                participant.participantName(),
                role,
                command.remarks(),
                now));
        }
        consultationRepository.startConsultation(consultationId, command.remarks(), now);
        diagnosticReportSupport.insertWorkflowEvent(command.caseId(), "CONSULTATION_CREATE", "CREATE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), consultationId);
        ConsultationRepository.ConsultationCase created = consultationRepository.findConsultationCaseById(consultationId).orElseThrow();
        return new DiagnosticReportModels.ConsultationResult(created.id(), created.caseId(), created.status());
    }

    @Transactional
    DiagnosticReportModels.ConsultationResult commentConsultationParticipant(DiagnosticReportModels.CommentConsultationParticipantCommand command) {
        ConsultationRepository.ConsultationCase consultationCase = consultationRepository.findConsultationCaseById(command.consultationId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Consultation not found"));
        if (!DiagnosticReportConstants.CONSULTATION_IN_PROGRESS.equals(consultationCase.status())
            && !DiagnosticReportConstants.CONSULTATION_PENDING.equals(consultationCase.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Consultation is not active");
        }
        ConsultationRepository.ConsultationParticipant participant = consultationRepository.findConsultationParticipantById(command.participantId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Consultation participant not found"));
        if (!participant.consultationId().equals(command.consultationId())) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Participant does not belong to consultation");
        }
        diagnosticReportSupport.ensureConsultationParticipant(participant, command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        consultationRepository.startConsultation(consultationCase.id(), command.remarks(), now);
        consultationRepository.commentConsultationParticipant(participant.id(), command.opinion(), command.operatorUserId(), command.operatorName(), command.remarks(), now);
        diagnosticReportSupport.insertWorkflowEvent(consultationCase.caseId(), "CONSULTATION_COMMENT", "COMMENT", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.opinion());
        ConsultationRepository.ConsultationCase updated = consultationRepository.findConsultationCaseById(consultationCase.id()).orElseThrow();
        return new DiagnosticReportModels.ConsultationResult(updated.id(), updated.caseId(), updated.status());
    }

    @Transactional
    DiagnosticReportModels.ConsultationResult completeConsultation(DiagnosticReportModels.CompleteConsultationCommand command) {
        ConsultationRepository.ConsultationCase consultationCase = consultationRepository.findConsultationCaseById(command.consultationId())
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Consultation not found"));
        if (DiagnosticReportConstants.CONSULTATION_COMPLETED.equals(consultationCase.status())) {
            throw new BlBusinessException(BlErrorCode.OPERATION_NOT_ALLOWED, 409, "Consultation already completed");
        }
        diagnosticReportSupport.ensureConsultationHost(consultationCase, command.operatorUserId());
        LocalDateTime now = LocalDateTime.now();
        consultationRepository.completeConsultation(consultationCase.id(), command.opinion(), command.remarks(), now);
        diagnosticReportSupport.insertWorkflowEvent(consultationCase.caseId(), "CONSULTATION_COMPLETE", "COMPLETE", "SUCCESS",
            command.operatorUserId(), command.operatorName(), command.terminalCode(), command.opinion());
        ConsultationRepository.ConsultationCase updated = consultationRepository.findConsultationCaseById(consultationCase.id()).orElseThrow();
        return new DiagnosticReportModels.ConsultationResult(updated.id(), updated.caseId(), updated.status());
    }
}
