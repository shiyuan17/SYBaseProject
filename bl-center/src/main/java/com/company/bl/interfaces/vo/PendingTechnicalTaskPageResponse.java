package com.company.bl.interfaces.vo;

import java.util.List;

public record PendingTechnicalTaskPageResponse(
    List<PendingTechnicalTaskResponse> items,
    int page,
    int size,
    long total
) {
}
