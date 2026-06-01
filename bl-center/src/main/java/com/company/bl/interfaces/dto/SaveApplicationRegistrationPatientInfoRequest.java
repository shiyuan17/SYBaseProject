package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "SaveApplicationRegistrationPatientInfoRequest", description = "申请登记工作台患者信息保存请求")
public class SaveApplicationRegistrationPatientInfoRequest {

    @Valid
    @NotNull
    private SaveApplicationRegistrationWorkbenchRequest.ContagiousSpecimen contagiousSpecimen;

    @Valid
    @NotNull
    private SaveApplicationRegistrationWorkbenchRequest.GynecologyInfo gynecologyInfo;

    @Valid
    @NotNull
    private SaveApplicationRegistrationWorkbenchRequest.PatientInfo patientInfo;

    @Valid
    @NotNull
    private SaveApplicationRegistrationWorkbenchRequest.SurgeryInfo surgeryInfo;
}
