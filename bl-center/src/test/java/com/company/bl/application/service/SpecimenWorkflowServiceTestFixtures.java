package com.company.bl.application.service;

import com.company.bl.domain.enums.ApplicationFormStatus;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.enums.TransportOrderStatus;
import com.company.bl.domain.model.Application;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TransportOrder;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.valueobject.ApplicationId;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;

final class SpecimenWorkflowServiceTestFixtures {

    private SpecimenWorkflowServiceTestFixtures() {
    }

    static SpecimenWorkflowSupport support(
        ApplicationRepository applicationRepository,
        SpecimenWorkflowQueryRepository specimenWorkflowQueryRepository
    ) {
        ApplicationRegistrationWorkbenchRepository workbenchRepository = mock(ApplicationRegistrationWorkbenchRepository.class);
        return new SpecimenWorkflowSupport(applicationRepository, workbenchRepository, specimenWorkflowQueryRepository);
    }

    static Application application(String applicationId, ApplicationStatus status) {
        return new Application(
            new ApplicationId(applicationId),
            "APP-NO-1",
            "PAT-1",
            "Patient",
            "F",
            "32",
            "BIOPSY",
            status,
            ApplicationFormStatus.PENDING,
            "EXT-1",
            "HIS",
            "H-1",
            "Hospital",
            "D-1",
            "Dept",
            "DOC-1",
            "Doctor",
            "Dx",
            "Symptom",
            "Liver",
            LocalDate.now(),
            LocalDate.now(),
            null,
            "remark",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now());
    }

    static Specimen specimen(String applicationId, String specimenId, String barcode) {
        return specimen(
            applicationId,
            specimenId,
            barcode,
            SpecimenStatus.REGISTERED,
            FixationStatus.PENDING,
            "UNVERIFIED",
            null,
            null,
            null);
    }

    static Specimen specimen(
        String applicationId,
        String specimenId,
        String barcode,
        SpecimenStatus specimenStatus,
        FixationStatus fixationStatus,
        String verificationStatus,
        LocalDateTime specimenConfirmedAt,
        String checkInStatus,
        LocalDateTime specimenRemovalAt
    ) {
        return new Specimen(
            specimenId,
            applicationId,
            null,
            "SP-NO-1",
            barcode,
            "TISSUE",
            "Liver",
            "L1",
            "SURGERY",
            1,
            null,
            false,
            null,
            "Bottle",
            1,
            specimenStatus,
            fixationStatus,
            verificationStatus,
            null,
            null,
            null,
            null,
            specimenRemovalAt,
            null,
            null,
            specimenConfirmedAt,
            checkInStatus,
            checkInStatus == null ? null : LocalDateTime.now(),
            checkInStatus == null ? null : "Operator",
            true,
            null,
            specimenStatus == SpecimenStatus.RECEIVED ? "RECEIVED" : null,
            "PASSED",
            null,
            "pain",
            "D-1",
            "Dept",
            "DOC-1",
            "Doctor",
            LocalDate.now(),
            "LP-1",
            "SUCCESS",
            "u1",
            "Operator",
            LocalDateTime.now().minusHours(1),
            "TERM-1",
            "remark");
    }

    static TransportOrder transportOrder(String transportOrderId, String applicationId, TransportOrderStatus status) {
        return new TransportOrder(
            transportOrderId,
            "TO-NO-1",
            applicationId,
            status,
            "handover-1",
            "Handover User",
            "dept-1",
            "Grossing",
            "dept-2",
            "Lab",
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now().minusHours(1),
            null,
            "TERM-1",
            "remark");
    }
}
