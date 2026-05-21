package com.company.bl.tools;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class BlCenterFlywayCli {

    private static final String DEFAULT_DRIVER = "dm.jdbc.driver.DmDriver";
    private static final String DEFAULT_URL = "jdbc:dm://127.0.0.1:5236";
    private static final String DEFAULT_USERNAME = "SYSDBA";
    private static final String DEFAULT_PASSWORD = "Dm.2027.Pwd.";
    private static final String DEFAULT_BASELINE_DESCRIPTION = "<< Flyway Baseline >>";
    private static final String FLYWAY_HISTORY_TABLE = "flyway_schema_history";

    private static final Path[] MANAGED_TABLE_SOURCE_DIRS = {
        Path.of("bl-center", "src", "main", "resources", "db", "migration"),
        Path.of("bl-center", "src", "main", "java", "db", "migration")
    };

    private static final Pattern CREATE_TABLE_PATTERN =
        Pattern.compile("(?i)CREATE\\s+TABLE\\s+\"?([A-Za-z0-9_]+)\"?");

    private static final List<Pattern> INTERNAL_TABLE_PATTERNS = List.of(
        Pattern.compile("^##"),
        Pattern.compile("^AQ\\$_"),
        Pattern.compile("^DBMS_"),
        Pattern.compile("^REG\\$"),
        Pattern.compile("^SREF_")
    );

    private static final Set<String> FALLBACK_MANAGED_TABLES = Set.of(
        "APPLICATIONS",
        "AUTH_ACCESS_TOKENS",
        "BODY_PART_DICT",
        "CASE_MEDIA_ASSETS",
        "DEHYDRATION_BATCHES",
        "DEHYDRATION_BATCH_ITEMS",
        "DIAGNOSTIC_TASKS",
        "EMBEDDINGS",
        "EMBEDDING_BOXES",
        "MEDICAL_ORDER_CHARGE_ITEMS",
        "MEDICAL_ORDER_DICT_CATEGORIES",
        "MEDICAL_ORDER_DICT_ITEMS",
        "MEDICAL_ORDER_PACKAGE_ITEMS",
        "MEDICAL_ORDER_PACKAGES",
        "MENUS",
        "MESSAGE_TOPICS",
        "NUMBERING_COUNTERS",
        "NUMBERING_RULES",
        "OPERATION_LOGS",
        "PATHOLOGY_CASES",
        "PATHOLOGY_REPORTS",
        "PERMISSIONS",
        "REPORT_VERSIONS",
        "REWORK_ORDERS",
        "ROLES",
        "ROLE_MENUS",
        "ROLE_MESSAGE_SUBSCRIPTIONS",
        "ROLE_PERMISSIONS",
        "ROLE_STAT_AUTHORIZATIONS",
        "SAMPLINGS",
        "SAMPLING_BLOCKS",
        "SAMPLING_GUIDELINES",
        "SAMPLING_GUIDELINE_CATEGORIES",
        "SAMPLING_TEMPLATES",
        "SAMPLING_TEMPLATE_CATEGORIES",
        "SAMPLING_TEMPLATE_SITE_REL",
        "SLICINGS",
        "SLIDES",
        "SLIDE_QC_EVALUATIONS",
        "SLIDE_STAININGS",
        "SPECIMENS",
        "SPECIMEN_COLLECTION_RECORDS",
        "SPECIMEN_FIXATION_RECORDS",
        "SPECIMEN_RECEIPTS",
        "STAT_CATEGORIES",
        "SYSTEM_CONFIG_CATEGORIES",
        "SYSTEM_CONFIG_ITEMS",
        "TECHNICAL_PENDING_TASKS",
        "TRANSPORT_ORDERS",
        "TRANSPORT_ORDER_ITEMS",
        "USERS",
        "USER_LOGIN_LOGS",
        "USER_ROLES",
        "WORKFLOW_EVENTS"
    );

    private BlCenterFlywayCli() {
    }

    public static void main(String[] args) {
        int exitCode = execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
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
        if (!report.validationSuccessful()) {
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
        if (!preSyncReport.validationSuccessful()) {
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
        if (!postSyncReport.validationSuccessful()) {
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

            Set<String> managedTables = resolveManagedTables();
            Set<String> userTables = loadUserTables(statement);
            List<String> unmanagedTables = userTables.stream()
                .filter(table -> !managedTables.contains(table))
                .filter(table -> !FLYWAY_HISTORY_TABLE.equalsIgnoreCase(table))
                .filter(table -> !isInternalTable(table))
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
                pending.length
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
            .load();
    }

    private static Set<String> resolveManagedTables() {
        Set<String> managedTables = new LinkedHashSet<>();
        for (Path sourceDir : MANAGED_TABLE_SOURCE_DIRS) {
            if (!Files.isDirectory(sourceDir)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.endsWith(".sql") || fileName.endsWith(".java");
                    })
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> collectManagedTables(path, managedTables));
            } catch (IOException exception) {
                throw new CliFailure(
                    "managed-table-scan",
                    "Failed to scan managed table definitions under " + sourceDir + ": " + exception.getMessage(),
                    "Make sure the repository is complete before running inspect or sync."
                );
            }
        }
        if (managedTables.isEmpty()) {
            managedTables.addAll(FALLBACK_MANAGED_TABLES);
        }
        return managedTables;
    }

    private static void collectManagedTables(Path path, Set<String> managedTables) {
        try {
            String content = Files.readString(path);
            Matcher matcher = CREATE_TABLE_PATTERN.matcher(content);
            while (matcher.find()) {
                managedTables.add(normalizeTableName(matcher.group(1)));
            }
        } catch (IOException exception) {
            throw new CliFailure(
                "managed-table-scan",
                "Failed to read managed table definition file " + path + ": " + exception.getMessage(),
                "Check file permissions and rerun the command."
            );
        }
    }

    private static Set<String> loadUserTables(Statement statement) throws Exception {
        Set<String> tables = new LinkedHashSet<>();
        try (ResultSet resultSet = statement.executeQuery("select table_name from user_tables order by table_name")) {
            while (resultSet.next()) {
                tables.add(normalizeTableName(resultSet.getString(1)));
            }
        }
        return tables;
    }

    private static boolean isInternalTable(String tableName) {
        for (Pattern pattern : INTERNAL_TABLE_PATTERNS) {
            if (pattern.matcher(tableName).find()) {
                return true;
            }
        }
        return false;
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
        String sql = "select count(*) from user_tables where upper(table_name) = '" + normalizeTableName(tableName) + "'";
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

    private static String normalizeTableName(String tableName) {
        return tableName == null ? "" : tableName.trim().toUpperCase(Locale.ROOT);
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
        String baselineDescription
    ) {
        private static DatabaseConfig fromEnv() {
            return new DatabaseConfig(
                env("BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME", DEFAULT_DRIVER),
                env("BL_CENTER_DATASOURCE_URL", DEFAULT_URL),
                env("BL_CENTER_DATASOURCE_USERNAME", DEFAULT_USERNAME),
                env("BL_CENTER_DATASOURCE_PASSWORD", DEFAULT_PASSWORD),
                Boolean.parseBoolean(env("SPRING_FLYWAY_BASELINE_ON_MIGRATE", "false")),
                env("SPRING_FLYWAY_BASELINE_VERSION", "1"),
                env("SPRING_FLYWAY_BASELINE_DESCRIPTION", DEFAULT_BASELINE_DESCRIPTION)
            );
        }
    }

    private record InspectionReport(
        boolean validationSuccessful,
        String validationMessage,
        String currentVersion,
        int pendingMigrationCount
    ) {
    }

    private static final class CliFailure extends RuntimeException {

        private final String stage;
        private final String suggestion;

        private CliFailure(String stage, String message, String suggestion) {
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
