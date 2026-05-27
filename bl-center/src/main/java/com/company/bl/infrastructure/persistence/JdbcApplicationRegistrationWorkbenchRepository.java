package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Repository
public class JdbcApplicationRegistrationWorkbenchRepository implements ApplicationRegistrationWorkbenchRepository {

    private static final String WORKBENCH_TABLE_NAME = "APPLICATION_REGISTRATION_WORKBENCH";
    private static final String CREATE_WORKBENCH_TABLE_SQL = """
        CREATE TABLE application_registration_workbench (
            application_id VARCHAR(64) NOT NULL,
            inpatient_no VARCHAR(64),
            bed_no VARCHAR(64),
            ward_name VARCHAR(100),
            phone VARCHAR(32),
            id_no VARCHAR(64),
            check_item VARCHAR(200),
            clinical_history VARCHAR(1000),
            imaging_result VARCHAR(1000),
            endoscopy_diagnosis VARCHAR(1000),
            delivery_requirement VARCHAR(200),
            specimen_type VARCHAR(100),
            surgery_name VARCHAR(200),
            clinical_findings VARCHAR(1000),
            fixative_type VARCHAR(100),
            fixation_person VARCHAR(100),
            fixation_time TIMESTAMP,
            building_id VARCHAR(64),
            room_id VARCHAR(64),
            contagious_isolation INTEGER DEFAULT 0,
            contagious_hiv INTEGER DEFAULT 0,
            contagious_tuberculosis INTEGER DEFAULT 0,
            contagious_hepatitis INTEGER DEFAULT 0,
            contagious_syphilis INTEGER DEFAULT 0,
            gynecology_menopause INTEGER DEFAULT 0,
            last_menstrual_period VARCHAR(64),
            hpv_result VARCHAR(100),
            previous_cytology VARCHAR(1000),
            previous_treatment VARCHAR(1000),
            additional_notes VARCHAR(1000),
            condition_abnormal_bleeding INTEGER DEFAULT 0,
            condition_birth_control INTEGER DEFAULT 0,
            condition_hormone_replacement INTEGER DEFAULT 0,
            condition_hysterectomy INTEGER DEFAULT 0,
            condition_iud INTEGER DEFAULT 0,
            condition_lactation INTEGER DEFAULT 0,
            condition_pregnancy INTEGER DEFAULT 0,
            condition_radiotherapy INTEGER DEFAULT 0,
            other_special_condition VARCHAR(500),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT pk_application_registration_workbench PRIMARY KEY (application_id),
            CONSTRAINT fk_application_registration_workbench_application FOREIGN KEY (application_id) REFERENCES applications (id)
        )
        """;
    private static final String CREATE_WORKBENCH_INPATIENT_INDEX_SQL = """
        CREATE INDEX idx_app_reg_workbench_inpatient_no
            ON application_registration_workbench (inpatient_no)
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private volatile boolean workbenchTableReady;
    private volatile Boolean patientsTableAvailable;

    public JdbcApplicationRegistrationWorkbenchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<WorkbenchApplicationRow> findApplicationByKeyword(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return Optional.empty();
        }
        String keywordLike = "%" + normalizedKeyword.toUpperCase() + "%";
        return withWorkbenchTable(() -> jdbcTemplate.query(buildLookupSql(), new MapSqlParameterSource()
                .addValue("keyword", normalizedKeyword)
                .addValue("keywordLike", keywordLike), this::mapWorkbenchApplicationRow)
                .stream()
                .findFirst());
    }

    private String buildLookupSql() {
        if (hasPatientsTable()) {
            return """
                select
                    a.id as application_id,
                    a.application_no,
                    a.patient_id,
                    a.patient_name,
                    a.patient_gender,
                    a.patient_age,
                    a.submitting_department_name,
                    a.submitting_doctor_name,
                    a.clinical_diagnosis,
                    a.remarks,
                    a.status,
                    a.application_date,
                    a.submission_date
                from applications a
                left join application_registration_workbench w on w.application_id = a.id
                left join patients p
                    on p.id = a.patient_id
                    or p.patient_no = a.patient_id
                where a.id = :keyword
                   or upper(a.application_no) = upper(:keyword)
                   or upper(coalesce(a.external_order_no, '')) = upper(:keyword)
                   or upper(coalesce(a.patient_id, '')) = upper(:keyword)
                   or upper(coalesce(w.inpatient_no, '')) = upper(:keyword)
                   or upper(coalesce(p.patient_no, '')) = upper(:keyword)
                   or upper(coalesce(p.inpatient_no, '')) = upper(:keyword)
                   or upper(coalesce(p.outpatient_no, '')) = upper(:keyword)
                   or upper(coalesce(a.application_no, '')) like :keywordLike
                   or upper(coalesce(a.external_order_no, '')) like :keywordLike
                   or upper(coalesce(a.patient_id, '')) like :keywordLike
                   or upper(coalesce(a.patient_name, '')) like :keywordLike
                   or upper(coalesce(w.inpatient_no, '')) like :keywordLike
                   or upper(coalesce(p.patient_no, '')) like :keywordLike
                   or upper(coalesce(p.inpatient_no, '')) like :keywordLike
                   or upper(coalesce(p.outpatient_no, '')) like :keywordLike
                order by case
                    when a.id = :keyword then 0
                    when upper(a.application_no) = upper(:keyword) then 0
                    when upper(coalesce(a.external_order_no, '')) = upper(:keyword) then 0
                    when upper(coalesce(a.patient_id, '')) = upper(:keyword) then 0
                    when upper(coalesce(w.inpatient_no, '')) = upper(:keyword) then 0
                    when upper(coalesce(p.patient_no, '')) = upper(:keyword) then 0
                    when upper(coalesce(p.inpatient_no, '')) = upper(:keyword) then 0
                    when upper(coalesce(p.outpatient_no, '')) = upper(:keyword) then 0
                    else 1
                end,
                a.updated_at desc
                fetch next 1 rows only
                """;
        }
        return """
            select
                a.id as application_id,
                a.application_no,
                a.patient_id,
                a.patient_name,
                a.patient_gender,
                a.patient_age,
                a.submitting_department_name,
                a.submitting_doctor_name,
                a.clinical_diagnosis,
                a.remarks,
                a.status,
                a.application_date,
                a.submission_date
            from applications a
            left join application_registration_workbench w on w.application_id = a.id
            where a.id = :keyword
               or upper(a.application_no) = upper(:keyword)
               or upper(coalesce(a.external_order_no, '')) = upper(:keyword)
               or upper(coalesce(a.patient_id, '')) = upper(:keyword)
               or upper(coalesce(w.inpatient_no, '')) = upper(:keyword)
               or upper(coalesce(a.application_no, '')) like :keywordLike
               or upper(coalesce(a.external_order_no, '')) like :keywordLike
               or upper(coalesce(a.patient_id, '')) like :keywordLike
               or upper(coalesce(a.patient_name, '')) like :keywordLike
               or upper(coalesce(w.inpatient_no, '')) like :keywordLike
            order by case
                when a.id = :keyword then 0
                when upper(a.application_no) = upper(:keyword) then 0
                when upper(coalesce(a.external_order_no, '')) = upper(:keyword) then 0
                when upper(coalesce(a.patient_id, '')) = upper(:keyword) then 0
                when upper(coalesce(w.inpatient_no, '')) = upper(:keyword) then 0
                else 1
            end,
            a.updated_at desc
            fetch next 1 rows only
            """;
    }

    @Override
    public Optional<WorkbenchExtensionData> findExtensionByApplicationId(String applicationId) {
        return withWorkbenchTable(() -> jdbcTemplate.query("""
                select *
                from application_registration_workbench
                where application_id = :applicationId
                """, Map.of("applicationId", applicationId), this::mapWorkbenchExtensionData)
                .stream()
                .findFirst());
    }

    @Override
    public void upsertExtension(SaveWorkbenchExtensionCommand command) {
        withWorkbenchTable(() -> {
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
                return null;
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
            return null;
        });
    }

    @Override
    public void updateApplicationEditableFields(String applicationId, String clinicalDiagnosis, String remarks) {
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

    @Override
    public boolean hasStartedDownstreamWorkflow(String applicationId) {
        Long count = jdbcTemplate.queryForObject("""
            select count(1)
            from specimens s
            where s.application_id = :applicationId
              and (
                  s.fixation_status <> 'PENDING'
                  or s.specimen_status <> 'REGISTERED'
              )
            """, Map.of("applicationId", applicationId), Long.class);
        if (count != null && count > 0) {
            return true;
        }
        Long caseCount = jdbcTemplate.queryForObject("""
            select count(1)
            from pathology_cases
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId), Long.class);
        return caseCount != null && caseCount > 0;
    }

    @Override
    public void clearPreDownstreamRegistrationData(String applicationId) {
        jdbcTemplate.update("""
            delete from workflow_events
            where application_id = :applicationId
              and specimen_id is not null
            """, Map.of("applicationId", applicationId));
        jdbcTemplate.update("""
            delete from specimen_collection_records
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId));
        jdbcTemplate.update("""
            delete from specimen_fixation_records
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId));
        jdbcTemplate.update("""
            delete from specimen_receipts
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId));
        jdbcTemplate.update("""
            delete from transport_order_items
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId));
        jdbcTemplate.update("""
            delete from transport_orders
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId));
        jdbcTemplate.update("""
            delete from specimens
            where application_id = :applicationId
            """, Map.of("applicationId", applicationId));
    }

    private MapSqlParameterSource extensionParameters(SaveWorkbenchExtensionCommand command) {
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

    private <T> T withWorkbenchTable(Supplier<T> action) {
        try {
            T result = action.get();
            workbenchTableReady = true;
            return result;
        } catch (DataAccessException exception) {
            if (!isMissingWorkbenchTable(exception)) {
                throw exception;
            }
            ensureWorkbenchTableExists();
            T result = action.get();
            workbenchTableReady = true;
            return result;
        }
    }

    private synchronized void ensureWorkbenchTableExists() {
        if (workbenchTableReady || canAccessWorkbenchTable()) {
            workbenchTableReady = true;
            return;
        }

        DataAccessException createTableException = null;
        try {
            jdbcTemplate.getJdbcOperations().execute(CREATE_WORKBENCH_TABLE_SQL);
        } catch (DataAccessException exception) {
            createTableException = exception;
        }

        if (!canAccessWorkbenchTable()) {
            if (createTableException != null) {
                throw createTableException;
            }
            throw new IllegalStateException("Failed to initialize application registration workbench table");
        }

        try {
            jdbcTemplate.getJdbcOperations().execute(CREATE_WORKBENCH_INPATIENT_INDEX_SQL);
        } catch (DataAccessException ignored) {
            // Ignore duplicate-index style errors after the table is already accessible.
        }
        workbenchTableReady = true;
    }

    private boolean canAccessWorkbenchTable() {
        try {
            jdbcTemplate.getJdbcOperations().queryForObject(
                "select count(1) from application_registration_workbench where 1 = 0",
                Integer.class);
            return true;
        } catch (DataAccessException exception) {
            return false;
        }
    }

    private boolean hasPatientsTable() {
        Boolean cached = patientsTableAvailable;
        if (cached != null) {
            return cached;
        }
        Boolean resolved = jdbcTemplate.getJdbcOperations().execute((ConnectionCallback<Boolean>) connection ->
            tableExists(connection.getMetaData(), "PATIENTS"));
        patientsTableAvailable = Boolean.TRUE.equals(resolved);
        return patientsTableAvailable;
    }

    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, tableName, null)) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet tables = metadata.getTables(null, null, tableName.toLowerCase(), null)) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMissingWorkbenchTable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                && message.contains(WORKBENCH_TABLE_NAME)
                && message.contains("无效的表或视图名")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private WorkbenchApplicationRow mapWorkbenchApplicationRow(ResultSet rs, int rowNum) throws SQLException {
        return new WorkbenchApplicationRow(
            rs.getString("application_id"),
            rs.getString("application_no"),
            rs.getString("patient_id"),
            rs.getString("patient_name"),
            rs.getString("patient_gender"),
            rs.getString("patient_age"),
            rs.getString("submitting_department_name"),
            rs.getString("submitting_doctor_name"),
            rs.getString("clinical_diagnosis"),
            rs.getString("remarks"),
            rs.getString("status"),
            rs.getDate("application_date") == null ? null : rs.getDate("application_date").toLocalDate(),
            rs.getDate("submission_date") == null ? null : rs.getDate("submission_date").toLocalDate());
    }

    private WorkbenchExtensionData mapWorkbenchExtensionData(ResultSet rs, int rowNum) throws SQLException {
        return new WorkbenchExtensionData(
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
}
