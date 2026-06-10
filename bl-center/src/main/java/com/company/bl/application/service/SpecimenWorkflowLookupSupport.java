package com.company.bl.application.service;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.ApplicationDomainException;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.PathologyCase;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.model.TransportOrderItem;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.valueobject.ApplicationId;

import java.util.List;
import java.util.Optional;

class SpecimenWorkflowLookupSupport {

    private final ApplicationRepository applicationRepository;
    private final SpecimenWorkflowQueryRepository specimenWorkflowRepository;

    SpecimenWorkflowLookupSupport(ApplicationRepository applicationRepository,
                                  SpecimenWorkflowQueryRepository specimenWorkflowRepository) {
        this.applicationRepository = applicationRepository;
        this.specimenWorkflowRepository = specimenWorkflowRepository;
    }

    Application getApplication(String applicationId) {
        return applicationRepository.findById(new ApplicationId(applicationId))
            .orElseThrow(() -> new ApplicationDomainException(ApplicationErrorCode.APPLICATION_NOT_FOUND, 404));
    }

    Specimen getSpecimen(String barcode) {
        return specimenWorkflowRepository.findSpecimenByBarcode(barcode)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen barcode not found"));
    }

    Specimen getSpecimenById(String specimenId) {
        return specimenWorkflowRepository.findSpecimenById(specimenId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen id not found"));
    }

    TransportOrder getTransportOrder(String transportOrderId) {
        return specimenWorkflowRepository.findTransportOrderById(transportOrderId)
            .orElseThrow(() -> new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Transport order not found"));
    }

    Optional<TransportOrder> findActiveTransportOrderBySpecimenId(String specimenId) {
        return specimenWorkflowRepository.findActiveTransportOrderBySpecimenId(specimenId);
    }

    List<Specimen> getSpecimensByApplicationId(String applicationId) {
        return specimenWorkflowRepository.findSpecimensByApplicationId(applicationId);
    }

    List<Specimen> getSpecimensByLabelPrintBatchNoAndStatuses(String labelPrintBatchNo, List<String> labelPrintStatuses) {
        return specimenWorkflowRepository.findSpecimensByLabelPrintBatchNoAndStatuses(labelPrintBatchNo, labelPrintStatuses);
    }

    Optional<PathologyCase> findPathologyCaseByApplicationId(String applicationId) {
        return specimenWorkflowRepository.findPathologyCaseByApplicationId(applicationId);
    }

    List<TransportOrderItem> getTransportOrderItems(String transportOrderId) {
        return specimenWorkflowRepository.findTransportOrderItems(transportOrderId);
    }

    Specimen resolveSpecimenForRemoval(String identifierType, String identifier) {
        return resolveSpecimenByIdentifier(identifierType, identifier);
    }

    Specimen resolveSpecimenByIdentifier(String identifierType, String identifier) {
        if ("SPECIMEN_ID".equals(identifierType)) {
            return getSpecimenById(identifier);
        }
        if ("BARCODE".equals(identifierType)) {
            return getSpecimen(identifier);
        }
        if ("SPECIMEN_NO".equals(identifierType)) {
            return resolveSpecimenBySpecimenNo(identifier);
        }
        throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Unsupported specimen identifier type");
    }

    Specimen resolveSpecimenByPreferredIdentifier(String specimenId, String barcode, String specimenNo) {
        if (!blank(specimenId)) {
            return getSpecimenById(specimenId.trim());
        }
        if (!blank(barcode)) {
            return getSpecimen(barcode.trim());
        }
        if (!blank(specimenNo)) {
            return resolveSpecimenBySpecimenNo(specimenNo.trim());
        }
        throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen identifier is required");
    }

    void ensureBarcodeAvailable(String barcode) {
        if (specimenWorkflowRepository.findSpecimenByBarcode(barcode).isPresent()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "Specimen barcode already exists");
        }
    }

    private Specimen resolveSpecimenBySpecimenNo(String specimenNo) {
        List<Specimen> specimens = specimenWorkflowRepository.findSpecimensBySpecimenNo(specimenNo);
        if (specimens.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Specimen specimenNo not found");
        }
        if (specimens.size() > 1) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Specimen number matches multiple records");
        }
        return specimens.get(0);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
