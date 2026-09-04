package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class V120__repair_block_numbering_template extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Rule rule = findRule(connection);
        if (rule == null) {
            return;
        }

        String prefix = blank(rule.prefix()) ? "BK" : rule.prefix().trim();
        String datePattern = blank(rule.datePattern()) ? "yyyyMMdd" : rule.datePattern().trim();
        int seqLength = rule.seqLength() < 1 ? 3 : rule.seqLength();
        updateRule(connection, prefix, datePattern, seqLength);

        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern(datePattern));
        String periodKey = datePart.isBlank() ? "GLOBAL" : datePart;
        long maximum = findPersistedMaximum(connection, prefix + datePart);
        upsertGlobalCounter(connection, rule.ruleCode(), periodKey, maximum);
    }

    private Rule findRule(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "select rule_code, prefix_pattern, date_pattern, seq_length from numbering_rules where biz_type = 'BLOCK_NO'")) {
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                    ? new Rule(result.getString(1), result.getString(2), result.getString(3), result.getInt(4))
                    : null;
            }
        }
    }

    private void updateRule(Connection connection, String prefix, String datePattern, int seqLength) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
            update numbering_rules
            set prefix_pattern = ?, date_pattern = ?, seq_length = ?, updated_at = ?
            where biz_type = 'BLOCK_NO'
            """)) {
            statement.setString(1, prefix);
            statement.setString(2, datePattern);
            statement.setInt(3, seqLength);
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    private long findPersistedMaximum(Connection connection, String prefixDate) throws Exception {
        if (!hasSamplingBlocksTable(connection)) {
            return 0;
        }
        Pattern pattern = Pattern.compile("^" + Pattern.quote(prefixDate) + "(\\d+)$");
        long maximum = 0;
        try (PreparedStatement statement = connection.prepareStatement(
            "select block_code from sampling_blocks where block_code is not null")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Matcher matcher = pattern.matcher(result.getString(1));
                    if (matcher.matches()) {
                        maximum = Math.max(maximum, Long.parseLong(matcher.group(1)));
                    }
                }
            }
        }
        return maximum;
    }

    private boolean hasSamplingBlocksTable(Connection connection) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        return hasTable(metadata, "sampling_blocks") || hasTable(metadata, "SAMPLING_BLOCKS");
    }

    private boolean hasTable(DatabaseMetaData metadata, String tableName) throws Exception {
        try (ResultSet tables = metadata.getTables(null, null, tableName, new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private void upsertGlobalCounter(Connection connection, String ruleCode, String periodKey, long value) throws Exception {
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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record Rule(String ruleCode, String prefix, String datePattern, int seqLength) {
    }
}
