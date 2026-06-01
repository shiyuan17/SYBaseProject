package com.company.bl.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

final class BlCenterFlywayTableSupport {

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
        "TECHNICAL_SPECIMEN_REGISTRATIONS",
        "TECHNICAL_PENDING_TASKS",
        "TRANSPORT_ORDERS",
        "TRANSPORT_ORDER_ITEMS",
        "USERS",
        "USER_LOGIN_LOGS",
        "USER_ROLES",
        "WORKFLOW_EVENTS"
    );

    private BlCenterFlywayTableSupport() {
    }

    static Set<String> resolveManagedTables() {
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
                throw new BlCenterFlywayCliSupport.CliFailure(
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

    static Set<String> loadUserTables(Statement statement) throws Exception {
        Set<String> tables = new LinkedHashSet<>();
        try (ResultSet resultSet = statement.executeQuery("select table_name from user_tables order by table_name")) {
            while (resultSet.next()) {
                tables.add(normalizeTableName(resultSet.getString(1)));
            }
        }
        return tables;
    }

    static boolean isInternalTable(String tableName) {
        for (Pattern pattern : INTERNAL_TABLE_PATTERNS) {
            if (pattern.matcher(tableName).find()) {
                return true;
            }
        }
        return false;
    }

    static String normalizeTableName(String tableName) {
        return tableName == null ? "" : tableName.trim().toUpperCase(Locale.ROOT);
    }

    private static void collectManagedTables(Path path, Set<String> managedTables) {
        try {
            String content = Files.readString(path);
            Matcher matcher = CREATE_TABLE_PATTERN.matcher(content);
            while (matcher.find()) {
                managedTables.add(normalizeTableName(matcher.group(1)));
            }
        } catch (IOException exception) {
            throw new BlCenterFlywayCliSupport.CliFailure(
                "managed-table-scan",
                "Failed to read managed table definition file " + path + ": " + exception.getMessage(),
                "Check file permissions and rerun the command."
            );
        }
    }
}
