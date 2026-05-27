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

    List<String> findTransportOrderSpecimenBarcodes(String transportOrderId);

    List<TrackingEvent> findTrackingEventsByApplicationId(String applicationId);

    Optional<String> findApplicationIdByBarcode(String barcode);

    boolean existsApplicationByExternalSource(String externalOrderNo, String thirdPartySource);

    void updateApplicationStatus(String applicationId, String status);

    void updateSpecimenLabelPrintStatus(String specimenId, String labelPrintStatus);

    Specimen insertSpecimen(Specimen specimen);

    List<Specimen> findSpecimensByLabelPrintBatchNoAndStatus(String labelPrintBatchNo, String labelPrintStatus);

    List<Specimen> findSpecimensByLabelPrintBatchNoAndStatuses(String labelPrintBatchNo, List<String> labelPrintStatuses);

    Optional<RegistrationSnapshotData> findRegistrationSnapshotByApplicationIdAndBatchNo(
        String applicationId,
        String labelPrintBatchNo
    );

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

    PagedPendingSpecimens findPendingFixations(PendingSpecimenQuery query);

    PagedPendingSpecimens findPendingReceipts(PendingSpecimenQuery query);

    PagedPendingTransportOrders findPendingTransportOrders(PendingTransportOrderQuery query);

    PagedApplications findApplications(ApplicationListQuery query);

    List<DuplicateApplicationRow> findDuplicateApplications(DuplicateApplicationQuery query);

    PagedSpecimenManagementItems findSpecimenManagementItems(SpecimenManagementListQuery query);

    ApplicationTracking getApplicationTracking(String applicationId, com.company.bl.domain.model.Application application);

    record PendingSpecimenQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo,
        String departmentId,
        String fixationStatus,
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
        String transportOrderId,
        String specimenId,
        String specimenNo,
        String barcode,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    record PagedPendingSpecimens(List<PendingSpecimenRow> items, long total) {
    }

    record PendingTransportOrderQuery(
        int page,
        int size,
        String applicationId,
        String specimenNo,
        String departmentId,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        String status
    ) {
    }

    record PendingTransportOrderRow(
        String id,
        String transportOrderNo,
        String applicationId,
        String applicationNo,
        String patientName,
        String handoverDepartmentName,
        String receiverDepartmentName,
        String status,
        LocalDateTime toBeTransportedAt,
        LocalDateTime handedOverAt
    ) {
    }

    record PagedPendingTransportOrders(List<PendingTransportOrderRow> items, long total) {
    }

    record ApplicationListQuery(
        int page,
        int size,
        String applicationNo,
        String patientName,
        String submittingDepartmentId,
        String applicationType,
        String applicationFormStatus,
        java.time.LocalDate dateFrom,
        java.time.LocalDate dateTo
    ) {
    }

    record ApplicationListRow(
        String id,
        String applicationNo,
        String patientName,
        String patientGender,
        String patientAge,
        String status,
        String submittingDepartmentName,
        String submittingDoctorName,
        String applicationType,
        String applicationFormStatus,
        String currentNode,
        boolean abnormalFlag,
        int registeredSpecimenCount,
        String latestLabelPrintStatus,
        java.time.LocalDate applicationDate,
        java.time.LocalDate submissionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    record PagedApplications(List<ApplicationListRow> items, long total) {
    }

    record DuplicateApplicationQuery(
        String patientId,
        String patientName,
        String externalOrderNo,
        java.time.LocalDate applicationDate,
        String applicationType,
        String specimenSite
    ) {
    }

    record DuplicateApplicationRow(
        String id,
        String applicationNo,
        String patientName,
        String specimenSite,
        String status,
        String currentNode,
        java.time.LocalDate applicationDate,
        boolean externalOrderMatched,
        boolean sameDaySiteMatched
    ) {
    }

    record RegistrationSnapshotData(
        String collectionScene,
        String operatorUserId,
        String operatorName,
        String printerCode,
        String terminalCode,
        String remarks
    ) {
    }

    record SpecimenManagementListQuery(
        int page,
        int size,
        String keyword,
        String applicationNo,
        String departmentId,
        String specimenStatus,
        String labelPrintStatus,
        Boolean abnormalFlag,
        LocalDateTime dateFrom,
        LocalDateTime dateTo
    ) {
    }

    record SpecimenManagementListRow(
        String specimenId,
        String specimenNo,
        String barcode,
        String applicationId,
        String applicationNo,
        String patientName,
        String submittingDepartmentId,
        String submittingDepartmentName,
        String specimenName,
        String specimenType,
        String specimenSite,
        Integer specimenCount,
        String containerName,
        Integer containerCount,
        String specimenStatus,
        String fixationStatus,
        String labelPrintStatus,
        String labelPrintBatchNo,
        LocalDateTime registeredAt,
        LocalDateTime latestTrackingAt,
        boolean abnormalFlag
    ) {
    }

    record SpecimenManagementSummary(
        long totalCount,
        long labelPrintedCount,
        long pendingLabelCount,
        long abnormalCount
    ) {
    }

    record PagedSpecimenManagementItems(
        List<SpecimenManagementListRow> items,
        long total,
        SpecimenManagementSummary summary
    ) {
    }
}
