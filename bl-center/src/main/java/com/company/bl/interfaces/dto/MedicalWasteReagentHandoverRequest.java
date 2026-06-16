package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class MedicalWasteReagentHandoverRequest {

    @NotBlank
    private String handedOverByName;

    @NotNull
    private LocalDateTime handedOverAt;

    private String handoverRemarks;

    public String getHandedOverByName() {
        return handedOverByName;
    }

    public void setHandedOverByName(String handedOverByName) {
        this.handedOverByName = handedOverByName;
    }

    public LocalDateTime getHandedOverAt() {
        return handedOverAt;
    }

    public void setHandedOverAt(LocalDateTime handedOverAt) {
        this.handedOverAt = handedOverAt;
    }

    public String getHandoverRemarks() {
        return handoverRemarks;
    }

    public void setHandoverRemarks(String handoverRemarks) {
        this.handoverRemarks = handoverRemarks;
    }
}
