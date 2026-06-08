package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "MedicalOrderBillingRequest", description = "Medical order billing action request")
@RejectLegacyOperatorFields
public class MedicalOrderBillingRequest {

    @Schema(description = "Case ID")
    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Schema(description = "Target medical order IDs. Empty means all uncharged orders in the case.")
    @Size(max = 200)
    private List<@Size(max = 64) String> orderIds;

    @Schema(description = "Terminal code")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "Remarks")
    @Size(max = 500)
    private String remarks;
}
