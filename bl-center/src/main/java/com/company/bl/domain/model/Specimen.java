package com.company.bl.domain.model;

import com.company.bl.domain.enums.FixationStatus;
import com.company.bl.domain.enums.SpecimenStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Specimen(
    String id,
    String applicationId,
    String caseId,
    String specimenNo,
    String barcode,
    String specimenType,
    String specimenNameStandardized,
    String specimenSite,
    String collectionMode,
    Integer specimenCount,
    SpecimenStatus specimenStatus,
    FixationStatus fixationStatus,
    boolean qualified,
    String unqualifiedReason,
    String clinicalSymptom,
    String applicantDepartmentId,
    String applicantDepartmentName,
    String applicantDoctorUserId,
    String applicantDoctorName,
    LocalDate submissionDate,
    String labelPrintBatchNo,
    String labelPrintStatus,
    String registeredByUserId,
    String registeredByName,
    LocalDateTime registeredAt,
    String terminalCode,
    String remarks
) {
}
