package com.company.bl.domain.repository;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.ReceiptStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportItemStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TrackingEvent;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;

import java.time.LocalDateTime;

public interface SpecimenWorkflowCommandRepository {

    void updateApplicationStatus(String applicationId, String status);

    void updateSpecimenLabelPrintStatus(String specimenId, String labelPrintStatus);

    Specimen insertSpecimen(Specimen specimen);

    void insertCollectionRecord(String applicationId,
                                String specimenId,
                                String collectionStatus,
                                String collectionScene,
                                String collectionMode,
                                String labelPrintBatchNo,
                                String printerCode,
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

    void startSpecimenVerification(String applicationId,
                                   String specimenId,
                                   String verifiedByUserId,
                                   String verifiedByName,
                                   LocalDateTime verificationStartedAt,
                                   String terminalCode,
                                   String remarks);

    void completeSpecimenVerification(String specimenId,
                                      String verifiedByUserId,
                                      String verifiedByName,
                                      LocalDateTime verificationCompletedAt,
                                      String terminalCode,
                                      String remarks);

    void confirmSpecimen(String specimenId,
                         LocalDateTime specimenConfirmedAt);

    void checkInSpecimen(String specimenId,
                         String checkInStatus,
                         LocalDateTime checkedInAt,
                         String checkedInByUserId,
                         String checkedInByName);

    void confirmSpecimenRemoval(String specimenId,
                                LocalDateTime specimenRemovalAt,
                                String removalOperatorUserId,
                                String removalOperatorName);

    void completeSpecimenVerificationFromRemoval(String applicationId,
                                                 String specimenId,
                                                 LocalDateTime verificationCompletedAt,
                                                 String verifiedByUserId,
                                                 String verifiedByName,
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
                               String qualityCheckResult,
                               String qualityIssueCodes,
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
}
