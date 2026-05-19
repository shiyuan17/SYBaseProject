package com.company.bl.domain.repository;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpecimenWorkflowRepository {

    Optional<Specimen> findSpecimenByBarcode(String barcode);

    List<Specimen> findSpecimensByApplicationId(String applicationId);

    Optional<PathologyCase> findPathologyCaseByApplicationId(String applicationId);

    Optional<TransportOrder> findTransportOrderById(String transportOrderId);

    List<TransportOrderItem> findTransportOrderItems(String transportOrderId);

    List<TrackingEvent> findTrackingEventsByApplicationId(String applicationId);

    Optional<String> findApplicationIdByBarcode(String barcode);

    boolean existsApplicationByExternalSource(String externalOrderNo, String thirdPartySource);

    void updateApplicationStatus(String applicationId, String status);

    void updateSpecimenLabelPrintStatus(String specimenId, String labelPrintStatus);

    Specimen insertSpecimen(Specimen specimen);

    List<Specimen> findSpecimensByLabelPrintBatchNoAndStatus(String labelPrintBatchNo, String labelPrintStatus);

    void insertCollectionRecord(String applicationId,
                                String specimenId,
                                String collectionStatus,
                                String collectionScene,
                                String collectionMode,
                                String labelPrintBatchNo,
                                String collectorUserId,
                                String collectorName,
                                LocalDateTime collectedAt,
                                String terminalCode,
                                String remarks);

    void upsertFixationRecord(String applicationId,
                              String specimenId,
                              FixationStatus fixationStatus,
                              String fixationLiquidType,
                              LocalDateTime fixationStartAt,
                              LocalDateTime fixationCompletedAt,
                              String verifiedByUserId,
                              String verifiedByName,
                              LocalDateTime verifiedAt,
                              String terminalCode,
                              String remarks);

    void updateSpecimenStatus(String specimenId,
                              SpecimenStatus specimenStatus,
                              FixationStatus fixationStatus,
                              String unqualifiedReason,
                              String remarks,
                              String caseId);

    TransportOrder insertTransportOrder(TransportOrder order);

    TransportOrder updateTransportOrderStatus(String transportOrderId,
                                             TransportOrderStatus status,
                                             String receiverUserId,
                                             String receiverUserName,
                                             LocalDateTime printedAt,
                                             LocalDateTime handedOverAt);

    void insertTransportOrderItem(TransportOrderItem item);

    void updateTransportOrderItemStatus(String transportOrderId,
                                        String specimenId,
                                        TransportItemStatus status,
                                        String verificationResult,
                                        String verifiedByUserId,
                                        String verifiedByName,
                                        LocalDateTime verifiedAt,
                                        String remarks);

    PathologyCase insertPathologyCase(PathologyCase pathologyCase);

    void insertSpecimenReceipt(String applicationId,
                               String caseId,
                               String specimenId,
                               String transportOrderId,
                               ReceiptStatus receiptStatus,
                               Integer containerCount,
                               String barcode,
                               String receivedByUserId,
                               String receivedByName,
                               LocalDateTime receivedAt,
                               String terminalCode,
                               String rejectReason,
                               String returnReason,
                               String remarks);

    void insertWorkflowEvent(TrackingEvent event);

    void upsertTechnicalPendingTask(String applicationId, String caseId, String payload);

    PagedPendingSpecimens findPendingFixations(PendingSpecimenQuery query);

    PagedPendingSpecimens findPendingReceipts(PendingSpecimenQuery query);

    ApplicationTracking getApplicationTracking(String applicationId, com.company.bl.domain.model.Application application);

    record PendingSpecimenQuery(
        int page,
        int size,
        String applicationId,
        String departmentId,
        LocalDateTime dateFrom,
        LocalDateTime dateTo
    ) {
    }

    record PendingSpecimenRow(
        String applicationId,
        String applicationNo,
        String patientName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String specimenId,
        String specimenNo,
        String barcode,
        String specimenStatus,
        String fixationStatus,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    record PagedPendingSpecimens(List<PendingSpecimenRow> items, long total) {
    }
}
