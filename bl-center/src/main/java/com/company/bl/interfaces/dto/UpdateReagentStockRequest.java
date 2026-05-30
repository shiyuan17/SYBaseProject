package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@RejectLegacyOperatorFields
public class UpdateReagentStockRequest {

    private BigDecimal stockQuantity;

    @NotBlank
    @Size(max = 32)
    private String stockStatus;

    private LocalDate expiryDate;

    @Size(max = 200)
    private String storageLocation;

    private BigDecimal lowStockThreshold;

    private Integer nearExpiryDays;

    @Size(max = 500)
    private String remarks;
}
