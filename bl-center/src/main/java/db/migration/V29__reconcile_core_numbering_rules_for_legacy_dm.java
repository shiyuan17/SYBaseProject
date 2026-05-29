package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class V29__reconcile_core_numbering_rules_for_legacy_dm extends BaseJavaMigration {

    private static final List<NumberingRuleSeed> RULES = List.of(
        new NumberingRuleSeed("NR_APPLICATION", "RULE_APPLICATION_NO", "APPLICATION_NO", "AP", "yyyyMMdd", 4, "DAILY", "GLOBAL", "Application number"),
        new NumberingRuleSeed("NR_PATHOLOGY", "RULE_PATHOLOGY_NO", "PATHOLOGY_NO", "BL", "yyyyMMdd", 4, "DAILY", "GLOBAL", "Pathology number"),
        new NumberingRuleSeed("NR_SPECIMEN", "RULE_SPECIMEN_NO", "SPECIMEN_NO", "SP", "yyyyMMdd", 5, "DAILY", "GLOBAL", "Specimen number"),
        new NumberingRuleSeed("NR_BLOCK", "RULE_BLOCK_NO", "BLOCK_NO", "BK", "yyyyMMdd", 3, "DAILY", "GLOBAL", "Block number"),
        new NumberingRuleSeed("NR_SLIDE", "RULE_SLIDE_NO", "SLIDE_NO", "SL", "yyyyMMdd", 3, "DAILY", "GLOBAL", "Slide number"),
        new NumberingRuleSeed("NR_TRANSPORT", "RULE_TRANSPORT_ORDER_NO", "TRANSPORT_ORDER_NO", "TR", "yyyyMMdd", 4, "DAILY", "GLOBAL", "Transport order number"),
        new NumberingRuleSeed("NR_REPORT", "RULE_REPORT_NO", "REPORT_NO", "RP", "yyyyMMdd", 4, "DAILY", "GLOBAL", "Report number")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (NumberingRuleSeed rule : RULES) {
            ensureRule(connection, rule);
        }
    }

    private void ensureRule(Connection connection, NumberingRuleSeed rule) throws SQLException {
        if (exists(connection, rule.bizType())) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
            insert into numbering_rules
                (id, rule_code, biz_type, prefix_pattern, date_pattern, seq_length, reset_policy, scope_type, enabled, remarks, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
            """)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            statement.setString(1, rule.id());
            statement.setString(2, rule.ruleCode());
            statement.setString(3, rule.bizType());
            statement.setString(4, rule.prefixPattern());
            statement.setString(5, rule.datePattern());
            statement.setInt(6, rule.seqLength());
            statement.setString(7, rule.resetPolicy());
            statement.setString(8, rule.scopeType());
            statement.setString(9, rule.remarks());
            statement.setTimestamp(10, now);
            statement.setTimestamp(11, now);
            statement.executeUpdate();
        }
    }

    private boolean exists(Connection connection, String bizType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            select 1
            from numbering_rules
            where biz_type = ?
            """)) {
            statement.setString(1, bizType);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private record NumberingRuleSeed(
        String id,
        String ruleCode,
        String bizType,
        String prefixPattern,
        String datePattern,
        int seqLength,
        String resetPolicy,
        String scopeType,
        String remarks
    ) {
    }
}
