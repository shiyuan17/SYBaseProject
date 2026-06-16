package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class SaveMedicalWasteReagentBagRequest {

    private String id;

    @NotBlank
    private String bagName;

    private String remarks;

    private String source;

    private BigDecimal volumeMl;

    private String wasteType;

    private BigDecimal weightKg;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBagName() {
        return bagName;
    }

    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getVolumeMl() {
        return volumeMl;
    }

    public void setVolumeMl(BigDecimal volumeMl) {
        this.volumeMl = volumeMl;
    }

    public String getWasteType() {
        return wasteType;
    }

    public void setWasteType(String wasteType) {
        this.wasteType = wasteType;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }
}
