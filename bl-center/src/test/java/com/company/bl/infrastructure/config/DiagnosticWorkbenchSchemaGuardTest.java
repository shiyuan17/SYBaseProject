package com.company.bl.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagnosticWorkbenchSchemaGuardTest {

    @Test
    void acceptsCompleteV122Schema() {
        String url = "jdbc:h2:mem:diagnostic_schema_guard_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        Flyway.configure()
            .dataSource(url, "sa", "")
            .locations("classpath:db/migration")
            .target("122")
            .load()
            .migrate();

        DiagnosticWorkbenchSchemaGuard guard = new DiagnosticWorkbenchSchemaGuard(
            new DriverManagerDataSource(url, "sa", ""));

        assertDoesNotThrow(() -> guard.run(new DefaultApplicationArguments()));
    }

    @Test
    void rejectsSchemaWithoutV122Objects() throws Exception {
        String url = "jdbc:h2:mem:diagnostic_schema_guard_missing_" + System.nanoTime()
            + ";MODE=LEGACY;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        try (Connection connection = java.sql.DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("create table pathology_reports (id varchar(64))");
            statement.execute("create table report_versions (id varchar(64))");
        }

        DiagnosticWorkbenchSchemaGuard guard = new DiagnosticWorkbenchSchemaGuard(
            new DriverManagerDataSource(url, "sa", ""));

        assertThrows(IllegalStateException.class,
            () -> guard.run(new DefaultApplicationArguments()));
    }
}
