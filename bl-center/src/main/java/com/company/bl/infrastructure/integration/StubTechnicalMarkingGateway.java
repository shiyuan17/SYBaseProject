package com.company.bl.infrastructure.integration;

import com.company.bl.application.gateway.TechnicalMarkingGateway;
import org.springframework.stereotype.Component;

@Component
public class StubTechnicalMarkingGateway implements TechnicalMarkingGateway {

    @Override
    public MarkingResult mark(MarkingRequest request) {
        if (request.deviceCode() != null && "FAIL".equalsIgnoreCase(request.deviceCode().trim())) {
            return new MarkingResult(false, "Technical marking failed");
        }
        return new MarkingResult(true, "Technical marking succeeded");
    }
}
