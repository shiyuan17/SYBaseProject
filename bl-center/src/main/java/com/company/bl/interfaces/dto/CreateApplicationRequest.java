package com.company.bl.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(name = "CreateApplicationRequest", description = "病理申请单创建请求")
public class CreateApplicationRequest {

    @Schema(description = "申请单号，不传则由系统按编号规则生成")
    @Size(max = 64, message = "Application number must not exceed 64 characters")
    private String applicationNo;

    @Schema(description = "患者 ID")
    @Size(max = 64, message = "Patient id must not exceed 64 characters")
    private String patientId;

    @Schema(description = "患者姓名")
    @Size(max = 100, message = "Patient name must not exceed 100 characters")
    private String patientName;

    @Schema(description = "患者性别")
    @Size(max = 16, message = "Patient gender must not exceed 16 characters")
    private String patientGender;

    @Schema(description = "患者年龄")
    @Size(max = 32, message = "Patient age must not exceed 32 characters")
    private String patientAge;

    @Schema(description = "申请类型，例如 ROUTINE")
    @Size(max = 50, message = "Application type must not exceed 50 characters")
    private String applicationType;

    @Schema(description = "申请单状态")
    @Size(max = 32, message = "Status must not exceed 32 characters")
    private String status;

    @Schema(description = "外部单号")
    @Size(max = 64, message = "External order number must not exceed 64 characters")
    private String externalOrderNo;

    @Schema(description = "第三方来源标识")
    @Size(max = 64, message = "Third-party source must not exceed 64 characters")
    private String thirdPartySource;

    @Schema(description = "来源医院 ID")
    @Size(max = 64, message = "Source hospital id must not exceed 64 characters")
    private String sourceHospitalId;

    @Schema(description = "来源医院名称")
    @Size(max = 100, message = "Source hospital name must not exceed 100 characters")
    private String sourceHospitalName;

    @Schema(description = "送检科室 ID")
    @Size(max = 64, message = "Submitting department id must not exceed 64 characters")
    private String submittingDepartmentId;

    @Schema(description = "送检科室名称")
    @Size(max = 100, message = "Submitting department name must not exceed 100 characters")
    private String submittingDepartmentName;

    @Schema(description = "送检医生用户 ID")
    @Size(max = 64, message = "Submitting doctor user id must not exceed 64 characters")
    private String submittingDoctorUserId;

    @Schema(description = "送检医生姓名")
    @Size(max = 100, message = "Submitting doctor name must not exceed 100 characters")
    private String submittingDoctorName;

    @Schema(description = "临床诊断")
    @Size(max = 500, message = "Clinical diagnosis must not exceed 500 characters")
    private String clinicalDiagnosis;

    @Schema(description = "临床症状")
    @Size(max = 500, message = "Clinical symptom must not exceed 500 characters")
    private String clinicalSymptom;

    @Schema(description = "送检部位")
    @Size(max = 200, message = "Specimen site must not exceed 200 characters")
    private String specimenSite;

    @Schema(description = "申请日期", type = "string", format = "date")
    private LocalDate applicationDate;
    @Schema(description = "送检日期", type = "string", format = "date")
    private LocalDate submissionDate;

    @Schema(description = "备注")
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
