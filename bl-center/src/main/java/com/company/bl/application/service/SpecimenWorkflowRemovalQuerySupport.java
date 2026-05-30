package com.company.bl.application.service;

import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.company.bl.application.service.SpecimenWorkflowQueryModels.*;

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
                item.patientName(),
                item.submittingDepartmentId(),
                item.submittingDepartmentName(),
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
                item.checkInStatus(),
                item.checkedInAt(),
                item.checkedInByName(),
                item.labelPrintStatus(),
                item.labelPrintBatchNo(),
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
                result.summary().abnormalCount()));
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
