package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CommentConsultationParticipantRequest", description = "Comment consultation participant request")
@RejectLegacyOperatorFields
public class CommentConsultationParticipantRequest {

    @Schema(description = "Opinion", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 2000)
    private String opinion;

    @Schema(description = "Terminal code")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "Remarks")
    @Size(max = 500)
    private String remarks;
}
