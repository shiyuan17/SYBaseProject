package com.company.bl.interfaces.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateApplicationRequest {

    @Size(max = 64, message = "Application number must not exceed 64 characters")
    private String applicationNo;

    @Size(max = 64, message = "Patient id must not exceed 64 characters")
    private String patientId;

    @Size(max = 100, message = "Patient name must not exceed 100 characters")
    private String patientName;

    @Size(max = 16, message = "Patient gender must not exceed 16 characters")
    private String patientGender;

    @Size(max = 32, message = "Patient age must not exceed 32 characters")
    private String patientAge;

    @Size(max = 50, message = "Application type must not exceed 50 characters")
    private String applicationType;

    @Size(max = 32, message = "Status must not exceed 32 characters")
    private String status;

    @Size(max = 64, message = "External order number must not exceed 64 characters")
    private String externalOrderNo;

    @Size(max = 64, message = "Third-party source must not exceed 64 characters")
    private String thirdPartySource;

    @Size(max = 64, message = "Source hospital id must not exceed 64 characters")
    private String sourceHospitalId;

    @Size(max = 100, message = "Source hospital name must not exceed 100 characters")
    private String sourceHospitalName;

    @Size(max = 64, message = "Submitting department id must not exceed 64 characters")
    private String submittingDepartmentId;

    @Size(max = 100, message = "Submitting department name must not exceed 100 characters")
    private String submittingDepartmentName;

    @Size(max = 64, message = "Submitting doctor user id must not exceed 64 characters")
    private String submittingDoctorUserId;

    @Size(max = 100, message = "Submitting doctor name must not exceed 100 characters")
    private String submittingDoctorName;

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
}
