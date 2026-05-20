package com.company.bl.tools;

import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public final class BlCenterFlywayCli {

    private BlCenterFlywayCli() {
    }

    public static void main(String[] args) throws Exception {
        String driver = env("BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME", "dm.jdbc.driver.DmDriver");
        String url = env("BL_CENTER_DATASOURCE_URL", "jdbc:dm://127.0.0.1:5236");
        String username = env("BL_CENTER_DATASOURCE_USERNAME", "SYSDBA");
        String password = env("BL_CENTER_DATASOURCE_PASSWORD", "Dm.2027.Pwd.");
        boolean baselineOnMigrate = Boolean.parseBoolean(env("SPRING_FLYWAY_BASELINE_ON_MIGRATE", "false"));
        String baselineVersion = env("SPRING_FLYWAY_BASELINE_VERSION", "1");
        String baselineDescription = env("SPRING_FLYWAY_BASELINE_DESCRIPTION", "<< Flyway Baseline >>");

        Class.forName(driver);
        reconcileKnownPartialV12(url, username, password);

        Flyway flyway = Flyway.configure()
            .dataSource(url, username, password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(baselineOnMigrate)
            .baselineVersion(baselineVersion)
            .baselineDescription(baselineDescription)
            .load();

        System.out.println("Running Flyway repair...");
        flyway.repair();
        System.out.println("Running Flyway migrate...");
        flyway.migrate();
        System.out.println("Flyway sync completed.");
    }

    private static void reconcileKnownPartialV12(String url, String username, String password) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            if (!tableExists(statement, "flyway_schema_history")) {
                return;
            }
            if (!hasFailedV12(statement)) {
                return;
            }

            String[] tables = {"REPORT_VERSIONS", "PATHOLOGY_REPORTS", "DIAGNOSTIC_TASKS"};
            boolean foundAny = false;
            for (String table : tables) {
                if (!tableExists(statement, table)) {
                    continue;
                }
                foundAny = true;
                if (countRows(statement, table) > 0) {
                    throw new IllegalStateException(
                        "Detected failed Flyway V12 and non-empty table " + table +
                            ". Please back up data and clean up manually before rerunning Flyway.");
                }
            }

            if (!foundAny) {
                return;
            }

            System.out.println("Detected failed V12 with empty partial M4 tables, cleaning up before repair...");
            for (String table : tables) {
                if (tableExists(statement, table)) {
                    statement.execute("DROP TABLE " + table + " CASCADE CONSTRAINTS");
                }
            }
        }
    }

    private static boolean hasFailedV12(Statement statement) throws Exception {
        try (ResultSet rs = statement.executeQuery(
            "select count(*) from \"flyway_schema_history\" where \"version\" = '12' and \"success\" = 0")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean tableExists(Statement statement, String tableName) throws Exception {
        String sql = "select count(*) from user_tables where table_name = '" + tableName + "'";
        try (ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static int countRows(Statement statement, String tableName) throws Exception {
        try (ResultSet rs = statement.executeQuery("select count(*) from " + tableName)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
