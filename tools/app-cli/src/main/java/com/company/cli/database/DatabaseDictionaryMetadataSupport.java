package com.company.cli.database;

import com.company.common.core.enums.ErrorCode;
import com.company.common.core.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class DatabaseDictionaryTargetResolver {

    private static final String DEFAULT_DRIVER = "dm.jdbc.driver.DmDriver";
    private static final String DEFAULT_URL = "jdbc:dm://127.0.0.1:5236";
    private static final String DEFAULT_USERNAME = "SYSDBA";
    private static final String DEFAULT_PASSWORD = "Dm.2027.Pwd.";

    private final Environment environment;

    List<DatabaseConnectionTarget> resolve(List<String> requestedTargets) {
        LinkedHashMap<String, DatabaseConnectionTarget> deduped = new LinkedHashMap<>();
        for (String requestedTarget : requestedTargets) {
            DatabaseRequestedTargetDefinition definition = DatabaseRequestedTargetDefinition.fromCliValue(requestedTarget);
            DatabaseConnectionTarget target = resolveSingle(definition);
            String key = target.url() + "|" + target.username();
            DatabaseConnectionTarget existing = deduped.get(key);
            if (existing == null) {
                deduped.put(key, target);
            } else {
                List<String> mergedLabels = new ArrayList<>(existing.labels());
                mergedLabels.addAll(target.labels());
                deduped.put(key, new DatabaseConnectionTarget(
                    mergedLabels.stream().distinct().toList(),
                    existing.driverClassName(),
                    existing.url(),
                    existing.username(),
                    existing.password()));
            }
        }
        return new ArrayList<>(deduped.values());
    }

    private DatabaseConnectionTarget resolveSingle(DatabaseRequestedTargetDefinition definition) {
        String url = environment.getProperty(definition.urlVar());
        String username = environment.getProperty(definition.usernameVar());
        String password = environment.getProperty(definition.passwordVar());
        String driverClassName = environment.getProperty(definition.driverVar());

        if (isBlank(url) && isBlank(username) && isBlank(password)) {
            return new DatabaseConnectionTarget(
                List.of(definition.cliValue()),
                DEFAULT_DRIVER,
                DEFAULT_URL,
                DEFAULT_USERNAME,
                DEFAULT_PASSWORD);
        }

        List<String> missingVars = new ArrayList<>();
        if (isBlank(url)) {
            missingVars.add(definition.urlVar());
        }
        if (isBlank(username)) {
            missingVars.add(definition.usernameVar());
        }
        if (isBlank(password)) {
            missingVars.add(definition.passwordVar());
        }
        if (!missingVars.isEmpty()) {
            throw DatabaseDictionaryException.invalidConfiguration(
                definition.cliValue() + " datasource configuration is incomplete. Missing: " + String.join(", ", missingVars));
        }

        return new DatabaseConnectionTarget(
            List.of(definition.cliValue()),
            isBlank(driverClassName) ? DEFAULT_DRIVER : driverClassName,
            url.trim(),
            username.trim(),
            password);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

@Component
class DatabaseDictionaryMetadataLoader {

    private static final String TABLE_SQL = """
        select t.owner,
               t.table_name,
               c.comments as table_comment
          from all_tables t
          left join all_tab_comments c
            on c.owner = t.owner
           and c.table_name = t.table_name
         order by t.owner, t.table_name
        """;

    private static final String COLUMN_SQL = """
        select c.owner,
               c.table_name,
               c.column_name,
               c.column_id,
               c.data_type,
               c.data_length,
               c.data_precision,
               c.data_scale,
               c.nullable,
               c.data_default,
               cc.comments as column_comment
          from all_tab_columns c
          left join all_col_comments cc
            on cc.owner = c.owner
           and cc.table_name = c.table_name
           and cc.column_name = c.column_name
         order by c.owner, c.table_name, c.column_id
        """;

    private static final String CONSTRAINT_SQL = """
        select c.owner,
               c.table_name,
               c.constraint_name,
               c.constraint_type,
               cc.column_name,
               cc.position,
               rc.owner as referenced_owner,
               rc.table_name as referenced_table,
               rcc.column_name as referenced_column_name
          from all_constraints c
          join all_cons_columns cc
            on cc.owner = c.owner
           and cc.constraint_name = c.constraint_name
          left join all_constraints rc
            on rc.owner = c.r_owner
           and rc.constraint_name = c.r_constraint_name
          left join all_cons_columns rcc
            on rcc.owner = rc.owner
           and rcc.constraint_name = rc.constraint_name
           and rcc.position = cc.position
         where c.constraint_type in ('P', 'U', 'R')
         order by c.owner, c.table_name, c.constraint_name, cc.position
        """;

    private static final String INDEX_SQL = """
        select i.table_owner,
               i.table_name,
               i.index_name,
               i.uniqueness,
               i.index_type,
               ic.column_name,
               ic.column_position
          from all_indexes i
          join all_ind_columns ic
            on ic.index_owner = i.owner
           and ic.index_name = i.index_name
         order by i.table_owner, i.table_name, i.index_name, ic.column_position
        """;

    private final DatabaseDictionaryMetadataAssembler assembler = new DatabaseDictionaryMetadataAssembler();

    DatabaseSourceReport load(DatabaseConnectionTarget target) {
        try {
            Class.forName(target.driverClassName());
            try (Connection connection = DriverManager.getConnection(target.url(), target.username(), target.password())) {
                return assembler.assemble(
                    target,
                    queryTableRows(connection),
                    queryColumnRows(connection),
                    queryConstraintRows(connection),
                    queryIndexRows(connection));
            }
        } catch (ClassNotFoundException ex) {
            throw DatabaseDictionaryException.generationFailed("Unable to load JDBC driver: " + target.driverClassName(), ex);
        } catch (SQLException ex) {
            throw DatabaseDictionaryException.generationFailed(
                "Failed to load database metadata from " + String.join(", ", target.labels()) + ": " + ex.getMessage(),
                ex);
        }
    }

    private List<TableRow> queryTableRows(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(TABLE_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<TableRow> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(new TableRow(resultSet.getString(1), resultSet.getString(2), normalizeText(resultSet.getString(3))));
            }
            return rows;
        }
    }

    private List<ColumnRow> queryColumnRows(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(COLUMN_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<ColumnRow> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(new ColumnRow(
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    integerValue(resultSet, 4),
                    resultSet.getString(5),
                    integerValue(resultSet, 6),
                    integerValue(resultSet, 7),
                    integerValue(resultSet, 8),
                    "Y".equalsIgnoreCase(resultSet.getString(9)),
                    normalizeText(resultSet.getString(10)),
                    normalizeText(resultSet.getString(11))));
            }
            return rows;
        }
    }

    private List<ConstraintRow> queryConstraintRows(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CONSTRAINT_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<ConstraintRow> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(new ConstraintRow(
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getString(4),
                    resultSet.getString(5),
                    integerValue(resultSet, 6),
                    resultSet.getString(7),
                    resultSet.getString(8),
                    resultSet.getString(9)));
            }
            return rows;
        }
    }

    private List<IndexRow> queryIndexRows(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INDEX_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<IndexRow> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(new IndexRow(
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getString(4),
                    resultSet.getString(5),
                    resultSet.getString(6),
                    integerValue(resultSet, 7)));
            }
            return rows;
        }
    }

    private Integer integerValue(ResultSet resultSet, int index) throws SQLException {
        int value = resultSet.getInt(index);
        return resultSet.wasNull() ? null : value;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }
}

