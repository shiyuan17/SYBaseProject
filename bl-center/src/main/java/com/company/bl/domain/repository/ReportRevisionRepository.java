package com.company.bl.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRevisionRepository {

    void insertRevisionRequest(CreateReportRevisionRequestCommand command);

    Optional<ReportRevisionRequest> findRevisionRequestById(String requestId);

    Optional<ReportRevisionRequest> findPendingRevisionRequestByReportId(String reportId);

    List<ReportRevisionRequest> findRevisionRequestsByCaseId(String caseId);

    void approveRevisionRequest(String requestId,
                                String reviewedByUserId,
                                String reviewedByName,
                                int approvedVersionNo,
                                String remarks,
                                LocalDateTime reviewedAt);

    void rejectRevisionRequest(String requestId,
                               String reviewedByUserId,
                               String reviewedByName,
                               String rejectReason,
                               LocalDateTime reviewedAt);

    record CreateReportRevisionRequestCommand(
        String id,
        String caseId,
        String reportId,
        int currentVersionNo,
        String requestStatus,
        String requestReason,
        String requestedByUserId,
        String requestedByName,
        LocalDateTime requestedAt,
        String remarks
    ) {
    }

    record ReportRevisionRequest(
        String id,
        String caseId,
        String reportId,
        int currentVersionNo,
        String requestStatus,
        String requestReason,
        String requestedByUserId,
        String requestedByName,
        LocalDateTime requestedAt,
        String reviewedByUserId,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String rejectReason,
        Integer approvedVersionNo,
        String remarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
}
