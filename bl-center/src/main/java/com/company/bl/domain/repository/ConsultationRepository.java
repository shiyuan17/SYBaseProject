package com.company.bl.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository {

    void insertConsultationCase(CreateConsultationCaseCommand command);

    void insertConsultationParticipant(CreateConsultationParticipantCommand command);

    Optional<ConsultationCase> findConsultationCaseById(String consultationId);

    Optional<ConsultationParticipant> findConsultationParticipantById(String participantId);

    List<ConsultationCase> findConsultationsByCaseId(String caseId);

    List<ConsultationParticipant> findConsultationParticipants(String consultationId);

    void startConsultation(String consultationId, String remarks, LocalDateTime updatedAt);

    void commentConsultationParticipant(String participantId,
                                        String opinion,
                                        String draftedByUserId,
                                        String draftedByName,
                                        String remarks,
                                        LocalDateTime commentedAt);

    void completeConsultation(String consultationId,
                              String opinion,
                              String remarks,
                              LocalDateTime completedAt);

    record CreateConsultationCaseCommand(
        String id,
        String caseId,
        String consultationType,
        String status,
        String requestedByUserId,
        String requestedByName,
        LocalDateTime requestedAt,
        String hostUserId,
        String hostName,
        String remarks
    ) {
    }

    record CreateConsultationParticipantCommand(
        String id,
        String consultationId,
        String caseId,
        String participantUserId,
        String participantName,
        String participantRole,
        String remarks,
        LocalDateTime createdAt
    ) {
    }

    record ConsultationCase(
        String id,
        String caseId,
        String consultationType,
        String status,
        String requestedByUserId,
        String requestedByName,
        LocalDateTime requestedAt,
        String hostUserId,
        String hostName,
        String opinion,
        LocalDateTime completedAt,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record ConsultationParticipant(
        String id,
        String consultationId,
        String caseId,
        String participantUserId,
        String participantName,
        String participantRole,
        String opinion,
        String draftedByUserId,
        String draftedByName,
        boolean readFlag,
        LocalDateTime readAt,
        LocalDateTime commentedAt,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
