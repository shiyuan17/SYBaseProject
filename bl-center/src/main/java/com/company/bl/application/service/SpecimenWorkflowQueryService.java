package com.company.bl.application.service;

import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;
import static com.company.bl.application.service.SpecimenWorkflowQueryModels.*;
import static com.company.bl.application.service.SpecimenWorkflowTransportModels.*;

@Service
@RequiredArgsConstructor
class SpecimenWorkflowQueryService {

    private final SpecimenWorkflowPendingQuerySupport pendingQuerySupport;
    private final SpecimenWorkflowApplicationQuerySupport applicationQuerySupport;
    private final SpecimenWorkflowTrackingQuerySupport trackingQuerySupport;
    private final SpecimenWorkflowRemovalQuerySupport removalQuerySupport;

    PendingSpecimenPage listPendingFixations(PendingSpecimenQuery query) {
        return pendingQuerySupport.listPendingFixations(query);
    }

    PendingSpecimenPage listPendingReceipts(PendingSpecimenQuery query) {
        return pendingQuerySupport.listPendingReceipts(query);
    }

    List<SpecimenVerificationRecord> listSpecimenVerificationRecords(String barcode) {
        return pendingQuerySupport.listSpecimenVerificationRecords(barcode);
    }

    PendingTransportOrderPage listPendingTransportOrders(PendingTransportOrderQuery query) {
        return pendingQuerySupport.listPendingTransportOrders(query);
    }

    ApplicationPage listApplications(ApplicationListQuery query) {
        return applicationQuerySupport.listApplications(query);
    }

    DuplicateCheckResult checkApplicationDuplicate(DuplicateCheckCommand command) {
        return applicationQuerySupport.checkApplicationDuplicate(command);
    }

    SpecimenManagementListPage listSpecimenManagementItems(SpecimenManagementListQuery query) {
        return removalQuerySupport.listSpecimenManagementItems(query);
    }

    SpecimenRemovalListPage listSpecimenRemovalItems(SpecimenRemovalQuery query) {
        return removalQuerySupport.listSpecimenRemovalItems(query);
    }

    byte[] exportSpecimenRemovalItems(SpecimenRemovalQuery query) {
        return removalQuerySupport.exportSpecimenRemovalItems(query);
    }

    ApplicationTracking getApplicationTracking(String applicationId) {
        return trackingQuerySupport.getApplicationTracking(applicationId);
    }

    ApplicationTracking getTrackingByBarcode(String barcode) {
        return trackingQuerySupport.getTrackingByBarcode(barcode);
    }

    LatestSpecimenRegistrationResult getLatestRegistrationResult(String applicationId) {
        return trackingQuerySupport.getLatestRegistrationResult(applicationId);
    }

    ApplicationListItem getRegistrationApplicationByApplicationNo(String applicationNo) {
        return applicationQuerySupport.getRegistrationApplicationByApplicationNo(applicationNo);
    }

    ApplicationOperationState resolveApplicationOperationState(Application application) {
        return applicationQuerySupport.resolveApplicationOperationState(application);
    }
}