class DatabaseDictionaryMetadataAssembler {

    DatabaseSourceReport assemble(
        DatabaseConnectionTarget target,
        List<TableRow> tableRows,
        List<ColumnRow> columnRows,
        List<ConstraintRow> constraintRows,
        List<IndexRow> indexRows
    ) {
        LinkedHashMap<TableKey, TableBuilder> tableBuilders = new LinkedHashMap<>();
        for (TableRow tableRow : tableRows) {
            tableBuilders.computeIfAbsent(TableKey.of(tableRow.owner(), tableRow.tableName()),
                ignored -> new TableBuilder(tableRow.owner(), tableRow.tableName(), tableRow.comment()));
        }
        for (ColumnRow columnRow : columnRows) {
            tableBuilders.computeIfAbsent(TableKey.of(columnRow.owner(), columnRow.tableName()),
                ignored -> new TableBuilder(columnRow.owner(), columnRow.tableName(), null))
                .addColumn(columnRow);
        }

        LinkedHashMap<ConstraintKey, ConstraintAccumulator> constraintAccumulators = new LinkedHashMap<>();
        for (ConstraintRow constraintRow : constraintRows) {
            TableBuilder tableBuilder = tableBuilders.computeIfAbsent(
                TableKey.of(constraintRow.owner(), constraintRow.tableName()),
                ignored -> new TableBuilder(constraintRow.owner(), constraintRow.tableName(), null));
            ConstraintAccumulator accumulator = constraintAccumulators.computeIfAbsent(
                ConstraintKey.of(constraintRow.owner(), constraintRow.tableName(), constraintRow.constraintName()),
                ignored -> new ConstraintAccumulator(constraintRow.constraintName(), constraintRow.constraintType(),
                    constraintRow.referencedOwner(), constraintRow.referencedTable()));
            accumulator.add(constraintRow);
            tableBuilder.markConstraintColumn(constraintRow);
        }

        LinkedHashMap<IndexKey, IndexAccumulator> indexAccumulators = new LinkedHashMap<>();
        for (IndexRow indexRow : indexRows) {
            tableBuilders.computeIfAbsent(TableKey.of(indexRow.owner(), indexRow.tableName()),
                ignored -> new TableBuilder(indexRow.owner(), indexRow.tableName(), null));
            indexAccumulators.computeIfAbsent(
                    IndexKey.of(indexRow.owner(), indexRow.tableName(), indexRow.indexName()),
                    ignored -> new IndexAccumulator(indexRow.indexName(), indexRow.uniqueness(), indexRow.indexType()))
                .add(indexRow);
        }

        Map<TableKey, List<DatabaseUniqueConstraintReport>> uniqueByTable = constraintAccumulators.values().stream()
            .filter(ConstraintAccumulator::isKeyConstraint)
            .collect(Collectors.groupingBy(
                accumulator -> TableKey.of(accumulator.owner(), accumulator.tableName()),
                LinkedHashMap::new,
                Collectors.mapping(ConstraintAccumulator::toUniqueConstraintReport, Collectors.toList())));
        Map<TableKey, List<DatabaseForeignKeyReport>> foreignKeyByTable = constraintAccumulators.values().stream()
            .filter(ConstraintAccumulator::isForeignKey)
            .collect(Collectors.groupingBy(
                accumulator -> TableKey.of(accumulator.owner(), accumulator.tableName()),
                LinkedHashMap::new,
                Collectors.mapping(ConstraintAccumulator::toForeignKeyReport, Collectors.toList())));
        Map<TableKey, List<DatabaseIndexReport>> indexByTable = indexAccumulators.values().stream()
            .collect(Collectors.groupingBy(
                accumulator -> TableKey.of(accumulator.owner(), accumulator.tableName()),
                LinkedHashMap::new,
                Collectors.mapping(IndexAccumulator::toIndexReport, Collectors.toList())));

        LinkedHashMap<String, List<DatabaseTableReport>> tablesByOwner = new LinkedHashMap<>();
        tableBuilders.values().stream()
            .sorted(Comparator.comparing(TableBuilder::owner).thenComparing(TableBuilder::tableName))
            .forEach(tableBuilder -> {
                TableKey tableKey = TableKey.of(tableBuilder.owner(), tableBuilder.tableName());
                DatabaseTableReport tableReport = new DatabaseTableReport(
                    tableBuilder.owner(),
                    tableBuilder.tableName(),
                    tableBuilder.comment(),
                    tableBuilder.toColumnReports(),
                    uniqueByTable.getOrDefault(tableKey, List.of()),
                    foreignKeyByTable.getOrDefault(tableKey, List.of()),
                    indexByTable.getOrDefault(tableKey, List.of()));
                tablesByOwner.computeIfAbsent(tableBuilder.owner(), ignored -> new ArrayList<>()).add(tableReport);
            });

        List<DatabaseOwnerReport> owners = tablesByOwner.entrySet().stream()
            .map(entry -> new DatabaseOwnerReport(entry.getKey(), entry.getValue()))
            .toList();
        return new DatabaseSourceReport(target.labels(), target.url(), target.username(), owners);
    }
}

