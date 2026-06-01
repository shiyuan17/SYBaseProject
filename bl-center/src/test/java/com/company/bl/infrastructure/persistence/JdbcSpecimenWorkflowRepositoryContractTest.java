package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcSpecimenWorkflowRepositoryContractTest {

    @Test
    void shouldImplementSpecimenWorkflowCommandRepository() {
        assertThat(SpecimenWorkflowCommandRepository.class)
            .isAssignableFrom(JdbcSpecimenWorkflowRepository.class);
    }
}
