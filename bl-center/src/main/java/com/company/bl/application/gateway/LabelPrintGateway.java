package com.company.bl.application.gateway;

import java.util.List;

public interface LabelPrintGateway {

    LabelPrintResult print(LabelPrintRequest request);

    record LabelPrintRequest(
        String applicationId,
        String batchNo,
        String printerCode,
        List<String> barcodes
    ) {
    }

    record LabelPrintResult(boolean success, String message) {
    }
}
