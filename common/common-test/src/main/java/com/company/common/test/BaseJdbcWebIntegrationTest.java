package com.company.common.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public abstract class BaseJdbcWebIntegrationTest extends BaseMockMvcIntegrationTest {

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;
}
