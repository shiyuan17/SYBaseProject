package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class MedicalWasteSpecimenPreviewRequest {

    private String bagName;

    @NotBlank
    private String grossingOperatorName;

    @NotBlank
    private String grossingPeriod;

    @NotBlank
    private String grossingStationName;

    @NotNull
    private LocalDate grossingDate;

    public String getBagName() {
        return bagName;
    }

    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    public String getGrossingOperatorName() {
        return grossingOperatorName;
    }

    public void setGrossingOperatorName(String grossingOperatorName) {
        this.grossingOperatorName = grossingOperatorName;
    }

    public String getGrossingPeriod() {
        return grossingPeriod;
    }

    public void setGrossingPeriod(String grossingPeriod) {
        this.grossingPeriod = grossingPeriod;
    }

    public String getGrossingStationName() {
        return grossingStationName;
    }

    public void setGrossingStationName(String grossingStationName) {
        this.grossingStationName = grossingStationName;
    }

    public LocalDate getGrossingDate() {
        return grossingDate;
    }

    public void setGrossingDate(LocalDate grossingDate) {
        this.grossingDate = grossingDate;
    }
}
