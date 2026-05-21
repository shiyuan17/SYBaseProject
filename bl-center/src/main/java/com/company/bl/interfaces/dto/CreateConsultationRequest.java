package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "CreateConsultationRequest", description = "Create internal consultation request")
public class CreateConsultationRequest {

    @Schema(description = "Case ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 64)
    private String caseId;

    @Valid
    @NotEmpty
    @Schema(description = "Participants", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ParticipantItem> participants;

    @Schema(description = "Operator user ID")
    @Size(max = 64)
    private String operatorUserId;

    @Schema(description = "Operator name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String operatorName;

    @Schema(description = "Terminal code")
    @Size(max = 64)
    private String terminalCode;

    @Schema(description = "Remarks")
    @Size(max = 500)
    private String remarks;

    @Getter
    @Setter
    @Schema(name = "CreateConsultationParticipantItem", description = "Consultation participant input")
    public static class ParticipantItem {

        @Schema(description = "Participant user ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 64)
        private String participantUserId;

        @Schema(description = "Participant name", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        private String participantName;

        @Schema(description = "Participant role", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 32)
        private String participantRole;
    }
}
