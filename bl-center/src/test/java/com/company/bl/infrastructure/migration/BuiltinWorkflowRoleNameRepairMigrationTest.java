package com.company.bl.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltinWorkflowRoleNameRepairMigrationTest {

    @Test
    void shouldRepairBuiltinWorkflowRoleNamesWithoutTouchingCustomRoles() throws Exception {
        String url = "jdbc:h2:mem:workflow_role_name_repair_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("99"))
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                update roles
                set role_name = 'M2 Clinical Register'
                where id = 'ROLE_M2_CLINICAL_REGISTER'
                """);
            statement.execute("""
                update roles
                set role_name = 'M3 Grossing'
                where id = 'ROLE_M3_GROSSING'
                """);
            statement.execute("""
                update roles
                set role_name = 'M4 Diagnosis'
                where id = 'ROLE_M4_DIAGNOSIS'
                """);
            statement.execute("""
                update roles
                set role_name = 'M4 Medical Order Execute'
                where id = 'ROLE_M4_MEDICAL_ORDER_EXECUTE'
                """);
            statement.execute("""
                insert into roles
                    (id, role_code, role_name, role_type, data_scope, remarks, enabled, created_at, updated_at)
                values
                    ('ROLE_CUSTOM_KEEP', 'CUSTOM_KEEP', 'M3 自定义角色', 'BUSINESS', 'DEPARTMENT', 'custom keep', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals("标本登记员", queryString(statement, "select role_name from roles where id = 'ROLE_M2_CLINICAL_REGISTER'"));
            assertEquals("取材员", queryString(statement, "select role_name from roles where id = 'ROLE_M3_GROSSING'"));
            assertEquals("诊断医生", queryString(statement, "select role_name from roles where id = 'ROLE_M4_DIAGNOSIS'"));
            assertEquals("医嘱执行员", queryString(statement, "select role_name from roles where id = 'ROLE_M4_MEDICAL_ORDER_EXECUTE'"));
            assertEquals("M3 自定义角色", queryString(statement, "select role_name from roles where id = 'ROLE_CUSTOM_KEEP'"));
        }

        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .load()
            .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertEquals("标本登记员", queryString(statement, "select role_name from roles where id = 'ROLE_M2_CLINICAL_REGISTER'"));
            assertEquals("M3 自定义角色", queryString(statement, "select role_name from roles where id = 'ROLE_CUSTOM_KEEP'"));
        }
    }

    private String queryString(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
