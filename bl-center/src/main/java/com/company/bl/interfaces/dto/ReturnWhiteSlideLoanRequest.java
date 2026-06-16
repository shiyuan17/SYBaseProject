package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RejectLegacyOperatorFields
public class ReturnWhiteSlideLoanRequest {

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;
}
