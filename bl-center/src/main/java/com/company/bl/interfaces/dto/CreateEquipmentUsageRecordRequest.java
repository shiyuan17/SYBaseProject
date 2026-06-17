package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RejectLegacyOperatorFields
public class CreateEquipmentUsageRecordRequest {

    @Size(max = 64)
    private String equipmentId;

    @NotBlank
    @Size(max = 64)
    private String equipmentCategory;

    @NotBlank
    @Size(max = 100)
    private String equipmentName;

    @NotNull
    private Boolean commonlyUsed;

    @NotBlank
    @Size(max = 64)
    private String startedAt;

    @NotBlank
    @Size(max = 64)
    private String endedAt;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal runtimeHours;

    @NotNull
    @Min(0)
    private Integer diagnosisCount;

    @NotBlank
    @Size(max = 32)
    private String equipmentCondition;

    @NotBlank
    @Size(max = 100)
    private String usageOperatorName;

    @Size(max = 500)
    private String usageContent;

    @Size(max = 500)
    private String remarks;
}
