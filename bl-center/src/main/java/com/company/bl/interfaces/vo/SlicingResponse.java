package com.company.bl.interfaces.vo;

import java.util.List;

public record SlicingResponse(
    String taskId,
    String slicingId,
    List<String> slideIds,
    String caseStatus
) {
}
