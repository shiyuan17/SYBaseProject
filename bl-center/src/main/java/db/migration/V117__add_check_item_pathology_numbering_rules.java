package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class V117__add_check_item_pathology_numbering_rules extends BaseJavaMigration {

    private static final String BIZ_PREFIX = "CHECK_ITEM_PATHOLOGY_NO:";
    private static final String MENU_ID = "MENU_CHECK_ITEM_RULES";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{(YYYY|YY|MM|DD|0:D(?:[1-9]|1[0-2]))}");
    private static final List<RuleSeed> RULES = List.of(
        dayRule("ROUTINE", "BL", 4),
        yearRule("CONSULTATION", "HZ", 5),
        yearRule("CYTOLOGY", "XB", 5),
        yearRule("CYTOLOGY_CONSULTATION", "XH", 5),
        yearRule("CYTOLOGY_SMEAR", "GP", 5),
        yearRule("DIFFICULT_CONSULTATION", "YN", 5),
        yearRule("ELECTRON_MICROSCOPY", "EM", 5),
        yearRule("FISH", "FISH", 5),
        dayRule("FROZEN", "BD", 4),
        yearRule("GENE_TEST", "JY", 5),
        yearRule("GYNECOLOGY_LBC_CYTOLOGY", "FY", 5),
        yearRule("GYNECOLOGY_LBC_DNA", "FD", 5),
        yearRule("GYNECOLOGY_LBC_HPV", "FH", 5),
        yearRule("HPV", "HPV", 5),
        yearRule("IHC", "IH", 5),
        yearRule("IMMUNE_FLUORESCENCE", "IF", 5),
        yearRule("LIVER_BIOPSY", "GC", 5),
        yearRule("MOLECULAR_PATHOLOGY", "FZ", 5),
        yearRule("NGS", "NGS", 5),
        yearRule("NON_GYNECOLOGY_LBC_CYTOLOGY", "NF", 5),
        yearRule("PUNCTURE_BIOPSY", "CC", 5),
        yearRule("RAPID", "KS", 5),
        yearRule("RESEARCH", "KY", 5),
        yearRule("SUPPLEMENTAL_REPORT", "MS", 5),
        yearRule("TECHNICAL_ORDER", "JS", 5)
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureColumn(connection, "NUMBERING_RULES", "FORMAT_TEMPLATE",
            "ALTER TABLE numbering_rules ADD COLUMN format_template VARCHAR(128)");
        ensureColumn(connection, "NUMBERING_RULES", "AUTO_INCREMENT",
            "ALTER TABLE numbering_rules ADD COLUMN \"AUTO_INCREMENT\" INTEGER DEFAULT 1");
        execute(connection, "UPDATE numbering_rules SET \"AUTO_INCREMENT\" = 1 WHERE \"AUTO_INCREMENT\" IS NULL");

        LocalDate today = LocalDate.now();
        for (RuleSeed rule : RULES) {
            ensureRule(connection, rule);
            long historicalMax = findHistoricalMax(connection, rule, today);
            if (historicalMax > 0) {
                upsertCounter(connection, rule.ruleCode(), periodKey(rule.period(), today), historicalMax);
            }
        }
        ensureMenu(connection);
        copyRoleMenus(connection);
    }

    private void ensureRule(Connection connection, RuleSeed rule) throws SQLException {
        if (exists(connection, "select 1 from numbering_rules where biz_type = ?", BIZ_PREFIX + rule.applicationType())) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
            insert into numbering_rules
                (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy,
                 scope_type, enabled, remarks, format_template, "AUTO_INCREMENT", created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, 'GLOBAL', 1, ?, ?, 1, ?, ?)
            """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setString(1, rule.id());
            statement.setString(2, rule.ruleCode());
            statement.setString(3, BIZ_PREFIX + rule.applicationType());
            statement.setString(4, rule.prefix());
            statement.setString(5, rule.datePattern());
            statement.setInt(6, rule.sequenceLength());
            statement.setString(7, rule.resetPolicy());
            statement.setString(8, "检查项病理号规则：" + rule.applicationType());
            statement.setString(9, rule.template());
            statement.setTimestamp(10, Timestamp.valueOf(now));
            statement.setTimestamp(11, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    private long findHistoricalMax(Connection connection, RuleSeed rule, LocalDate today) throws SQLException {
        Pattern pattern = currentPeriodPattern(rule.template(), today);
        long maximum = 0;
        try (PreparedStatement statement = connection.prepareStatement("""
            select pathology_no
            from pathology_cases
            where pathology_no is not null
            """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Matcher matcher = pattern.matcher(resultSet.getString(1));
                    if (matcher.matches()) {
                        maximum = Math.max(maximum, Long.parseLong(matcher.group(1)));
                    }
                }
            }
        }
        return maximum;
    }

    private Pattern currentPeriodPattern(String template, LocalDate date) {
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        StringBuilder regex = new StringBuilder("^");
        int cursor = 0;
        while (matcher.find()) {
            regex.append(Pattern.quote(template.substring(cursor, matcher.start())));
            String token = matcher.group(1);
            regex.append(switch (token) {
                case "YYYY" -> date.format(DateTimeFormatter.ofPattern("yyyy"));
                case "YY" -> date.format(DateTimeFormatter.ofPattern("yy"));
                case "MM" -> date.format(DateTimeFormatter.ofPattern("MM"));
                case "DD" -> date.format(DateTimeFormatter.ofPattern("dd"));
                default -> "(\\d{" + token.substring(3) + "})";
            });
            cursor = matcher.end();
        }
        regex.append(Pattern.quote(template.substring(cursor))).append('$');
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }

    private void upsertCounter(Connection connection, String ruleCode, String periodKey, long value) throws SQLException {
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
            insert.setString(1, "NC_CHECK_ITEM_" + UUID.randomUUID().toString().replace("-", ""));
            insert.setString(2, ruleCode);
            insert.setString(3, periodKey);
            insert.setLong(4, value);
            insert.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
    }

    private void ensureMenu(Connection connection) throws SQLException {
        String parentId = exists(connection, "select 1 from menus where id = ?", "MENU_SYSTEM")
            ? "MENU_SYSTEM" : "MENU_SYS_ROOT";
        if (exists(connection, "select 1 from menus where id = ?", MENU_ID)) {
            try (PreparedStatement update = connection.prepareStatement("""
                update menus
                set parent_id = ?, menu_code = 'CHECK_ITEM_RULES', menu_name = '检查项规则',
                    menu_type = 'MENU', path = '/system/check-item-rules', component_name = 'CheckItemRules',
                    permission_prefix = 'support:numbering', sort_order = 105, visible = 1, enabled = 1,
                    updated_at = ?
                where id = ?
                """)) {
                update.setString(1, parentId);
                update.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                update.setString(3, MENU_ID);
                update.executeUpdate();
            }
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
            insert into menus
                (id, parent_id, menu_code, menu_name, menu_type, path, component_name, permission_prefix,
                 sort_order, visible, enabled, created_at, updated_at)
            values (?, ?, 'CHECK_ITEM_RULES', '检查项规则', 'MENU', '/system/check-item-rules',
                    'CheckItemRules', 'support:numbering', 105, 1, 1, ?, ?)
            """)) {
            LocalDateTime now = LocalDateTime.now();
            insert.setString(1, MENU_ID);
            insert.setString(2, parentId);
            insert.setTimestamp(3, Timestamp.valueOf(now));
            insert.setTimestamp(4, Timestamp.valueOf(now));
            insert.executeUpdate();
        }
    }

    private void copyRoleMenus(Connection connection) throws SQLException {
        try (PreparedStatement roles = connection.prepareStatement(
            "select role_id from role_menus where menu_id = 'MENU_NUMBERING'");
             ResultSet resultSet = roles.executeQuery()) {
            while (resultSet.next()) {
                String roleId = resultSet.getString(1);
                if (existsRoleMenu(connection, roleId)) {
                    continue;
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                    insert into role_menus (id, role_id, menu_id, assigned_at)
                    values (?, ?, ?, ?)
                    """)) {
                    insert.setString(1, "RM_CHECK_ITEM_" + UUID.randomUUID().toString().replace("-", ""));
                    insert.setString(2, roleId);
                    insert.setString(3, MENU_ID);
                    insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    insert.executeUpdate();
                }
            }
        }
    }

    private boolean existsRoleMenu(Connection connection, String roleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "select 1 from role_menus where role_id = ? and menu_id = ?")) {
            statement.setString(1, roleId);
            statement.setString(2, MENU_ID);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean exists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, ddl);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        for (String candidate : List.of(tableName, tableName.toLowerCase())) {
            try (ResultSet columns = metadata.getColumns(null, null, candidate, null)) {
                while (columns.next()) {
                    if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String periodKey(String period, LocalDate date) {
        return switch (period) {
            case "YEAR" -> date.format(DateTimeFormatter.ofPattern("yyyy"));
            case "MONTH" -> date.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "DAY" -> date.format(DateTimeFormatter.BASIC_ISO_DATE);
            default -> "GLOBAL";
        };
    }

    private static RuleSeed yearRule(String type, String prefix, int length) {
        return new RuleSeed(type, prefix, prefix + "{YY}{0:D" + length + "}", "YEAR", "yy", "YEARLY", length);
    }

    private static RuleSeed dayRule(String type, String prefix, int length) {
        return new RuleSeed(type, prefix, prefix + "{YYYY}{MM}{DD}{0:D" + length + "}", "DAY", "yyyyMMdd", "DAILY", length);
    }

    private record RuleSeed(
        String applicationType,
        String prefix,
        String template,
        String period,
        String datePattern,
        String resetPolicy,
        int sequenceLength
    ) {
        String id() {
            return "NR_CHECK_ITEM_" + applicationType;
        }

        String ruleCode() {
            return "RULE_CHECK_ITEM_" + applicationType;
        }
    }
}
