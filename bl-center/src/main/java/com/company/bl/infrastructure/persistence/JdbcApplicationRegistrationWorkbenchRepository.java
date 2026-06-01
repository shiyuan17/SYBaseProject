package com.company.bl.infrastructure.persistence;

import com.company.bl.domain.repository.ApplicationRegistrationWorkbenchRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
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
    private final JdbcApplicationRegistrationWorkbenchLookupSupport lookupSupport;
    private final JdbcApplicationRegistrationWorkbenchExtensionSupport extensionSupport;
    private volatile boolean workbenchTableReady;

    public JdbcApplicationRegistrationWorkbenchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.lookupSupport = new JdbcApplicationRegistrationWorkbenchLookupSupport(jdbcTemplate);
        this.extensionSupport = new JdbcApplicationRegistrationWorkbenchExtensionSupport(jdbcTemplate);
    }

    @Override
    public Optional<WorkbenchApplicationRow> findApplicationByKeyword(String keyword) {
        return findApplicationByKeyword(keyword, "AUTO");
    }

    @Override
    public Optional<WorkbenchApplicationRow> findApplicationByKeyword(String keyword, String queryType) {
        return withWorkbenchTable(() -> lookupSupport.findApplicationByKeyword(keyword, queryType));
    }

    @Override
    public Optional<WorkbenchExtensionData> findExtensionByApplicationId(String applicationId) {
        return withWorkbenchTable(() -> extensionSupport.findExtensionByApplicationId(applicationId));
    }

    @Override
    public void upsertExtension(SaveWorkbenchExtensionCommand command) {
        withWorkbenchTable(() -> {
            extensionSupport.upsertExtension(command);
            return null;
        });
    }

    @Override
    public void updateApplicationEditableFields(String applicationId, String clinicalDiagnosis, String remarks) {
        extensionSupport.updateApplicationEditableFields(applicationId, clinicalDiagnosis, remarks);
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
    public List<OperatingBuildingOption> listOperatingBuildingOptions() {
        return withWorkbenchTable(() -> jdbcTemplate.query("""
            select distinct building_id
            from application_registration_workbench
            where building_id is not null
              and trim(building_id) <> ''
            order by building_id asc
            """, (rs, rowNum) -> mapOperatingBuildingOption(rs.getString("building_id"))));
    }

    @Override
    public List<OperatingRoomOption> listOperatingRoomOptions(String buildingId) {
        return withWorkbenchTable(() -> listOperatingRoomOptionsInternal(buildingId));
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

    private OperatingBuildingOption mapOperatingBuildingOption(String buildingId) {
        return new OperatingBuildingOption(
            buildingId,
            buildingId,
            0,
            null,
            listOperatingRoomOptionsInternal(buildingId));
    }

    private List<OperatingRoomOption> listOperatingRoomOptionsInternal(String buildingId) {
        if (buildingId == null || buildingId.isBlank()) {
            return jdbcTemplate.query("""
                select distinct building_id, room_id
                from application_registration_workbench
                where room_id is not null
                  and trim(room_id) <> ''
                order by building_id asc, room_id asc
                """, this::mapOperatingRoomOption);
        }

        return jdbcTemplate.query("""
            select distinct building_id, room_id
            from application_registration_workbench
            where room_id is not null
              and trim(room_id) <> ''
              and building_id = :buildingId
            order by building_id asc, room_id asc
            """, new MapSqlParameterSource()
            .addValue("buildingId", buildingId), this::mapOperatingRoomOption);
    }

    private OperatingRoomOption mapOperatingRoomOption(ResultSet rs, int rowNum) throws SQLException {
        return new OperatingRoomOption(
            rs.getString("building_id"),
            null,
            0,
            rs.getString("room_id"),
            rs.getString("room_id"),
            null);
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

    private boolean isMissingWorkbenchTable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                && message.contains(WORKBENCH_TABLE_NAME)
                && message.contains("鏃犳晥鐨勮〃鎴栬鍥惧悕")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
