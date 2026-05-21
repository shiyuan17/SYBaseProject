package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
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

    @Size(max = 64)
    private String operatorUserId;

    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Size(max = 500)
    private String remarks;
}
