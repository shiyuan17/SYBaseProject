package com.company.bl.domain.repository;

import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;

import java.util.List;
import java.util.Optional;

public interface SpecimenWorkflowQueryRepository {

    Optional<Specimen> findSpecimenByBarcode(String barcode);

    Optional<Specimen> findSpecimenById(String specimenId);

    List<Specimen> findSpecimensBySpecimenNo(String specimenNo);

    List<Specimen> findSpecimensByApplicationId(String applicationId);

    Optional<PathologyCase> findPathologyCaseByApplicationId(String applicationId);

    Optional<TransportOrder> findTransportOrderById(String transportOrderId);

    Optional<TransportOrder> findActiveTransportOrderBySpecimenId(String specimenId);

    List<TransportOrderItem> findTransportOrderItems(String transportOrderId);

    List<String> findTransportOrderSpecimenBarcodes(String transportOrderId);

    List<TrackingEvent> findTrackingEventsByApplicationId(String applicationId);

    Optional<String> findApplicationIdByBarcode(String barcode);

    boolean existsApplicationByExternalSource(String externalOrderNo, String thirdPartySource);

    List<Specimen> findSpecimensByLabelPrintBatchNoAndStatus(String labelPrintBatchNo, String labelPrintStatus);

    List<Specimen> findSpecimensByLabelPrintBatchNoAndStatuses(String labelPrintBatchNo, List<String> labelPrintStatuses);

    Optional<SpecimenWorkflowRepository.RegistrationSnapshotData> findRegistrationSnapshotByApplicationIdAndBatchNo(
        String applicationId,
        String labelPrintBatchNo
    );

    SpecimenWorkflowRepository.PagedPendingSpecimens findPendingFixations(SpecimenWorkflowRepository.PendingSpecimenQuery query);

    SpecimenWorkflowRepository.PagedPendingSpecimens findPendingReceipts(SpecimenWorkflowRepository.PendingSpecimenQuery query);

    SpecimenWorkflowRepository.PagedPendingTransportOrders findPendingTransportOrders(
        SpecimenWorkflowRepository.PendingTransportOrderQuery query
    );

    SpecimenWorkflowRepository.PagedSpecimenOutbounds findSpecimenOutbounds(
        SpecimenWorkflowRepository.SpecimenOutboundListQuery query
    );

    SpecimenWorkflowRepository.PagedApplications findApplications(SpecimenWorkflowRepository.ApplicationListQuery query);

    List<SpecimenWorkflowRepository.DuplicateApplicationRow> findDuplicateApplications(
        SpecimenWorkflowRepository.DuplicateApplicationQuery query
    );

    SpecimenWorkflowRepository.PagedSpecimenManagementItems findSpecimenManagementItems(
        SpecimenWorkflowRepository.SpecimenManagementListQuery query
    );

    SpecimenWorkflowRepository.PagedSpecimenRemovalItems findSpecimenRemovalItems(
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query
    );

    ApplicationTracking getApplicationTracking(String applicationId, com.company.bl.domain.model.Application application);

    List<SpecimenWorkflowRepository.SpecimenVerificationRecordRow> listSpecimenVerificationRecords(String barcode);

    List<SpecimenWorkflowRepository.SpecimenRemovalListRow> listSpecimenRemovalExportRows(
        SpecimenWorkflowRepository.SpecimenRemovalListQuery query
    );
}
