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
public class UpdateReagentRequest {

    @NotBlank
    @Size(max = 100)
    private String reagentName;

    @Size(max = 100)
    private String specification;

    @Size(max = 32)
    private String unit;

    @Size(max = 200)
    private String manufacturer;

    @Size(max = 64)
    private String reagentType;

    @Size(max = 200)
    private String reagentUsage;

    @Size(max = 64)
    private String orderDictItemId;

    @Size(max = 100)
    private String cloneNo;

    @Size(max = 100)
    private String recommendedDilution;

    @Size(max = 100)
    private String applicationDilution;

    @Size(max = 32)
    private String templateStatus;

    private Integer validityDays;

    private BigDecimal defaultLowStockThreshold;

    private BigDecimal defaultStockThreshold;

    private Integer defaultNearExpiryDays;

    private BigDecimal stainCapacity;

    private BigDecimal stainThreshold;

    private boolean enabled = true;

    @Size(max = 500)
    private String remarks;
}
