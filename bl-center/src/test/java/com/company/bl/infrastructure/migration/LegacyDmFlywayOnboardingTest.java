package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDmFlywayOnboardingTest {

    @Test
    void shouldBaselineLegacySchemaAtV10AndApplyLaterSecurityMigrations() throws Exception {
        String url = "jdbc:h2:mem:legacy_dm_" + System.nanoTime() + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("3"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history");
            statement.execute("DROP TABLE technical_pending_tasks");
            statement.execute("DELETE FROM users");
            statement.execute("DELETE FROM user_roles");
            statement.execute("DELETE FROM role_menus");
            statement.execute("""
                INSERT INTO roles
                    (id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at)
                VALUES
                    ('ROLE_SUPER_ADMIN', 'SUPER_ADMIN', 'Super Admin', 'SYSTEM', 'ALL', 'legacy manual role', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
            statement.execute("""
                INSERT INTO role_menus (id, role_id, menu_id, assigned_at)
                VALUES ('RM_SUPER_ADMIN_SYSTEM', 'ROLE_SUPER_ADMIN', 'MENU_SYSTEM', CURRENT_TIMESTAMP)
                """);
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion(MigrationVersion.fromVersion("10"))
            .baselineDescription("legacy-dm-onboard")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertTrue(queryInt(statement, "SELECT COUNT(*) FROM flyway_schema_history") >= 2);
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '11'
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE LOWER(TABLE_NAME) = 'technical_pending_tasks'
                """));
            assertEquals(13, queryInt(statement, "SELECT COUNT(*) FROM roles WHERE role_code LIKE 'M2_%' OR role_code LIKE 'M3_%'"));
            assertEquals(15, queryInt(statement, "SELECT COUNT(*) FROM menus WHERE menu_code LIKE 'M2_%' OR menu_code LIKE 'M3_%'"));
            assertEquals(16, queryInt(statement, "SELECT COUNT(*) FROM permissions WHERE permission_group IN ('M2', 'M3')"));
            assertTrue(queryInt(statement, "SELECT COUNT(*) FROM users WHERE id LIKE 'USER_M%'") >= 28);
            assertTrue(queryInt(statement, "SELECT COUNT(*) FROM user_roles WHERE id LIKE 'UR_M%'") >= 25);
            assertEquals(1, queryInt(statement, "SELECT COUNT(*) FROM users WHERE id = 'USER_M1_ADMIN' AND password_algo = 'SM3'"));
            assertEquals(1, queryInt(statement, "SELECT COUNT(*) FROM users WHERE id = 'USER_M4_ORDER_EXECUTE'"));
            assertEquals(3, queryInt(statement, """
                SELECT COUNT(*)
                FROM system_config_items
                WHERE config_key IN (
                    'technical.timeout.grossingMinutes',
                    'technical.timeout.dehydrationMinutes',
                    'technical.timeout.stainingMinutes'
                )
                """));
            assertTrue(queryInt(statement, "SELECT COUNT(*) FROM role_menus WHERE role_id = 'ROLE_PATHOLOGY_ADMIN' AND menu_id = 'MENU_M3_TASKS'") > 0);
            assertEquals(1, queryInt(statement, "SELECT COUNT(*) FROM roles WHERE role_code = 'SUPER_ADMIN'"));
        }
    }

    @Test
    void shouldReconcileLegacyM1PermissionCodesToCanonicalCodes() throws Exception {
        String url = "jdbc:h2:mem:legacy_m1_permissions_" + System.nanoTime() + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("16"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                UPDATE permissions
                SET permission_code = 'sys:medical-order-dict:query'
                WHERE id = 'PERM_SYS_ORDER_DICT_QUERY'
                """);
            statement.execute("""
                UPDATE permissions
                SET permission_code = 'sys:medical-order-charge:query'
                WHERE id = 'PERM_SYS_ORDER_CHARGE_QUERY'
                """);
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM permissions
                WHERE permission_code = 'PERM_SYS_ORDER_DICT_QUERY'
                  AND enabled = 1
                """));
            assertEquals(1, queryInt(statement, """
                SELECT COUNT(*)
                FROM permissions
                WHERE permission_code = 'PERM_SYS_ORDER_CHARGE_QUERY'
                  AND enabled = 1
                """));
            assertEquals(0, queryInt(statement, """
                SELECT COUNT(*)
                FROM permissions
                WHERE permission_code IN ('sys:medical-order-dict:query', 'sys:medical-order-charge:query')
                  AND enabled = 1
                """));
            assertEquals(2, queryInt(statement, """
                SELECT COUNT(*)
                FROM role_permissions
                JOIN permissions ON permissions.id = role_permissions.permission_id
                WHERE role_permissions.role_id = 'ROLE_PATHOLOGY_ADMIN'
                  AND permissions.permission_code IN ('PERM_SYS_ORDER_DICT_QUERY', 'PERM_SYS_ORDER_CHARGE_QUERY')
                  AND permissions.enabled = 1
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
