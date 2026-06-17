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

import java.time.LocalDateTime;
import java.util.Optional;

import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenBarcodeBindingCommand;
import static com.company.bl.application.service.SpecimenWorkflowModels.SpecimenBarcodeUnbindCommand;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenBarcodeBindingServiceTest {

    @Mock
    private SpecimenWorkflowCommandRepository commandRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Test
    void bindShouldRejectWhenSpecimenAlreadyBound() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenById("SPEC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SPEC-1",
                "BC-1",
                SpecimenStatus.REGISTERED,
                FixationStatus.PENDING,
                "UNVERIFIED",
                null,
                null,
                null)));
        SpecimenBarcodeBindingService service = new SpecimenBarcodeBindingService(commandRepository, support);

        assertThatThrownBy(() -> service.bindSpecimenBarcode(
            new SpecimenBarcodeBindingCommand("SPEC-1", "BC-NEW-1", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("already bound");
    }

    @Test
    void bindShouldAllowUncheckedWorkflowLockedSpecimenWhenBarcodeIsBlank() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenById("SPEC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SPEC-1",
                null,
                SpecimenStatus.CHECKED_IN,
                FixationStatus.COMPLETED,
                "VERIFIED",
                LocalDateTime.now().minusMinutes(40),
                "CHECKED_IN",
                null)));
        when(queryRepository.findSpecimenByBarcode("BC-NEW-1"))
            .thenReturn(Optional.empty());
        SpecimenBarcodeBindingService service = new SpecimenBarcodeBindingService(commandRepository, support);

        assertThatCode(() -> service.bindSpecimenBarcode(
            new SpecimenBarcodeBindingCommand("SPEC-1", "BC-NEW-1", "u1", "Operator", "TERM-1", "remark")))
            .doesNotThrowAnyException();
    }

    @Test
    void rebindShouldRejectWhenSpecimenIsNotBound() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenById("SPEC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SPEC-1",
                null,
                SpecimenStatus.REGISTERED,
                FixationStatus.PENDING,
                "UNVERIFIED",
                null,
                null,
                null)));
        SpecimenBarcodeBindingService service = new SpecimenBarcodeBindingService(commandRepository, support);

        assertThatThrownBy(() -> service.rebindSpecimenBarcode(
            new SpecimenBarcodeBindingCommand("SPEC-1", "BC-NEW-1", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("must be bound before rebinding");
    }

    @Test
    void unbindShouldRejectCheckedInSpecimen() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        when(queryRepository.findSpecimenById("SPEC-1"))
            .thenReturn(Optional.of(SpecimenWorkflowServiceTestFixtures.specimen(
                "APP-1",
                "SPEC-1",
                "BC-1",
                SpecimenStatus.CHECKED_IN,
                FixationStatus.COMPLETED,
                "VERIFIED",
                LocalDateTime.now().minusMinutes(30),
                "CHECKED_IN",
                null)));
        SpecimenBarcodeBindingService service = new SpecimenBarcodeBindingService(commandRepository, support);

        assertThatThrownBy(() -> service.unbindSpecimenBarcode(
            new SpecimenBarcodeUnbindCommand("SPEC-1", "u1", "Operator", "TERM-1", "remark")))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("不能已入库");
    }
}
