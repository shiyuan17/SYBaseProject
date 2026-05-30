package com.company.bl.application.service;

import com.company.bl.application.gateway.LabelPrintGateway;
import com.company.bl.domain.enums.ApplicationStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.valueobject.ApplicationId;
import com.company.bl.support.application.NumberingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.company.bl.application.service.SpecimenWorkflowModels.RegisterSpecimensCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.RetryLabelPrintCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenRegistrationItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenRegistrationServiceTest {

    @Mock
    private SpecimenWorkflowCommandRepository commandRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NumberingService numberingService;

    @Mock
    private LabelPrintGateway labelPrintGateway;

    @Test
    void registrationShouldRejectBarcodeConflict() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(applicationRepository.findById(any(ApplicationId.class)))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.application("APP-1", ApplicationStatus.DRAFT)));
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen("APP-1", "SP-EXIST", "BC-1")));
        SpecimenRegistrationService service =
            new SpecimenRegistrationService(commandRepository, support, numberingService, labelPrintGateway);

        RegisterSpecimensCommand command = new RegisterSpecimensCommand(
            "APP-1",
            "PRINTER-1",
            "OPERATING_ROOM",
            "u1",
            "Operator",
            "TERM-1",
            "remark",
            List.of(new SpecimenRegistrationItem("Liver", "TISSUE", "L1", "SURGERY", 1, "Bottle", 1, "BC-1", "pain")));

        assertThatThrownBy(() -> service.registerSpecimens(command))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Specimen barcode already exists");

        verify(labelPrintGateway, never()).print(any());
    }

    @Test
    void retryLabelPrintShouldReturnEmptyResultWhenNoPendingLabels() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimensByLabelPrintBatchNoAndStatuses("LP-1", List.of("FAILED", "PENDING")))
            .thenReturn(List.of());
        SpecimenRegistrationService service =
            new SpecimenRegistrationService(commandRepository, support, numberingService, labelPrintGateway);

        var result = service.retryLabelPrint(
            new RetryLabelPrintCommand("LP-1", "u1", "Operator", "PRINTER-1", "TERM-1", "remark"));

        assertThat(result.retriedCount()).isZero();
        assertThat(result.allSuccessful()).isTrue();
        verify(labelPrintGateway, never()).print(any());
    }
}
