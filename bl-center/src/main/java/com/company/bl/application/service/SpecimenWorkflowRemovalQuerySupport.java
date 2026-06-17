package com.company.bl.application.service;

import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static com.company.bl.application.service.SpecimenWorkflowQueryModels.*;
import static com.company.bl.application.service.SpecimenWorkflowTransportModels.*;

@Component
class SpecimenWorkflowRemovalQuerySupport extends AbstractSpecimenWorkflowQuerySupport {

    SpecimenWorkflowRemovalQuerySupport(ApplicationRepository applicationRepository,
                                        SpecimenWorkflowQueryRepository specimenWorkflowRepository,
                                        SpecimenWorkflowSupport specimenWorkflowSupport) {
        super(applicationRepository, specimenWorkflowRepository, specimenWorkflowSupport);
    }

    @Transactional(readOnly = true)
    SpecimenManagementListPage listSpecimenManagementItems(SpecimenManagementListQuery query) {
        int page = specimenWorkflowSupport.normalizePage(query.page());
        int size = specimenWorkflowSupport.normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedSpecimenManagementItems result =
            specimenWorkflowRepository.findSpecimenManagementItems(
                new SpecimenWorkflowRepository.SpecimenManagementListQuery(
                    page,
                    size,
                    specimenWorkflowSupport.trim(query.keyword()),
                    specimenWorkflowSupport.trim(query.applicationNo()),
                    specimenWorkflowSupport.trim(query.departmentId()),
                    specimenWorkflowSupport.trim(query.buildingId()),
                    specimenWorkflowSupport.trim(query.roomId()),
                    specimenWorkflowSupport.normalizeStatus(query.barcodeBindingStatus()),
                    specimenWorkflowSupport.normalizeStatus(query.specimenStatus()),
                    specimenWorkflowSupport.normalizeStatus(query.labelPrintStatus()),
                    query.abnormalFlag(),
                    specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return new SpecimenManagementListPage(
            result.items().stream().map(item -> new SpecimenManagementListItem(
                item.specimenId(),
                item.specimenNo(),
                item.barcode(),
                item.applicationId(),
                item.applicationNo(),
                item.patientId(),
                item.patientIdDisplay(),
                item.patientName(),
                item.patientGender(),
                item.inpatientNo(),
                item.wardName(),
                item.submittingDepartmentId(),
                item.submittingDepartmentName(),
                item.buildingId(),
                item.roomId(),
                item.surgeryName(),
                item.specimenName(),
                item.specimenType(),
                item.specimenSite(),
                item.specimenCount(),
                item.containerName(),
                item.containerCount(),
                item.specimenStatus(),
                item.fixationStatus(),
                item.fixationStartedAt(),
                item.fixationCompletedAt(),
                item.fixationLiquidType(),
                item.fixationOperatorUserId(),
                item.fixationOperatorName(),
                item.verificationStatus(),
                item.specimenConfirmedAt(),
                item.specimenConfirmedByUserId(),
                item.specimenConfirmedByName(),
                item.specimenRemovalAt(),
                item.specimenRemovalOperatorName(),
                item.checkInStatus(),
                item.checkedInAt(),
                item.checkedInByName(),
                item.labelPrintStatus(),
                item.labelPrintBatchNo(),
                item.registrationOperatorName(),
                item.registeredAt(),
                item.latestTrackingAt(),
                item.abnormalFlag()))
                .toList(),
            page,
            size,
            result.total(),
            new SpecimenManagementSummary(
                result.summary().totalCount(),
                result.summary().labelPrintedCount(),
                result.summary().pendingLabelCount(),
                result.summary().abnormalCount(),
                result.summary().unboundCount()));
    }

    @Transactional(readOnly = true)
    SpecimenOutboundPage listSpecimenOutbounds(SpecimenOutboundListQuery query) {
        int page = specimenWorkflowSupport.normalizePage(query.page());
        int size = specimenWorkflowSupport.normalizeSize(query.size());
        String applicationId = specimenWorkflowSupport.trim(query.applicationId());
        String identifier = specimenWorkflowSupport.trim(query.identifier());
        String specimenNo = specimenWorkflowSupport.trim(query.specimenNo());
        if ((applicationId == null || applicationId.isBlank())
            && (identifier == null || identifier.isBlank())
            && specimenNo != null
            && !specimenNo.isBlank()) {
            var matchedSpecimens = specimenWorkflowRepository.findSpecimensBySpecimenNo(specimenNo);
            if (matchedSpecimens.size() == 1) {
                applicationId = matchedSpecimens.get(0).applicationId();
                specimenNo = null;
            }
        } else if ((applicationId == null || applicationId.isBlank())
            && identifier != null
            && !identifier.isBlank()) {
            Specimen matchedSpecimen = resolveUniqueSpecimenIdentifierMatch(identifier);
            if (matchedSpecimen != null) {
                applicationId = matchedSpecimen.applicationId();
                identifier = null;
                specimenNo = null;
            }
        } else if (applicationId != null && !applicationId.isBlank()) {
            identifier = null;
            specimenNo = null;
        }
        SpecimenWorkflowRepository.PagedSpecimenOutbounds result =
            specimenWorkflowRepository.findSpecimenOutbounds(
                new SpecimenWorkflowRepository.SpecimenOutboundListQuery(
                    page,
                    size,
                    applicationId,
                    identifier,
                    specimenNo));
        return new SpecimenOutboundPage(
            result.items().stream().map(item -> new SpecimenOutboundItem(
                item.specimenId(),
                item.transportOrderId(),
                item.applicationId(),
                item.applicationNo(),
                item.barcode(),
                item.specimenNo(),
                item.patientName(),
                item.patientGender(),
                item.patientId(),
                item.patientIdDisplay(),
                item.inpatientNo(),
                item.surgeryName(),
                item.specimenName(),
                item.specimenStatus(),
                item.fixationStatus(),
                item.checkInStatus(),
                item.specimenConfirmedAt(),
                item.submittingDepartmentId(),
                item.submittingDepartmentName(),
                item.registeredAt(),
                item.registeredByName(),
                item.outboundAt(),
                item.outboundUserName()))
                .toList(),
            page,
            size,
            result.total());
    }

    private Specimen resolveUniqueSpecimenIdentifierMatch(String identifier) {
        ArrayList<Specimen> matches = new ArrayList<>();
        specimenWorkflowRepository.findSpecimenByBarcode(identifier).ifPresent(matches::add);
        for (Specimen specimen : specimenWorkflowRepository.findSpecimensBySpecimenNo(identifier)) {
            boolean duplicate = matches.stream().anyMatch(match -> match.id().equals(specimen.id()));
            if (!duplicate) {
                matches.add(specimen);
            }
        }
        return matches.size() == 1 ? matches.get(0) : null;
    }

    @Transactional(readOnly = true)
    SpecimenRemovalListPage listSpecimenRemovalItems(SpecimenRemovalQuery query) {
        int page = specimenWorkflowSupport.normalizePage(query.page());
        int size = specimenWorkflowSupport.normalizeSize(query.size());
        SpecimenWorkflowRepository.PagedSpecimenRemovalItems result =
            specimenWorkflowRepository.findSpecimenRemovalItems(
                new SpecimenWorkflowRepository.SpecimenRemovalListQuery(
                    page,
                    size,
                    specimenWorkflowSupport.trim(query.keyword()),
                    specimenWorkflowSupport.trim(query.applicationNo()),
                    specimenWorkflowSupport.trim(query.departmentId()),
                    specimenWorkflowSupport.normalizeStatus(query.specimenStatus()),
                    query.abnormalFlag(),
                    specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseDateTo(query.dateTo())));
        return new SpecimenRemovalListPage(
            result.items().stream().map(item -> new SpecimenRemovalListItem(
                item.specimenId(),
                item.specimenNo(),
                item.barcode(),
                item.applicationId(),
                item.applicationNo(),
                item.patientName(),
                item.patientGender(),
                item.inpatientNo(),
                item.surgeryName(),
                item.submittingDepartmentId(),
                item.submittingDepartmentName(),
                item.specimenName(),
                item.specimenType(),
                item.specimenCount(),
                item.containerName(),
                item.containerCount(),
                item.specimenStatus(),
                item.fixationStatus(),
                item.verificationStatus(),
                item.specimenRemovalAt(),
                item.specimenRemovalOperatorName(),
                item.registeredAt(),
                item.labelPrintBatchNo(),
                item.registeredByName(),
                item.latestTrackingAt(),
                item.abnormalFlag()))
                .toList(),
            page,
            size,
            result.total(),
            new SpecimenRemovalSummary(
                result.summary().totalCount(),
                result.summary().confirmedCount(),
                result.summary().pendingCount(),
                result.summary().abnormalCount()));
    }

    @Transactional(readOnly = true)
    byte[] exportSpecimenRemovalItems(SpecimenRemovalQuery query) {
        return specimenWorkflowSupport.buildSpecimenRemovalExport(
            specimenWorkflowRepository.listSpecimenRemovalExportRows(
                new SpecimenWorkflowRepository.SpecimenRemovalListQuery(
                    specimenWorkflowSupport.normalizePage(query.page()),
                    specimenWorkflowSupport.normalizeSize(query.size()),
                    specimenWorkflowSupport.trim(query.keyword()),
                    specimenWorkflowSupport.trim(query.applicationNo()),
                    specimenWorkflowSupport.trim(query.departmentId()),
                    specimenWorkflowSupport.normalizeStatus(query.specimenStatus()),
                    query.abnormalFlag(),
                    specimenWorkflowSupport.parseDateFrom(query.dateFrom()),
                    specimenWorkflowSupport.parseDateTo(query.dateTo()))));
    }
}
