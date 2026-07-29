package com.company.bl.application.service;

import com.company.bl.domain.repository.ApplicationRepository;
import com.company.bl.domain.repository.SpecimenWorkflowQueryRepository;
import com.company.bl.domain.repository.SpecimenWorkflowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecimenWorkflowRemovalQuerySupportTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SpecimenWorkflowQueryRepository queryRepository;

    @Test
    void shouldKeepExportPageSizeUpToTenThousandForSpecimenManagementExport() {
        SpecimenWorkflowSupport support = SpecimenWorkflowServiceTestFixtures.support(applicationRepository, queryRepository);
        SpecimenWorkflowRemovalQuerySupport querySupport =
            new SpecimenWorkflowRemovalQuerySupport(applicationRepository, queryRepository, support);
        when(queryRepository.listSpecimenManagementExportRows(any())).thenReturn(List.of());

        byte[] workbook = querySupport.exportSpecimenManagementItems(
            new SpecimenWorkflowQueryModels.SpecimenManagementListQuery(
                1,
                10000,
                " BC-001 ",
                null,
                null,
                null,
                null,
                null,
                "REJECTED",
                null,
                Boolean.TRUE,
                "2026-07-01",
                "2026-07-06"));

        ArgumentCaptor<SpecimenWorkflowRepository.SpecimenManagementListQuery> captor =
            ArgumentCaptor.forClass(SpecimenWorkflowRepository.SpecimenManagementListQuery.class);
        verify(queryRepository).listSpecimenManagementExportRows(captor.capture());
        SpecimenWorkflowRepository.SpecimenManagementListQuery repositoryQuery = captor.getValue();

        assertThat(workbook).isNotEmpty();
        assertThat(repositoryQuery.page()).isEqualTo(1);
        assertThat(repositoryQuery.size()).isEqualTo(10000);
        assertThat(repositoryQuery.keyword()).isEqualTo("BC-001");
        assertThat(repositoryQuery.specimenStatus()).isEqualTo("REJECTED");
        assertThat(repositoryQuery.abnormalFlag()).isTrue();
        assertThat(repositoryQuery.dateFrom()).isNotNull();
        assertThat(repositoryQuery.dateTo()).isNotNull();
    }
}