class TableBuilder {

    private final String owner;
    private final String tableName;
    private final String comment;
    private final LinkedHashMap<String, ColumnBuilder> columns = new LinkedHashMap<>();

    TableBuilder(String owner, String tableName, String comment) {
        this.owner = owner;
        this.tableName = tableName;
        this.comment = comment;
    }

    void addColumn(ColumnRow columnRow) {
        columns.computeIfAbsent(columnRow.columnName(), ignored -> new ColumnBuilder(columnRow)).merge(columnRow);
    }

    void markConstraintColumn(ConstraintRow constraintRow) {
        ColumnBuilder columnBuilder = columns.computeIfAbsent(
            constraintRow.columnName(),
            ignored -> new ColumnBuilder(new ColumnRow(
                constraintRow.owner(),
                constraintRow.tableName(),
                constraintRow.columnName(),
                Integer.MAX_VALUE,
                "UNKNOWN",
                null,
                null,
                null,
                true,
                null,
                null)));
        columnBuilder.markConstraint(constraintRow);
    }

    List<DatabaseColumnReport> toColumnReports() {
        return columns.values().stream()
            .sorted(Comparator.comparingInt(ColumnBuilder::columnOrder).thenComparing(ColumnBuilder::name))
            .map(ColumnBuilder::toReport)
            .toList();
    }

