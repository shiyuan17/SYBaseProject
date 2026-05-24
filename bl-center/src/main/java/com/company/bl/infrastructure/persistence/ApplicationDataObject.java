package com.company.bl.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("applications")
@Getter
@Setter
public class ApplicationDataObject {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("application_no")
    private String applicationNo;

    @TableField("patient_id")
    private String patientId;

    @TableField("patient_name")
    private String patientName;

    @TableField("patient_gender")
    private String patientGender;

    @TableField("patient_age")
    private String patientAge;

    @TableField("application_type")
    private String applicationType;

    @TableField("status")
    private String status;

    @TableField("external_order_no")
    private String externalOrderNo;

    @TableField("third_party_source")
    private String thirdPartySource;

    @TableField("source_hospital_id")
    private String sourceHospitalId;

    @TableField("source_hospital_name")
    private String sourceHospitalName;

    @TableField("submitting_department_id")
    private String submittingDepartmentId;

    @TableField("submitting_department_name")
    private String submittingDepartmentName;

    @TableField("submitting_doctor_user_id")
    private String submittingDoctorUserId;

    @TableField("submitting_doctor_name")
    private String submittingDoctorName;

    @TableField("application_form_status")
    private String applicationFormStatus;

    @TableField("clinical_diagnosis")
    private String clinicalDiagnosis;

    @TableField("clinical_symptom")
    private String clinicalSymptom;

    @TableField("specimen_site")
    private String specimenSite;

    @TableField("application_date")
    private LocalDate applicationDate;

    @TableField("submission_date")
    private LocalDate submissionDate;

    @TableField("specimen_removal_time")
    private LocalDateTime specimenRemovalTime;

    @TableField("remarks")
    private String remarks;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
