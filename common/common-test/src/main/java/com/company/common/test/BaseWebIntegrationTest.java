package com.company.common.test;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureObservability
@AutoConfigureMockMvc
@Tag("slow")
public abstract class BaseWebIntegrationTest {
}
