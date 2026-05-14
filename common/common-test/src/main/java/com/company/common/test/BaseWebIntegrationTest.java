package com.company.common.test;

import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureObservability
@AutoConfigureMockMvc
public abstract class BaseWebIntegrationTest {
}
