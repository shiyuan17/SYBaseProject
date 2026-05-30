package com.company.bl.application.service;

import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;

class SpecimenWorkflowModelAssembler {

    private final SpecimenWorkflowApplicationPolicy applicationPolicy;
    private final SpecimenWorkflowStatusPolicy statusPolicy;

    SpecimenWorkflowModelAssembler(SpecimenWorkflowApplicationPolicy applicationPolicy,
                                   SpecimenWorkflowStatusPolicy statusPolicy) {
        this.applicationPolicy = applicationPolicy;
        this.statusPolicy = statusPolicy;
    }

    SpecimenVerificationResult buildSpecimenVerificationResult(Specimen specimen) {
        return new SpecimenVerificationResult(
            specimen.id(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenNameStandardized(),
            specimen.specimenType(),
            specimen.specimenSite(),
            specimen.collectionMode(),
            specimen.clinicalSymptom(),
            specimen.specimenCount(),
            specimen.containerName(),
            specimen.containerCount(),
            specimen.specimenStatus() == null ? null : specimen.specimenStatus().name(),
            specimen.fixationStatus() == null ? null : specimen.fixationStatus().name(),
            specimen.verificationStatus(),
            specimen.verificationStartedAt(),
            specimen.verificationCompletedAt(),
            specimen.labelPrintStatus(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            specimen.unqualifiedReason());
    }

    PendingSpecimenItem toPendingItem(SpecimenWorkflowRepository.PendingSpecimenRow row) {
        return new PendingSpecimenItem(
            row.applicationId(),
            row.applicationNo(),
            row.patientName(),
            row.submittingDepartmentId(),
            row.submittingDepartmentName(),
            row.transportOrderId(),
            row.specimenId(),
            row.specimenNo(),
            row.barcode(),
            row.containerName(),
            row.containerCount(),
            row.specimenStatus(),
            row.fixationStatus(),
            row.fixationStartedAt(),
            row.fixationCompletedAt(),
            row.fixationLiquidType(),
            row.fixationOperatorUserId(),
            row.fixationOperatorName(),
            row.verificationStatus(),
            row.verificationStartedAt(),
            row.verificationCompletedAt(),
            row.specimenConfirmedAt(),
            row.checkInStatus(),
            row.checkedInAt(),
            row.checkedInByName(),
            row.registeredAt(),
            row.latestTrackingAt(),
            row.abnormalFlag());
    }

    ApplicationListItem toApplicationListItem(ApplicationTracking tracking) {
        String latestBatchNo = statusPolicy.resolveLatestLabelPrintBatchNo(tracking.specimens());
        String latestLabelPrintStatus = latestBatchNo == null
            ? null
            : statusPolicy.resolveLatestBatchLabelPrintStatus(tracking.specimens(), latestBatchNo);
        ApplicationOperationState operationState = applicationPolicy.resolveApplicationOperationState(tracking.application());
        return new ApplicationListItem(
            tracking.application().getId().value(),
            tracking.application().getApplicationNo(),
            tracking.application().getPatientName(),
            tracking.application().getPatientGender(),
            tracking.application().getPatientAge(),
            tracking.application().getStatus().name(),
            tracking.application().getSubmittingDepartmentName(),
            tracking.application().getSubmittingDoctorName(),
            tracking.application().getApplicationType(),
            tracking.application().getApplicationFormStatus().name(),
            tracking.currentNode(),
            tracking.abnormal(),
            tracking.specimens().size(),
            latestLabelPrintStatus,
            operationState.editable(),
            operationState.deletable(),
            operationState.voided(),
            operationState.disabledReason(),
            tracking.application().getApplicationDate(),
            tracking.application().getSubmissionDate(),
            tracking.application().getCreatedAt(),
            tracking.application().getUpdatedAt());
    }

    Specimen copyWithLabelPrintStatus(Specimen specimen, String labelPrintStatus) {
        return new Specimen(
            specimen.id(),
            specimen.applicationId(),
            specimen.caseId(),
            specimen.specimenNo(),
            specimen.barcode(),
            specimen.specimenType(),
            specimen.specimenNameStandardized(),
            specimen.specimenSite(),
            specimen.collectionMode(),
            specimen.specimenCount(),
            specimen.containerName(),
            specimen.containerCount(),
            specimen.specimenStatus(),
            specimen.fixationStatus(),
            specimen.verificationStatus(),
            specimen.verificationStartedAt(),
            specimen.verificationCompletedAt(),
            specimen.specimenRemovalAt(),
            specimen.specimenRemovalOperatorUserId(),
            specimen.specimenRemovalOperatorName(),
            specimen.specimenConfirmedAt(),
            specimen.checkInStatus(),
            specimen.checkedInAt(),
            specimen.checkedInByName(),
            specimen.qualified(),
            specimen.unqualifiedReason(),
            specimen.receiptStatus(),
            specimen.qualityCheckResult(),
            specimen.qualityIssueCodes(),
            specimen.clinicalSymptom(),
            specimen.applicantDepartmentId(),
            specimen.applicantDepartmentName(),
            specimen.applicantDoctorUserId(),
            specimen.applicantDoctorName(),
            specimen.submissionDate(),
            specimen.labelPrintBatchNo(),
            labelPrintStatus,
            specimen.registeredByUserId(),
            specimen.registeredByName(),
            specimen.registeredAt(),
            specimen.terminalCode(),
            specimen.remarks());
    }
}
