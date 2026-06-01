package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SpecimenWorkflowQueryServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Test
    void listSpecimenVerificationRecordsShouldRejectBlankBarcode() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        SpecimenWorkflowPendingQuerySupport pendingQuerySupport =
            new SpecimenWorkflowPendingQuerySupport(applicationRepository, queryRepository, support);
        SpecimenWorkflowQueryService service = new SpecimenWorkflowQueryService(
            pendingQuerySupport,
            mock(SpecimenWorkflowApplicationQuerySupport.class),
            mock(SpecimenWorkflowTrackingQuerySupport.class),
            mock(SpecimenWorkflowRemovalQuerySupport.class));

        assertThatThrownBy(() -> service.listSpecimenVerificationRecords("  "))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Specimen barcode is required");
    }
}
