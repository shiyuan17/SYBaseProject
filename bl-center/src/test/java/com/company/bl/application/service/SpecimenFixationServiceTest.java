package com.company.bl.application.service;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.company.bl.application.service.SpecimenWorkflowModels.FixationCommand;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenFixationServiceTest {

    @Mock
    private SpecimenWorkflowCommandRepository commandRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Test
    void fixationShouldRejectUnverifiedSpecimen() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenByBarcode("BC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen("APP-1", "SP-1", "BC-1")));
        SpecimenFixationService service = new SpecimenFixationService(commandRepository, support);

        assertThatThrownBy(() -> service.startFixation(
            new FixationCommand(null, "BC-1", null, "FORMALIN", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("must be verified before fixation");
    }

    @Test
    void fixationShouldStartBySpecimenIdWhenBarcodeIsMissing() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenById("SP-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SP-1",
                null,
                SpecimenStatus.VERIFIED,
                FixationStatus.PENDING,
                "VERIFIED",
                null,
                null,
                null)));
        SpecimenFixationService service = new SpecimenFixationService(commandRepository, support);

        service.startFixation(
            new FixationCommand("SP-1", null, null, "FORMALIN", "u1", "Operator", "TERM-1", "remark"));

        verify(commandRepository).upsertFixationRecord(
            eq("APP-1"),
            eq("SP-1"),
            eq(FixationStatus.FIXING),
            eq("FORMALIN"),
            any(),
            eq(null),
            eq(null),
            eq(null),
            eq(null),
            eq("TERM-1"),
            eq("remark"));
        verify(commandRepository).updateSpecimenStatus(
            eq("SP-1"),
            eq(SpecimenStatus.FIXING),
            eq(FixationStatus.FIXING),
            eq(null),
            eq("remark"),
            eq(null));
    }

    @Test
    void fixationCompletionShouldRejectAlreadyCompletedSpecimen() {
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
        SpecimenFixationService service = new SpecimenFixationService(commandRepository, support);

        assertThatThrownBy(() -> service.completeFixation(
            new FixationCommand(null, "BC-1", null, "FORMALIN", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("already completed");
    }

    @Test
    void fixationShouldCompleteBySpecimenIdWhenBarcodeIsMissing() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenById("SP-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SP-1",
                null,
                SpecimenStatus.FIXING,
                FixationStatus.FIXING,
                "VERIFIED",
                null,
                null,
                null)));
        SpecimenFixationService service = new SpecimenFixationService(commandRepository, support);

        service.completeFixation(
            new FixationCommand("SP-1", null, null, "FORMALIN", "u1", "Operator", "TERM-1", "remark"));

        verify(commandRepository).upsertFixationRecord(
            eq("APP-1"),
            eq("SP-1"),
            eq(FixationStatus.COMPLETED),
            eq("FORMALIN"),
            eq(null),
            any(),
            eq("u1"),
            eq("Operator"),
            any(),
            eq("TERM-1"),
            eq("remark"));
        verify(commandRepository).updateSpecimenStatus(
            eq("SP-1"),
            eq(SpecimenStatus.FIXED),
            eq(FixationStatus.COMPLETED),
            eq(null),
            eq("remark"),
            eq(null));
    }
}
