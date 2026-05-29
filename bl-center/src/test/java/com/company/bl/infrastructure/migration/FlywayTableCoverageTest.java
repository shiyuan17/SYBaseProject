package com.company.bl.infrastructure.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayTableCoverageTest {

    private static final Pattern CREATE_TABLE_PATTERN =
        Pattern.compile("(?i)CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?\"?([A-Za-z0-9_]+)\"?");

    private static final Pattern TABLE_NAME_PATTERN =
        Pattern.compile("@TableName\\(\"([A-Za-z0-9_]+)\"\\)");

    private static final List<Pattern> SQL_TABLE_PATTERNS = List.of(
        Pattern.compile("(?i)\\bFROM\\s+([A-Za-z0-9_]+)\\s*(?:,|\\)|\\r|\\n|WHERE\\b|JOIN\\b|ORDER\\b|GROUP\\b)"),
        Pattern.compile("(?i)\\bJOIN\\s+([A-Za-z0-9_]+)\\s*(?:\\r|\\n|ON\\b|WHERE\\b|ORDER\\b|GROUP\\b)"),
        Pattern.compile("(?i)\\bUPDATE\\s+([A-Za-z0-9_]+)\\s*(?:\\r|\\n|SET\\b)"),
        Pattern.compile("(?i)\\bINSERT\\s+INTO\\s+([A-Za-z0-9_]+)\\s*(?:\\(|\\r|\\n)"),
        Pattern.compile("(?i)\\bDELETE\\s+FROM\\s+([A-Za-z0-9_]+)\\s*(?:\\r|\\n|WHERE\\b)")
    );

    private static final Set<String> NON_BUSINESS_SQL_TABLES = Set.of(
        "USER_TABLES"
    );

    @Test
    void shouldCoverAllCodeReferencedTablesWithFlywayMigrations() throws Exception {
        Path moduleRoot = resolveModuleRoot();
        Set<String> managedTables = scanManagedTables(moduleRoot);
        Set<String> codeReferencedTables = scanCodeReferencedTables(moduleRoot);

        Set<String> missingTables = new LinkedHashSet<>(codeReferencedTables);
        missingTables.removeAll(managedTables);

        assertTrue(
            missingTables.isEmpty(),
            "Code-referenced tables missing in Flyway migrations: " + missingTables
        );
    }

    private static Path resolveModuleRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("src/main/resources/db/migration"))) {
            return current;
        }
        Path nested = current.resolve("bl-center");
        if (Files.isDirectory(nested.resolve("src/main/resources/db/migration"))) {
            return nested;
        }
        throw new IllegalStateException("Unable to locate bl-center module root from " + current);
    }

    private static Set<String> scanManagedTables(Path moduleRoot) throws IOException {
        Set<String> managedTables = new LinkedHashSet<>();
        collectCreateTableMatches(moduleRoot.resolve("src/main/resources/db/migration"), managedTables);
        collectCreateTableMatches(moduleRoot.resolve("src/main/java/db/migration"), managedTables);
        return managedTables;
    }

    private static void collectCreateTableMatches(Path directory, Set<String> collector) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            files.filter(Files::isRegularFile)
                .sorted(Comparator.comparing(Path::toString))
                .forEach(path -> {
                    try {
                        Matcher matcher = CREATE_TABLE_PATTERN.matcher(Files.readString(path));
                        while (matcher.find()) {
                            collector.add(normalizeTableName(matcher.group(1)));
                        }
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to read migration file " + path, exception);
                    }
                });
        }
    }

    private static Set<String> scanCodeReferencedTables(Path moduleRoot) throws IOException {
        Set<String> codeTables = new LinkedHashSet<>();
        Path mainJavaDir = moduleRoot.resolve("src/main/java");
        Path migrationJavaDir = mainJavaDir.resolve("db/migration");

        try (Stream<Path> files = Files.walk(mainJavaDir)) {
            files.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.startsWith(migrationJavaDir))
                .sorted(Comparator.comparing(Path::toString))
                .forEach(path -> collectCodeTableMatches(path, codeTables));
        }

        return codeTables;
    }

    private static void collectCodeTableMatches(Path path, Set<String> collector) {
        try {
            String content = Files.readString(path);
            Matcher tableNameMatcher = TABLE_NAME_PATTERN.matcher(content);
            while (tableNameMatcher.find()) {
                collector.add(normalizeTableName(tableNameMatcher.group(1)));
            }

            for (Pattern pattern : SQL_TABLE_PATTERNS) {
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    String tableName = normalizeTableName(matcher.group(1));
                    if (!NON_BUSINESS_SQL_TABLES.contains(tableName)) {
                        collector.add(tableName);
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file " + path, exception);
        }
    }

    private static String normalizeTableName(String tableName) {
        return tableName == null ? "" : tableName.trim().toUpperCase(Locale.ROOT);
    }
}
