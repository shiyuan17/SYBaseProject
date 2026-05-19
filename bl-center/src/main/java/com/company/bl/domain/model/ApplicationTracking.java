package com.company.bl.domain.model;

import java.util.List;

public record ApplicationTracking(
    Application application,
    String currentNode,
    boolean abnormal,
    List<Specimen> specimens,
    List<TrackingEvent> events
) {
}
