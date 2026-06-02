package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class JdbcApplicationRegistrationWorkbenchExtensionSupport {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcApplicationRegistrationWorkbenchExtensionSupport(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData> findExtensionByApplicationId(String applicationId) {
        return jdbcTemplate.query("""
                select *
                from application_registration_workbench
                where application_id = :applicationId
                """, Map.of("applicationId", applicationId), this::mapWorkbenchExtensionData)
            .stream()
            .findFirst();
    }

    Optional<ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides>
    findTechnicalRegistrationDetailSectionOverridesByApplicationId(String applicationId) {
        return jdbcTemplate.query("""
                select *
                from application_registration_workbench
                where application_id = :applicationId
                """, Map.of("applicationId", applicationId), this::mapTechnicalRegistrationDetailSectionOverrides)
            .stream()
            .findFirst();
    }

    void upsertExtension(ApplicationRegistrationWorkbenchRepository.SaveWorkbenchExtensionCommand command) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from application_registration_workbench
            where application_id = :applicationId
            """, Map.of("applicationId", command.applicationId()), Long.class);
        MapSqlParameterSource parameters = extensionParameters(command)
            .addValue("updatedAt", LocalDateTime.now())
            .addValue("createdAt", LocalDateTime.now());
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                update application_registration_workbench
                set inpatient_no = :inpatientNo,
                    bed_no = :bedNo,
                    ward_name = :wardName,
                    phone = :phone,
                    id_no = :idNo,
                    check_item = :checkItem,
                    clinical_history = :clinicalHistory,
                    imaging_result = :imagingResult,
                    endoscopy_diagnosis = :endoscopyDiagnosis,
                    delivery_requirement = :deliveryRequirement,
                    specimen_type = :specimenType,
                    surgery_name = :surgeryName,
                    clinical_findings = :clinicalFindings,
                    fixative_type = :fixativeType,
                    fixation_person = :fixationPerson,
                    fixation_time = :fixationTime,
                    building_id = :buildingId,
                    room_id = :roomId,
                    contagious_isolation = :contagiousIsolation,
                    contagious_hiv = :contagiousHiv,
                    contagious_tuberculosis = :contagiousTuberculosis,
                    contagious_hepatitis = :contagiousHepatitis,
                    contagious_syphilis = :contagiousSyphilis,
                    gynecology_menopause = :gynecologyMenopause,
                    last_menstrual_period = :lastMenstrualPeriod,
                    hpv_result = :hpvResult,
                    previous_cytology = :previousCytology,
                    previous_treatment = :previousTreatment,
                    additional_notes = :additionalNotes,
                    condition_abnormal_bleeding = :conditionAbnormalBleeding,
                    condition_birth_control = :conditionBirthControl,
                    condition_hormone_replacement = :conditionHormoneReplacement,
                    condition_hysterectomy = :conditionHysterectomy,
                    condition_iud = :conditionIud,
                    condition_lactation = :conditionLactation,
                    condition_pregnancy = :conditionPregnancy,
                    condition_radiotherapy = :conditionRadiotherapy,
                    other_special_condition = :otherSpecialCondition,
                    updated_at = :updatedAt
                where application_id = :applicationId
                """, parameters);
            return;
        }
        jdbcTemplate.update("""
            insert into application_registration_workbench (
                application_id,
                inpatient_no,
                bed_no,
                ward_name,
                phone,
                id_no,
                check_item,
                clinical_history,
                imaging_result,
                endoscopy_diagnosis,
                delivery_requirement,
                specimen_type,
                surgery_name,
                clinical_findings,
                fixative_type,
                fixation_person,
                fixation_time,
                building_id,
                room_id,
                contagious_isolation,
                contagious_hiv,
                contagious_tuberculosis,
                contagious_hepatitis,
                contagious_syphilis,
                gynecology_menopause,
                last_menstrual_period,
                hpv_result,
                previous_cytology,
                previous_treatment,
                additional_notes,
                condition_abnormal_bleeding,
                condition_birth_control,
                condition_hormone_replacement,
                condition_hysterectomy,
                condition_iud,
                condition_lactation,
                condition_pregnancy,
                condition_radiotherapy,
                other_special_condition,
                created_at,
                updated_at
            ) values (
                :applicationId,
                :inpatientNo,
                :bedNo,
                :wardName,
                :phone,
                :idNo,
                :checkItem,
                :clinicalHistory,
                :imagingResult,
                :endoscopyDiagnosis,
                :deliveryRequirement,
                :specimenType,
                :surgeryName,
                :clinicalFindings,
                :fixativeType,
                :fixationPerson,
                :fixationTime,
                :buildingId,
                :roomId,
                :contagiousIsolation,
                :contagiousHiv,
                :contagiousTuberculosis,
                :contagiousHepatitis,
                :contagiousSyphilis,
                :gynecologyMenopause,
                :lastMenstrualPeriod,
                :hpvResult,
                :previousCytology,
                :previousTreatment,
                :additionalNotes,
                :conditionAbnormalBleeding,
                :conditionBirthControl,
                :conditionHormoneReplacement,
                :conditionHysterectomy,
                :conditionIud,
                :conditionLactation,
                :conditionPregnancy,
                :conditionRadiotherapy,
                :otherSpecialCondition,
                :createdAt,
                :updatedAt
            )
            """, parameters);
    }

    void upsertTechnicalRegistrationDetailSectionOverrides(
        ApplicationRegistrationWorkbenchRepository.SaveTechnicalRegistrationDetailSectionOverridesCommand command) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from application_registration_workbench
            where application_id = :applicationId
            """, Map.of("applicationId", command.applicationId()), Long.class);
        MapSqlParameterSource parameters = technicalRegistrationOverrideParameters(command)
            .addValue("updatedAt", LocalDateTime.now())
            .addValue("createdAt", LocalDateTime.now());
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                update application_registration_workbench
                set technical_history_summary_override = :historySummaryOverride,
                    technical_clinical_exam_surgery_override = :clinicalExaminationAndSurgeryFindingsOverride,
                    technical_lab_imaging_override = :labAndImagingExaminationsOverride,
                    technical_submission_requirement_override = :clinicalSubmissionRequirementsOverride,
                    technical_infectious_past_history_override = :infectiousAndPastHistorySummaryOverride,
                    technical_external_pathology_diagnosis_override = :externalPathologyDiagnosisOverride,
                    updated_at = :updatedAt
                where application_id = :applicationId
                """, parameters);
            return;
        }
        jdbcTemplate.update("""
            insert into application_registration_workbench (
                application_id,
                technical_history_summary_override,
                technical_clinical_exam_surgery_override,
                technical_lab_imaging_override,
                technical_submission_requirement_override,
                technical_infectious_past_history_override,
                technical_external_pathology_diagnosis_override,
                created_at,
                updated_at
            ) values (
                :applicationId,
                :historySummaryOverride,
                :clinicalExaminationAndSurgeryFindingsOverride,
                :labAndImagingExaminationsOverride,
                :clinicalSubmissionRequirementsOverride,
                :infectiousAndPastHistorySummaryOverride,
                :externalPathologyDiagnosisOverride,
                :createdAt,
                :updatedAt
            )
            """, parameters);
    }

    void updateApplicationEditableFields(String applicationId, String clinicalDiagnosis, String remarks) {
        jdbcTemplate.update("""
            update applications
            set clinical_diagnosis = :clinicalDiagnosis,
                remarks = :remarks,
                updated_at = :updatedAt
            where id = :applicationId
            """, new MapSqlParameterSource()
            .addValue("applicationId", applicationId)
            .addValue("clinicalDiagnosis", clinicalDiagnosis)
            .addValue("remarks", remarks)
            .addValue("updatedAt", LocalDateTime.now()));
    }

    private MapSqlParameterSource extensionParameters(ApplicationRegistrationWorkbenchRepository.SaveWorkbenchExtensionCommand command) {
        return new MapSqlParameterSource()
            .addValue("applicationId", command.applicationId())
            .addValue("inpatientNo", command.inpatientNo())
            .addValue("bedNo", command.bedNo())
            .addValue("wardName", command.wardName())
            .addValue("phone", command.phone())
            .addValue("idNo", command.idNo())
            .addValue("checkItem", command.checkItem())
            .addValue("clinicalHistory", command.clinicalHistory())
            .addValue("imagingResult", command.imagingResult())
            .addValue("endoscopyDiagnosis", command.endoscopyDiagnosis())
            .addValue("deliveryRequirement", command.deliveryRequirement())
            .addValue("specimenType", command.specimenType())
            .addValue("surgeryName", command.surgeryName())
            .addValue("clinicalFindings", command.clinicalFindings())
            .addValue("fixativeType", command.fixativeType())
            .addValue("fixationPerson", command.fixationPerson())
            .addValue("fixationTime", command.fixationTime())
            .addValue("buildingId", command.buildingId())
            .addValue("roomId", command.roomId())
            .addValue("contagiousIsolation", booleanToInt(command.contagiousIsolation()))
            .addValue("contagiousHiv", booleanToInt(command.contagiousHiv()))
            .addValue("contagiousTuberculosis", booleanToInt(command.contagiousTuberculosis()))
            .addValue("contagiousHepatitis", booleanToInt(command.contagiousHepatitis()))
            .addValue("contagiousSyphilis", booleanToInt(command.contagiousSyphilis()))
            .addValue("gynecologyMenopause", booleanToInt(command.gynecologyMenopause()))
            .addValue("lastMenstrualPeriod", command.lastMenstrualPeriod())
            .addValue("hpvResult", command.hpvResult())
            .addValue("previousCytology", command.previousCytology())
            .addValue("previousTreatment", command.previousTreatment())
            .addValue("additionalNotes", command.additionalNotes())
            .addValue("conditionAbnormalBleeding", booleanToInt(command.conditionAbnormalBleeding()))
            .addValue("conditionBirthControl", booleanToInt(command.conditionBirthControl()))
            .addValue("conditionHormoneReplacement", booleanToInt(command.conditionHormoneReplacement()))
            .addValue("conditionHysterectomy", booleanToInt(command.conditionHysterectomy()))
            .addValue("conditionIud", booleanToInt(command.conditionIud()))
            .addValue("conditionLactation", booleanToInt(command.conditionLactation()))
            .addValue("conditionPregnancy", booleanToInt(command.conditionPregnancy()))
            .addValue("conditionRadiotherapy", booleanToInt(command.conditionRadiotherapy()))
            .addValue("otherSpecialCondition", command.otherSpecialCondition());
    }

    private int booleanToInt(boolean value) {
        return value ? 1 : 0;
    }

    private MapSqlParameterSource technicalRegistrationOverrideParameters(
        ApplicationRegistrationWorkbenchRepository.SaveTechnicalRegistrationDetailSectionOverridesCommand command) {
        return new MapSqlParameterSource()
            .addValue("applicationId", command.applicationId())
            .addValue("historySummaryOverride", command.historySummaryOverride())
            .addValue(
                "clinicalExaminationAndSurgeryFindingsOverride",
                command.clinicalExaminationAndSurgeryFindingsOverride())
            .addValue(
                "labAndImagingExaminationsOverride",
                command.labAndImagingExaminationsOverride())
            .addValue(
                "clinicalSubmissionRequirementsOverride",
                command.clinicalSubmissionRequirementsOverride())
            .addValue(
                "infectiousAndPastHistorySummaryOverride",
                command.infectiousAndPastHistorySummaryOverride())
            .addValue(
                "externalPathologyDiagnosisOverride",
                command.externalPathologyDiagnosisOverride());
    }

    private ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData mapWorkbenchExtensionData(ResultSet rs, int rowNum)
        throws SQLException {
        return new ApplicationRegistrationWorkbenchRepository.WorkbenchExtensionData(
            rs.getString("inpatient_no"),
            rs.getString("bed_no"),
            rs.getString("ward_name"),
            rs.getString("phone"),
            rs.getString("id_no"),
            rs.getString("check_item"),
            rs.getString("clinical_history"),
            rs.getString("imaging_result"),
            rs.getString("endoscopy_diagnosis"),
            rs.getString("delivery_requirement"),
            rs.getString("specimen_type"),
            rs.getString("surgery_name"),
            rs.getString("clinical_findings"),
            rs.getString("fixative_type"),
            rs.getString("fixation_person"),
            rs.getTimestamp("fixation_time") == null ? null : rs.getTimestamp("fixation_time").toLocalDateTime(),
            rs.getString("building_id"),
            rs.getString("room_id"),
            rs.getInt("contagious_isolation") == 1,
            rs.getInt("contagious_hiv") == 1,
            rs.getInt("contagious_tuberculosis") == 1,
            rs.getInt("contagious_hepatitis") == 1,
            rs.getInt("contagious_syphilis") == 1,
            rs.getInt("gynecology_menopause") == 1,
            rs.getString("last_menstrual_period"),
            rs.getString("hpv_result"),
            rs.getString("previous_cytology"),
            rs.getString("previous_treatment"),
            rs.getString("additional_notes"),
            rs.getInt("condition_abnormal_bleeding") == 1,
            rs.getInt("condition_birth_control") == 1,
            rs.getInt("condition_hormone_replacement") == 1,
            rs.getInt("condition_hysterectomy") == 1,
            rs.getInt("condition_iud") == 1,
            rs.getInt("condition_lactation") == 1,
            rs.getInt("condition_pregnancy") == 1,
            rs.getInt("condition_radiotherapy") == 1,
            rs.getString("other_special_condition"));
    }

    private ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides
    mapTechnicalRegistrationDetailSectionOverrides(ResultSet rs, int rowNum) throws SQLException {
        Set<String> availableColumns = resolveAvailableColumns(rs);
        return new ApplicationRegistrationWorkbenchRepository.TechnicalRegistrationDetailSectionOverrides(
            getNullableString(rs, availableColumns, "technical_history_summary_override"),
            getNullableString(rs, availableColumns, "technical_clinical_exam_surgery_override"),
            getNullableString(rs, availableColumns, "technical_lab_imaging_override"),
            getNullableString(rs, availableColumns, "technical_submission_requirement_override"),
            getNullableString(rs, availableColumns, "technical_infectious_past_history_override"),
            getNullableString(rs, availableColumns, "technical_external_pathology_diagnosis_override"));
    }

    private Set<String> resolveAvailableColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData metadata = rs.getMetaData();
        int columnCount = metadata.getColumnCount();
        Set<String> availableColumns = new LinkedHashSet<>(columnCount);
        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            String columnLabel = metadata.getColumnLabel(columnIndex);
            if (columnLabel == null || columnLabel.isBlank()) {
                columnLabel = metadata.getColumnName(columnIndex);
            }
            if (columnLabel == null || columnLabel.isBlank()) {
                continue;
            }
            availableColumns.add(normalizeColumnName(columnLabel));
        }
        return availableColumns;
    }

    private String getNullableString(ResultSet rs, Set<String> availableColumns, String columnName) throws SQLException {
        if (!availableColumns.contains(normalizeColumnName(columnName))) {
            return null;
        }
        return rs.getString(columnName);
    }

    private String normalizeColumnName(String columnName) {
        return columnName.toLowerCase(Locale.ROOT);
    }
}
