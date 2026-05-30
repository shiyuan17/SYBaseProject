package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenWorkflowSupportTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationRegistrationWorkbenchRepository workbenchRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Test
    void resolveSpecimenForRemovalShouldRejectUnknownIdentifierType() {
        SpecimenWorkflowSupport support = new SpecimenWorkflowSupport(applicationRepository, workbenchRepository, queryRepository);

        assertThatThrownBy(() -> support.resolveSpecimenForRemoval("UNKNOWN", "ABC"))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Unsupported specimen identifier type");
    }

    @Test
    void resolveSpecimenForRemovalShouldRejectDuplicateSpecimenNumbers() {
        SpecimenWorkflowSupport support = new SpecimenWorkflowSupport(applicationRepository, workbenchRepository, queryRepository);
        when(queryRepository.findSpecimensBySpecimenNo("SP-1")).thenReturn(List.of(
            SpecimenWorkflowServiceTestFixtures.specimen("APP-1", "SP-1", "BC-1"),
            SpecimenWorkflowServiceTestFixtures.specimen("APP-1", "SP-2", "BC-2")));

        assertThatThrownBy(() -> support.resolveSpecimenForRemoval("SPECIMEN_NO", "SP-1"))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("multiple records");
    }
}
