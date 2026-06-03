package com.company.bl.application.service;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SpecimenWorkflowStatusPolicy {

    boolean isReceiptTerminalStatus(SpecimenStatus status) {
        return status == SpecimenStatus.RECEIVED
            || status == SpecimenStatus.REJECTED
            || status == SpecimenStatus.RETURNED;
    }

    boolean isTransportOrderReadyForReceipt(TransportOrderStatus status) {
        return status == TransportOrderStatus.PRINTED
            || status == TransportOrderStatus.HANDED_OVER
            || status == TransportOrderStatus.PARTIALLY_RECEIVED;
    }

    boolean isTransportItemTerminal(TransportItemStatus status) {
        return status == TransportItemStatus.COMPLETED || status == TransportItemStatus.RETURNED;
    }

    String resolveLatestLabelPrintBatchNo(List<Specimen> specimens) {
        String latestBatchNo = null;
        for (Specimen specimen : specimens) {
            if (!blank(specimen.labelPrintBatchNo())) {
                latestBatchNo = specimen.labelPrintBatchNo().trim();
            }
        }
        return latestBatchNo;
    }

    String resolveLatestBatchLabelPrintStatus(List<Specimen> specimens, String batchNo) {
        String resolvedStatus = null;
        int resolvedPriority = 0;
        for (Specimen specimen : specimens) {
            if (!batchNo.equals(specimen.labelPrintBatchNo())) {
                continue;
            }
            String status = normalizeStatus(specimen.labelPrintStatus());
            int priority = labelPrintStatusPriority(status);
            if (priority > resolvedPriority) {
                resolvedStatus = status;
                resolvedPriority = priority;
            }
        }
        return resolvedStatus;
    }

    String resolveLatestBatchLabelPrintMessage(List<TrackingEvent> events, List<Specimen> batchSpecimens) {
        if (batchSpecimens.isEmpty()) {
            return null;
        }
        Set<String> batchSpecimenIds = new HashSet<>();
        for (Specimen specimen : batchSpecimens) {
            batchSpecimenIds.add(specimen.id());
        }
        String latestMessage = null;
        for (TrackingEvent event : events) {
            if (!"LABEL_PRINT".equals(event.nodeCode())) {
                continue;
            }
            if (event.specimenId() == null || !batchSpecimenIds.contains(event.specimenId())) {
                continue;
            }
            latestMessage = event.eventContent();
        }
        return latestMessage;
    }

    String commandCheckInStatus(Specimen specimen) {
        return blank(specimen.checkInStatus()) ? "NOT_CHECKED_IN" : specimen.checkInStatus().trim().toUpperCase();
    }

    boolean canCheckInSpecimen(Specimen specimen) {
        return !isReceiptTerminalStatus(specimen.specimenStatus())
            && !"CHECKED_IN".equals(commandCheckInStatus(specimen))
            && isVerificationCompleted(specimen)
            && specimen.fixationStatus() == FixationStatus.COMPLETED
            && specimen.specimenConfirmedAt() != null;
    }

    boolean canCheckInApplication(List<Specimen> specimens) {
        return !specimens.isEmpty() && specimens.stream().allMatch(this::isCheckInSatisfiedOrReady);
    }

    boolean canTransportApplication(List<Specimen> specimens) {
        return !specimens.isEmpty() && specimens.stream().allMatch(this::isTransportReady);
    }

    boolean isVerificationCompleted(Specimen specimen) {
        if ("VERIFIED".equals(normalizeStatus(specimen.verificationStatus()))) {
            return true;
        }
        if (specimen.verificationCompletedAt() != null) {
            return true;
        }
        return specimen.specimenStatus() == SpecimenStatus.VERIFIED
            || specimen.specimenStatus() == SpecimenStatus.FIXING
            || specimen.specimenStatus() == SpecimenStatus.FIXED
            || specimen.specimenStatus() == SpecimenStatus.CHECKED_IN
            || specimen.specimenStatus() == SpecimenStatus.IN_TRANSIT
            || specimen.specimenStatus() == SpecimenStatus.RECEIVED
            || specimen.specimenStatus() == SpecimenStatus.REJECTED
            || specimen.specimenStatus() == SpecimenStatus.RETURNED;
    }

    private boolean isCheckInSatisfiedOrReady(Specimen specimen) {
        if (isReceiptTerminalStatus(specimen.specimenStatus())) {
            return false;
        }
        if ("CHECKED_IN".equals(commandCheckInStatus(specimen))) {
            return true;
        }
        return isVerificationCompleted(specimen)
            && specimen.fixationStatus() == FixationStatus.COMPLETED
            && specimen.specimenConfirmedAt() != null;
    }

    private boolean isTransportReady(Specimen specimen) {
        return !isReceiptTerminalStatus(specimen.specimenStatus())
            && specimen.specimenStatus() == SpecimenStatus.CHECKED_IN
            && "CHECKED_IN".equals(commandCheckInStatus(specimen));
    }

    private int labelPrintStatusPriority(String status) {
        if ("FAILED".equals(status)) {
            return 3;
        }
        if ("PENDING".equals(status)) {
            return 2;
        }
        if ("SUCCESS".equals(status)) {
            return 1;
        }
        return 0;
    }

    private String normalizeStatus(String value) {
        return blank(value) ? null : value.trim().toUpperCase();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
