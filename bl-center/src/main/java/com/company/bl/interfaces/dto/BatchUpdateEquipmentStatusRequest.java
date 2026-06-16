package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchUpdateEquipmentStatusRequest {

    private List<@NotBlank @Size(max = 64) String> equipmentIds;

    @NotBlank
    @Size(max = 32)
    private String equipmentStatus;
}
