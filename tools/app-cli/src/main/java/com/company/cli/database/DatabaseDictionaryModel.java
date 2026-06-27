package com.company.cli.database;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

enum DatabaseRequestedTargetDefinition {
    AUTH_CENTER("auth-center", "AUTH_CENTER_DATASOURCE_DRIVER_CLASS_NAME", "AUTH_CENTER_DATASOURCE_URL",
        "AUTH_CENTER_DATASOURCE_USERNAME", "AUTH_CENTER_DATASOURCE_PASSWORD"),
    BL_CENTER("bl-center", "BL_CENTER_DATASOURCE_DRIVER_CLASS_NAME", "BL_CENTER_DATASOURCE_URL",
        "BL_CENTER_DATASOURCE_USERNAME", "BL_CENTER_DATASOURCE_PASSWORD");

    private final String cliValue;
    private final String driverVar;
    private final String urlVar;
    private final String usernameVar;
    private final String passwordVar;

    DatabaseRequestedTargetDefinition(String cliValue, String driverVar, String urlVar, String usernameVar, String passwordVar) {
        this.cliValue = cliValue;
        this.driverVar = driverVar;
        this.urlVar = urlVar;
        this.usernameVar = usernameVar;
        this.passwordVar = passwordVar;
    }

    static DatabaseRequestedTargetDefinition fromCliValue(String cliValue) {
        return java.util.Arrays.stream(values())
            .filter(definition -> definition.cliValue.equalsIgnoreCase(cliValue))
            .findFirst()
            .orElseThrow(() -> DatabaseDictionaryException.invalidConfiguration("Unsupported target: " + cliValue));
    }

    String cliValue() {
        return cliValue;
    }

    String driverVar() {
        return driverVar;
    }

    String urlVar() {
        return urlVar;
    }

    String usernameVar() {
        return usernameVar;
    }

    String passwordVar() {
        return passwordVar;
    }
}

enum DatabaseDictionaryScope {
    VISIBLE_ALL("visible-all");

    private final String cliValue;

    DatabaseDictionaryScope(String cliValue) {
        this.cliValue = cliValue;
    }

    static DatabaseDictionaryScope fromCliValue(String cliValue) {
        return java.util.Arrays.stream(values())
            .filter(scope -> scope.cliValue.equalsIgnoreCase(cliValue))
            .findFirst()
            .orElseThrow(() -> DatabaseDictionaryException.invalidConfiguration("Unsupported scope: " + cliValue));
    }

    @Override
    public String toString() {
        return cliValue;
    }
}

enum DatabaseDictionaryReportStatus {
    OK("OK", "ok"),
    WARNING("WARNING", "warning");

    private final String label;
    private final String cssClass;

    DatabaseDictionaryReportStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    String label() {
        return label;
    }

    String cssClass() {
        return cssClass;
    }
}

record DatabaseDictionaryRequest(List<String> targets, Path outputPath, DatabaseDictionaryScope scope) {
}

record DatabaseDictionaryGenerationResult(String html, DatabaseDictionaryReport report) {
}

record DatabaseConnectionTarget(
    List<String> labels,
    String driverClassName,
    String url,
    String username,
    String password
) {
}

record DatabaseDictionaryReport(
    Instant generatedAt,
    DatabaseDictionaryReportStatus status,
    DatabaseDictionaryScope scope,
    List<DatabaseSourceReport> sources,
    DatabaseDictionarySummary summary
) {
}

record DatabaseDictionarySummary(
    int sourceCount,
    int ownerCount,
    int tableCount,
    int columnCount,
    int indexCount,
    int foreignKeyCount
) {
}

record DatabaseSourceReport(
    List<String> labels,
    String url,
    String username,
    List<DatabaseOwnerReport> owners
) {
}

record DatabaseOwnerReport(String owner, List<DatabaseTableReport> tables) {
}

record DatabaseTableReport(
    String owner,
    String tableName,
    String comment,
    List<DatabaseColumnReport> columns,
    List<DatabaseUniqueConstraintReport> uniqueConstraints,
    List<DatabaseForeignKeyReport> foreignKeys,
    List<DatabaseIndexReport> indexes
) {
}

record DatabaseColumnReport(
    String name,
    String type,
    boolean nullable,
    String defaultValue,
    String comment,
    boolean primaryKey,
    List<String> keyConstraintNames,
    List<String> foreignKeyNames
) {
}

record DatabaseUniqueConstraintReport(String name, boolean primaryKey, List<String> columns) {
}

record DatabaseForeignKeyReport(
    String name,
    List<String> columns,
    String referencedOwner,
    String referencedTable,
    List<String> referencedColumns
) {
}

record DatabaseIndexReport(String name, String uniqueness, String indexType, List<String> columns) {
}

record TableRow(String owner, String tableName, String comment) {
}

record ColumnRow(
    String owner,
    String tableName,
    String columnName,
    Integer columnId,
    String dataType,
    Integer dataLength,
    Integer dataPrecision,
    Integer dataScale,
    boolean nullable,
    String dataDefault,
    String comment
) {
}

record ConstraintRow(
    String owner,
    String tableName,
    String constraintName,
    String constraintType,
    String columnName,
    Integer position,
    String referencedOwner,
    String referencedTable,
    String referencedColumnName
) {
}

record IndexRow(
    String owner,
    String tableName,
    String indexName,
    String uniqueness,
    String indexType,
    String columnName,
    Integer columnPosition
) {
}

record TableKey(String owner, String tableName) {
    static TableKey of(String owner, String tableName) {
        return new TableKey(owner, tableName);
    }
}

record ConstraintKey(String owner, String tableName, String constraintName) {
    static ConstraintKey of(String owner, String tableName, String constraintName) {
        return new ConstraintKey(owner, tableName, constraintName);
    }
}

record IndexKey(String owner, String tableName, String indexName) {
    static IndexKey of(String owner, String tableName, String indexName) {
        return new IndexKey(owner, tableName, indexName);
    }
}
