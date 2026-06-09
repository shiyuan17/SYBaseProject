package com.company.bl.tools;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class BlCenterFlywayCliSupport {

    private static final String DEFAULT_DRIVER = "dm.jdbc.driver.DmDriver";
    private static final String DEFAULT_URL = "jdbc:dm://127.0.0.1:5236";
    private static final String DEFAULT_USERNAME = "SYSDBA";
    private static final String DEFAULT_PASSWORD = "Dm.2027.Pwd.";
    private static final String DEFAULT_BASELINE_DESCRIPTION = "<< Flyway Baseline >>";
    private static final String FLYWAY_HISTORY_TABLE = "flyway_schema_history";

    private BlCenterFlywayCliSupport() {
    }

    static int execute(String[] args) {
        try {
            Mode mode = Mode.parse(args);
            DatabaseConfig config = DatabaseConfig.fromEnv();
            switch (mode) {
                case INSPECT -> {
                    runInspect(config);
                    return 0;
                }
                case SYNC -> {
                    runSync(config);
                    return 0;
                }
                default -> throw new CliFailure("arguments", "Unsupported mode " + mode, "Use inspect or sync.");
            }
        } catch (CliFailure failure) {
            System.err.println("Flyway command failed.");
            System.err.println("failed_stage=" + failure.stage());
            System.err.println("message=" + failure.getMessage());
            if (failure.suggestion() != null && !failure.suggestion().isBlank()) {
                System.err.println("suggestion=" + failure.suggestion());
            }
            return 1;
        } catch (Exception exception) {
            System.err.println("Flyway command failed.");
            System.err.println("failed_stage=unexpected");
            System.err.println("message=" + exception.getMessage());
            exception.printStackTrace(System.err);
            return 1;
        }
    }

    private static void runInspect(DatabaseConfig config) throws Exception {
        InspectionReport report = inspect(config, buildFlyway(config), "inspect");
        if (!report.validationAcceptable()) {
            throw new CliFailure(
                "validate",
                "Flyway validation failed during inspect.",
                report.validationMessage()
            );
        }
        System.out.println("inspect_result=OK");
    }

    private static void runSync(DatabaseConfig config) throws Exception {
        InspectionReport preSyncReport = inspect(config, buildFlyway(config), "pre-sync");
        if (!preSyncReport.validationAcceptable()) {
            System.out.println("pre_sync_validation_warning=" + preSyncReport.validationMessage());
        }

        reconcileKnownPartialV12(config.url(), config.username(), config.password());

        Flyway flyway = buildFlyway(config);
        System.out.println("repair_status=STARTED");
        flyway.repair();
        System.out.println("repair_status=OK");

        System.out.println("migrate_status=STARTED");
        MigrateResult migrateResult = flyway.migrate();
        System.out.println("migrate_status=" + (migrateResult.success ? "OK" : "FAILED"));
        System.out.println("migrations_executed=" + migrateResult.migrationsExecuted);

        InspectionReport postSyncReport = inspect(config, buildFlyway(config), "post-sync");
        if (!postSyncReport.validationAcceptable()) {
            throw new CliFailure(
                "post-check",
                "Flyway validation still fails after sync.",
                postSyncReport.validationMessage()
            );
        }
        if (postSyncReport.pendingMigrationCount() > 0) {
            throw new CliFailure(
                "post-check",
                "Flyway still has pending migrations after sync.",
                "Add the missing migration files or rerun sync after fixing validation issues."
            );
        }

        System.out.println("sync_result=OK");
        System.out.println("current_schema_version=" + valueOrNone(postSyncReport.currentVersion()));
        System.out.println("pending_migration_count=" + postSyncReport.pendingMigrationCount());
    }

    private static InspectionReport inspect(DatabaseConfig config, Flyway flyway, String phase) throws Exception {
        System.out.println("phase=" + phase);
        System.out.println("connection_status=STARTED");

        try (Connection connection = DriverManager.getConnection(config.url(), config.username(), config.password());
             Statement statement = connection.createStatement()) {
            System.out.println("connection_status=OK");
            System.out.println("connection_url=" + config.url());
            System.out.println("connection_username=" + config.username());

            Set<String> managedTables = BlCenterFlywayTableSupport.resolveManagedTables();
            Set<String> userTables = BlCenterFlywayTableSupport.loadUserTables(statement);
            List<String> unmanagedTables = userTables.stream()
                .filter(table -> !managedTables.contains(table))
                .filter(table -> !FLYWAY_HISTORY_TABLE.equalsIgnoreCase(table))
                .filter(table -> !BlCenterFlywayTableSupport.isInternalTable(table))
                .sorted()
                .toList();

            boolean flywayHistoryExists = userTables.stream()
                .anyMatch(table -> FLYWAY_HISTORY_TABLE.equalsIgnoreCase(table));

            MigrationInfoService info = flyway.info();
            MigrationInfo current = info.current();
            MigrationInfo[] applied = info.applied();
            MigrationInfo[] pending = info.pending();
            ValidateResult validateResult = flyway.validateWithResult();

            System.out.println("flyway_schema_history_exists=" + flywayHistoryExists);
            System.out.println("managed_table_count=" + managedTables.size());
            System.out.println("database_table_count=" + userTables.size());
            System.out.println("current_schema_version=" + valueOrNone(current == null ? null : current.getVersion() == null ? null : current.getVersion().toString()));
            System.out.println("installed_migration_count=" + applied.length);
            if (applied.length == 0) {
                System.out.println("installed_migrations=(none)");
            } else {
                for (MigrationInfo migration : applied) {
                    System.out.println("installed=" + describeMigration(migration));
                }
            }

            System.out.println("pending_migration_count=" + pending.length);
            if (pending.length == 0) {
                System.out.println("pending_migrations=(none)");
            } else {
                for (MigrationInfo migration : pending) {
                    System.out.println("pending=" + describeMigration(migration));
                }
            }

            System.out.println("validation_success=" + validateResult.validationSuccessful);
            if (validateResult.validationSuccessful) {
                System.out.println("validation_message=OK");
            } else {
                System.out.println("validation_message=" + validateResult.getAllErrorMessages());
                if (validateResult.invalidMigrations != null) {
                    for (ValidateOutput invalidMigration : validateResult.invalidMigrations) {
                        String errorMessage = invalidMigration.errorDetails == null
                            ? "Unknown validation error"
                            : invalidMigration.errorDetails.errorMessage;
                        System.out.println(
                            "validation_issue="
                                + valueOrNone(invalidMigration.version)
                                + "|"
                                + valueOrNone(invalidMigration.description)
                                + "|"
                                + errorMessage
                        );
                    }
                }
            }

            System.out.println("unmanaged_table_count=" + unmanagedTables.size());
            if (unmanagedTables.isEmpty()) {
                System.out.println("UNMANAGED_LEGACY_OR_PLANNED=(none)");
            } else {
                for (String table : unmanagedTables) {
                    System.out.println("UNMANAGED_LEGACY_OR_PLANNED=" + table);
                }
            }

            return new InspectionReport(
                validateResult.validationSuccessful,
                validateResult.validationSuccessful ? "OK" : validateResult.getAllErrorMessages(),
                current == null ? null : current.getVersion() == null ? null : current.getVersion().toString(),
                pending.length,
                isOnlyPendingValidationIssue(validateResult)
            );
        } catch (CliFailure failure) {
            throw failure;
        } catch (Exception exception) {
            throw new CliFailure(
                "inspect",
                "Failed to inspect Flyway state: " + exception.getMessage(),
                "Check database connectivity, credentials, and schema permissions."
            );
        }
    }

    private static Flyway buildFlyway(DatabaseConfig config) throws ClassNotFoundException {
        Class.forName(config.driver());
        return Flyway.configure()
            .dataSource(config.url(), config.username(), config.password())
            .locations("classpath:db/migration")
            .baselineOnMigrate(config.baselineOnMigrate())
            .baselineVersion(config.baselineVersion())
            .baselineDescription(config.baselineDescription())
            .outOfOrder(config.outOfOrder())
            .load();
    }

    private static String describeMigration(MigrationInfo migration) {
        String version = migration.getVersion() == null ? "(repeatable)" : migration.getVersion().toString();
        return version
            + "|"
            + valueOrNone(migration.getDescription())
            + "|"
            + migration.getType()
            + "|"
            + migration.getState();
    }

    private static boolean isOnlyPendingValidationIssue(ValidateResult validateResult) {
        if (validateResult.validationSuccessful) {
            return false;
        }
        if (validateResult.invalidMigrations == null || validateResult.invalidMigrations.isEmpty()) {
            return false;
        }
        return validateResult.invalidMigrations.stream()
            .allMatch(invalidMigration -> invalidMigration.errorDetails != null
                && invalidMigration.errorDetails.errorMessage != null
                && invalidMigration.errorDetails.errorMessage.contains("not applied to database"));
    }

    private static void reconcileKnownPartialV12(String url, String username, String password) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            if (!tableExists(statement, FLYWAY_HISTORY_TABLE)) {
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
                    throw new CliFailure(
                        "repair",
                        "Detected failed Flyway V12 and non-empty table " + table + ".",
                        "Back up the partial M4 data, clean up manually, then rerun sync."
                    );
                }
            }

            if (!foundAny) {
                return;
            }

            System.out.println("repair_notice=Detected failed V12 with empty partial M4 tables, cleaning up before repair.");
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
        String sql = "select count(*) from user_tables where upper(table_name) = '"
            + BlCenterFlywayTableSupport.normalizeTableName(tableName)
            + "'";
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

    private static String valueOrNone(String value) {
        return value == null || value.isBlank() ? "(none)" : value;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private enum Mode {
        INSPECT,
        SYNC;

        private static Mode parse(String[] args) {
            if (args == null || args.length == 0) {
                return SYNC;
            }
            if (args.length != 1) {
                throw new CliFailure("arguments", "Expected zero or one argument.", "Use inspect or sync.");
            }
            String raw = args[0] == null ? "" : args[0].trim().toLowerCase(Locale.ROOT);
            return switch (raw) {
                case "", "sync" -> SYNC;
                case "inspect" -> INSPECT;
                default -> throw new CliFailure("arguments", "Unknown mode " + args[0], "Use inspect or sync.");
            };
        }
    }

    private record DatabaseConfig(
        String driver,
        String url,
        String username,
        String password,
        boolean baselineOnMigrate,
        String baselineVersion,
        String baselineDescription,
        boolean outOfOrder
    ) {
        private static DatabaseConfig fromEnv() {
            return new DatabaseConfig(
                env("BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME", DEFAULT_DRIVER),
                env("BL_CENTER_DATASOURCE_URL", DEFAULT_URL),
                env("BL_CENTER_DATASOURCE_USERNAME", DEFAULT_USERNAME),
                env("BL_CENTER_DATASOURCE_PASSWORD", DEFAULT_PASSWORD),
                Boolean.parseBoolean(env("SPRING_FLYWAY_BASELINE_ON_MIGRATE", "false")),
                env("SPRING_FLYWAY_BASELINE_VERSION", "1"),
                env("SPRING_FLYWAY_BASELINE_DESCRIPTION", DEFAULT_BASELINE_DESCRIPTION),
                Boolean.parseBoolean(env(
                    "BL_CENTER_FLYWAY_OUT_OF_ORDER",
                    env("SPRING_FLYWAY_OUT_OF_ORDER", "false")
                ))
            );
        }
    }

    private record InspectionReport(
        boolean validationSuccessful,
        String validationMessage,
        String currentVersion,
        int pendingMigrationCount,
        boolean onlyPendingValidationIssue
    ) {
        private boolean validationAcceptable() {
            return validationSuccessful || onlyPendingValidationIssue;
        }
    }

    static final class CliFailure extends RuntimeException {

        private final String stage;
        private final String suggestion;

        CliFailure(String stage, String message, String suggestion) {
            super(message);
            this.stage = stage;
            this.suggestion = suggestion;
        }

        private String stage() {
            return stage;
        }

        private String suggestion() {
            return suggestion;
        }
    }
}
