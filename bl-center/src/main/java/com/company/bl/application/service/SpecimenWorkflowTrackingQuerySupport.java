package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.common.web.observability.ObservedOperation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;

@Component
class SpecimenWorkflowTrackingQuerySupport extends AbstractSpecimenWorkflowQuerySupport {

    SpecimenWorkflowTrackingQuerySupport(ApplicationRepository applicationRepository,
                                         SpecimenWorkflowQueryRepository specimenWorkflowRepository,
                                         SpecimenWorkflowSupport specimenWorkflowSupport) {
        super(applicationRepository, specimenWorkflowRepository, specimenWorkflowSupport);
    }

    @Transactional(readOnly = true)
    @ObservedOperation(
        operation = "get_application",
        successCounter = "application_query_total",
        failureCounter = "application_query_failed_total",
        durationMetric = "application_query_duration")
    ApplicationTracking getApplicationTracking(String applicationId) {
        Application application = specimenWorkflowSupport.getApplication(applicationId);
        return specimenWorkflowRepository.getApplicationTracking(applicationId, application);
    }

    @Transactional(readOnly = true)
    ApplicationTracking getTrackingByBarcode(String barcode) {
        String applicationId = specimenWorkflowRepository.findApplicationIdByBarcode(barcode)
            .orElseThrow(() -> new BlBusinessException(
                BlErrorCode.RESOURCE_NOT_FOUND,
                404,
                "Specimen barcode not found"));
        return getApplicationTracking(applicationId);
    }

    @Transactional(readOnly = true)
    LatestSpecimenRegistrationResult getLatestRegistrationResult(String applicationId) {
        specimenWorkflowSupport.getApplication(applicationId);
        List<Specimen> specimens = specimenWorkflowRepository.findSpecimensByApplicationId(applicationId);
        if (specimens.isEmpty()) {
            return new LatestSpecimenRegistrationResult(applicationId, List.of(), null, false, null, null);
        }
        String latestBatchNo = specimenWorkflowSupport.resolveLatestLabelPrintBatchNo(specimens);
        if (latestBatchNo == null) {
            return new LatestSpecimenRegistrationResult(applicationId, List.of(), null, false, null, null);
        }
        List<Specimen> batchSpecimens = specimens.stream()
            .filter(specimen -> latestBatchNo.equals(specimen.labelPrintBatchNo()))
            .toList();
        List<TrackingEvent> trackingEvents = specimenWorkflowRepository.findTrackingEventsByApplicationId(applicationId);
        String latestPrintMessage = specimenWorkflowSupport.resolveLatestBatchLabelPrintMessage(trackingEvents, batchSpecimens);
        RegistrationSnapshot snapshot = specimenWorkflowRepository
            .findRegistrationSnapshotByApplicationIdAndBatchNo(applicationId, latestBatchNo)
            .map(item -> new RegistrationSnapshot(
                item.collectionScene(),
                item.operatorUserId(),
                item.operatorName(),
                item.printerCode(),
                item.terminalCode(),
                item.remarks()))
            .orElse(null);
        boolean labelPrintSuccess = batchSpecimens.stream()
            .allMatch(specimen -> "SUCCESS".equalsIgnoreCase(specimen.labelPrintStatus()));
        return new LatestSpecimenRegistrationResult(
            applicationId,
            batchSpecimens,
            latestBatchNo,
            labelPrintSuccess,
            latestPrintMessage,
            snapshot);
    }
}
