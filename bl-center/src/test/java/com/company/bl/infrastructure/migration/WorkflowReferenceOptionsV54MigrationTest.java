package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowReferenceOptionsV54MigrationTest {

    @Test
    void shouldReconcileWorkflowReferenceTreeAndPermissionsIdempotently() throws Exception {
        String url = "jdbc:h2:mem:workflow_reference_v54_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("53"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                update system_config_categories
                set parent_id = null, enabled = 0
                where category_code = 'CUT_SURFACE_FEATURE'
                """);
            statement.execute("""
                update system_config_items
                set category_id = 'SCC_GENERAL',
                    config_value = 'WRONG',
                    sort_order = 999,
                    enabled = 0,
                    remarks = 'stale'
                where config_key = 'WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.GRAY_WHITE'
                """);
            statement.execute("""
                delete from role_permissions
                where role_id = 'ROLE_M3_GROSSING'
                  and permission_id = 'PERM_WORKFLOW_REFERENCE_QUERY'
                """);
        }

        rerunFromBaseline53(url);
        assertReconciled(url);

        rerunFromBaseline53(url);
        assertReconciled(url);
    }

    private void rerunFromBaseline53(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("53"))
            .baselineDescription("workflow-reference-v54")
            .load()
            .migrate();
    }

    private void assertReconciled(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                select count(*)
                from system_config_categories
                where category_code = 'WORKFLOW_REFERENCE'
                  and parent_id = 'SCC_ROOT'
                  and enabled = 1
                """));
            assertEquals(9, queryInt(statement, """
                select count(*)
                from system_config_categories
                where parent_id = 'SCC_WORKFLOW_REFERENCE'
                  and category_code in (
                    'SPECIMEN_TYPE',
                    'COLLECTION_MODE',
                    'CLINICAL_SYMPTOM',
                    'FIXATION_LIQUID_TYPE',
                    'CONTAINER_NAME',
                    'SPECIMEN_IMAGE_SIZE',
                    'CUT_SURFACE_FEATURE',
                    'MARGIN_MARKING',
                    'OPERATING_ROOM'
                  )
                  and enabled = 1
                """));
            assertEquals(34, queryInt(statement, """
                select count(*)
                from system_config_items
                where config_key like 'WORKFLOW_REFERENCE.%'
                  and enabled = 1
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from system_config_items
                where config_key = 'WORKFLOW_REFERENCE.CUT_SURFACE_FEATURE.GRAY_WHITE'
                  and category_id = 'SCC_WORKFLOW_REFERENCE_CUT_SURFACE_FEATURE'
                  and config_value = '灰白'
                  and sort_order = 10
                  and remarks = '取材切面特征预置项'
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from permissions
                where id = 'PERM_WORKFLOW_REFERENCE_QUERY'
                  and permission_code = 'PERM_WORKFLOW_REFERENCE_QUERY'
                  and resource_path = '/api/v1/workflow-reference-options'
                  and enabled = 1
                """));
            assertEquals(4, queryInt(statement, """
                select count(*)
                from role_permissions
                where permission_id = 'PERM_WORKFLOW_REFERENCE_QUERY'
                  and role_id in (
                    'ROLE_PATHOLOGY_ADMIN',
                    'ROLE_M2_CLINICAL_REGISTER',
                    'ROLE_M2_FIXATION_VERIFY',
                    'ROLE_M3_GROSSING'
                  )
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from flyway_schema_history
                where version = '54' and success = 1
                """));
        }
    }

    private int queryInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
