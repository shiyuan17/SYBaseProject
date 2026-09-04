package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class V119__enforce_global_block_numbering extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        BlockRule rule = findBlockRule(connection);
        if (rule == null) {
            return;
        }

        forceGlobalScope(connection);
        String datePart = resolveDatePart(rule.datePattern());
        String periodKey = datePart.isBlank() ? "GLOBAL" : datePart;
        long currentMaximum = Math.max(
            findCounterMaximum(connection, rule.ruleCode(), periodKey),
            findPersistedBlockMaximum(connection, rule.prefixPattern(), datePart));
        upsertGlobalCounter(connection, rule.ruleCode(), periodKey, currentMaximum);
    }

    private BlockRule findBlockRule(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select rule_code, prefix_pattern, date_pattern
            from numbering_rules
            where biz_type = 'BLOCK_NO'
            """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new BlockRule(
                    resultSet.getString("rule_code"),
                    nullToEmpty(resultSet.getString("prefix_pattern")),
                    nullToEmpty(resultSet.getString("date_pattern")));
            }
        }
    }

    private void forceGlobalScope(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            update numbering_rules
            set scope_type = 'GLOBAL', updated_at = ?
            where biz_type = 'BLOCK_NO' and (scope_type is null or upper(scope_type) <> 'GLOBAL')
            """)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    private long findCounterMaximum(Connection connection, String ruleCode, String periodKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select current_value
            from numbering_counters
            where rule_code = ? and period_key = ?
            """)) {
            statement.setString(1, ruleCode);
            statement.setString(2, periodKey);
            long maximum = 0;
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    maximum = Math.max(maximum, resultSet.getLong("current_value"));
                }
            }
            return maximum;
        }
    }

    private long findPersistedBlockMaximum(Connection connection, String prefix, String datePart) throws SQLException {
        if (!hasSamplingBlocksTable(connection)) {
            return 0;
        }
        Pattern blockPattern = Pattern.compile("^" + Pattern.quote(prefix + datePart) + "(\\d+)$");
        long maximum = 0;
        try (PreparedStatement statement = connection.prepareStatement("""
            select block_code
            from sampling_blocks
            where block_code is not null
            """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Matcher matcher = blockPattern.matcher(resultSet.getString("block_code"));
                if (matcher.matches()) {
                    maximum = Math.max(maximum, Long.parseLong(matcher.group(1)));
                }
            }
        }
        return maximum;
    }

    private boolean hasSamplingBlocksTable(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return hasTable(metadata, "sampling_blocks") || hasTable(metadata, "SAMPLING_BLOCKS");
    }

    private boolean hasTable(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private void upsertGlobalCounter(Connection connection, String ruleCode, String periodKey, long value) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
            update numbering_counters
            set current_value = case when current_value < ? then ? else current_value end,
                version = version + 1, updated_at = ?
            where rule_code = ? and period_key = ? and scope_key = 'GLOBAL'
            """)) {
            update.setLong(1, value);
            update.setLong(2, value);
            update.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(4, ruleCode);
            update.setString(5, periodKey);
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into numbering_counters
                (id, rule_code, period_key, scope_key, current_value, version, updated_at)
            values (?, ?, ?, 'GLOBAL', ?, 0, ?)
            """)) {
            insert.setString(1, "NC_BLOCK_" + UUID.randomUUID().toString().replace("-", ""));
            insert.setString(2, ruleCode);
            insert.setString(3, periodKey);
            insert.setLong(4, value);
            insert.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private String resolveDatePart(String datePattern) {
        if (datePattern.isBlank()) {
            return "";
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(datePattern));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record BlockRule(String ruleCode, String prefixPattern, String datePattern) {
    }
}
