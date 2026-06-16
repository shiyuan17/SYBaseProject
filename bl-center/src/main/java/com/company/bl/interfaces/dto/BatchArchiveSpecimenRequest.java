package com.company.bl.interfaces.dto;

import com.company.bl.interfaces.auth.RejectLegacyOperatorFields;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@RejectLegacyOperatorFields
public class BatchArchiveSpecimenRequest {

    @NotBlank
    @Size(max = 64)
    private String archiveCabinetId;

    @NotEmpty
    private List<@NotBlank @Size(max = 64) String> objectIds;

    private LocalDateTime archiveExpiresAt;

    @Min(0)
    @Max(9999)
    private Integer archiveReminderDays;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;
}
