package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateApplicationRequest {

    @NotBlank(message = "Application number must not be blank")
    @Size(max = 64, message = "Application number must not exceed 64 characters")
    private String applicationNo;

    @Size(max = 64, message = "Patient id must not exceed 64 characters")
    private String patientId;

    @Size(max = 50, message = "Application type must not exceed 50 characters")
    private String applicationType;

    @Size(max = 32, message = "Status must not exceed 32 characters")
    private String status;

    @Size(max = 64, message = "External order number must not exceed 64 characters")
    private String externalOrderNo;

    @Size(max = 64, message = "Third-party source must not exceed 64 characters")
    private String thirdPartySource;

    @Size(max = 500, message = "Clinical diagnosis must not exceed 500 characters")
    private String clinicalDiagnosis;

    @Size(max = 500, message = "Clinical symptom must not exceed 500 characters")
    private String clinicalSymptom;

    @Size(max = 200, message = "Specimen site must not exceed 200 characters")
    private String specimenSite;

    private LocalDate applicationDate;
    private LocalDate submissionDate;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

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
}
