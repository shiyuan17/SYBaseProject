package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TechnicalTaskActionPermissionsV114MigrationTest {

    @Test
    void shouldSeedTechnicalTaskActionPermissionsAndRoleGrantsIdempotently() throws Exception {
        String url = "jdbc:h2:mem:tech_task_perm_v114_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("114"))
            .load()
            .migrate();

        assertSeeded(url);

        rerunFromBaseline113(url);
        assertSeeded(url);
    }

    private void rerunFromBaseline113(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("113"))
            .baselineDescription("tech-task-permission-v114")
            .load()
            .migrate();
    }

    private void assertSeeded(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                select count(*)
                from permissions
                where id = 'PERM_M3_TECH_TASK_ASSIGN'
                  and permission_code = 'PERM_M3_TECH_TASK_ASSIGN'
                  and permission_name = '分派技术任务'
                  and menu_id = 'MENU_M3_TASKS'
                  and action_key = 'ASSIGN'
                  and http_method = 'POST'
                  and resource_path = '/api/v1/technical-tasks/{id}/assign'
                  and permission_group = 'M3'
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from permissions
                where id = 'PERM_M3_TECH_TASK_CLAIM'
                  and permission_name = '领取技术任务'
                  and action_key = 'CLAIM'
                  and http_method = 'POST'
                  and resource_path = '/api/v1/technical-tasks/{id}/claim'
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from permissions
                where id = 'PERM_M3_TECH_TASK_RELEASE'
                  and permission_name = '释放技术任务'
                  and action_key = 'RELEASE'
                  and http_method = 'POST'
                  and resource_path = '/api/v1/technical-tasks/{id}/release'
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from permissions
                where id = 'PERM_M3_TECH_TASK_PRIORITY'
                  and permission_name = '调整任务优先级'
                  and action_key = 'PRIORITY'
                  and http_method = 'POST'
                  and resource_path = '/api/v1/technical-tasks/{id}/priority'
                """));
            assertEquals(1, queryInt(statement, """
                select count(*)
                from permissions
                where id = 'PERM_M3_TECH_TASK_REMARKS'
                  and permission_name = '编辑任务备注'
                  and action_key = 'REMARKS'
                  and http_method = 'PATCH'
                  and resource_path = '/api/v1/technical-tasks/{id}/remarks'
                """));
            assertEquals(5, queryInt(statement, """
                select count(*)
                from role_permissions
                where role_id = 'ROLE_PATHOLOGY_ADMIN'
                  and permission_id in (
                    'PERM_M3_TECH_TASK_ASSIGN',
                    'PERM_M3_TECH_TASK_CLAIM',
                    'PERM_M3_TECH_TASK_RELEASE',
                    'PERM_M3_TECH_TASK_PRIORITY',
                    'PERM_M3_TECH_TASK_REMARKS'
                  )
                """));
            assertEquals(6, queryInt(statement, """
                select count(*)
                from role_permissions
                where role_id in (
                    'ROLE_M3_GROSSING',
                    'ROLE_M3_DEHYDRATION',
                    'ROLE_M3_EMBEDDING',
                    'ROLE_M3_SLICING',
                    'ROLE_M3_STAINING',
                    'ROLE_M3_REWORK'
                  )
                  and permission_id = 'PERM_M3_TECH_TASK_CLAIM'
                """));
            assertEquals(6, queryInt(statement, """
                select count(*)
                from role_permissions
                where role_id in (
                    'ROLE_M3_GROSSING',
                    'ROLE_M3_DEHYDRATION',
                    'ROLE_M3_EMBEDDING',
                    'ROLE_M3_SLICING',
                    'ROLE_M3_STAINING',
                    'ROLE_M3_REWORK'
                  )
                  and permission_id = 'PERM_M3_TECH_TASK_RELEASE'
                """));
            assertEquals(6, queryInt(statement, """
                select count(*)
                from role_permissions
                where role_id in (
                    'ROLE_M3_GROSSING',
                    'ROLE_M3_DEHYDRATION',
                    'ROLE_M3_EMBEDDING',
                    'ROLE_M3_SLICING',
                    'ROLE_M3_STAINING',
                    'ROLE_M3_REWORK'
                  )
                  and permission_id = 'PERM_M3_TECH_TASK_REMARKS'
                """));
            assertEquals(0, queryInt(statement, """
                select count(*)
                from role_permissions
                where role_id = 'ROLE_M3_TRACKING'
                  and permission_id in (
                    'PERM_M3_TECH_TASK_ASSIGN',
                    'PERM_M3_TECH_TASK_CLAIM',
                    'PERM_M3_TECH_TASK_RELEASE',
                    'PERM_M3_TECH_TASK_PRIORITY',
                    'PERM_M3_TECH_TASK_REMARKS'
                  )
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
