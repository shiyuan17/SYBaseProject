package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.SpecimenWorkflowCommandRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSpecimenWorkflowRepository
    extends JdbcSpecimenWorkflowTransportMutationSupport
    implements SpecimenWorkflowCommandRepository {

    public JdbcSpecimenWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }
}
