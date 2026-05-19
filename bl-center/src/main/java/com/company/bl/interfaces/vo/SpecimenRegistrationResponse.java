package com.company.bl.interfaces.vo;

import java.util.List;

public record SpecimenRegistrationResponse(
    String labelPrintBatchNo,
    boolean labelPrintSuccess,
    String labelPrintMessage,
    List<SpecimenSummaryResponse> specimens
) {
}
