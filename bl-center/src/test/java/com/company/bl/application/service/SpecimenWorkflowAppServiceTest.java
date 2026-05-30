package com.company.bl.application.service;

import com.company.bl.domain.model.ApplicationTracking;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.model.TransportOrder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.company.bl.application.service.SpecimenWorkflowModels.ConfirmSpecimenCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.FixationCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.RegisterSpecimensCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.CreateTransportOrderCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpecimenWorkflowAppServiceTest {

    @Test
    void facadeShouldDelegateTrackingQueryToQueryService() {
        SpecimenWorkflowQueryService queryService = mock(SpecimenWorkflowQueryService.class);
        SpecimenWorkflowAppService appService = new SpecimenWorkflowAppService(
            null, null, null, null, null, queryService);

        ApplicationTracking tracking = new ApplicationTracking(
            SpecimenWorkflowServiceTestFixtures.application("APP-1", com.company.bl.domain.enums.ApplicationStatus.SUBMITTED),
            "SPECIMEN_COLLECTION",
            false,
            List.of(),
            List.of());
        when(queryService.getApplicationTracking("APP-1")).thenReturn(tracking);

        assertThat(appService.getApplicationTracking("APP-1")).isSameAs(tracking);
    }

    @Test
    void facadeShouldDelegateCommandOperationsToSpecificServices() {
        SpecimenRegistrationService registrationService = mock(SpecimenRegistrationService.class);
        SpecimenFixationService fixationService = mock(SpecimenFixationService.class);
        SpecimenVerificationService verificationService = mock(SpecimenVerificationService.class);
        SpecimenTransportService transportService = mock(SpecimenTransportService.class);
        SpecimenReceiptAndRemovalService receiptService = mock(SpecimenReceiptAndRemovalService.class);
        SpecimenWorkflowQueryService queryService = mock(SpecimenWorkflowQueryService.class);

        SpecimenWorkflowAppService appService = new SpecimenWorkflowAppService(
            registrationService,
            fixationService,
            verificationService,
            transportService,
            receiptService,
            queryService);

        Specimen specimen = SpecimenWorkflowServiceTestFixtures.specimen("APP-1", "SP-1", "BC-1");
        var registrationResult = new SpecimenWorkflowModels.SpecimenRegistrationResult(
            List.of(specimen),
            "LP-1",
            true,
            "ok");
        var fixationResult = new SpecimenWorkflowModels.FixationResult(
            "SP-1",
            "BC-1",
            "COMPLETED",
            LocalDateTime.now(),
            "u1",
            "Operator",
            "FORMALIN");
        TransportOrder transportOrder = SpecimenWorkflowServiceTestFixtures.transportOrder(
            "TO-1",
            "APP-1",
            com.company.bl.domain.enums.TransportOrderStatus.PENDING);
        RegisterSpecimensCommand registerCommand = new RegisterSpecimensCommand(
            "APP-1",
            "PRINTER-1",
            "OPERATING_ROOM",
            "u1",
            "Operator",
            "TERM-1",
            "remark",
            List.of(new SpecimenWorkflowModels.SpecimenRegistrationItem(
                "Liver",
                "TISSUE",
                "L1",
                "SURGERY",
                1,
                "Bottle",
                1,
                "BC-1",
                "pain")));
        FixationCommand fixationCommand = new FixationCommand("BC-1", "FORMALIN", "u1", "Operator", "TERM-1", "remark");
        ConfirmSpecimenCommand confirmCommand = new ConfirmSpecimenCommand("BC-1", "u1", "Operator", "TERM-1", "remark");
        CreateTransportOrderCommand transportCommand = new CreateTransportOrderCommand(
            "APP-1",
            List.of("BC-1"),
            "u1",
            "Operator",
            "D-1",
            "Dept",
            "D-2",
            "Lab",
            "TERM-1",
            "remark");

        when(registrationService.registerSpecimens(registerCommand)).thenReturn(registrationResult);
        when(fixationService.startFixation(fixationCommand)).thenReturn(fixationResult);
        when(verificationService.confirmSpecimen(confirmCommand)).thenReturn(specimen);
        when(transportService.createTransportOrder(transportCommand)).thenReturn(transportOrder);

        assertThat(appService.registerSpecimens(registerCommand)).isSameAs(registrationResult);
        assertThat(appService.startFixation(fixationCommand)).isSameAs(fixationResult);
        assertThat(appService.confirmSpecimen(confirmCommand)).isSameAs(specimen);
        assertThat(appService.createTransportOrder(transportCommand)).isSameAs(transportOrder);
    }
}
