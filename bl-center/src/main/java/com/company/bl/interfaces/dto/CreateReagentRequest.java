package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RejectLegacyOperatorFields
public class CreateReagentRequest {

    @NotBlank
    @Size(max = 64)
    private String reagentCode;

    @NotBlank
    @Size(max = 100)
    private String reagentName;

    @Size(max = 100)
    private String specification;

    @Size(max = 32)
    private String unit;

    @Size(max = 200)
    private String manufacturer;

    private BigDecimal defaultLowStockThreshold;

    private Integer defaultNearExpiryDays;

    private boolean enabled = true;

    @Size(max = 500)
    private String remarks;
}
