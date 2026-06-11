package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RejectLegacyOperatorFields
public class ReagentStockActionRequest {

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal quantity;

    @Size(max = 500)
    private String remarks;
}
