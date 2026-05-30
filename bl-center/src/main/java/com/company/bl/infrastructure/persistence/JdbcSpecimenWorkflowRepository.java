package com.company.bl.infrastructure.persistence;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSpecimenWorkflowRepository extends JdbcSpecimenWorkflowTransportMutationSupport {

    public JdbcSpecimenWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }
}
