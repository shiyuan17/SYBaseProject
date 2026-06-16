package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
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
public class CreateWhiteSlideLoanRequest {

    @NotBlank
    @Size(max = 64)
    private String stockId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @Size(max = 64)
    private String caseId;

    @Size(max = 64)
    private String pathologyNo;

    @Size(max = 100)
    private String patientName;

    @Size(max = 64)
    private String embeddingBoxNo;

    @Size(max = 500)
    private String slicePurpose;

    @Size(max = 100)
    private String sliceThickness;

    @NotBlank
    @Size(max = 100)
    private String borrowerName;

    @Size(max = 64)
    private String borrowerIdentityNo;

    @Size(max = 200)
    private String borrowerUnit;

    @Size(max = 32)
    private String borrowerPhone;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private Boolean saveDirectPrint;

    @Size(max = 500)
    private String waxBlockUsage;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;
}