    String owner() {
        return owner;
    }

    String tableName() {
        return tableName;
    }

    String comment() {
        return comment;
    }
}

class ColumnBuilder {

    private final String name;
    private int columnOrder;
    private String type;
    private boolean nullable;
    private String defaultValue;
    private String comment;
    private boolean primaryKey;
    private final LinkedHashSet<String> keyConstraintNames = new LinkedHashSet<>();
    private final LinkedHashSet<String> foreignKeyNames = new LinkedHashSet<>();

    ColumnBuilder(ColumnRow row) {
        this.name = row.columnName();
        merge(row);
    }

    void merge(ColumnRow row) {
        this.columnOrder = row.columnId() == null ? Integer.MAX_VALUE : row.columnId();
        this.type = buildType(row);
        this.nullable = row.nullable();
        this.defaultValue = row.dataDefault();
        this.comment = row.comment();
    }

    void markConstraint(ConstraintRow row) {
        if ("P".equalsIgnoreCase(row.constraintType())) {
            primaryKey = true;
            keyConstraintNames.add(row.constraintName());
        } else if ("U".equalsIgnoreCase(row.constraintType())) {
            keyConstraintNames.add(row.constraintName());
        } else if ("R".equalsIgnoreCase(row.constraintType())) {
            foreignKeyNames.add(row.constraintName());
        }
    }

    DatabaseColumnReport toReport() {
        return new DatabaseColumnReport(
            name,
            type,
            nullable,
            defaultValue,
            comment,
            primaryKey,
            List.copyOf(keyConstraintNames),
            List.copyOf(foreignKeyNames));
    }

    int columnOrder() {
        return columnOrder;
    }

    String name() {
        return name;
    }

    private String buildType(ColumnRow row) {
        String baseType = row.dataType() == null ? "UNKNOWN" : row.dataType();
        if (row.dataPrecision() != null) {
            if (row.dataScale() != null && row.dataScale() > 0) {
                return baseType + "(" + row.dataPrecision() + ", " + row.dataScale() + ")";
            }
            return baseType + "(" + row.dataPrecision() + ")";
        }
        if (row.dataLength() != null && shouldShowLength(baseType)) {
            return baseType + "(" + row.dataLength() + ")";
        }
        return baseType;
    }

    private boolean shouldShowLength(String dataType) {
        String normalized = dataType.toUpperCase(Locale.ROOT);
        return !(normalized.contains("DATE")
            || normalized.contains("TIME")
            || normalized.contains("CLOB")
            || normalized.contains("BLOB")
            || normalized.contains("TEXT"));
    }
}

class ConstraintAccumulator {

    private final String constraintName;
    private final String constraintType;
    private final String referencedOwner;
    private final String referencedTable;
    private String owner;
    private String tableName;
    private final LinkedHashMap<Integer, String> columns = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, String> referencedColumns = new LinkedHashMap<>();

    ConstraintAccumulator(String constraintName, String constraintType, String referencedOwner, String referencedTable) {
        this.constraintName = constraintName;
        this.constraintType = constraintType;
        this.referencedOwner = referencedOwner;
        this.referencedTable = referencedTable;
    }

