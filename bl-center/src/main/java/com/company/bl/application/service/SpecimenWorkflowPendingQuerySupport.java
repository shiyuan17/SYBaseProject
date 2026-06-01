package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.company.bl.application.service.SpecimenWorkflowModels.*;
import static com.company.bl.application.service.SpecimenWorkflowQueryModels.*;
import static com.company.bl.application.service.SpecimenWorkflowTransportModels.*;

@Component
class SpecimenWorkflowPendingQuerySupport extends AbstractSpecimenWorkflowQuerySupport {

    SpecimenWorkflowPendingQuerySupport(ApplicationRepository applicationRepository,
                                        SpecimenWorkflowQueryRepository specimenWorkflowRepository,
                                        SpecimenWorkflowSupport specimenWorkflowSupport) {
        super(applicationRepository, specimenWorkflowRepository, specimenWorkflowSupport);
    }

    @Transactional(readOnly = true)
    PendingSpecimenPage listPendingFixations(PendingSpecimenQuery query) {
        SpecimenWorkflowRepository.PagedPendingSpecimens page = specimenWorkflowRepository.findPendingFixations(
            new SpecimenWorkflowRepository.PendingSpecimenQuery(
                specimenWorkflowSupport.normalizePage(query.page()),
                specimenWorkflowSupport.normalizeSize(query.size()),
                specimenWorkflowSupport.trim(query.applicationId()),
                specimenWorkflowSupport.trim(query.specimenNo()),
                specimenWorkflowSupport.trim(query.departmentId()),
                specimenWorkflowSupport.trim(query.fixationStatus()),
                specimenWorkflowSupport.trim(query.verificationStatus()),
                specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return new PendingSpecimenPage(
            page.items().stream().map(specimenWorkflowSupport::toPendingItem).toList(),
            specimenWorkflowSupport.normalizePage(query.page()),
            specimenWorkflowSupport.normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    PendingSpecimenPage listPendingReceipts(PendingSpecimenQuery query) {
        SpecimenWorkflowRepository.PagedPendingSpecimens page = specimenWorkflowRepository.findPendingReceipts(
            new SpecimenWorkflowRepository.PendingSpecimenQuery(
                specimenWorkflowSupport.normalizePage(query.page()),
                specimenWorkflowSupport.normalizeSize(query.size()),
                specimenWorkflowSupport.trim(query.applicationId()),
                specimenWorkflowSupport.trim(query.specimenNo()),
                specimenWorkflowSupport.trim(query.departmentId()),
                null,
                null,
                specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return new PendingSpecimenPage(
            page.items().stream().map(specimenWorkflowSupport::toPendingItem).toList(),
            specimenWorkflowSupport.normalizePage(query.page()),
            specimenWorkflowSupport.normalizeSize(query.size()),
            page.total());
    }

    @Transactional(readOnly = true)
    List<SpecimenVerificationRecord> listSpecimenVerificationRecords(String barcode) {
        if (specimenWorkflowSupport.blank(barcode)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen barcode is required");
        }
        return specimenWorkflowRepository.listSpecimenVerificationRecords(specimenWorkflowSupport.trim(barcode)).stream()
            .map(row -> new SpecimenVerificationRecord(
                row.applicationId(),
                row.specimenId(),
                row.barcode(),
                row.verificationType(),
                row.result(),
                row.operatorName(),
                row.terminalCode(),
                row.remarks(),
                row.verifiedAt()))
            .toList();
    }

    @Transactional(readOnly = true)
    PendingTransportOrderPage listPendingTransportOrders(PendingTransportOrderQuery query) {
        SpecimenWorkflowRepository.PagedPendingTransportOrders page =
            specimenWorkflowRepository.findPendingTransportOrders(
                new SpecimenWorkflowRepository.PendingTransportOrderQuery(
                    specimenWorkflowSupport.normalizePage(query.page()),
                    specimenWorkflowSupport.normalizeSize(query.size()),
                    specimenWorkflowSupport.trim(query.applicationId()),
                    specimenWorkflowSupport.trim(query.specimenNo()),
                    specimenWorkflowSupport.trim(query.departmentId()),
                    specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseDateTo(query.dateTo()),
                    specimenWorkflowSupport.normalizeStatus(query.status())));
        return new PendingTransportOrderPage(
            page.items().stream().map(item -> new PendingTransportOrderItem(
                item.id(),
                item.transportOrderNo(),
                item.applicationId(),
                item.applicationNo(),
                item.patientName(),
                item.handoverDepartmentName(),
                item.receiverDepartmentName(),
                item.status(),
                item.toBeTransportedAt(),
                item.handedOverAt(),
                item.outboundUserId(),
                item.outboundUserName(),
                specimenWorkflowRepository.findTransportOrderSpecimenBarcodes(item.id()))).toList(),
            specimenWorkflowSupport.normalizePage(query.page()),
            specimenWorkflowSupport.normalizeSize(query.size()),
            page.total());
    }
}
