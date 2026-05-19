package com.company.bl.infrastructure.integration;

import com.company.bl.application.gateway.LabelPrintGateway;
import org.springframework.stereotype.Component;

@Component
public class StubLabelPrintGateway implements LabelPrintGateway {

    @Override
    public LabelPrintResult print(LabelPrintRequest request) {
        if ("FAIL".equalsIgnoreCase(request.printerCode())) {
            return new LabelPrintResult(false, "Printer placeholder forced failure");
        }
        return new LabelPrintResult(true, "Printed by placeholder gateway");
    }
}
