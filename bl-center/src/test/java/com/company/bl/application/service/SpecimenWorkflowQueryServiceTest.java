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

@ExtendWith(MockitoExtension.class)
class SpecimenWorkflowQueryServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Test
    void listSpecimenVerificationRecordsShouldRejectBlankBarcode() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        SpecimenWorkflowQueryService service = new SpecimenWorkflowQueryService(applicationRepository, queryRepository, support);

        assertThatThrownBy(() -> service.listSpecimenVerificationRecords("  "))
            .isInstanceOf(BlBusinessException.class)
            .hasMessageContaining("Specimen barcode is required");
    }
}
