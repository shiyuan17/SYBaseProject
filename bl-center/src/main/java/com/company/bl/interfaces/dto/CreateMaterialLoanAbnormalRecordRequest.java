package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@RejectLegacyOperatorFields
public class CreateMaterialLoanAbnormalRecordRequest {

    @NotBlank
    @Size(max = 32)
    private String materialType;

    @NotBlank
    @Size(max = 64)
    private String materialId;

    @Size(max = 64)
    private String loanId;

    @NotBlank
    @Size(max = 1000)
    private String abnormalReason;

    private Boolean contacted;

    @Size(max = 1000)
    private String contactResult;

    @Size(max = 100)
    private String borrowedSlideNo;

    @Size(max = 100)
    private String borrowerName;

    @Size(max = 100)
    private String borrowerRelationship;

    @Size(max = 100)
    private String borrowerPhone;

    @Size(max = 200)
    private String borrowerUnit;

    @Size(max = 100)
    private String borrowerIdentityNo;

    private LocalDateTime borrowedAt;

    private LocalDateTime expectedReturnAt;

    @Min(0)
    private Integer slideCount;

    @DecimalMin("0")
    private BigDecimal depositAmount;

    @Size(max = 1000)
    private String borrowedContent;

    @Size(max = 1000)
    private String returnAbnormalInfo;

    @Size(max = 64)
    private String terminalCode;
}
