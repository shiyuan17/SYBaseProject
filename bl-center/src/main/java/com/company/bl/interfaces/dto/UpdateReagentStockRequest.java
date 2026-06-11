package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@RejectLegacyOperatorFields
public class UpdateReagentStockRequest {

    private BigDecimal initialQuantity;

    private BigDecimal stockQuantity;

    private BigDecimal remainingQuantity;

    @NotBlank
    @Size(max = 32)
    private String stockStatus;

    private LocalDate productionDate;

    private LocalDateTime inboundAt;

    private LocalDate expiryDate;

    @Size(max = 200)
    private String storageLocation;

    private BigDecimal lowStockThreshold;

    private Integer nearExpiryDays;

    private Integer testReminderThreshold;

    private Integer expiryReminderThreshold;

    @Size(max = 100)
    private String recommendedDilution;

    @Size(max = 100)
    private String applicationDilution;

    private BigDecimal stainCapacity;

    private BigDecimal stainThreshold;

    private Integer validityDays;

    @Size(max = 500)
    private String remarks;
}
