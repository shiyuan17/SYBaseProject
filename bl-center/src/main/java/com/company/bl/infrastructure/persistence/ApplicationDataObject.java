package com.company.bl.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("applications")
public class ApplicationDataObject {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("application_no")
    private String applicationNo;

    @TableField("patient_id")
    private String patientId;

    @TableField("application_type")
    private String applicationType;

    @TableField("status")
    private String status;

    @TableField("external_order_no")
    private String externalOrderNo;

    @TableField("third_party_source")
    private String thirdPartySource;

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

    @TableField("remarks")
    private String remarks;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicationNo() {
        return applicationNo;
    }

    public void setApplicationNo(String applicationNo) {
        this.applicationNo = applicationNo;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExternalOrderNo() {
        return externalOrderNo;
    }

    public void setExternalOrderNo(String externalOrderNo) {
        this.externalOrderNo = externalOrderNo;
    }

    public String getThirdPartySource() {
        return thirdPartySource;
    }

    public void setThirdPartySource(String thirdPartySource) {
        this.thirdPartySource = thirdPartySource;
    }

    public String getApplicationFormStatus() {
        return applicationFormStatus;
    }

    public void setApplicationFormStatus(String applicationFormStatus) {
        this.applicationFormStatus = applicationFormStatus;
    }

    public String getClinicalDiagnosis() {
        return clinicalDiagnosis;
    }

    public void setClinicalDiagnosis(String clinicalDiagnosis) {
        this.clinicalDiagnosis = clinicalDiagnosis;
    }

    public String getClinicalSymptom() {
        return clinicalSymptom;
    }

    public void setClinicalSymptom(String clinicalSymptom) {
        this.clinicalSymptom = clinicalSymptom;
    }

    public String getSpecimenSite() {
        return specimenSite;
    }

    public void setSpecimenSite(String specimenSite) {
        this.specimenSite = specimenSite;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public LocalDate getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDate submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
