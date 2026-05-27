package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(name = "SaveApplicationRegistrationWorkbenchRequest", description = "申请登记工作台保存请求")
public class SaveApplicationRegistrationWorkbenchRequest {

    @Valid
    @NotNull
    private ContagiousSpecimen contagiousSpecimen;

    @Valid
    @NotNull
    private GynecologyInfo gynecologyInfo;

    @Valid
    @NotNull
    private PatientInfo patientInfo;

    @Valid
    @NotEmpty
    private List<SpecimenItem> specimenItems;

    @Valid
    @NotNull
    private SurgeryInfo surgeryInfo;

    @Getter
    @Setter
    public static class ContagiousSpecimen {
        private boolean hepatitis;
        private boolean hiv;
        private boolean isolation;
        private boolean syphilis;
        private boolean tuberculosis;
    }

    @Getter
    @Setter
    public static class SpecialConditions {
        private boolean abnormalBleeding;
        private boolean birthControl;
        private boolean hormoneReplacement;
        private boolean hysterectomy;
        private boolean iud;
        private boolean lactation;
        private boolean menopause;
        @Size(max = 500)
        private String other;
        private boolean pregnancy;
        private boolean radiotherapy;
    }

    @Getter
    @Setter
    public static class GynecologyInfo {
        @Size(max = 1000)
        private String additionalNotes;
        @Size(max = 100)
        private String hpvResult;
        @Size(max = 64)
        private String lastMenstrualPeriod;
        private boolean menopause;
        @Size(max = 1000)
        private String previousCytology;
        @Size(max = 1000)
        private String previousTreatment;
        @Valid
        @NotNull
        private SpecialConditions specialConditions;
    }

    @Getter
    @Setter
    public static class PatientInfo {
        @Size(max = 32)
        private String age;
        @Size(max = 32)
        private String applicationDate;
        @Size(max = 64)
        private String applicationNo;
        @Size(max = 100)
        private String applyDept;
        @Size(max = 100)
        private String applyDoctor;
        @Size(max = 64)
        private String bedNo;
        @Size(max = 200)
        private String checkItem;
        @Size(max = 500)
        private String clinicalDiagnosis;
        @Size(max = 1000)
        private String clinicalHistory;
        @Size(max = 200)
        private String deliveryRequirement;
        @Size(max = 1000)
        private String endoscopyDiagnosis;
        private boolean frozenReminder;
        @Size(max = 16)
        private String gender;
        @Size(max = 64)
        private String idNo;
        @Size(max = 1000)
        private String imagingResult;
        @Size(max = 64)
        private String inpatientNo;
        @Size(max = 100)
        private String patientName;
        private boolean patientVerified;
        @Size(max = 32)
        private String phone;
        @Size(max = 32)
        private String registrationStatus;
        @Size(max = 500)
        private String remark;
        @Size(max = 100)
        private String specimenType;
        @Size(max = 100)
        private String wardName;
    }

    @Getter
    @Setter
    public static class SpecimenItem {
        @NotNull
        @Min(1)
        private Integer quantity;
        @NotBlank
        @Size(max = 200)
        private String specimenName;
        @Size(max = 64)
        private String specimenNo;
        @NotBlank
        @Size(max = 200)
        private String specimenSite;
        @Size(max = 64)
        private String status;
    }

    @Getter
    @Setter
    public static class SurgeryInfo {
        @Size(max = 64)
        private String buildingId;
        @Size(max = 1000)
        private String clinicalFindings;
        @Size(max = 100)
        private String fixativeType;
        @Size(max = 100)
        private String fixationPerson;
        @Size(max = 32)
        private String fixationTime;
        @Size(max = 64)
        private String roomId;
        @Size(max = 200)
        private String surgeryName;
    }
}