    void add(ConstraintRow row) {
        this.owner = row.owner();
        this.tableName = row.tableName();
        int position = row.position() == null ? Integer.MAX_VALUE : row.position();
        columns.put(position, row.columnName());
        if (row.referencedColumnName() != null) {
            referencedColumns.put(position, row.referencedColumnName());
        }
    }

    boolean isKeyConstraint() {
        return "P".equalsIgnoreCase(constraintType) || "U".equalsIgnoreCase(constraintType);
    }

    boolean isForeignKey() {
        return "R".equalsIgnoreCase(constraintType);
    }

    DatabaseUniqueConstraintReport toUniqueConstraintReport() {
        return new DatabaseUniqueConstraintReport(
            constraintName,
            "P".equalsIgnoreCase(constraintType),
            DatabaseDictionaryCollections.orderedValues(columns));
    }

    DatabaseForeignKeyReport toForeignKeyReport() {
        return new DatabaseForeignKeyReport(
            constraintName,
            DatabaseDictionaryCollections.orderedValues(columns),
            referencedOwner == null ? "-" : referencedOwner,
            referencedTable == null ? "-" : referencedTable,
            DatabaseDictionaryCollections.orderedValues(referencedColumns));
    }

    String owner() {
        return owner;
    }

    String tableName() {
        return tableName;
    }
}

class IndexAccumulator {

    private final String indexName;
    private final String uniqueness;
    private final String indexType;
    private String owner;
    private String tableName;
    private final LinkedHashMap<Integer, String> columns = new LinkedHashMap<>();

    IndexAccumulator(String indexName, String uniqueness, String indexType) {
        this.indexName = indexName;
        this.uniqueness = uniqueness;
        this.indexType = indexType;
    }

    void add(IndexRow row) {
        this.owner = row.owner();
        this.tableName = row.tableName();
        int position = row.columnPosition() == null ? Integer.MAX_VALUE : row.columnPosition();
        columns.put(position, row.columnName());
    }

    DatabaseIndexReport toIndexReport() {
        return new DatabaseIndexReport(
            indexName,
            uniqueness,
            indexType,
            DatabaseDictionaryCollections.orderedValues(columns));
    }

    String owner() {
        return owner;
    }

    String tableName() {
        return tableName;
    }
}

class DatabaseDictionaryException extends BaseException {

    DatabaseDictionaryException(ErrorCode errorCode, int httpStatus, String detailMessage) {
        super(errorCode, httpStatus, detailMessage);
    }

    static DatabaseDictionaryException invalidConfiguration(String message) {
        return new DatabaseDictionaryException(DatabaseDictionaryErrorCode.CONFIGURATION_INVALID, 400, message);
    }

    static DatabaseDictionaryException generationFailed(String message, Exception cause) {
        DatabaseDictionaryException exception = new DatabaseDictionaryException(
            DatabaseDictionaryErrorCode.GENERATION_FAILED,
            500,
            message);
        exception.initCause(cause);
        return exception;
    }

    static DatabaseDictionaryException outputWriteFailed(java.nio.file.Path output, Exception cause) {
        DatabaseDictionaryException exception = new DatabaseDictionaryException(
            DatabaseDictionaryErrorCode.OUTPUT_WRITE_FAILED,
            500,
            "Failed to write database dictionary HTML: " + output);
        exception.initCause(cause);
        return exception;
    }
}

enum DatabaseDictionaryErrorCode implements ErrorCode {
    CONFIGURATION_INVALID("DATABASE_DICTIONARY_CONFIGURATION_INVALID", "数据库字典数据源配置无效"),
    GENERATION_FAILED("DATABASE_DICTIONARY_GENERATION_FAILED", "数据库字典生成失败"),
    OUTPUT_WRITE_FAILED("DATABASE_DICTIONARY_OUTPUT_WRITE_FAILED", "数据库字典输出失败");

    private final String code;
    private final String message;

    DatabaseDictionaryErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}

final class DatabaseDictionaryCollections {

    private DatabaseDictionaryCollections() {
    }

    static List<String> orderedValues(Map<Integer, String> values) {
        return values.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .filter(Objects::nonNull)
            .toList();
    }
}
