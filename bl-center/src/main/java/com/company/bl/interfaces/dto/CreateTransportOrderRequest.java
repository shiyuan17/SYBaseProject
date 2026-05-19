package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTransportOrderRequest {

    @NotBlank
    @Size(max = 64)
    private String applicationId;

    @NotEmpty
    private List<String> specimenBarcodes;

    @Size(max = 64)
    private String handoverUserId;

    @NotBlank
    @Size(max = 100)
    private String handoverUserName;

    @Size(max = 64)
    private String handoverDepartmentId;

    @NotBlank
    @Size(max = 100)
    private String handoverDepartmentName;

    @Size(max = 64)
    private String receiverDepartmentId;

    @NotBlank
    @Size(max = 100)
    private String receiverDepartmentName;

    @Size(max = 64)
    private String terminalCode;

    @Size(max = 500)
    private String remarks;
}
