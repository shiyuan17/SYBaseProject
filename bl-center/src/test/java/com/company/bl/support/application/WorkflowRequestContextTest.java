package com.company.bl.support.application;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowRequestContextTest {

    @Test
    void resolvesClientIpFromForwardedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            assertThat(WorkflowRequestContext.resolveClientIp()).isEqualTo("203.0.113.10");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void returnsNullWhenRequestContextIsUnavailable() {
        RequestContextHolder.resetRequestAttributes();
        assertThat(WorkflowRequestContext.resolveClientIp()).isNull();
    }
}
