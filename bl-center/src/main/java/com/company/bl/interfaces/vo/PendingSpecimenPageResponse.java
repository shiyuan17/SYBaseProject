package com.company.bl.interfaces.vo;

import java.util.List;

public record PendingSpecimenPageResponse(
    List<PendingSpecimenItemResponse> items,
    int page,
    int size,
    long total
) {
}
