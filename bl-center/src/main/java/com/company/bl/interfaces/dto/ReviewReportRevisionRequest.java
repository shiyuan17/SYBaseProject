package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ReviewReportRevisionRequest", description = "Report revision review request")
@RejectLegacyOperatorFields
public class ReviewReportRevisionRequest {

    @Schema(description = "Terminal code")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "Remarks")
    @Size(max = 500)
    private String remarks;

    @Schema(description = "Reject reason")
    @Size(max = 1000)
    private String rejectReason;
}
