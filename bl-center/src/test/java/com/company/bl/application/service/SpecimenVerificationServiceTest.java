package com.company.bl.application.service;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.model.Specimen;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.company.bl.application.service.SpecimenWorkflowModels.CheckInSpecimenCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenVerificationCommand;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenVerificationServiceTest {

    @Mock
    private SpecimenWorkflowCommandRepository commandRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Test
    void verificationShouldRejectRepeatedStart() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SP-1",
                "BC-1",
                SpecimenStatus.REGISTERED,
                FixationStatus.PENDING,
                "VERIFYING",
                null,
                null,
                null)));
        SpecimenVerificationService service = new SpecimenVerificationService(commandRepository, support);

        assertThatThrownBy(() -> service.startSpecimenVerification(
            new SpecimenVerificationCommand("BC-1", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("already started");
    }

    @Test
    void checkInShouldRejectUnconfirmedSpecimen() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SP-1",
                "BC-1",
                SpecimenStatus.FIXED,
                FixationStatus.COMPLETED,
                "VERIFIED",
                null,
                null,
                null)));
        SpecimenVerificationService service = new SpecimenVerificationService(commandRepository, support);

        assertThatThrownBy(() -> service.checkInSpecimen(
            new CheckInSpecimenCommand("BC-1", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("must be confirmed before check-in");
    }

    @Test
    void checkInShouldRejectWhenApplicationStillHasUnreadySpecimens() {
        Specimen readySpecimen = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-1",
            "BC-1",
            SpecimenStatus.FIXED,
            FixationStatus.COMPLETED,
            "VERIFIED",
            java.time.LocalDateTime.now(),
            null,
            null);
        Specimen unconfirmedSibling = SpecimenWorkflowServiceTestFixtures.specimen(
            "APP-1",
            "SP-2",
            "BC-2",
            SpecimenStatus.FIXED,
            FixationStatus.COMPLETED,
            "VERIFIED",
            null,
            null,
            null);
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenByBarcode("BC-1")).thenReturn(Optional.of(readySpecimen));
        when(queryRepository.findSpecimensByApplicationId(eq("APP-1")))
            .thenReturn(java.util.List.of(readySpecimen, unconfirmedSibling));
        SpecimenVerificationService service = new SpecimenVerificationService(commandRepository, support);

        assertThatThrownBy(() -> service.checkInSpecimen(
            new CheckInSpecimenCommand("BC-1", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("All specimens of the application");
    }
}
