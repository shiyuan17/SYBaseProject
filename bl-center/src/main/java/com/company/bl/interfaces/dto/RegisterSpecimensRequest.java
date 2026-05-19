package com.company.bl.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RegisterSpecimensRequest {

    @NotBlank
    @Size(max = 64)
    private String applicationId;

    @Size(max = 64)
    private String printerCode;

    @Size(max = 100)
    private String collectionScene;

    @Size(max = 64)
    private String operatorUserId;

    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;

    @Valid
    @NotEmpty
    private List<SpecimenItem> items;

    @Getter
    @Setter
    public static class SpecimenItem {
        @NotBlank
        @Size(max = 200)
        private String specimenNameStandardized;

        @Size(max = 100)
        private String specimenType;

        @Size(max = 200)
        private String specimenSite;

        @Size(max = 50)
        private String collectionMode;

        @NotNull
        private Integer specimenCount;

        @Size(max = 128)
        private String barcode;

        @Size(max = 500)
        private String clinicalSymptom;
    }
}
